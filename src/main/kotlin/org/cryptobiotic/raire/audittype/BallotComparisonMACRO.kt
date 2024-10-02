/*
  Copyright 2023 Democracy Developers
  This is a Java re-implementation of raire-rs https://github.com/DemocracyDevelopers/raire-rs
  It attempts to copy the design, API, and naming as much as possible subject to being idiomatic and efficient Java.

  This file is part of raire-java.
  raire-java is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
  raire-java is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details.
  You should have received a copy of the GNU Affero General Public License along with ConcreteSTV.  If not, see <https://www.gnu.org/licenses/>.

*/
package org.cryptobiotic.raire.audittype

import kotlin.math.ln

/** A MACRO ballot comparison audit as described in the paper "RAIRE: Risk-limiting audits for IRV elections",
 * arXiv preprint arXiv:1903.08804.  */
data class BallotComparisonMACRO(
    /** The desired confidence α. A number between 0 and 1 bounding the probability of not rejecting a false result. */
    val confidence: Double,
    /** Gamma parameter: γ ≥ 1  */
    val error_inflation_factor: Double,
    /** The total number of ballots in the auditing universe of the contest we are generating assertions for.  */
    val total_auditable_ballots: Int
) : AuditType {

    override fun difficulty(lowestTallyWinner: Int, highestTallyLoser: Int): Double {
        if (lowestTallyWinner <= highestTallyLoser) return Double.POSITIVE_INFINITY
        else {
            val v = (lowestTallyWinner - highestTallyLoser).toDouble()
            val u = (2.0 * error_inflation_factor * total_auditable_ballots) / v
            return -ln(confidence) * u
        }
    }
}
