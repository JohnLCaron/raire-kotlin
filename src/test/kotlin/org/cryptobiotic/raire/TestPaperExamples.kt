/*
  Copyright 2023 Democracy Developers
  This is a Java re-implementation of raire-rs https://github.com/DemocracyDevelopers/raire-rs
  It attempts to copy the design, API, and naming as much as possible subject to being idiomatic and efficient Java.

  This file is part of raire-java.
  raire-java is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
  raire-java is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details.
  You should have received a copy of the GNU Affero General Public License along with ConcreteSTV.  If not, see <https://www.gnu.org/licenses/>.

 */
// Test the examples given in https://arxiv.org/pdf/1903.08804.pdf
package org.cryptobiotic.raire

import org.cryptobiotic.raire.algorithm.RaireResult
import org.cryptobiotic.raire.assertions.NotEliminatedBefore
import org.cryptobiotic.raire.assertions.NotEliminatedNext
import org.cryptobiotic.raire.audittype.BallotComparisonMACRO
import org.cryptobiotic.raire.audittype.BallotPollingBRAVO
import org.cryptobiotic.raire.irv.IRVResult
import org.cryptobiotic.raire.irv.Vote
import org.cryptobiotic.raire.irv.Votes
import org.cryptobiotic.raire.pruning.TrimAlgorithm
import org.cryptobiotic.raire.time.TimeOut
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.jupiter.api.Assertions.assertArrayEquals

class TestPaperExamples {
    val votesInTable1: Votes
        /// Get the votes in table 1.
        get() {
            val c1 = 0
            val c2 = 1
            val c3 = 2
            val c4 = 3
            val votes: Array<Vote> = arrayOf<Vote>(
                Vote(4000, intArrayOf(c2, c3)),
                Vote(20000, intArrayOf(c1)),
                Vote(9000, intArrayOf(c3, c4)),
                Vote(6000, intArrayOf(c2, c3, c4)),
                Vote(15000, intArrayOf(c4, c1, c2)),
                Vote(6000, intArrayOf(c1, c3)),
            )
            return Votes(votes, 4)
        }

    val votesInExample9: Votes
        /// Get the votes for example 9.
        get() {
            val c1 = 0
            val c2 = 1
            val c3 = 2
            val votes: Array<Vote> = arrayOf<Vote>(
                Vote(10000, intArrayOf(c1, c2, c3)),
                Vote(6000, intArrayOf(c2, c1, c3)),
                Vote(5999, intArrayOf(c3, c1, c2)),
            )
            return Votes(votes, 3)
        }

    val votesInExample12: Votes
        /// Get the votes for example 12.
        get() {
            val c1 = 0
            val c2 = 1
            val c3 = 2
            val c4 = 3
            val votes: Array<Vote> = arrayOf<Vote>(
                Vote(5000, intArrayOf(c1, c2, c3)),
                Vote(5000, intArrayOf(c1, c3, c2)),
                Vote(5000, intArrayOf(c2, c3, c1)),
                Vote(1500, intArrayOf(c2, c1, c3)),
                Vote(5000, intArrayOf(c3, c2, c1)),
                Vote(500, intArrayOf(c3, c1, c2)),
                Vote(5000, intArrayOf(c4, c1)),
            )
            return Votes(votes, 4)
        }

    /** test the getVotesInTable1 function and the methods on the Votes object.  */
    @Test
    fun testVotesStructure() {
        val votes: Votes = votesInTable1
        assertEquals(60000, votes.totalVotes())
        assertEquals(26000, votes.firstPreferenceOnlyTally(0))
        assertEquals(10000, votes.firstPreferenceOnlyTally(1))
        assertEquals(9000, votes.firstPreferenceOnlyTally(2))
        assertEquals(15000, votes.firstPreferenceOnlyTally(3))
        assertArrayEquals(intArrayOf(26000, 10000, 24000), votes.restrictedTallies(intArrayOf(0, 1, 3)))
        assertArrayEquals(intArrayOf(26000, 30000), votes.restrictedTallies(intArrayOf(0, 3)))
        val result: IRVResult = votes.runElection(TimeOut.never())
        assertArrayEquals(intArrayOf(3), result.possibleWinners)
        assertArrayEquals(intArrayOf(2, 1, 0, 3), result.eliminationOrder)
    }


    /** Test ASNs for example 10 in the paper  */
    @Test
    fun test_example10() {
        val votes: Votes = votesInExample9
        val BRAVO_EG5: BallotPollingBRAVO = BallotPollingBRAVO(0.05, 21999)
        assertEquals(BRAVO_EG5.total_auditable_ballots, votes.totalVotes())
        val assertion1: NotEliminatedBefore = NotEliminatedBefore(0, 1)
        val assertion2: NotEliminatedBefore = NotEliminatedBefore(0, 2)
        val asn1: Double = assertion1.difficulty(votes, BRAVO_EG5).difficulty
        val asn2: Double = assertion2.difficulty(votes, BRAVO_EG5).difficulty
        println("Example 10 : ASN1=$asn1 ASN2=$asn2")
        assertEquals(135.3, asn1, 0.1)
        assertEquals(135.2, asn2, 0.1)
    }

    /** Test ASNs for example 11 in the paper  */
    @Test
    fun test_example11() {
        val votes: Votes = votesInExample9
        val MACRO_EG5 = BallotComparisonMACRO(0.05, 1.1, 21999)
        assertEquals(MACRO_EG5.total_auditable_ballots, votes.totalVotes())
        val assertion1: NotEliminatedBefore = NotEliminatedBefore(0, 1)
        val assertion2: NotEliminatedBefore = NotEliminatedBefore(0, 2)
        val asn1: Double = assertion1.difficulty(votes, MACRO_EG5).difficulty
        val asn2: Double = assertion2.difficulty(votes, MACRO_EG5).difficulty
        println("Example 11 : ASN1=$asn1 ASN2=$asn2")
        assertEquals(36.2, asn1, 0.1)
        assertEquals(36.2, asn2, 0.1)
    }

    /** Test ASNs for example 12 in the paper  */
    @Test
    fun test_example12_asns() {
        val votes: Votes = votesInExample12
        val BRAVO_EG12: BallotPollingBRAVO = BallotPollingBRAVO(0.05, 27000)
        val MACRO_EG12: BallotComparisonMACRO = BallotComparisonMACRO(0.05, 1.1, 27000)
        assertEquals(MACRO_EG12.total_auditable_ballots, votes.totalVotes())
        assertEquals(BRAVO_EG12.total_auditable_ballots, votes.totalVotes())
        run {
            // test bravo
            val assertion1 = NotEliminatedNext(0, 1, intArrayOf(0, 1))
            val assertion2 = NotEliminatedNext(0, 2, intArrayOf(0, 2))
            val assertion3 = NotEliminatedBefore(0, 3)
            val assertion4 = NotEliminatedNext(0, 2, intArrayOf(0, 1, 2))
            val asn1: Double = assertion1.difficulty(votes, BRAVO_EG12)
            val asn2: Double = assertion2.difficulty(votes, BRAVO_EG12)
            val asn3: Double = assertion3.difficulty(votes, BRAVO_EG12).difficulty
            val asn4: Double = assertion4.difficulty(votes, BRAVO_EG12)
            println("Example 12 : ASN1=$asn1 ASN2=$asn2  ASN3=$asn3  ASN4=$asn4")
            val asn1p: Double = 100.0 * asn1 / votes.totalVotes()
            val asn2p: Double = 100.0 * asn2 / votes.totalVotes()
            val asn3p: Double = 100.0 * asn3 / votes.totalVotes()
            val asn4p: Double = 100.0 * asn4 / votes.totalVotes()
            println("Example 12 percentages : ASN1=$asn1p% ASN2=$asn2p%  ASN3=$asn3p%  ASN4=$asn4p%")
            assertEquals(1.0, asn1p, 0.1)
            assertEquals(0.5, asn2p, 0.1)
            assertEquals(0.4, asn3p, 0.1)
            assertEquals(0.1, asn4p, 0.1)
        }
        run {
            // ballot comparison
            val assertion1: NotEliminatedNext = NotEliminatedNext(0, 1, intArrayOf(0, 1))
            val assertion2: NotEliminatedNext = NotEliminatedNext(0, 2, intArrayOf(0, 1, 2))
            val assertion3: NotEliminatedNext = NotEliminatedNext(0, 2, intArrayOf(0, 2))
            val assertion4: NotEliminatedBefore = NotEliminatedBefore(0, 3)
            val assertion5a: NotEliminatedNext = NotEliminatedNext(1, 3, intArrayOf(1, 3))
            val assertion5b: NotEliminatedNext = NotEliminatedNext(2, 3, intArrayOf(2, 3))
            val asn1: Double = assertion1.difficulty(votes, MACRO_EG12)
            val asn2: Double = assertion2.difficulty(votes, MACRO_EG12)
            val asn3: Double = assertion3.difficulty(votes, MACRO_EG12)
            val asn4: Double = assertion4.difficulty(votes, MACRO_EG12).difficulty
            val asn5a: Double = assertion5a.difficulty(votes, MACRO_EG12)
            val asn5b: Double = assertion5b.difficulty(votes, MACRO_EG12)
            println("Example 12 : ASN1=$asn1 ASN2=$asn2  ASN3=$asn3  ASN4=$asn4 ASN5=$asn5a and $asn5b")
            val asn1p: Double = 100.0 * asn1 / votes.totalVotes()
            val asn2p: Double = 100.0 * asn2 / votes.totalVotes()
            val asn3p: Double = 100.0 * asn3 / votes.totalVotes()
            val asn4p: Double = 100.0 * asn4 / votes.totalVotes()
            val asn5ap: Double = 100.0 * asn5a / votes.totalVotes()
            val asn5bp: Double = 100.0 * asn5b / votes.totalVotes()
            println("Example 12 percentages : ASN1=$asn1p% ASN2=$asn2p%  ASN3=$asn3p%  ASN4=$asn4p%  ASN5=$asn5ap% and $asn5bp%")
            assertEquals(0.17, asn1p, 0.01)
            assertEquals(0.07, asn2p, 0.01)
            assertEquals(0.11, asn3p, 0.01)
            assertEquals(0.13, asn4p, 0.01)
            assertEquals(0.04, asn5ap, 0.01)
            assertEquals(0.04, asn5bp, 0.01)
        }
    }

    /** Test that RAIRE produces reasonable answers for the BRAVO audit type.  */
    @Test
    fun test_example12_raire_bravo() {
        val votes: Votes = votesInExample12
        val BRAVO_EG12: BallotPollingBRAVO = BallotPollingBRAVO(0.05, 27000)
        assertEquals(BRAVO_EG12.total_auditable_ballots, votes.totalVotes())
        val res: RaireResult = RaireResult(votes, 0, BRAVO_EG12, TrimAlgorithm.None, TimeOut.never())
        assertEquals(278.25, res.difficulty, 0.01)
    }

    /** Test that RAIRE produces reasonable answers for the MACRO audit type.  */
    @Test
    fun test_example12_raire_macro() {
        val votes: Votes = votesInExample12
        val MACRO_EG12 = BallotComparisonMACRO(0.05, 1.1, 27000)
        assertEquals(MACRO_EG12.total_auditable_ballots, votes.totalVotes())
        val res = RaireResult(votes, 0, MACRO_EG12, TrimAlgorithm.None, TimeOut.never())
        assertEquals(44.49, res.difficulty, 0.01)
    }
}
