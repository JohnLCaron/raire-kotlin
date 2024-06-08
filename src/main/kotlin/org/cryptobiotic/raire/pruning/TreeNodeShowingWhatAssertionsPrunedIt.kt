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
import org.cryptobiotic.raire.assertions.EffectOfAssertionOnEliminationOrderSuffix
import org.cryptobiotic.raire.time.TimeOut
import org.cryptobiotic.raire.util.toArray
import org.cryptobiotic.raire.util.toIntArray
import java.util.*

/** Produce a tree of reverse-elimination-order descending down until either
 * * At least one assertion prunes all subsequent orders
 * * No assertions prune any subsequent order
 *
 * One can optionally ask for an extended, which extends beyond pruned nodes if it is possible
 * for their children to be pruned. See HowFarToContinueSearchTreeWhenPruningAssertionFound for details.
 * This is useful for finding redundant assertions
 * that can be removed, at the cost of making the frontier larger.
 */

class TreeNodeShowingWhatAssertionsPrunedIt(
    parent_elimination_order_suffix: IntArray, // The candidate eliminated at this step.
    val candidate_being_eliminated_at_this_node: Int,
    relevant_assertions: ArrayList<Int>,
    all_assertions: Array<Assertion>,
    num_candidates: Int,
    consider_children_of_eliminated_nodes: HowFarToContinueSearchTreeWhenPruningAssertionFound,
    timeout: TimeOut
) {
    var pruning_assertions: IntArray // if any assertions prune it, their index in the main assertion list.
    var children: Array<TreeNodeShowingWhatAssertionsPrunedIt> // its children, if any.
    var valid: Boolean // true if this node or a child thereof is not eliminated by any assertion.

    /**
     * Create a new tree node with a given path back to the root and candidate being eliminated.
     */
    init {
        if (timeout.quickCheckTimeout()) throw RaireException(RaireError.TimeoutTrimmingAssertions())
        val elimination_order_suffix =
            IntArray(parent_elimination_order_suffix.size + 1) // candidate_being_eliminated_at_this_node prepended to parent_elimination_order_suffix
        elimination_order_suffix[0] = candidate_being_eliminated_at_this_node
        System.arraycopy(
            parent_elimination_order_suffix,
            0,
            elimination_order_suffix,
            1,
            parent_elimination_order_suffix.size
        )
        val pruning_assertions = mutableListOf<Int>()
        val still_relevant_assertions = ArrayList<Int>()
        for (assertion_index in relevant_assertions) {
            when (all_assertions[assertion_index].okEliminationOrderSuffix(elimination_order_suffix)) {
                EffectOfAssertionOnEliminationOrderSuffix.Contradiction -> pruning_assertions.add(assertion_index)
                EffectOfAssertionOnEliminationOrderSuffix.Ok -> {}
                EffectOfAssertionOnEliminationOrderSuffix.NeedsMoreDetail -> still_relevant_assertions.add(assertion_index)
            }
        }
        val children = mutableListOf<TreeNodeShowingWhatAssertionsPrunedIt>()
        var valid = pruning_assertions.isEmpty() && still_relevant_assertions.isEmpty()
        val pruned_by_neb = pruning_assertions.stream().anyMatch { a: Int? -> all_assertions[a!!].isNEB }
        if ((pruning_assertions.isEmpty() || consider_children_of_eliminated_nodes.should_continue_if_pruning_assertion_found(
                pruned_by_neb
            )) && !still_relevant_assertions.isEmpty()
        ) {
            val next_consider_children_of_eliminated_nodes =
                if (pruning_assertions.isEmpty()) consider_children_of_eliminated_nodes else consider_children_of_eliminated_nodes.next_level_if_pruning_assertion_found()
            for (candidate in 0 until num_candidates) {
                val finalCandidate = candidate
                if (Arrays.stream(elimination_order_suffix)
                        .noneMatch { c: Int -> c == finalCandidate }
                ) { // candidate has not already been eliminated.
                    val child = TreeNodeShowingWhatAssertionsPrunedIt(
                        elimination_order_suffix,
                        candidate,
                        still_relevant_assertions,
                        all_assertions,
                        num_candidates,
                        next_consider_children_of_eliminated_nodes,
                        timeout
                    )
                    if (child.valid) {
                        if (pruning_assertions.isEmpty()) valid = true
                        else { // we were continuing searching beyond a pruned branch. There is no point doing this.
                            children.clear()
                            break
                        }
                    }
                    children.add(child)
                }
            }
        }
        this.valid = valid
        this.children = toArray(children)
        this.pruning_assertions = toIntArray(pruning_assertions)
    }
}
