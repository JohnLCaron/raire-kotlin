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

/** A measure of the time taken to do something, both in units of work and clock time.  */
data class TimeTaken(
    /** Time taken in units of 'work'.  */
    val work: Long,
    /** Time taken in clock time (seconds).  */
    val seconds: Double
) {
    /** Get the difference between two times  */
    fun minus(rhs: TimeTaken): TimeTaken {
        return TimeTaken(work - rhs.work, seconds - rhs.seconds)
    }

    override fun toString(): String {
        if (seconds >= 0.99999) {
            return String.format("%.3fs", seconds)
        } else {
            val ms = Math.round(seconds * 1000.0)
            return "" + ms + "ms"
        }
    }
}


