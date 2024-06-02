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

import java.util.*

/** RAIRE operates on a consolidated collection of votes cast in a given contest. This consolidated set of
 * votes takes each unique ranking that appears on a vote in the contest, and counts the number of votes with that
 * ranking. A 'Vote' is now defined by a ranking, and the number of times that ranking appears on a vote. This
 * consolidation means that RAIRE can be more efficient by iterating over a smaller set of votes than if we
 * considered each ballot individually.  */
data class Vote (
    val n: Int, // The number of votes that expressed the ranking 'prefs' on their ballot.
    val prefs: IntArray // A preference ranking. Note that prefs[0] denotes the first (highest) ranked candidate.
) {
    /** Find the highest preferenced candidate on this vote amongst the given set of continuing candidates.
     * return null or a candidate index.  */
    fun topPreference(continuing: BitSet): Int? {
        for (c in prefs) {
            if (continuing[c]) {
                return c
            }
        }
        return null
    }

    /** Find the highest preferenced candidate on this vote amongst the given set of continuing candidates.
     * return null or the argument of the hashmap for the key of the continuing candidate.  */
    fun topSubPreference(continuing: HashMap<Int?, Int?>): Int? {
        for (c in prefs) {
            val found = continuing[c]
            if (found != null) {
                return found
            }
        }
        return null
    }

    /** Find the highest preferenced candidate on this vote amongst the given set of continuing candidates.
     * return null or the argument of the hashmap for the key of the continuing candidate.  */
    fun topSubPreferenceArray(continuing: Array<Int?>): Int? {
        for (c in prefs) {
            if (c < continuing.size) {
                val found = continuing[c]
                if (found != null) {
                    return found
                }
            }
        }
        return null
    }
}
