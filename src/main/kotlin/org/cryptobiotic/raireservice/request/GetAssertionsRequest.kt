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
package org.cryptobiotic.raireservice.request

import io.github.oshai.kotlinlogging.KotlinLogging
import org.cryptobiotic.raireservice.repository.ContestRepository
import java.math.BigDecimal

/**
 * Request (expected to be json) identifying the contest for which assertions should be retrieved
 * from the database (expected to be exported as json).
 * This extends ContestRequest and uses the contest name and candidate list, plus validations,
 * from there. A GetAssertionsRequest identifies a contest by name along with the candidate list
 * (which is necessary for producing the metadata for later visualization). riskLimit states the
 * risk limit for the audit. This is not actually used in raire-service computations,
 * but will be output later with the assertion export, so that it can be used in the assertion
 * visualizer. Validation consists only of checking that the request is reasonable, including calling
 * ContestRequest.Validate to check that the contest exists and is all IRV, and that the candidate
 * names are reasonable. GetAssertionsRequest.Validate then checks that the risk limit is non-negative.
 */
class GetAssertionsRequest(contestName: String, candidates: List<String>, riskLimit: BigDecimal) :
    ContestRequest(contestName, candidates) {

    /** The risk limit for the audit, expected to be in the range [0,1]. */
    val riskLimit: BigDecimal = riskLimit

    /**
     * Validates the request to retrieve assertions for the contest, checking that the contest exists
     * and is an IRV contest, that the risk limit has a sensible value, and that there are candidates.
     * Note it does _not_ check whether the candidates are present in the CVRs.
     * @param contestRepository the repository for getting Contest objects from the database.
     * @throws RequestValidationException if the request is invalid.
     */
    override fun Validate(contestRepository: ContestRepository) {
        val prefix = "[Validate]"
        logger.debug {
            String.format(
                "%s Validating a Get Assertions Request for contest %s " +
                "with specified candidates %s and risk limit %s.", prefix, contestName, candidates, riskLimit
            )
        }
        super.Validate(contestRepository)

        // Check for a negative risk limit. Risk limits >1 are vacuous but not illegal.
        // Risk limits of exactly zero are unattainable but will not cause a problem.
        if (riskLimit.compareTo(BigDecimal.ZERO) < 0) {
            val msg = String.format(
                "%s Null or negative risk limit specified in request (%s). "
                        + "Throwing a RequestValidationException.", prefix, riskLimit
            )
            logger.error { msg }
            throw RequestValidationException(msg)
        }

        logger.debug { String.format("%s Get Assertions Request validated.", prefix) }
    }

    companion object {
        private val logger = KotlinLogging.logger("GetAssertionsRequest")
    }
}

