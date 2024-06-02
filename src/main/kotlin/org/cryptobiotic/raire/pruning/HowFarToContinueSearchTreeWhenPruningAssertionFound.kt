/*
  Copyright 2023 Democracy Developers
  This is a Java re-implementation of raire-rs https://github.com/DemocracyDevelopers/raire-rs
  It attempts to copy the design, API, and naming as much as possible subject to being idiomatic and efficient Java.

  This file is part of raire-java.
  raire-java is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
  raire-java is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details.
  You should have received a copy of the GNU Affero General Public License along with ConcreteSTV.  If not, see <https://www.gnu.org/licenses/>.

 */
package org.cryptobiotic.raire.pruning

/**
 * When creating a pruning tree, one can keep going beyond a point where there is a pruning assertion
 */
enum class HowFarToContinueSearchTreeWhenPruningAssertionFound {
    /** When a pruning assertion is found, don't look any further. Minimizes size of pruning tree and is fast.  */
    StopImmediately,

    /** When a pruning assertion is found, continue and see if its descendents are sufficient to stop it. But once it is stopped by a frontier of descendents, don't try each of their descendents.  */
    ContinueOnce,

    /** When a pruning assertion is found, continue. Don't stop unless no assertions left.  */
    Forever,

    /** Like forever, but do stop at a pruning assertion if at least one NEB prunes it. This is a useful heuristic as in practice NEBs are almost never redundant but often have very large descendent trees that need searching.  */
    StopOnNEB;

    /** See whether one should continue looking at children if an assertion is found. This may be different if the pruning assertion is a NEB  */
    fun should_continue_if_pruning_assertion_found(pruned_by_neb: Boolean): Boolean {
        return when (this) {
            StopImmediately -> false // never continue
            StopOnNEB -> !pruned_by_neb
            else -> true
        }
    }

    fun next_level_if_pruning_assertion_found(): HowFarToContinueSearchTreeWhenPruningAssertionFound {
        return if (this == ContinueOnce) {
            StopImmediately
        } else this
    }
}
