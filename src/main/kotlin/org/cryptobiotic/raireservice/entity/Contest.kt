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
 * A contest class used for reading contest data out of the corla database.
 * This class omits the fields that are not relevant to input validation - we only care about
 * checking whether any requests raire-service receives for assertion generation or retrieval make
 * sense and are valid.
 */

data class Contest(
    val contestID: Long = 0,
    val name: String? = null,
    val description: String? = null,
    val countyID: Long = 0,
    val version: Long = 0,
)