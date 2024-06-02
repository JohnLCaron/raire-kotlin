/*
Copyright 2024 Democracy Developers

The Raire Service is designed to connect colorado-rla and its associated database to
the raire assertion generation engine (https://github.com/DemocracyDevelopers/raire-java).

This file is part of raire-service.

raire-service is free software: you can redistribute it and/or modify it under the terms
of the GNU Affero General Public License as published by the Free Software Foundation, either
version 3 of the License, or (at your option) any later version.

raire-service is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
See the GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License along with
raire-service. If not, see <https://www.gnu.org/licenses/>.
*/
package org.cryptobiotic.raireservice.service

/**
 * This class defines the names of contest/audit metadata fields. Some of these are used when
 * forming the metadata map passed to raire-java in a generate assertions request, and when
 * forming contest metadata in the construction of a JSON assertion export (for visualisation).
 * Some are used when exporting assertions as CSV.
 */
object Metadata {
    // Used for json export.
    /**
     * Metadata field name for the contest's candidates.
     */
    const val CANDIDATES: String = "candidates"

    /**
     * Metadata field name for the contest's risk limit.
     */
    const val RISK_LIMIT: String = "risk_limit"

    /**
     * Metadata field name for the contest's name.
     */
    const val CONTEST: String = "contest"

    /**
     * Status attribute describing a risk level.
     */
    const val STATUS_RISK: String = "risk"

    // Used for CSV export
    //
    // The first 6 are the values on which we compute maxima or minima in the csv preface.
    //
    /**
     * The absolute margin
     */
    const val MARGIN: String = "Margin"

    /**
     * The diluted margin, i.e. the absolute margin divided by the total ballots in the universe.
     */
    const val DILUTED_MARGIN: String = "Diluted margin"

    /**
     * The difficulty estimated by raire. This is directly proportional to the initial optimistic
     * sample size.
     */
    const val DIFFICULTY: String = "Raire difficulty"

    /**
     * The current calculated risk, based on the audit ballots observed so far.
     */
    const val CURRENT_RISK: String = "Current risk"

    /**
     * The optimistic samples to audit. Colorado-rla calculates this and we retrieve it from the
     * database.
     */
    const val OPTIMISTIC_SAMPLES: String = "Optimistic samples to audit"


    /**
     * The estimated samples to audit. Colorado-rla calculates this, and we retrieve it from the
     * database.
     */
    const val ESTIMATED_SAMPLES: String = "Estimated samples to audit"

    // Other headers used in parts of the csv
    const val CONTEST_NAME_HEADER: String = "Contest name"
    const val CANDIDATES_HEADER: String = "Candidates"

    val extremumHeaders: List<String> = listOf("Extreme item", "Value", "Assertion IDs")

    val csvHeaders: List<String> = listOf(
        "ID",  // The assertion ID, starting at 1 for each csv file (not the ID in the database).
        "Type",  // NEN or NEB.
        "Winner",  // The winner of the assertion.
        "Loser",  // The loser of the assertion.
        "Assumed continuing",  // The set of assumed continuing candidates (NEN only).
        "Difficulty",  // The difficulty estimated by raire.
        "Margin",  // The absolute margin.
        "Diluted margin",  // The diluted margin, i.e. absolute margin divided by universe size.
        "Risk",  // The current calculated risk, based on audit ballot observations.
        "Estimated samples to audit",  // colorado-rla's estimated samples to audit.
        "Optimistic samples to audit",  // colorado-rla's optimistic samples to audit.
        "Two vote over count",  // The number of two-vote overcounts for this assertion.
        "One vote over count",  // The number of one-vote overcounts for this assertion.
        "Other discrepancy count",  // The count of discrepancies that do not affect this assertion's score.
        "One vote under count",  // The number of one-vote undercounts for this assertion.
        "Two vote under count"
    ) // The number of two-vote undercounts for this assertion.
}
