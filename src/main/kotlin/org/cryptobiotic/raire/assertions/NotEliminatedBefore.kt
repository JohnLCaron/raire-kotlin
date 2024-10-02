/*
  Copyright 2023 Democracy Developers
  This is a Java re-implementation of raire-rs https://github.com/DemocracyDevelopers/raire-rs
  It attempts to copy the design, API, and naming as much as possible subject to being idiomatic and efficient Java.

  This file is part of raire-java.
  raire-java is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
  raire-java is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details.
  You should have received a copy of the GNU Affero General Public License along with ConcreteSTV.  If not, see <https://www.gnu.org/licenses/>.

 */
package org.cryptobiotic.raire.assertions

import org.cryptobiotic.raire.audittype.AuditType
import org.cryptobiotic.raire.irv.Votes
import java.util.stream.IntStream

/**
 * A NotEliminatedBefore assertion (or NEB) says that a candidate _winner_ will always have
 * a higher tally than a candidate _loser_. What this means is that the minimum possible tally
 * that _winner_ will have at any stage of tabulation is greater than the maximum possible
 * tally _loser_ can ever achieve. For more detail on NEB assertions, refer to the Guide to RAIRE.
 */
class NotEliminatedBefore(val winner: Int, val loser: Int) : Assertion() {

    override fun equals(other: Any?): Boolean {
        if (other is NotEliminatedBefore) {
            return other.winner == winner && other.loser == loser
        } else {
            return false
        }
    }

    override val isNEB: Boolean
        get() = true

    override fun okEliminationOrderSuffix(eliminationOrderSuffix: IntArray?): EffectOfAssertionOnEliminationOrderSuffix {
        for (i in eliminationOrderSuffix!!.indices.reversed()) {
            val c = eliminationOrderSuffix[i] // iterate in reverse order over eliminationOrderSuffix
            if (c == winner) return EffectOfAssertionOnEliminationOrderSuffix.Ok // winner is after loser
            else if (c == loser) return EffectOfAssertionOnEliminationOrderSuffix.Contradiction // loser is after winner
        }
        return EffectOfAssertionOnEliminationOrderSuffix.NeedsMoreDetail // no information on relative order
    }

    /** Compute the minimum tally of the assertion's winner (its first preference tally) and the
     * maximum tally of the assertion's loser, according to the given set of Votes. This
     * maximum tally contains all votes that preference the loser higher than the winner, or on
     * which the loser appears and the winner does not. The given AuditType, audit, defines the
     * chosen method of computing assertion difficulty given these winner and loser tallies. */
    fun difficulty(votes: Votes, audit: AuditType): DifficultyAndMargin {
        val tallyWinner: Int = votes.firstPreferenceOnlyTally(winner)
        val tallies: IntArray = votes.restrictedTallies(intArrayOf(winner, loser))
        val tallyLoser = tallies[1]
        // active paper count = tally_winner+tally_loser for historical reenactment
        val difficulty: Double = audit.difficulty(tallyWinner, tallyLoser)
        return DifficultyAndMargin(difficulty, if (tallyWinner >= tallyLoser) tallyWinner - tallyLoser else 0)
    }

    override fun toString(): String {
        return "NotEliminatedBefore(winner=$winner, loser=$loser)"
    }

    companion object {
        /**
         * Find the NEB assertion that best rules out the given candidate being the next eliminated, given that
         * candidatesLaterInPi are the other continuing candidates.
         * @return null or an assertion with an associated (finite) difficulty.
         */
        fun findBestAssertion(
            candidate: Int,
            candidatesLaterInPi: IntArray,
            votes: Votes,
            audit: AuditType
        ): AssertionAndDifficulty? {
            var bestDifficulty = Double.MAX_VALUE
            var bestAssertion: NotEliminatedBefore? = null
            var bestMargin = 0
            for (altC in 0 until votes.numCandidates()) if (altC != candidate) {
                val finalAltC = altC
                val contest = if (IntStream.of(*candidatesLaterInPi)
                        .anyMatch { x: Int -> x == finalAltC }
                ) // consider WO(c,c′): Assertion that c beats c′ ∈ π, where c′ != c appears later in π
                    NotEliminatedBefore(
                        candidate,
                        altC
                    ) else  // consider WO(c′′,c): Assertion that c′′ ∈ C\π beats c in a winner-only audit with winner c′′ and loser c
                    NotEliminatedBefore(altC, candidate)
                val dam = contest.difficulty(votes, audit)
                if (dam.difficulty < bestDifficulty) {
                    bestDifficulty = dam.difficulty
                    bestAssertion = contest
                    bestMargin = dam.margin
                }
            }
            return if (bestAssertion != null) {
                AssertionAndDifficulty(bestAssertion, bestDifficulty, bestMargin)
            } else {
                null
            }
        }

        /**
         * Find the NEB assertion that best rules out the given candidate being the next eliminated, given that
         * candidatesLaterInPi are the other continuing candidates.
         * @return null or an assertion with an associated (finite) difficulty.
         */
        fun findBestAssertionUsingCache(
            candidate: Int,
            candidatesLaterInPi: IntArray,
            votes: Votes,
            cache: NotEliminatedBeforeCache
        ): AssertionAndDifficulty? {
            var bestDifficulty = Double.MAX_VALUE
            var bestAssertion: NotEliminatedBefore? = null
            var bestMargin = 0
            for (altC in 0 until votes.numCandidates()) if (altC != candidate) {
                val finalAltC = altC
                val contest = if (IntStream.of(*candidatesLaterInPi)
                        .anyMatch { x: Int -> x == finalAltC }
                ) // consider WO(c,c′): Assertion that c beats c′ ∈ π, where c′ != c appears later in π
                    NotEliminatedBefore(
                        candidate,
                        altC
                    ) else  // consider WO(c′′,c): Assertion that c′′ ∈ C\π beats c in a winner-only audit with winner c′′ and loser c
                    NotEliminatedBefore(altC, candidate)
                val dam = cache.difficulty(contest)
                if (dam.difficulty < bestDifficulty) {
                    bestDifficulty = dam.difficulty
                    bestAssertion = contest
                    bestMargin = dam.margin
                }
            }
            return if (bestAssertion != null) {
                AssertionAndDifficulty(bestAssertion, bestDifficulty, bestMargin)
            } else {
                null
            }
        }
    }
}
