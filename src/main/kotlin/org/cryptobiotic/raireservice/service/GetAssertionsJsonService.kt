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

import io.github.oshai.kotlinlogging.KotlinLogging
import org.cryptobiotic.raire.RaireSolution
import org.cryptobiotic.raire.assertions.AssertionAndDifficulty
import org.cryptobiotic.raire.util.toArray
import org.cryptobiotic.raireservice.RaireErrorCode
import org.cryptobiotic.raireservice.RaireServiceException
import org.cryptobiotic.raireservice.repository.AssertionRepository
import org.cryptobiotic.raireservice.request.GetAssertionsRequest
import org.cryptobiotic.raireservice.response.RaireResultMixIn
import java.util.*
import java.util.function.Function
import java.util.function.IntFunction
import java.util.function.ToDoubleFunction
import java.util.function.ToIntFunction

/**
 * Collection of functions responsible for retrieving assertions from the colorado-rla database,
 * through the use of an AssertionRepository, and packaging them in a form suitable for export
 * in desired forms. Currently, assertions are packaged and returned in the form of a RaireSolution.
 * Assertions are retrieved for a contest as specified in a GetAssertionsRequest.
 */
class GetAssertionsJsonService(private val assertionRepository: AssertionRepository) {

    /**
     * Given a request to retrieve assertions for a given contest, return these assertions as part
     * of a RaireSolution. This RaireSolution may be serialised to produce a JSON export suitable
     * for use by an assertion visualiser.
     * @param request Request to retrieve assertions for a specific contest.
     * @return A RaireSolution containing any assertions generated for the contest in the request.
     * @throws RaireServiceException when no assertions exist for the contest, or an error has
     * arisen during retrieval of assertions.
     */
    fun getRaireSolution(request: GetAssertionsRequest): RaireSolution {
        val prefix = "[getRaireSolution]"
        try {
            logger.debug {
                java.lang.String.format(
                    "%s Preparing to build a RaireSolution for serialisation into " +
                            "assertion visualiser report for contest %s.", prefix, request.contestName
                )
            }

            // Retrieve the assertions.
            val assertions = assertionRepository.getAssertionsThrowError(request.contestName)

            // Create contest metadata map, supplied as input when creating a RaireResult.
            logger.debug {
                java.lang.String.format(
                    "%s Creating contest metadata map (candidates: %s), " +
                            "risk limit (%s), and contest name (%s).", prefix, request.candidates, request.riskLimit,
                    request.contestName
                )
            }
            val metadata = mutableMapOf<String, Any>()
            metadata[Metadata.CANDIDATES] = request.candidates
            metadata[Metadata.RISK_LIMIT] = request.riskLimit
            metadata[Metadata.CONTEST] = request.contestName

            // Translate the assertions extracted from the database into AssertionAndDifficulty objects,
            // keeping track of the maximum difficulty and minimum margin.
            logger.debug {
                String.format(
                    "%s Converting %d assertions into raire-java format.", prefix,
                    assertions.size
                )
            }
            val translated= mutableListOf<AssertionAndDifficulty>()
            for (a in assertions) {
                translated.add(a.convert(request.candidates))
            }

            logger.debug {
                String.format(
                    "%s %d assertions translated to json.", prefix,
                    assertions.size
                )
            }

            // Get maximum difficulty and minimum margin across assertions.
            val maxDifficulty = if (translated.isEmpty()) 0.0 else translated.map{ it.difficulty }.max()
            logger.debug { String.format("%s Maximum difficulty across assertions: %f.", prefix, maxDifficulty) }

            val minMargin = if (translated.isEmpty()) 0 else translated.map{ it.margin }.min()
            logger.debug { String.format("%s Minimum margin across assertions: %d.", prefix, minMargin) }

            // Using a version of RaireResult in which certain attributes will be ignored in serialisation.
            val result = RaireResultMixIn( toArray(translated), maxDifficulty, minMargin, request.candidates.size)

            val solution = RaireSolution(metadata, RaireSolution.RaireResultOrError(result))
            logger.debug { String.format("%s Constructed RaireSolution for return and serialisation.", prefix) }
            return solution
        } catch (ex: RaireServiceException) {
            logger.error {
                String.format(
                    "%s RaireServiceException caught. Passing to caller: %s",
                    prefix, ex.message
                )
            }
            throw ex
        } catch (ex: Exception) {
            logger.error {
                String.format(
                    "%s Generic exception caught. Passing to caller: %s",
                    prefix, ex.message
                )
            }
            throw RaireServiceException(ex.message!!, RaireErrorCode.INTERNAL_ERROR)
        }
    }

    companion object {
        private val logger = KotlinLogging.logger("GetAssertionsJsonService")
    }
}
