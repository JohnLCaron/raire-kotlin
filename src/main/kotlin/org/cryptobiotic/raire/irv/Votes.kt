/*
  Copyright 2023 Democracy Developers
  This is a Java re-implementation of raire-rs https://github.com/DemocracyDevelopers/raire-rs
  It attempts to copy the design, API, and naming as much as possible subject to being idiomatic and efficient Java.

  This file is part of raire-java.
  raire-java is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
  raire-java is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details.
  You should have received a copy of the GNU Affero General Public License along with ConcreteSTV.  If not, see <https://www.gnu.org/licenses/>.

 */
package org.cryptobiotic.raire.irv

import org.cryptobiotic.raire.time.TimeOut
import java.util.*
import java.util.stream.IntStream

/** This class stores the set of consolidated votes cast in the contest we are generating assertions for. A
 * consolidated votes defines a ranking and the number of times that ranking appears on a vote cast in the contest.  */
class Votes(
    val votes: Array<Vote>, // Consolidated set of votes cast in a contest.
    numCandidates: Int
) {
    /** Array, indexed by candidate number, indicating the first preference tally of each candidate in the contest.  */
    private val firstPreferenceVotes = IntArray(numCandidates)

    init {
        for ((n, prefs) in votes) {
            if (prefs.size > 0) {
                val candidate = prefs[0]
                require (candidate in 0..<numCandidates)
                firstPreferenceVotes[candidate] += n
            }
        }
    }

    /** Return the first preference tally for the given candidate.  */
    fun firstPreferenceOnlyTally(candidate: Int): Int {
        return firstPreferenceVotes[candidate]
    }

    /** Get the tallies for each continuing candidate in the given array (continuing), returning an array of the same
     * length and order as the continuing structure.  */
    fun restrictedTallies(continuing: IntArray): IntArray {
        val res = IntArray(continuing.size)
        //HashMap<Integer,Integer> continuingMap = new HashMap<>();
        val continuingMap = arrayOfNulls<Int>(Arrays.stream(continuing).max().orElse(0) + 1)
        for (i in continuing.indices) continuingMap[continuing[i]] = i // continuingMap.put(continuing[i],i);

        for (v in votes) {
            val c = v.topSubPreferenceArray(continuingMap)
            if (c != null) res[c] += v.n
        }
        return res
    }

    /** Computes and returns the total number of votes cast in the contest.  */
    fun totalVotes(): Int {
        var res = 0
        for ((n) in votes) {
            res += n
        }
        return res
    }

    /** Returns the total number of candidates in the contest.  */
    fun numCandidates(): Int {
        return firstPreferenceVotes.size
    }

    /** Tabulates the outcome of the IRV election, returning the outcome as an IRVResult. The only
     * error that may arise during tabulation is a RaireError::TimeoutCheckingWinner exception.  */
    fun runElection(timeout: TimeOut): IRVResult {
        val work = IRVElectionWork()
        val all_candidates = IntStream.range(0, numCandidates()).toArray()
        val possible_winners: IntArray = work.findAllPossibleWinners(all_candidates, this, timeout)
        return IRVResult(possible_winners, work.possibleEliminationOrder())
    }
}
