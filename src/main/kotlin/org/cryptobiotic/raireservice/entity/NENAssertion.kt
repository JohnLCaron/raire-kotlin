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
import org.cryptobiotic.raire.assertions.NotEliminatedNext
import org.cryptobiotic.raire.util.toIntArray
import org.cryptobiotic.raireservice.RaireErrorCode
import org.cryptobiotic.raireservice.service.Metadata
import org.cryptobiotic.raireservice.RaireServiceException
import java.util.*

/**
 * A Not Eliminated Next assertion asserts that a _winner_ beats a _loser_ in an audit when all
 * candidates other that those in a specified _assumed to be continuing_ list have been removed.
 * In particular, this means that _winner_ can not be the next candidate eliminated.
 * This assertion type is also referred to as an NEN assertion in A Guide to RAIRE.
 * The constructor for this class takes a raire-java NEN assertion construct (NotEliminatedNext)
 * and translates it into a NENAssertion entity, suitable for storage in the corla database.
 */
class NENAssertion
/**
 * Construct a NENAssertion give a raire-java NotEliminatedNext construct.
 * @param contestName Name of the contest to which this assertion belongs.
 * @param universeSize Number of ballots in the auditing universe for the assertion.
 * @param margin Absolute margin of the assertion.
 * @param difficulty Difficulty of the assertion, as computed by raire-java.
 * @param candidates Names of the candidates in this assertion's contest.
 * @param nen Raire-java NotEliminatedNext assertion to be transformed into a NENAssertion.
 * @throws IllegalArgumentException if the caller supplies a non-positive universe size, invalid
 * margin, or invalid combination of winner, loser and list of assumed continuing candidates.
 * @throws ArrayIndexOutOfBoundsException if the winner or loser indices in the raire-java
 * assertion are invalid with respect to the given array of candidates.
 */(
    contestName: String,
    universeSize: Long,
    margin: Int,
    difficulty: Double,
    candidates: List<String>,
    nen: NotEliminatedNext

) : ServiceAssertion(contestName, candidates[nen.winner], candidates[nen.loser], margin, universeSize, difficulty, nen.continuing.map { candidates[it] }.toList()) {

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
        val continuing: List<Int> = assumedContinuing.map { o: String? -> candidates.indexOf(o) }

        logger.debug {
            String.format(
                "%s Winner index %d, Loser index %d, assumed continuing %s",
                prefix, w, l, continuing
            )
        }

        // Check for validity of the assertion with respect to the given list of candidate names
        // (w != -1 && l != -1 && Arrays.stream(continuing).noneMatch(c -> c == -1)) {
        if (w != -1 && l != -1 && continuing.find { it == -1 } == null) {
            val status: MutableMap<String, Any> = HashMap()
            status[Metadata.STATUS_RISK] = currentRisk

            logger.debug {
                java.lang.String.format(
                    "%s Constructing AssertionAndDifficulty, current risk %f.",
                    prefix, currentRisk
                )
            }
            return AssertionAndDifficulty(
                NotEliminatedNext(w, l, toIntArray(continuing)),
                difficulty,
                margin,
                status
            )
        } else {
            val msg = String.format(
                "%s Candidate list provided as parameter is inconsistent " +
                        "with assertion (winner or loser or some continuing candidate not present).", prefix
            )
            logger.error { msg }
            throw RaireServiceException(msg, RaireErrorCode.WRONG_CANDIDATE_NAMES)
        }
    }

    override val description: String
        get() = String.format(
            "%s NEN %s, assuming candidates %s are continuing, with diluted margin %f",
            winner, loser, assumedContinuing, dilutedMargin
        )

    override val assertionType: String
        get() = "NEN"

    companion object {
        private val logger = KotlinLogging.logger("NENAssertion")
    }

    init {
        val prefix = "[all args constructor]"
        logger.debug {
            java.lang.String.format(
                "%s Constructed NEN assertion with winner (%d) and loser (%d) " +
                        "indices with respect to candidate list %s: %s. " +
                        "Parameters: contest name %s; margin %d; universe size %d; and difficulty %f.", prefix,
                nen.winner, nen.loser, candidates, this.description,
                contestName, margin, universeSize, difficulty
            )
        }
        if (!assumedContinuing.contains(winner) || !assumedContinuing.contains(loser)) {
            val msg = String.format(
                "%s The winner (%s) and loser (%s) of an NEN assertion must " +
                        "also be continuing candidates. Continuing list: %s. " +
                        "Throwing an IllegalArgumentException.", prefix, winner, loser, assumedContinuing
            )
            logger.error { msg }
            throw IllegalArgumentException(msg)
        }
    }
}
