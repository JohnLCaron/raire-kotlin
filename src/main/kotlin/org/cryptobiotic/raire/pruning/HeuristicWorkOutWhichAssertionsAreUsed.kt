/*
  Copyright 2023 Democracy Developers
  This is a Java re-implementation of raire-rs https://github.com/DemocracyDevelopers/raire-rs
  It attempts to copy the design, API, and naming as much as possible subject to being idiomatic and efficient Java.

  This file is part of raire-java.
  raire-java is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
  raire-java is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details.
  You should have received a copy of the GNU Affero General Public License along with ConcreteSTV.  If not, see <https://www.gnu.org/licenses/>.

 */
package org.cryptobiotic.raire.pruning

import org.cryptobiotic.raire.RaireError
import org.cryptobiotic.raire.RaireException
import org.cryptobiotic.raire.assertions.Assertion
import org.cryptobiotic.raire.assertions.AssertionAndDifficulty
import org.cryptobiotic.raire.assertions.NotEliminatedBefore
import org.cryptobiotic.raire.assertions.NotEliminatedNext
import org.cryptobiotic.raire.time.TimeOut
import java.util.*
import java.util.function.Function
import java.util.function.IntFunction
import java.util.function.Supplier
import java.util.stream.Collectors
import java.util.stream.IntStream

/**
 * A pretty simple method of computing which assertions are used which may not always
 * be optimal, but is fast, and, in practice, has turned out to be optimal for every case
 * we tried it on.
 *
 * The general problem can be converted to a problem of selection at least one of a combination
 * of expressions. The heuristic is a first pass choosing ones where there is no choice, and
 * a second pass of choosing arbitrarily amongst the remaining ones where prior choices have
 * not solved it.
 */
class HeuristicWorkOutWhichAssertionsAreUsed {
    private val assertions_used = BitSet()

    private fun uses(index: Int): Boolean {
        return assertions_used[index]
    }

    /** Some (most) nodes have exactly one assertion. Assign these assertions, as they MUST be used.  */
    private fun add_tree_forced(node: TreeNodeShowingWhatAssertionsPrunedIt) {
        if (node.pruning_assertions.size > 0) {
            if (node.children.size == 0 && node.pruning_assertions.size == 1) { // must be used
                assertions_used.set(node.pruning_assertions[0])
            }
        } else {
            for (child in node.children) add_tree_forced(child)
        }
    }

    /** See if a node is already eliminated by the assertions marked as being used.  */
    private fun node_already_eliminated(node: TreeNodeShowingWhatAssertionsPrunedIt): Boolean {
        if (Arrays.stream(node.pruning_assertions)
                .anyMatch { index: Int -> this.uses(index) }
        ) return true // one of the assertions eliminates the node.

        // now check to see if all the children are eliminated.
        return node.children.size != 0 && Arrays.stream(node.children)
            .allMatch { subnode: TreeNodeShowingWhatAssertionsPrunedIt -> this.node_already_eliminated( subnode) } // LOOK changed
    }

    private fun add_tree_second_pass(node: TreeNodeShowingWhatAssertionsPrunedIt, timeout: TimeOut) {
        if (timeout.quickCheckTimeout()) throw RaireException(RaireError.TimeoutTrimmingAssertions())
        if (node.pruning_assertions.size > 0) {
            if (!node_already_eliminated(node)) {
                assertions_used.set(node.pruning_assertions[0])
            }
        } else {
            for (child in node.children) add_tree_second_pass(child, timeout)
        }
    }

    companion object {
        /** Sort the assertions in a human sensible manner, and then trim them.
         *
         * Note that if a timeout error is produced, the assertions array will be sorted but otherwise unchanged
         * from the original call.
         *
         * The algorithm is described in [AssertionTrimmingAlgorithm.md](https://github.com/DemocracyDevelopers/raire-rs/blob/main/raire/AssertionTrimmingAlgorithm.md)
         */
        fun order_assertions_and_remove_unnecessary(
            assertions: MutableList<AssertionAndDifficulty>,
            winner: Int,
            num_candidates: Int,
            trim_algorithm: TrimAlgorithm?,
            timeout: TimeOut
        ) {
            assertions.sortWith(CompareAAD())

            val consider_children_of_eliminated_nodes =
                when (trim_algorithm) {
                    TrimAlgorithm.MinimizeTree -> HowFarToContinueSearchTreeWhenPruningAssertionFound.StopImmediately
                    TrimAlgorithm.MinimizeAssertions -> HowFarToContinueSearchTreeWhenPruningAssertionFound.StopOnNEB
                    else -> return // LOOK changed
                }

            // do the actual trimming
            val all_assertions: Array<Assertion> = Array(assertions.size) { assertions[it].assertion }

            val all_assertion_indices = IntStream.range(0, all_assertions.size).boxed().collect(
                Collectors.toCollection(
                    Supplier { ArrayList() })
            ) // 0 to all_assertions.length

            val find_used = HeuristicWorkOutWhichAssertionsAreUsed()
            val trees = mutableListOf<TreeNodeShowingWhatAssertionsPrunedIt>()
            for (candidate in 0 until num_candidates) { // create trees and do first pass
                if (candidate != winner) {
                    val tree = TreeNodeShowingWhatAssertionsPrunedIt(
                        IntArray(0),
                        candidate,
                        all_assertion_indices,
                        all_assertions,
                        num_candidates,
                        consider_children_of_eliminated_nodes,
                        timeout
                    )
                    if (tree.valid) throw RaireException(RaireError.InternalErrorDidntRuleOutLoser())
                    find_used.add_tree_forced(tree)
                    trees.add(tree)
                }
            }

            // do second pass
            for (tree in trees) find_used.add_tree_second_pass(tree, timeout)
            val copy: Array<AssertionAndDifficulty> = Array(assertions.size) { assertions[it] }
            assertions.clear()
            for (i in copy.indices) {
                if (find_used.uses(i)) assertions.add(copy[i])
            }
        }
    }
}

// sort all NEBs before NENs,
// sort NENs by length
// ties - sort by winner, then loser, then continuing
class CompareAAD : Comparator<AssertionAndDifficulty> {

    override fun compare(o1: AssertionAndDifficulty, o2: AssertionAndDifficulty): Int {
        if (o1.assertion is NotEliminatedBefore) {
            if (o2.assertion is NotEliminatedBefore) {
                val neb1: NotEliminatedBefore = o1.assertion as NotEliminatedBefore
                val neb2: NotEliminatedBefore = o2.assertion as NotEliminatedBefore
                val d1: Int = neb1.winner - neb2.winner
                return if (d1 != 0) d1 else (neb1.loser - neb2.loser)
            } else return -1 // o1 is NEB, o2 is NEN, o1<o2.
        } else {
            if (o2.assertion is NotEliminatedNext) {
                val neb1: NotEliminatedNext = o1.assertion as NotEliminatedNext
                val neb2: NotEliminatedNext = o2.assertion as NotEliminatedNext
                val d0: Int = neb1.continuing.size - neb2.continuing.size
                if (d0 != 0) return d0
                val d1: Int = neb1.winner - neb2.winner
                if (d1 != 0) return d1
                val d2: Int = neb1.loser - neb2.loser
                if (d2 != 0) return d2
                return Arrays.compare(neb1.continuing, neb2.continuing)
            } else return 1 // o1 is NEN, o2 is NEB, o1>o2.
        }
    }
}
