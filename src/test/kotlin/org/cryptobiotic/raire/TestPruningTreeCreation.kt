/*
  Copyright 2023 Democracy Developers
  This is a Java re-implementation of raire-rs https://github.com/DemocracyDevelopers/raire-rs
  It attempts to copy the design, API, and naming as much as possible subject to being idiomatic and efficient Java.

  This file is part of raire-java.
  raire-java is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
  raire-java is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details.
  You should have received a copy of the GNU Affero General Public License along with ConcreteSTV.  If not, see <https://www.gnu.org/licenses/>.

 */
package org.cryptobiotic.raire

import org.cryptobiotic.raire.assertions.Assertion
import org.cryptobiotic.raire.assertions.NotEliminatedBefore
import org.cryptobiotic.raire.assertions.NotEliminatedNext
import org.cryptobiotic.raire.pruning.HowFarToContinueSearchTreeWhenPruningAssertionFound
import org.cryptobiotic.raire.pruning.TreeNodeShowingWhatAssertionsPrunedIt
import org.cryptobiotic.raire.time.TimeOut
import org.junit.jupiter.api.Assertions.assertArrayEquals
import kotlin.test.*

/**
 * This tests the HowFarToContinueSearchTreeWhenPruningAssertionFound creation.
 *
 * This matches the tests in raire-rs in src/tree_showing_what_assertions_pruned_leaves.rs
 */
class TestPruningTreeCreation {
    /// Get the assertions listed in "A guide to RAIRE".
    fun raire_guide_assertions(): Array<Assertion> {
        return arrayOf<Assertion>(
            NotEliminatedNext(0, 1, intArrayOf(0, 1, 2, 3)),
            NotEliminatedNext(0, 3, intArrayOf(0, 2, 3)),
            NotEliminatedNext(2, 0, intArrayOf(0, 2)),
            NotEliminatedNext(2, 3, intArrayOf(0, 2, 3)),
            NotEliminatedBefore(2, 1),
            NotEliminatedNext(0, 3, intArrayOf(0, 3)),
        )
    }

    @Test
    @Throws(RaireException::class)
    fun tree_creation_correct() {
        val all_assertions: Array<Assertion> = raire_guide_assertions()
        val relevant_assertions = java.util.stream.IntStream.range(0, all_assertions.size).boxed().collect(
            java.util.stream.Collectors.toCollection(
                java.util.function.Supplier { ArrayList() })
        ) // 0 to all_assertions.length
        val timeout: TimeOut = TimeOut(1000L, null)
        val timeout_instantly: TimeOut = TimeOut(1L, null)
        assertFailsWith<RaireException>{ // check timeout instantly actually happens
            TreeNodeShowingWhatAssertionsPrunedIt(
                IntArray(0),
                0,
                relevant_assertions,
                all_assertions,
                4,
                HowFarToContinueSearchTreeWhenPruningAssertionFound.StopImmediately,
                timeout_instantly
            )
        }
        val tree0: TreeNodeShowingWhatAssertionsPrunedIt = TreeNodeShowingWhatAssertionsPrunedIt(
            IntArray(0),
            0,
            relevant_assertions,
            all_assertions,
            4,
            HowFarToContinueSearchTreeWhenPruningAssertionFound.StopImmediately,
            timeout
        )
        val tree1: TreeNodeShowingWhatAssertionsPrunedIt = TreeNodeShowingWhatAssertionsPrunedIt(
            IntArray(0),
            1,
            relevant_assertions,
            all_assertions,
            4,
            HowFarToContinueSearchTreeWhenPruningAssertionFound.StopImmediately,
            timeout
        )
        val tree2: TreeNodeShowingWhatAssertionsPrunedIt = TreeNodeShowingWhatAssertionsPrunedIt(
            IntArray(0),
            2,
            relevant_assertions,
            all_assertions,
            4,
            HowFarToContinueSearchTreeWhenPruningAssertionFound.StopImmediately,
            timeout
        )
        val tree3: TreeNodeShowingWhatAssertionsPrunedIt = TreeNodeShowingWhatAssertionsPrunedIt(
            IntArray(0),
            3,
            relevant_assertions,
            all_assertions,
            4,
            HowFarToContinueSearchTreeWhenPruningAssertionFound.StopImmediately,
            timeout
        )
        // check tree0 (candidate 0 elimination)
        assertFalse(tree0.valid)
        assertEquals(3, tree0.children.size)
        assertArrayEquals(intArrayOf(4), tree0.children.get(0).pruning_assertions)
        assertArrayEquals(intArrayOf(2), tree0.children.get(1).pruning_assertions)
        assertArrayEquals(intArrayOf(), tree0.children.get(2).pruning_assertions)
        assertEquals(2, tree0.children.get(2).children.size)
        assertArrayEquals(intArrayOf(4), tree0.children.get(2).children.get(0).pruning_assertions)
        assertArrayEquals(intArrayOf(3), tree0.children.get(2).children.get(1).pruning_assertions)
        // check tree1
        assertFalse(tree1.valid)
        assertArrayEquals(intArrayOf(4), tree1.pruning_assertions)
        // check tree2
        assertTrue(tree2.valid) // candidate 2 won.
        // check tree3
        assertFalse(tree3.valid)
        assertEquals(3, tree3.children.size)
        assertArrayEquals(intArrayOf(5), tree3.children.get(0).pruning_assertions)
        assertArrayEquals(intArrayOf(4), tree3.children.get(1).pruning_assertions)
        assertArrayEquals(intArrayOf(), tree3.children.get(2).pruning_assertions)
        assertEquals(2, tree3.children.get(2).children.size)
        assertArrayEquals(intArrayOf(1), tree3.children.get(2).children.get(0).pruning_assertions)
        assertArrayEquals(intArrayOf(), tree3.children.get(2).children.get(1).pruning_assertions)
        assertEquals(1, tree3.children.get(2).children.get(1).children.size)
        assertArrayEquals(intArrayOf(0), tree3.children.get(2).children.get(1).children.get(0).pruning_assertions)
    }
}
