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
 * After the RAIRE algorithm has generated the assertions, it is possible that there are redundant assertions.
 *
 * This could happen as the algorithm found some assertion to trim one path, and then later some other
 * assertion is added to trim some other path, but it turns out that it also trims the path trimmed earlier
 * by some other assertion.
 *
 * There are a variety of algorithms for removing redundant assertions. It depends what you want to minimize.
 *
 * Example: In the very simple case given in the "Guide to RAIRE", there
 * are the assertions (and difficulties):
 * ```text
 * A1: 3     NEN: Alice > Diego if only {Alice,Diego} remain
 * A2: 27    NEN: Chuan > Alice if only {Alice,Chuan} remain
 * A3: 27    NEN: Alice > Diego if only {Alice,Chuan,Diego} remain
 * A4: 5.4   NEN: Chuan > Diego if only {Alice,Chuan,Diego} remain
 * A5: 4.5   NEN: Alice > Bob if only {Alice,Bob,Chuan,Diego} remain
 * A6: 3.375 Chuan NEB Bob
 * ```
 *
 * The elimination order `[...Alice,Diego]` is eliminated by `A1`.
 *
 * However, `[...,Bob,Alice,Diego]` is eliminated by `A6`, and
 * `[Chuan,Alice,Diego]` is eliminated by `A4`.
 *
 * So `A1` is technically unnecessary to prove who is elected, and `A6` and `A4`
 * are both needed elsewhere. But `A1` is necessary if you want to minimize
 * the elimination tree size.
 *
 * It is not clear what we want to minimize. A larger number of assertions
 * for a smaller tree is easier for a human to verify (probably), but has
 * a slightly higher chance of requiring an escalation.
 *
 * This gives you options. `MinimizeTree` (and `None`) will leave in `A1`, but
 * `MinimizeAssertions` will remove `A1`.
 */
enum class TrimAlgorithm {
    /** Don't do any trimming  */
    None,

    /** Expand the tree until an assertion rules the path out, removing redundant assertions with a simple heuristic. Minimizes size of tree for human to verify, but may have unnecessary assertions.  */
    MinimizeTree,

    /** Expand the tree until all all assertions are resolved or an NEB rules the path out, and remove redundant assertions with a simple heuristic. Minimizes the number of assertions, but may increase the size of the tree to verify.  */
    MinimizeAssertions,
}
