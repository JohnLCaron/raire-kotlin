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

import java.util.*

/** RAIRE generates a set of assertions for a given contest. The different types of assertion
 * that RAIRE can generate are defined as subclasses of this base Assertion class.  */
abstract class Assertion {
    abstract val isNEB: Boolean

    /** Given an elimination order suffix (a sequence of candidates that represents the ending of a
     * set of possible elimination orders), this method checks whether this assertion rules out none, some,
     * or all elimination orders that end in the suffix. Note that a suffix of [3, 2] represents the
     * set of elimination orders that end with candidate '3' as the runner-up and '2' as the winner.  */
    abstract fun okEliminationOrderSuffix(eliminationOrderSuffix: IntArray?): EffectOfAssertionOnEliminationOrderSuffix

    /** This method is used for testing purposes. Given an elimination order suffix,
     * let it through if it is allowed,
     * block if it is contradicted,
     * expand if it is not enough information.
     * Returns an array of elimination order suffixes.
     *
     * This is not an efficient thing to do; this is only useful for consistency checks in tests on tiny data sets.
     */
    fun allowed_suffixes(eliminationOrderSuffix: IntArray, numCandidates: Int): Array<IntArray?> {
        when (okEliminationOrderSuffix(eliminationOrderSuffix)) {
            EffectOfAssertionOnEliminationOrderSuffix.Contradiction -> return arrayOfNulls(0)
            EffectOfAssertionOnEliminationOrderSuffix.Ok -> return arrayOf(eliminationOrderSuffix)
            EffectOfAssertionOnEliminationOrderSuffix.NeedsMoreDetail -> {
                val res = ArrayList<IntArray?>()
                var c = 0
                while (c < numCandidates) {
                    val candidate = c
                    if (Arrays.stream(eliminationOrderSuffix)
                            .noneMatch { e: Int -> e == candidate }
                    ) { // if candidate is not in eliminationOrderSuffix
                        val v = IntArray(eliminationOrderSuffix.size + 1)
                        v[0] = c
                        System.arraycopy(eliminationOrderSuffix, 0, v, 1, eliminationOrderSuffix.size)
                        // v is now c prepended to eliminationOrderSuffix
                        val sub = allowed_suffixes(v, numCandidates)
                        res.addAll(Arrays.asList(*sub))
                    }
                    c++
                }
                return res.toTypedArray<IntArray?>()
            }

            else -> {throw RuntimeException("Not caught by any element of switch")}
        }

    }
}
