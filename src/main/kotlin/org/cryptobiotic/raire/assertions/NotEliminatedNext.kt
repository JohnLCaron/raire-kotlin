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
import java.util.*
import kotlin.math.max

/**
 * Assert that _winner_ beats _loser_ in an audit when all candidates other that
 * those in _remaining_ have been removed.
 *
 * In particular, this means that _winner_ can not be the next candidate eliminated.
 *
 * This assertion type is also referred to as an NEN assertion in A Guide to RAIRE.
 */
class NotEliminatedNext(
    /** The winning candidate of this NotEliminatedNext assertion.  */
    val winner: Int,
    /** The losing candidate of this NotEliminatedNext assertion.  */
    val loser: Int,
    continuing: IntArray
) : Assertion() {
    /** Each NotEliminatedNext assertion has an associated context. This context is
     * a set of candidates that we assume are 'continuing' (have not yet been eliminated).
     * All candidates not in this list are assumed to have been already eliminated.
     * Continuing candidates are sorted in ascending order of their identifier.
     *
     * This ordering makes it easy to check if two assertions are actually the same, and
     * it allows binary search for seeing if a particular candidate is in this list.  */
    val continuing: IntArray = continuing.clone()

    init {
        Arrays.sort(this.continuing)
    }

    override fun equals(other: Any?): Boolean {
        if (other is NotEliminatedNext) {
            val o = other
            return o.winner == winner && o.loser == loser && continuing.contentEquals(o.continuing)
        } else {
            return false
        }
    }

    /** Compute and return the difficulty estimate associated with this assertion. This method
     * computes the tallies of the assertion's winner and loser, in the relevant context,
     * according to the set of Votes (votes) provided as input. The given AuditType, audit,
     * defines the chosen method of computing assertion difficulty given these winner and loser
     * tallies. */
    fun difficulty(votes: Votes, audit: AuditType): Double {
        val tallies: IntArray = votes.restrictedTallies(continuing)
        var tally_winner = Int.MAX_VALUE
        var tally_loser = 0
        for (i in continuing.indices) {
            if (winner == continuing[i]) tally_winner = tallies[i]
            else if (loser == continuing[i]) tally_loser = tallies[i]
        }
        return audit.difficulty(tally_winner, tally_loser)
    }

    /** Returns true if the given candidate is in this assertion's continuing list.  */
    private fun isContinuing(c: Int): Boolean {
        return Arrays.binarySearch(continuing, c) >= 0
    }

    override val isNEB: Boolean
        get() = false

    override fun okEliminationOrderSuffix(eliminationOrderSuffix: IntArray?): EffectOfAssertionOnEliminationOrderSuffix? {
        // the order of the people who are left when down to the same length as self.continuing(). Or the whole thing if sub-prefix
        val startInclusive = max((eliminationOrderSuffix!!.size - continuing.size).toDouble(), 0.0)
            .toInt()
        // check to see the last candidates in the elimination order match the continuing candidates.
        if (Arrays.stream(eliminationOrderSuffix, startInclusive, eliminationOrderSuffix.size)
                .anyMatch { c: Int -> !isContinuing(c) }
        ) {
            return EffectOfAssertionOnEliminationOrderSuffix.Ok // the elimination order is not affected by this rule as the continuing candidates are wrong.
        }
        return if (eliminationOrderSuffix.size >= continuing.size) { // the whole elimination order is all present. The winner cannot be the first eliminated, as self.winner has more votes than self.loser at this point.
            if (eliminationOrderSuffix[startInclusive] == winner) EffectOfAssertionOnEliminationOrderSuffix.Contradiction else EffectOfAssertionOnEliminationOrderSuffix.Ok
        } else {
            if (Arrays.stream(eliminationOrderSuffix, startInclusive, eliminationOrderSuffix.size)
                    .anyMatch { c: Int -> c == winner }
            ) EffectOfAssertionOnEliminationOrderSuffix.Ok // winner wasn't the first eliminated.
            else EffectOfAssertionOnEliminationOrderSuffix.NeedsMoreDetail
        }
    }

    override fun toString(): String {
        return "NotEliminatedNext(winner=$winner, loser=$loser, continuing=${continuing.contentToString()})"
    }

    companion object {
        /** Find the best NEN assertion that will rule out the outcome where the given winner is eliminated
         * next when only the specified candidates are continuing, on the basis of the set of Votes (votes)
         * cast in the contest and the chosen method of computing assertion difficulty (audit). May return null
         * if no such assertions exist. The 'continuing' candidates must include the given winner.   */
        fun findBestDifficulty(
            votes: Votes,
            audit: AuditType,
            continuing: IntArray,
            winner: Int
        ): AssertionAndDifficulty? {
            val tallies: IntArray = votes.restrictedTallies(continuing)
            var tally_winner = Int.MAX_VALUE
            var tally_loser = Int.MAX_VALUE
            var best_loser: Int? = null
            for (i in continuing.indices) {
                if (winner == continuing[i]) tally_winner = tallies[i]
                else if (tallies[i] <= tally_loser) {
                    best_loser = continuing[i]
                    tally_loser = tallies[i]
                }
            }
            if (best_loser != null) {
                val difficulty: Double = audit.difficulty(tally_winner, tally_loser)
                val margin = max(0.0, (tally_winner - tally_loser).toDouble()).toInt()
                val assertion = NotEliminatedNext(winner, best_loser, continuing)
                return AssertionAndDifficulty(assertion, difficulty, margin)
            } else {
                return null
            }
        }
    }
}
