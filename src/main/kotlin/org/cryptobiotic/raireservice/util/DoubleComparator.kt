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
package org.cryptobiotic.raireservice.util

import kotlin.math.abs

/**
 * A double comparison with EPS precision. Useful when finding extrema for assertions.
 * Returns
 * 0 when x and y are within EPS,
 * -1 when x < y (by more than EPS),
 * +1 when x > y (by more than EPS).
 */
class DoubleComparator : Comparator<Double> {
    override fun compare(x: Double, y: Double): Int {
        if (abs(x - y) < EPS) {
            return 0
        } else if (x < y) {
            return -1
        }
        return 1
    }

    companion object {
        /** Error allowed when comparing doubles. */
        const val EPS: Double = 0.0000001
    }
}
