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

/**
 * Request (expected to be json) identifying a contest by name and listing its candidates.
 * This is an abstract class containing only the core input & validation for contests -
 * just the contest name and list of candidates, plus basic methods to check that they are
 * present, non-null and IRV.
 * Every actual request type inherits from this class and adds some other fields and/or validations.
 */
abstract class ContestRequest(val contestName: String, val candidates: List<String>) {
    init {
        if (contestName.isBlank()) {
            val msg = "No contest name specified. Throwing a RequestValidationException."
            logger.error { msg }
            throw RequestValidationException(msg)
        }

        if (candidates.isEmpty() || candidates.stream().anyMatch { obj: String -> obj.isBlank() }) {
            val msg = "Request for contest $contestName with a bad candidate list $candidates."
            logger.error { msg }
            throw RequestValidationException(msg)
        }
    }

    /**
     * Validates the contest request, checking that the contest exists and is an IRV contest, and
     * that the contest request has candidates. Note it does _not_ check whether the candidates are
     * present in the CVRs.
     * @param contestRepository the respository for getting Contest objects from the database.
     * @throws RequestValidationException if the request is invalid.
     */
    open fun Validate(contestRepository: ContestRepository) {
        val prefix = "[Validate]"
        logger.debug {
            String.format(
                "%s Validating a request to retrieve contest information from the " +
                        "database for contest %s with specified candidates %s.", prefix, contestName, candidates
            )
        }

        if (contestRepository.findFirstByName(contestName) == null) {
            val msg = String.format(
                "%s Request for contest %s. No such contest in database. " +
                        "Throwing a RequestValidationException.", prefix, contestName
            )
            logger.error { msg }
            throw RequestValidationException(msg)
        }

        if (!contestRepository.isAllIRV(contestName)) {
            val msg = String.format(
                "%s Request for contest %s: not comprised of all IRV " +
                        "contests. Throwing a RequestValidationException.", prefix, contestName
            )
            logger.error { msg }
            throw RequestValidationException(msg)
        }

        logger.debug { String.format("%s Request for contest information valid.", prefix) }
    }

    companion object {
        private val logger = KotlinLogging.logger("ContestRequest")
    }
}