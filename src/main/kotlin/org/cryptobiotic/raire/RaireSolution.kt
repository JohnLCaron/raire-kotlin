/*
  Copyright 2023 Democracy Developers
  This is a Java re-implementation of raire-rs https://github.com/DemocracyDevelopers/raire-rs
  It attempts to copy the design, API, and naming as much as possible subject to being idiomatic and efficient Java.

  This file is part of raire-java.
  raire-java is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
  raire-java is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details.
  You should have received a copy of the GNU Affero General Public License along with ConcreteSTV.  If not, see <https://www.gnu.org/licenses/>.

 */
package org.cryptobiotic.raire

import org.cryptobiotic.raire.algorithm.RaireResult

/**  */
data class RaireSolution(
    /** A replication of the metadata provided in RaireProblem. This is designed to include
     * information that is useful for election administrators (to associate with generated assertions)
     * or for assertion visualisation.  */
    val metadata: Map<String, Any>,
    /** If no error arose during assertion generation, this attribute will store the set of generated
     * assertions in the form of a RaireResult. Otherwise, it will provide information on the error
     * in the form of a RaireError.  */
    val solution: RaireResultOrError
) {
    /** A wrapper around the outcome of RAIRE. The outcome is either a RaireResult (if no error arose) or
     * a RaireError (if an error did arise). Exactly one of the fields will be null.  */
    class RaireResultOrError(val Ok: RaireResult?, val Err: RaireError?) {
          constructor(ok: RaireResult): this(ok, null)
          constructor(err: RaireError): this(null, err)

        override fun toString(): String {
            return if (Ok != null) Ok.toString() else Err.toString()
        }


    }
}
