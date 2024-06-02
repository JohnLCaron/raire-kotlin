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
import org.cryptobiotic.raireservice.entity.Contest
import java.util.function.Predicate

/*
 * Database retrieval for contests, either by name or by (CountyID, contestID) pairs.
 */
interface ContestRepository {
    /**
     * Find and return all contests with a given name from the corla database.
     * Not used except in isAllIRV.
     * Spring syntactic sugar for the obvious SELECT query.
     * @param contestName the name of the contest.
     * @return the contests with that name, as retrieved from the database.
     */
    fun findByName(contestName: String?): List<Contest>

    /**
     * Find and return the first contest with a given name from the corla database.
     * @param contestName the name of the contest.
     * @return the first of that name,
     */
    fun findFirstByName(contestName: String?): Contest?

    /**
     * Find and return contests by contest ID and county ID.
     * Contest ID is unique, so at most one result is possible.
     * @param contestID the ID of the contest
     * @param countyID the ID of the county
     * @return the (singleton or empty) matching contest.
     */
    fun findByContestAndCountyID(contestID: Long, countyID: Long): Contest?

    /**
     * Check whether all the contests of the given name have description 'IRV'.
     * Note it does _not_ test for existence - use findFirstByName for that.
     * @param contestName the name of the contest
     * @return false if there are any non-IRV descriptions for a contest of that name.
     */
    fun isAllIRV(contestName: String): Boolean {
        val prefix = "[isAllIRV]"
        logger.debug {
            String.format(
                "%s (Database access) Finding contests by name (%s).",
                prefix, contestName
            )
        }
        val contests: List<Contest> = findByName(contestName)

        val result = contests.stream()
            .allMatch(Predicate<Contest> { contest: Contest -> contest.description.equals("IRV") })
        logger.debug { String.format("%s Result: %b.", prefix, result) }
        return result
    }

    companion object {
        private val logger = KotlinLogging.logger("ContestRepository")
    }
}