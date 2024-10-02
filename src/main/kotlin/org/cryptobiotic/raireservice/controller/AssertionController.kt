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
package org.cryptobiotic.raireservice.controller

import com.github.michaelbull.result.*
import org.cryptobiotic.raire.RaireError

import org.cryptobiotic.raire.RaireSolution
import org.cryptobiotic.raire.algorithm.RaireResult
import org.cryptobiotic.raireservice.RaireErrorCode
import org.cryptobiotic.raireservice.RaireServiceException
import org.cryptobiotic.raireservice.repository.ContestRepository
import org.cryptobiotic.raireservice.request.GenerateAssertionsRequest
import org.cryptobiotic.raireservice.request.GetAssertionsRequest
import org.cryptobiotic.raireservice.response.GenerateAssertionsResponse
import org.cryptobiotic.raireservice.service.GenerateAssertionsService
import org.cryptobiotic.raireservice.service.GetAssertionsCsvService
import org.cryptobiotic.raireservice.service.GetAssertionsJsonService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * This class controls the post request mappings for all requests related to assertions.
 * /generate-assertions takes a generate assertions request (contest by name) and generates the
 * assertions for that contest. In the case of success, it returns the winner
 * and stores the assertions in the database. Otherwise, it returns an error.
 * /get-assertions takes a get assertions request (contest by name) and tries to retrieve
 * the assertions for that contest from the database. In the case of success, it returns
 * the assertions as json, in a form appropriate for the assertion explainer. Otherwise, it
 * returns an error.
 */
class AssertionController(
    private val contestRepository: ContestRepository,
    private val generateAssertionsService: GenerateAssertionsService,
    private val getAssertionsService: GetAssertionsJsonService,
    private val getAssertionsCSVService: GetAssertionsCsvService
) {

    /**
     * The API endpoint for generating assertions, by contest name, and returning the IRV winner as
     * part of a GenerateAssertionsResponse. The raire-java API will be accessed to generate
     * assertions for the contest. If this is successful, these assertions will be stored in the
     * database.
     * @param request a GenerateAssertionsRequest, specifying an IRV contest name for which to generate
     * the assertions.
     * @return the winner (in the case of success) or an error. The winner, together with the contest,
     * is a GenerateAssertionsResponse.
     * @throws RequestValidationException which is handled by ControllerExceptionHandler.
     * This tests for invalid requests, such as non-existent, null, or non-IRV contest names.
     * @throws RaireServiceException for other errors that are specific to assertion generation, such
     * as tied winners or timeouts. These are caught by ControllerExceptionHandler and translated into the
     * appropriate http error.
     */
    fun serve(request: GenerateAssertionsRequest) { // }: ResponseEntity<GenerateAssertionsResponse> {
        val prefix = "[endpoint:generate-assertions]"
        logger.debug(
            java.lang.String.format(
                "%s Assertion generation request received for contest: %s.",
                prefix, request.contestName
            )
        )

        // Validate request: validation errors will be thrown as RequestValidationExceptions to be
        // handled by the ControllerExceptionHandler.
        request.Validate(contestRepository)
        logger.debug(String.format("%s Assertion generation request successfully validated.", prefix))

        // Call raire-java to generate assertions, and check if it was able to do so successfully.
        logger.debug(String.format("%s Calling raire-java with assertion generation request.", prefix))
        val solution: Result<RaireResult, RaireError> = generateAssertionsService.generateAssertions(request)

        if (solution is Ok) {
            // Generation of assertions was successful, now save them to the database.
            logger.debug(
                java.lang.String.format(
                    "%s Assertion generation successful: %d assertions " +
                            "generated in %ss.", prefix, solution.value.assertAndDiff.size,
                    solution.value.time_to_find_assertions.seconds
                )
            )
            generateAssertionsService.persistAssertions(solution.value, request)

            logger.debug(
                java.lang.String.format(
                    "%s Assertions stored in database for contest %s.",
                    prefix, request.contestName
                )
            )

            // Form and return request response.
            val response: GenerateAssertionsResponse = GenerateAssertionsResponse(
                request.contestName,
                request.candidates.get(solution.value.winner)
            )

            logger.debug(String.format("%s Assertion generation and storage complete.", prefix))
            // return ResponseEntity(response, HttpStatus.OK)
        }

        // raire-java was not able to generate assertions successfully.
        if (solution is Err) {
            val msg = "An error occurred in raire-java, yet no error information was returned."
            logger.error(String.format("%s %s", prefix, msg))
            throw RaireServiceException(msg, RaireErrorCode.INTERNAL_ERROR)
        }

        // raire-java returned error information, form and throw an exception using that data. (Note:
        // we need to create the exception first to get a human readable message to log).
        val ex = RaireServiceException.makeFromError(solution.unwrapError(), request.candidates)
        val msg = "An error occurred in raire-java: " + ex.message
        logger.error(String.format("%s %s", prefix, msg))
        throw ex
    }


    /**
     * The API endpoint for finding and returning assertions, by contest name. This endpoint returns
     * assertions in the form of a JSON Visualiser Report.
     * @param request a GetAssertionsRequest, specifying an IRV contest name for which to retrieve the assertions.
     * @return the assertions, as JSON (in the case of success) or an error.
     * @throws RequestValidationException for invalid requests, such as non-existent, null, or non-IRV contest names.
     * @throws RequestValidationException for invalid requests, such as non-existent, null, or non-IRV contest names.
     * @throws RaireServiceException if the request is valid but assertion retrieval fails, for example
     *      if there are no assertions for the contest.
     * These exceptions are handled by ControllerExceptionHandler.
     */
    fun serveJson(request: GetAssertionsRequest) { // ResponseEntity<RaireSolution> {
        val prefix = "[endpoint:get-assertions(json)]"
        logger.debug(
            java.lang.String.format(
                "%s Get assertions request in JSON visualiser format for contest %s with candidates %s.",
                prefix, request.contestName, request.candidates
            )
        )

        // Validate request: errors in the request will be thrown as RequestValidationExceptions that
        // are handled by the ControllerExceptionHandler.
        request.Validate(contestRepository)
        logger.debug(String.format("%s Get assertions request successfully validated.", prefix))

        // Extract a RaireSolution containing the assertions that we want to serialise into
        // a JSON Assertion Visualiser report.
        val solution: RaireSolution = getAssertionsService.getRaireSolution(request)
        logger.debug(String.format("%s Report generated for return.", prefix))

        // return ResponseEntity(solution, HttpStatus.OK)
    }

    /**
     * The API endpoint for finding and returning assertions, by contest name. This endpoint returns
     * assertions as a csv file.
     * @param request a GetAssertionsRequest, specifying an IRV contest name for which to retrieve the
     * assertions.
     * @return the assertions, as a csv file (in the case of success) or an error.
     * @throws RequestValidationException for invalid requests, such as non-existent, null, or non-IRV
     * contest names.
     * @throws RaireServiceException if the request is valid but assertion retrieval fails, for example
     * if there are no assertions for the contest.
     * These exceptions are handled by ControllerExceptionHandler.
     */
    fun serveCSV(request: GetAssertionsRequest) { // }: ResponseEntity<String> {
        // Validate the request.

        request.Validate(contestRepository)

        // Extract the assertions as csv.
        val csv: String = getAssertionsCSVService.generateCSV(request)

        // return ResponseEntity(csv, HttpStatus.OK)
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(AssertionController::class.java)
    }
}
