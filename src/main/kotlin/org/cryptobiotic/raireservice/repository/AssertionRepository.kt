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
package org.cryptobiotic.raireservice.repository

import io.github.oshai.kotlinlogging.KotlinLogging
import org.cryptobiotic.raire.assertions.AssertionAndDifficulty
import org.cryptobiotic.raire.assertions.NotEliminatedBefore
import org.cryptobiotic.raire.assertions.NotEliminatedNext
import org.cryptobiotic.raireservice.entity.ServiceAssertion
import org.cryptobiotic.raireservice.RaireServiceException
import org.cryptobiotic.raireservice.RaireErrorCode
import org.cryptobiotic.raireservice.entity.NEBAssertion
import org.cryptobiotic.raireservice.entity.NENAssertion

/**
 * Database retrieval and storage for Assertions.
 */
interface AssertionRepository {
    /**
     * Retrieve all Assertions from the database belonging to the contest with the given name.
     * @param contestName Name of the contest whose assertions being retrieved.
     */
    fun findByContestName(contestName: String?): List<ServiceAssertion>

    /**
     * Delete all Assertions belonging to the contest with the given name from the database. This
     * is Spring syntactic sugar for the corresponding 'delete' query.
     * @param contestName The name of the contest whose assertions are to be deleted.
     * @return The number of records deleted from the database.
     */
    fun deleteByContestName(contestName: String?): Long

    // from JpaRepository superclass
    fun saveAll(translated: List<ServiceAssertion>)

    /**
     * For the given collection of raire-java assertions, transform them into a form suitable
     * for storing in the corla database and save them to the database. Note that this method will
     * not verify that the provided array of candidate names are the candidates for the contest or
     * that the names themselves are valid, as stored in the database, or that a contest of the
     * given name exists.
     * @param contestName Name of the contest to which these assertions belong.
     * @param universeSize Number of ballots in the auditing universe for these assertions.
     * @param candidates Names of the candidates in the contest.
     * @param assertions Array of raire-java assertions for the contest.
     * @throws IllegalArgumentException if the caller supplies a non-positive universe size,
     * invalid margin, or invalid combination of winner, loser and list of assumed continuing candidates.
     * @throws ArrayIndexOutOfBoundsException if the winner or loser indices in any of the raire-java
     * assertions are invalid with respect to the given array of candidates.
     */
    fun translateAndSaveAssertions(
        contestName: String,
        universeSize: Long,
        candidates: Array<String>,
        assertions: Array<AssertionAndDifficulty>
    ) {
        val prefix = "[translateAndSaveAssertions]"
        logger.debug {
            String.format(
                "%s Translating and saving %s raire-java assertions to the " +
                        "database. Additional parameters: contest name %s; universe size %d; and candidates %s.",
                prefix, assertions.size, contestName, universeSize, candidates.contentToString()
            )
        }

        val translated: List<ServiceAssertion> = assertions.map { a ->
            if (a.assertion.isNEB) {
                return@map NEBAssertion(
                    contestName, universeSize, a.margin, a.difficulty,
                    candidates, a.assertion as NotEliminatedBefore
                )
            } else {
                return@map NENAssertion(
                    contestName, universeSize, a.margin, a.difficulty,
                    candidates, a.assertion as NotEliminatedNext
                )
            }
        }.toList()

        logger.debug { String.format("%s Translation complete.", prefix) }
        logger.debug { String.format("%s (Database access) Proceeding to save generated assertions.", prefix) }
        this.saveAll(translated)
        logger.debug { String.format("%s Save all complete.", prefix) }
    }

    /**
     * Find and return the list of assertions generated for the given contest, throwing a
     * RaireServiceException with error code NO_ASSERTIONS_PRESENT when no assertions have been
     * generated for the contest.
     * @param contestName Name of the contest for which to return assertions.
     * @return The list of assertions generated for the contest with name 'contestName'
     * @throws RaireServiceException when no assertions have been generated for the given contest.
     */
    fun getAssertionsThrowError(contestName: String?): List<ServiceAssertion> {
        val prefix = "[getAssertionsThrowError]"
        logger.debug {
            String.format(
                "%s (Database access) Retrieve all assertions for contest %s.",
                prefix, contestName
            )
        }

        // Retrieve the assertions.
        val assertions: List<ServiceAssertion> = findByContestName(contestName)

        // If the contest has no assertions, return an error.
        if (assertions.isEmpty()) {
            val msg = String.format(
                "%s No assertions have been generated for the contest %s.",
                prefix, contestName
            )
            logger.error { msg }
            throw RaireServiceException(msg, RaireErrorCode.NO_ASSERTIONS_PRESENT)
        }

        return assertions
    }

    companion object {
        private val logger = KotlinLogging.logger("AssertionRepository")
    }
}
