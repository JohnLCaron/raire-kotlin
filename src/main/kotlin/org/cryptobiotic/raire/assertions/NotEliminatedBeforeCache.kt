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

/** At the start of the RAIRE algorithm, we compute the difficulty and margins of all possible
 * NotEliminatedBefore assertions that we can form between pairs of candidates. We remember this
 * information so that when we are faced with an elimination order we want to rule out, and a candidate
 * NEB assertion, we can look up its difficulty and margin without having to recompute them. The
 * NotEliminatedBeforeCaches stores difficulty and margin information for candidate NEB assertions.  */
class NotEliminatedBeforeCache(votes: Votes, audit: AuditType) {
    val cache: Array<Array<DifficultyAndMargin?>> =
        Array(votes.numCandidates()) { arrayOfNulls(votes.numCandidates()) }

    init {
        for (winner in 0 until votes.numCandidates()) {
            for (loser in 0 until votes.numCandidates()) {
                cache[winner][loser] =
                    if (winner == loser) DifficultyAndMargin(Double.POSITIVE_INFINITY, 0)
                    else NotEliminatedBefore(winner, loser).difficulty(votes, audit)
            }
        }
    }

    /** Return the difficulty and margin associated with a given NEB assertion (entry).  */
    fun difficulty(entry: NotEliminatedBefore): DifficultyAndMargin {
        return cache[entry.winner][entry.loser]!! // TODO invalid entry would give invalid index, but cant return null.
    }
}
