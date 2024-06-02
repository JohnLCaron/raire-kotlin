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
package org.cryptobiotic.raireservice.response

import org.cryptobiotic.raire.algorithm.RaireResult
import org.cryptobiotic.raire.assertions.AssertionAndDifficulty
import org.cryptobiotic.raire.time.TimeTaken

/**
 * Mixin for the raire-java class RaireResult. When raire-service serialises RaireResult as part of a
 * RaireSolution, we want to ignore some RaireResult attributes as this data is not stored in
 * the colorado-rla database.
 * The only purpose of this class is to ignore certain fields of RaireResult when serializing.
 */

/**
 * Constructor - make a RaireResultMixin from the attributes we care about and intend to serialize.
 * Create the superclass by inserting default values for the unspecified fields (winner, times taken,
 * trim warning) - these will not be serialized.
 * @param assertions the assertions with difficulties.
 * @param difficulty the highest difficulty.
 * @param margin the lowest margin.
 * @param num_candidates the number of candidates in the contest.
 */
class RaireResultMixIn(
    assertions: Array<AssertionAndDifficulty>,
    difficulty: Double,
    margin: Int,
    num_candidates: Int
) : RaireResult(
    assertions, difficulty, margin, -1, num_candidates, TimeTaken(0, 0.0),
    TimeTaken(0, 0.0), TimeTaken(0, 0.0), false
)
