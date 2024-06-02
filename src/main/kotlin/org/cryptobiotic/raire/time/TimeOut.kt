/*
  Copyright 2023 Democracy Developers
  This is a Java re-implementation of raire-rs https://github.com/DemocracyDevelopers/raire-rs
  It attempts to copy the design, API, and naming as much as possible subject to being idiomatic and efficient Java.

  This file is part of raire-java.
  raire-java is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
  raire-java is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details.
  You should have received a copy of the GNU Affero General Public License along with ConcreteSTV.  If not, see <https://www.gnu.org/licenses/>.

 */
package org.cryptobiotic.raire.time

import kotlin.math.ceil

/**
 * A check to see that we are not taking too long.
 * Allows efficient checking against clock time taken or work done.
 */
class TimeOut(
    /** Limit on 'work' done.  */
    private val work_limit: Long?,
    duration_limit_seconds: Double?
) {
    /** Record of the time at which computation, for which a time limit applies, began.  */
    private val start_time_ms = System.currentTimeMillis()

    /** Get the total number of units of work done.  */
    /** Total work done thus far in units 'of work'. Note that work is incremented with each
     * check on whether a timeout has occurred (see TimeOut::quickCheckTimeout()).  */
    var workDone: Long = 0
        private set

    /** Limit on the time, in ms, allowed to RAIRE for its computation.  */
    private val duration_limit_ms = if (duration_limit_seconds == null) null else ceil(duration_limit_seconds * 1000.0)
        .toLong()

    /** Return the time (in ms) since the timer  */
    fun clockTimeTakenSinceStartMillis(): Long {
        return System.currentTimeMillis() - start_time_ms
    }

    /** Return the time taken (as a TimeTaken structure) by RAIRE thus far. This data structure
     * indicates both the time and units of work consumed thus far by RAIRE.  */
    fun timeTaken(): TimeTaken {
        return TimeTaken(workDone, clockTimeTakenSinceStartMillis() / 1000.0)
    }

    /**
     * Increments work_done by 1, and returns true if a limit is exceeded.
     * Only checks duration every 100 calls.
     * @return true if and only if a limit (time or work) has been exceeded.
     */
    fun quickCheckTimeout(): Boolean {
        workDone += 1
        if (work_limit != null && workDone > work_limit) return true
        return duration_limit_ms != null && (workDone % UNITS_OF_WORK_PER_CLOCK_CHECK == 0L) && clockTimeTakenSinceStartMillis() > duration_limit_ms
    }

    companion object {
        /**  In case the clock is expensive to check, only check every UNITS_OF_WORK_PER_CLOCK_CHECK units of work.  */
        const val UNITS_OF_WORK_PER_CLOCK_CHECK: Long = 100

        /** Make a dummy timer that will never timeout.  */
        fun never(): TimeOut {
            return TimeOut(null, null)
        }
    }
}
