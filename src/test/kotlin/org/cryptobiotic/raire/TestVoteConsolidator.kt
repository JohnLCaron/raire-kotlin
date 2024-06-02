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

import org.cryptobiotic.raire.irv.Vote
import org.cryptobiotic.raire.util.VoteConsolidator
import org.junit.jupiter.api.Assertions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Assertions.assertArrayEquals


class TestVoteConsolidator {
    @Test
    fun testWithoutNames() {
        val consolidator = VoteConsolidator()
        assertEquals(0, consolidator.votes.size)
        consolidator.addVote(intArrayOf(0, 1))
        assertEquals(1, consolidator.votes.size)
        consolidator.addVote(intArrayOf(0, 1))
        val shouldBe1VoteMultiplicity2 = consolidator.votes
        Assertions.assertEquals(1, shouldBe1VoteMultiplicity2.size)
        assertEquals(2, shouldBe1VoteMultiplicity2[0].n)
        assertArrayEquals(intArrayOf(0, 1), shouldBe1VoteMultiplicity2[0].prefs)
        consolidator.addVote(intArrayOf(2))
        val shouldBe2VotesMultiplicities1and2  = consolidator.votes
        Assertions.assertEquals(2, shouldBe2VotesMultiplicities1and2.size)
        // order is not guaranteed. Work out which is which.
        val vote01: Vote =
            if (shouldBe2VotesMultiplicities1and2[0].n == 2) shouldBe2VotesMultiplicities1and2[0] else shouldBe2VotesMultiplicities1and2[1]
        val vote2: Vote =
            if (shouldBe2VotesMultiplicities1and2[0].n == 2) shouldBe2VotesMultiplicities1and2[1] else shouldBe2VotesMultiplicities1and2[0]
        assertEquals(2, vote01.n)
        assertArrayEquals(intArrayOf(0, 1), vote01.prefs)
        assertEquals(1, vote2.n)
        assertArrayEquals(intArrayOf(2), vote2.prefs)
    }

    @Test
    fun testWithNames() {
        val consolidator: VoteConsolidator = VoteConsolidator(arrayOf<String>("A", "B", "C"))
        assertEquals(0, consolidator.votes.size)
        consolidator.addVoteNames(arrayOf("A", "B"))
        assertEquals(1, consolidator.votes.size)
        consolidator.addVoteNames(arrayOf("A", "B"))
        val shouldBe1VoteMultiplicity2 = consolidator.votes
        Assertions.assertEquals(1, shouldBe1VoteMultiplicity2.size)
        assertEquals(2, shouldBe1VoteMultiplicity2[0].n)
        assertArrayEquals(intArrayOf(0, 1), shouldBe1VoteMultiplicity2[0].prefs)
        consolidator.addVoteNames(arrayOf("C"))
        val shouldBe2VotesMultiplicities1and2 = consolidator.votes
        Assertions.assertEquals(2, shouldBe2VotesMultiplicities1and2.size)
        // order is not guaranteed. Work out which is which.
        val vote01: Vote =
            if (shouldBe2VotesMultiplicities1and2[0].n == 2) shouldBe2VotesMultiplicities1and2[0] else shouldBe2VotesMultiplicities1and2[1]
        val vote2: Vote =
            if (shouldBe2VotesMultiplicities1and2[0].n == 2) shouldBe2VotesMultiplicities1and2[1] else shouldBe2VotesMultiplicities1and2[0]
        assertEquals(2, vote01.n)
        assertArrayEquals(intArrayOf(0, 1), vote01.prefs)
        assertEquals(1, vote2.n)
        assertArrayEquals(intArrayOf(2), vote2.prefs)
    }
}
