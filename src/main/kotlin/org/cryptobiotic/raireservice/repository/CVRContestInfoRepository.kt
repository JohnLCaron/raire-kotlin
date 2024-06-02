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

/**
 * Database retrieval of vote data associated with a specific contests on a CVR.
 */
interface CVRContestInfoRepository {
    /**
     * Retrieve the ranked choice data associated with a specific contest in a specific county
     * across all CVRS in the database. If a record in cvr_contest_info has a malformed choices
     * entry (ie. a non-empty string that is not a Json representation for a list), then a
     * JPASystemException will be thrown indicating that an error has occurred in attribute
     * conversion. If a choices column has nulls/blank strings, then a JPASystemException will be
     * also be thrown -- this indicates problems in the database data.
     * @param contestId the ID of the contest
     * @param countyId the ID of the county
     * @return a List of List<String> where each List<String> is a list of ranked choices.
     * @throws JpaSystemException when an error has occurred in the conversion of ranked choice vote
     * entries to an array of strings (most likely because the entry was either null or blank or not
     * a JSON representation of a list).
    </String></String> */
    fun getCVRs(contestId: Long, countyId: Long): List<Array<String>>
}
