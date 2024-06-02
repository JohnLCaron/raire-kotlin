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
import org.cryptobiotic.raire.RaireProblem
import org.cryptobiotic.raire.RaireSolution
import org.cryptobiotic.raire.algorithm.RaireResult
import org.cryptobiotic.raire.audittype.BallotComparisonOneOnDilutedMargin
import org.cryptobiotic.raire.pruning.TrimAlgorithm
import org.cryptobiotic.raire.util.VoteConsolidator
import org.cryptobiotic.raire.util.toArray
import org.cryptobiotic.raireservice.RaireServiceException
import org.cryptobiotic.raireservice.RaireErrorCode
import org.cryptobiotic.raireservice.entity.Contest
import org.cryptobiotic.raireservice.repository.AssertionRepository
import org.cryptobiotic.raireservice.repository.CVRContestInfoRepository
import org.cryptobiotic.raireservice.repository.ContestRepository
import org.cryptobiotic.raireservice.request.GenerateAssertionsRequest

/**
 * This class contains functionality for generating assertions for a given contest by calling
 * raire-java, and persisting those assertions to the colorado-rla database.
 */
class GenerateAssertionsService(
    cvrContestInfoRepository: CVRContestInfoRepository,
    contestRepository: ContestRepository, assertionRepository: AssertionRepository
) {
    private val cvrContestInfoRepository: CVRContestInfoRepository = cvrContestInfoRepository

    private val contestRepository: ContestRepository = contestRepository

    private val assertionRepository: AssertionRepository = assertionRepository

    /**
     * Given a request to generate assertions for a contest, this method collects all CVR vote
     * data for that contest, consolidates it into raire-java Votes, and accesses the raire-java
     * API to form assertions. A RaireResultOrError is returned containing either the successfully
     * generated assertions or error information detailing reasons why they were not generated.
     * @param request Assertions generation request specifying the contest name and candidates.
     * @return A RaireResultOrError containing either the generated assertions or error details
     * indicating why assertion generation was not successful.
     * @throws RaireServiceException if any vote data for a contest was found to be invalid (i.e.,
     * it referred to candidates that were not in the expected list) or an error arose in database
     * access.
     */
    fun generateAssertions(request: GenerateAssertionsRequest): RaireSolution.RaireResultOrError {
        val prefix = "[generateAssertions]"
        try {
            logger.debug {
                java.lang.String.format(
                    "%s Preparing to generate assertions for contest %s. Request " +
                            "parameters: candidate list (%s); total auditable ballots (%d); and time limit (%f)",
                    prefix, request.contestName, request.candidates, request.totalAuditableBallots,
                    request.timeLimitSeconds
                )
            }

            // Check that the contest exists and is all IRV. Otherwise this is an internal error because
            // it should be caught before here.
            if (contestRepository.findFirstByName(request.contestName) == null
                || !contestRepository.isAllIRV(request.contestName)
            ) {
                val msg = java.lang.String.format(
                    "%s Contest %s does not exist or is not all IRV", prefix,
                    request.contestName
                )
                logger.error { msg + "Throwing a RaireServiceException." }
                throw RaireServiceException(msg, RaireErrorCode.INTERNAL_ERROR)
            }

            // Use raire-java to consolidate the votes, collecting all votes with the same ranking
            // together and representing that collection as a single ranking with an associated number
            // denoting how many votes with that ranking exist.
            val consolidator = VoteConsolidator(request.candidates)

            // First extract all county level contests matching the contest name in the request. For
            // these contests, extract CVR vote data from the database, and add those votes to the
            // vote consolidator.
            logger.debug {
                java.lang.String.format(
                    "%s (Database access) Collecting all vote rankings for contest " +
                            "%s from CVRs in database.", prefix, request.contestName
                )
            }
            val votes: List<Array<String>> = contestRepository.findByName(request.contestName)
                .map { c : Contest -> cvrContestInfoRepository.getCVRs(c.contestID, c.countyID) }.flatten()

            if (votes.size > request.totalAuditableBallots) {
                val msg = java.lang.String.format(
                    "%s %d votes present for contest %s but a universe size of "
                            + "%d specified in the assertion generation request. Throwing a RaireServiceException.",
                    prefix, votes.size, request.contestName, request.totalAuditableBallots
                )
                logger.error { msg }
                throw RaireServiceException(msg, RaireErrorCode.INVALID_TOTAL_AUDITABLE_BALLOTS)
            }

            if (votes.isEmpty()) {
                val msg = java.lang.String.format(
                    "%s No votes present for contest %s.", prefix,
                    request.contestName
                )
                logger.error { "$msg Throwing a RaireServiceException." }
                throw RaireServiceException(msg, RaireErrorCode.NO_VOTES_PRESENT)
            }

            logger.debug {
                String.format(
                    "%s Adding all extracted rankings to a consolidator to identify " +
                            "unique rankings and their number.", prefix
                )
            }
            votes.forEach(consolidator::addVoteNames)

            logger.debug { String.format("%s Votes consolidated.", prefix) }

            // If the extracted votes are valid, get raire-java to generate assertions.
            // First, form a metadata map containing contest details.
            val metadata: MutableMap<String, Any> = HashMap()
            metadata[Metadata.CANDIDATES] = request.candidates
            metadata[Metadata.CONTEST] = request.contestName

            // Create the RaireProblem containing all information raire-java needs.
            logger.debug {
                java.lang.String.format(
                    "%s Creating the RaireProblem to provide to raire-java with " +
                            "parameters: candidates (%s); contest name (%s); number of candidates (%d); " +
                            "total auditable ballots (%d); minimize assertions trimming algorithm; and time limit %f.",
                    prefix, request.candidates, request.contestName, request.candidates.size,
                    request.totalAuditableBallots, request.timeLimitSeconds
                )
            }
            val raireProblem = RaireProblem(
                metadata, toArray(consolidator.votes), request.candidates.size, null,
                BallotComparisonOneOnDilutedMargin(request.totalAuditableBallots),
                TrimAlgorithm.MinimizeAssertions, null, request.timeLimitSeconds
            )

            // Tell raire-java to generate assertions, returning a RaireSolutionOrError.
            logger.debug { String.format("%s Calling raire-java.", prefix) }
            val result: RaireSolution.RaireResultOrError = raireProblem.solve().solution

            // Log fact that raire-java returned; more details about result will be logged in the caller.
            logger.debug { String.format("%s raire-java returned result; passing to controller.", prefix) }
            return result
        } catch (ex: VoteConsolidator.InvalidCandidateName) {
            val msg = java.lang.String.format(
                "%s Invalid vote sent to RAIRE for contest %s. %s",
                prefix, request.contestName, ex.message
            )
            logger.error { msg }
            throw RaireServiceException(msg, RaireErrorCode.WRONG_CANDIDATE_NAMES)
        } catch (ex: RaireServiceException) {
            val msg = java.lang.String.format(
                "%s A RaireServiceException was caught; passing to caller. %s",
                prefix, ex.message
            )
            logger.error { msg }
            throw ex
        } /* catch (ex: DataAccessException) { // TODO from Spring
            val msg = java.lang.String.format(
                "%s A data access exception arose when extracting " +
                        "CVR/Contest data for contest %s. %s", prefix, request.contestName, ex.getMessage()
            )
            logger.error { msg }
            throw RaireServiceException(msg, RaireErrorCode.INTERNAL_ERROR)
        } */ catch (ex: Exception) {
            val msg = String.format(
                "%s An exception arose when generating assertions. %s",
                prefix, ex.message
            )
            logger.error { msg }
            throw RaireServiceException(msg, RaireErrorCode.INTERNAL_ERROR)
        }
    }

    /**
     * Given successfully generated assertions stored within a RaireResult, persist these
     * assertions to the database.
     * @param solution RaireResult containing assertions to persist for a given contest.
     * @param request Assertions generation request containing contest information.
     * @throws RaireServiceException when an error arises in either the translation of
     * raire-java assertions into a form suitable for saving to the database, or in persisting these
     * translated assertions to the database.
     */
    fun persistAssertions(solution: RaireResult, request: GenerateAssertionsRequest) {
        val prefix = "[persistAssertions]"
        try {
            // Delete any existing assertions for this contest.
            logger.debug {
                java.lang.String.format(
                    "%s (Database access) Proceeding to delete any assertions " +
                            "stored for contest %s (if present).", prefix, request.contestName
                )
            }
            assertionRepository.deleteByContestName(request.contestName)

            // Persist assertions formed by raire-java.
            logger.debug {
                java.lang.String.format(
                    "%s Proceeding to translate and save %d assertions to the " +
                            "database for contest %s.", prefix, solution.assertionAndDifficulties.size, request.contestName
                )
            }
            assertionRepository.translateAndSaveAssertions(
                request.contestName,
                request.totalAuditableBallots.toLong(), toArray(request.candidates), solution.assertionAndDifficulties
            )

            logger.debug { String.format("%s Assertions persisted.", prefix) }
        } catch (ex: IllegalArgumentException) {
            val msg = String.format(
                "%s Invalid arguments were supplied to " +
                        "AssertionRepository::translateAndSaveAssertions. This is likely either a non-positive " +
                        "universe size, invalid margin, or invalid combination of winner, loser and list of " +
                        "assumed continuing candidates. %s", prefix, ex.message
            )
            logger.error { msg }
            throw RaireServiceException(msg, RaireErrorCode.INTERNAL_ERROR)
        } catch (ex: ArrayIndexOutOfBoundsException) {
            val msg = String.format(
                "%s Array index out of bounds access in " +
                        "AssertionRepository::translateAndSaveAssertions. This was likely due to a winner " +
                        "or loser index in a raire-java assertion being invalid with respect to the " +
                        "candidates list for the contest. %s", prefix, ex.message
            )
            logger.error { msg }
            throw RaireServiceException(msg, RaireErrorCode.INTERNAL_ERROR)
        } /* catch (ex: DataAccessException) { TODO Spring
            val msg = java.lang.String.format(
                "%s Data access exception arose when persisting assertions. %s",
                prefix, ex.getMessage()
            )
            logger.error { msg }
            throw RaireServiceException(msg, RaireErrorCode.INTERNAL_ERROR)
        } */ catch (ex: Exception) {
            val msg = String.format(
                "%s An exception arose when persisting assertions. %s",
                prefix, ex.message
            )
            logger.error { msg }
            throw RaireServiceException(msg, RaireErrorCode.INTERNAL_ERROR)
        }
    }

    companion object {
        private val logger = KotlinLogging.logger("GenerateAssertionsService")
    }
}
