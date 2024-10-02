/*
 Copyright 2023 Democracy Developers
 This is a Java re-implementation of raire-rs https://github.com/DemocracyDevelopers/raire-rs
 It attempts to copy the design, API, and naming as much as possible subject to being idiomatic and efficient Java.

 This file is part of raire-java.
 raire-java is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 raire-java is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details.
 You should have received a copy of the GNU Affero General Public License along with ConcreteSTV.  If not, see <https://www.gnu.org/licenses/>.
*/
package org.cryptobiotic.raire

import com.github.michaelbull.result.*

import org.cryptobiotic.raire.algorithm.RaireResult
import org.cryptobiotic.raire.algorithm.runRaire
import org.cryptobiotic.raire.audittype.AuditType
import org.cryptobiotic.raire.irv.Vote
import org.cryptobiotic.raire.irv.Votes
import org.cryptobiotic.raire.pruning.TrimAlgorithm
import org.cryptobiotic.raire.time.TimeOut
import org.cryptobiotic.raire.util.toArray

/**
 * RaireProblem represents the data and configurable parameters required for running the RAIRE algorithm to
 * generate assertions for an election contest.
 *
 * @property metadata The input to RAIRE will contain metadata that, while not used by RAIRE for computing assertions,
 * may be useful information for assertion visualization or information that election administrators would like to associate
 * with any assertions generated.
 *
 * @property votes The consolidated set of votes cast in the election. Note that each Vote is a ranking and the number of times
 * that ranking appeared on a vote cast in the contest.
 *
 * @property num_candidates The number of candidates in the contest.
 *
 * @property winner The reported winner of the contest (if provided as input to RAIRE). If this information was not provided
 * as input, this field will be null.
 *
 * @property audit The method that RAIRE should use to assess the difficulty of auditing a generated assertion.
 *
 * @property trim_algorithm The algorithm that RAIRE will use to filter the set of generated assertions, removing those that
 * are redundant.
 *
 * @property difficulty_estimate An estimate of the expected overall difficulty of the audit, optionally provided as input. RAIRE
 * may be able to use this estimate to generate assertions more efficiently. Note that the overall difficulty of an audit
 * is the difficulty of the most-difficulty-to-audit-assertion generated. See AuditType and its implementations for more
 * information on different approaches for computing assertion difficulty.
 *
 * @property time_limit_seconds Optional time limit to impose across all stages of computation by RAIRE.
 */
data class RaireProblem(
    val metadata: RaireMetadata?,
    val votes: List<Vote>,
    val num_candidates: Int,
    val winner: Int? = null,
    val audit: AuditType,
    val trim_algorithm: TrimAlgorithm?,
    val difficulty_estimate: Double?,
    val time_limit_seconds: Double?,
    ) {

    /** Generate assertions for the given contest, and return those assertions as a RaireSolution.  */
    fun solve(): RaireSolution {
        var result: Result<RaireResult, RaireError> =
            if (time_limit_seconds != null && (time_limit_seconds <= 0.0 || time_limit_seconds.isNaN())) {
                Err(RaireError.InvalidTimeout())
            } else {
                val timeout = TimeOut(null, time_limit_seconds)
                try {
                    if (this.num_candidates < 1) throw RaireException(RaireError.InvalidNumberOfCandidates())
                    val votes = Votes(toArray(this.votes), this.num_candidates)
                    // new RaireResult(votes,winner,audit,trim_algorithm==null?TrimAlgorithm.MinimizeTree:trim_algorithm,timeout));
                    Ok(
                        runRaire(
                            votes,
                            winner, // TODO null is allowed.
                            audit,
                            trim_algorithm?: TrimAlgorithm.MinimizeTree,
                            timeout
                        )
                    )
                } catch (e: RaireException) {
                    Err(e.error)
                }
            }
        return RaireSolution(metadata, result)
    }
}

data class RaireMetadata(
    val candidates: List<String> = emptyList(),
    val contest: String? = null,
)
