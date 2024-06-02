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

/** An elimination order will be either compatible with a suffix or not. For any given elimination order,
 * a suffix contains its 'tail-end'. For example, for the elimination order 1, 4, 0, 3, 2, one possible
 * suffix is [0, 3, 2]. Another is [3, 2]. For a given suffix, an assertion will either: rule out all
 * elimination orders that end in the suffix (Contradiction); rule out some elimination orders that end in
 * the suffix (NeedsMoreDetail); rule out none of the elimination orders that end in the suffix (Ok).
 */
enum class EffectOfAssertionOnEliminationOrderSuffix {
    /** The suffix is ruled out by the assertion, regardless of the rest of the elimination order. */
    Contradiction,

    /** The suffix is ok as far as the assertion is concerned, no more information needed.
     * This could mean that the suffix agrees with the assertion, or the assertion only applies to different suffixes.
     * Regardless, whatever the rest of the elimination order, the assertion will be fine with this.  */
    Ok,

    /** Some elimination orders ending with this suffix are OK, others are contradicted.  */
    NeedsMoreDetail,
}



