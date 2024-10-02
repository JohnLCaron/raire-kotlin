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

/** Simple tuple like structure that stores an Assertion alongside its difficulty and margin.
 * The difficulty of an assertion is a measure that reflects how much auditing effort is required
 * to check the assertion in an RLA. We expect that assertions with a higher difficulty will require
 * more ballot samples to check. A range of possible difficulty measures can be used by RAIRE (see
 * the AuditType interface and its implementations).
 *
 * @property assertion
 * @property difficulty A measure of how hard this assertion will be to audit. Assertions with a higher difficulty
 *      * will require more ballot samples to check in an audit.
 * @property margin Each assertion has a winner, a loser, and a context which determines whether a given
 *      * votes falls into the winner's pile or the loser's. The margin of the assertion is equal to
 *      * the difference in these tallies.
 * @property status This field is not used by raire-java for computing assertions,
 *      * may be useful information for assertion visualisation or information that election administrators would like
 *      * to associate with this specific assertion. This field will be created as null by raire-java for efficiency
 *      * reasons (rather than containing an empty object). If you want to use it, create an instance with
 *      * a non-null value using the constructor. This is useful primarily to people using this data type
 *      * in external software to annotate a set of assertions being verified.

 */
data class AssertionAndDifficulty(
    val assertion: Assertion,
    val difficulty: Double,
    val margin: Int,
    val status: MutableMap<String, Any> = mutableMapOf(), // TODO mutable ??
) {
    override fun toString(): String {
        return "  $assertion, difficulty=$difficulty, margin=$margin)"
    }
}
