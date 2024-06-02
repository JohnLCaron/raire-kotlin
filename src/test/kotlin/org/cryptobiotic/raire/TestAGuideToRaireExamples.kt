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

import org.cryptobiotic.raire.algorithm.RaireResult
import org.cryptobiotic.raire.assertions.NotEliminatedBefore
import org.cryptobiotic.raire.audittype.BallotComparisonOneOnDilutedMargin
import org.cryptobiotic.raire.irv.IRVResult
import org.cryptobiotic.raire.irv.Vote
import org.cryptobiotic.raire.irv.Votes
import org.cryptobiotic.raire.pruning.TrimAlgorithm
import org.cryptobiotic.raire.time.TimeOut
import org.junit.jupiter.api.Assertions.assertArrayEquals
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestAGuideToRaireExamples {

    val votes: Votes
        /** Get the votes in Example 10 (at the time of writing), used in examples in chapter 6, "Using RAIRE to generate assertions".  */
        get() {
            val votes: Array<Vote> = arrayOf<Vote>(
                Vote(5000, intArrayOf(C, B, A)),
                Vote(1000, intArrayOf(B, C, D)),
                Vote(1500, intArrayOf(D, A)),
                Vote(4000, intArrayOf(A, D)),
                Vote(2000, intArrayOf(D)),
            )
            return Votes(votes, 4)
        }

    // Test the get_votes() function and the methods on the Votes object.
    @Test
    fun testVotesStructure() {
        val votes: Votes = votes
        assertEquals(AUDIT.total_auditable_ballots, votes.totalVotes())
        assertEquals(4000, votes.firstPreferenceOnlyTally(A))
        assertEquals(1000, votes.firstPreferenceOnlyTally(B))
        assertEquals(5000, votes.firstPreferenceOnlyTally(C))
        assertEquals(3500, votes.firstPreferenceOnlyTally(D))
        assertArrayEquals(intArrayOf(4000, 6000, 3500), votes.restrictedTallies(intArrayOf(A, C, D)))
        assertArrayEquals(intArrayOf(5500, 6000), votes.restrictedTallies(intArrayOf(A, C)))
        val result: IRVResult = votes.runElection(TimeOut.never())
        assertArrayEquals(intArrayOf(C), result.possibleWinners)
        assertArrayEquals(intArrayOf(B, D, A, C), result.eliminationOrder)
    }

    fun testNEB(winner: Int, loser: Int): Double {
        val assertion = NotEliminatedBefore(winner, loser)
        return assertion.difficulty(votes, AUDIT).difficulty
    }

    /** Check NEB assertions in table 6.1 showing that A, B and C cannot be the last candidate standing.  */
    @Test
    fun test_neb_assertions() {
        assertTrue(java.lang.Double.isInfinite(testNEB(B, A)))
        assertTrue(java.lang.Double.isInfinite(testNEB(C, A)))
        assertTrue(java.lang.Double.isInfinite(testNEB(D, A)))
        assertTrue(java.lang.Double.isInfinite(testNEB(A, B)))
        assertEquals(3.375, testNEB(C, B), 0.001)
        assertTrue(java.lang.Double.isInfinite(testNEB(D, B)))
        assertTrue(java.lang.Double.isInfinite(testNEB(A, D)))
        assertTrue(java.lang.Double.isInfinite(testNEB(B, D)))
        assertTrue(java.lang.Double.isInfinite(testNEB(C, D)))
    }

    /// Test RAIRE
    @Test
    fun test_raire() {
        val votes: Votes = votes
        val minAssertions = RaireResult(votes, C, AUDIT, TrimAlgorithm.MinimizeAssertions, TimeOut.never())
        assertEquals(27.0, minAssertions.difficulty, 1e-6)
        assertEquals(5, minAssertions.assertionAndDifficulties.size)
        val minTree: RaireResult = RaireResult(votes, C, AUDIT, TrimAlgorithm.MinimizeTree, TimeOut.never())
        assertEquals(27.0, minTree.difficulty, 1e-6)
        assertEquals(6, minTree.assertionAndDifficulties.size)
    }

    companion object {
        const val A: Int = 0 // Alice
        const val B: Int = 1 // Bob
        const val C: Int = 2 // Chuan
        const val D: Int = 3 // Diego

        val AUDIT: BallotComparisonOneOnDilutedMargin = BallotComparisonOneOnDilutedMargin(13500)
    }
}
