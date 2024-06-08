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

import org.cryptobiotic.raire.algorithm.RaireResult
import org.cryptobiotic.raire.audittype.AuditType
import org.cryptobiotic.raire.irv.Vote
import org.cryptobiotic.raire.irv.Votes
import org.cryptobiotic.raire.pruning.TrimAlgorithm
import org.cryptobiotic.raire.time.TimeOut

/** Defines a contest for which we want to generate assertions, metadata for that contest, and all algorithmic
 * settings to be used by RAIRE when generating assertions.  */
class RaireProblem(
    /** The input to raire-java will contain metadata that, while not used by raire-java for computing assertions,
     * may be useful information for assertion visualisation or information that election administrators would like
     * to associate with any assertions generated.  */
    val metadata: Map<String, Any>,
    votes: Array<Vote>,
    /** The number of candidates in the contest.  */
    val num_candidates: Int,
    /** The reported winner of the contest (if provided as input to raire-java). If this information was not
     * provided as input, this field will be null.  */
    val winner: Int? = null,
    audit: AuditType,
    trim_algorithm: TrimAlgorithm?,
    difficulty_estimate: Double?,
    time_limit_seconds: Double?)
{
    /** The consolidated set of votes cast in the election. Note that each Vote is a ranking and the number of times
     * that ranking appeared on a vote cast in the contest.  */
    val votes: Array<Vote> = votes

    /** The method that RAIRE should use to assess the difficulty of auditing a generated assertion.  */
    val audit: AuditType

    /** The algorithm that raire-java will use to filter the set of generated assertions, removing those that
     * are redundant.  */
    val trim_algorithm: TrimAlgorithm? // may be null.

    /** An estimate of the expected overall difficulty of the audit, optionally provided as input. RAIRE may
     * be able to use this estimate to generate assertions more efficiently. Note that the overall difficulty
     * of an audit is the difficulty of the most-difficulty-to-audit-assertion generated. See AuditType and its
     * implementations for more information on different approaches for computing assertion difficulty.  */
    val difficulty_estimate: Double? // may be null.

    /** Optional time limit to impose across all stages of computation by raire-java.  */
    val time_limit_seconds: Double? // may be null.

    init {
        this.audit = audit
        this.trim_algorithm = trim_algorithm
        this.difficulty_estimate = difficulty_estimate
        this.time_limit_seconds = time_limit_seconds
    }

    /** Generate assertions for the given contest, and return those assertions as a RaireSolution.  */
    fun solve(): RaireSolution {
        var result: RaireSolution.RaireResultOrError =
        if (time_limit_seconds != null && (time_limit_seconds <= 0.0 || time_limit_seconds.isNaN())) {
            RaireSolution.RaireResultOrError(RaireError.InvalidTimeout())
        } else {
            val timeout = TimeOut(null, time_limit_seconds)
            try {
                if (this.num_candidates < 1) throw RaireException(RaireError.InvalidNumberOfCandidates())
                val votes = Votes(this.votes, this.num_candidates)
                RaireSolution.RaireResultOrError(
                    RaireResult(
                        votes,
                        winner, // TODO null is allowed.
                        audit,
                        trim_algorithm ?: TrimAlgorithm.MinimizeTree,
                        timeout
                    )
                )
            } catch (e: RaireException) {
                RaireSolution.RaireResultOrError(e.error)
            }
        }
        return RaireSolution(metadata, result)
    }
}
