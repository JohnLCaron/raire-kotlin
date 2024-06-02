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

/**
 * Each CVR contains vote information for a series of contests. A CVRContestInfo will contain
 * the vote information for a specific contest on a specific CVR. For the purpose of
 * assertion-generation and retrieval by RAIRE, we only care about the choice data, contest and
 * county IDs associated with a given entry in the cvr_contest_info table.
 */
data class CVRContestInfo(

    /**
     * Unique identifier for a CVRContestInfo instance. It is comprised of a
     * CVR ID and a contest ID. These two together form a unique key.
     */
    val cVRId: Long = 0,

    /**
     * Unique identifier for a CVRContestInfo instance. It is comprised of a
     * CVR ID and a contest ID. These two together form a unique key.
     */
    val contestId: Long = 0,

    /**
     * Ranked order of choices on the CVR for the relevant contest (in order of most preferred
     * to least). This is stored in a single String in the database (for efficiency), and converted
     * into an array of String for later processing.
     */
    val choices: Array<String>,

    /**
     * ID for the county associated with the contest associated with this CVRContestInfo.
     */
    val countyId: Long = 0,
)
