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
package org.cryptobiotic.raireservice.entity

import io.github.oshai.kotlinlogging.KotlinLogging
import org.cryptobiotic.raire.assertions.AssertionAndDifficulty
import org.cryptobiotic.raire.assertions.NotEliminatedBefore
import org.cryptobiotic.raireservice.RaireErrorCode
import org.cryptobiotic.raireservice.service.Metadata
import org.cryptobiotic.raireservice.RaireServiceException

/**
 * A Not Eliminated Before assertion (or NEB) says that a candidate _winner_ will always have
 * a higher tally than a candidate _loser_. What this means is that the minimum possible tally
 * that _winner_ will have at any stage of tabulation is greater than the maximum possible
 * tally _loser_ can ever achieve. For more detail on NEB assertions, refer to the Guide to RAIRE.
 * The constructor for this class takes a raire-java NEB assertion construct (NotEliminatedBefore)
 * and translates it into a NEBAssertion entity, suitable for storage in the corla database.
 */
class NEBAssertion
/**
 * Construct a NEBAssertion give a raire-java NotEliminatedBefore construct.
 * @param contestName Name of the contest to which this assertion belongs.
 * @param universeSize Number of ballots in the auditing universe for the assertion.
 * @param margin Absolute margin of the assertion.
 * @param difficulty Difficulty of the assertion, as computed by raire-java.
 * @param candidates Names of the candidates in this assertion's contest.
 * @param neb Raire-java NotEliminatedBefore assertion to be transformed into a NENAssertion.
 * @throws IllegalArgumentException if the caller supplies a non-positive universe size, invalid
 * margin, or invalid combination of winner, loser and list of assumed continuing candidates.
 * @throws ArrayIndexOutOfBoundsException if the winner or loser indices in the raire-java
 * assertion are invalid with respect to the given array of candidates.
 */(
    contestName: String,
    universeSize: Long,
    margin: Int,
    difficulty: Double,
    candidates: Array<String>,
    neb: NotEliminatedBefore

) : ServiceAssertion(contestName, candidates[neb.winner], candidates[neb.loser], margin, universeSize, difficulty) {

    /**
     * {@inheritDoc}
     */
    override fun convert(candidates: List<String?>): AssertionAndDifficulty {
        val prefix = "[convert]"
        logger.debug {
            String.format(
                "%s Constructing a raire-java AssertionAndDifficulty for the " +
                        "assertion %s with candidate list parameter %s.", prefix, this.description, candidates
            )
        }

        val w = candidates.indexOf(winner)
        val l = candidates.indexOf(loser)

        logger.debug { String.format("%s Winner index %d, Loser index %d.", prefix, w, l) }
        if (w != -1 && l != -1) {
            val status: MutableMap<String, Any> = HashMap()
            status[Metadata.STATUS_RISK] = currentRisk

            logger.debug {
                java.lang.String.format(
                    "%s Constructing AssertionAndDifficulty, current risk %f.",
                    prefix, currentRisk
                )
            }
            return AssertionAndDifficulty(NotEliminatedBefore(w, l), difficulty, margin, status)
        } else {
            val msg = String.format(
                "%s Candidate list provided as parameter is inconsistent " +
                        "with assertion (winner or loser not present).", prefix
            )
            logger.error { msg }
            throw RaireServiceException(msg, RaireErrorCode.WRONG_CANDIDATE_NAMES)
        }
    }

    override val assertionType: String
        get() = "NEB"

    override val description: String
        get() = String.format("%s NEB %s with diluted margin %f", winner, loser, dilutedMargin)

    companion object {
        private val logger = KotlinLogging.logger("NEBAssertion")
    }

    init {
        val prefix = "[all args constructor]"
        logger.debug {
            java.lang.String.format(
                "%s Constructed NEB assertion with winner (%d) and loser (%d) " +
                        "indices with respect to candidate list %s: %s. " +
                        "Parameters: contest name %s; margin %d; universe size %d; and difficulty %f.", prefix,
                neb.winner, neb.loser, candidates.contentToString(), this.description,
                contestName, margin, universeSize, difficulty
            )
        }
    }
}
