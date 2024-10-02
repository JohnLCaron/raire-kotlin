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


/**
 * Everything that could go wrong in Raire. Typically, this will be returned as a thrown RaireException with this as its argument.
 * It is implemented as a class rather than an Exception hierarchy to facilitate detailed error serialization.
 */
abstract class RaireError {
    /** The RAIRE algorithm is given an integer number of candidates. This must be at least one.
     * If a negative or 0 value is provided as the number of candidates, then the InvalidNumberOfCandidates
     * error is generated. Don't get this confused with the InvalidCandidateNumber error below, which is a
     * vote for a candidate who doesn't exist.  */
    class InvalidNumberOfCandidates : RaireError()

    /** The RAIRE algorithm is given a time limit in seconds to limit its runtime.
     * If a negative or NaN value is provided as the time limit, then the InvalidTimout
     * error is generated.  */
    class InvalidTimeout : RaireError()

    /** RAIRE treats votes as a list of integers between 0 (inclusive) and the number of
     * candidates (exclusive). If any vote provided to raire-java, has some other (invalid)
     * integer an InvalidCandidateNumber error is generated.  */
    class InvalidCandidateNumber : RaireError()

    /** There are three stages of computation involved in assertion generation, the
     * first of which is finding out who won (usually very fast). However, if
     * this step exceeds the provided time limit then the TimeoutCheckingWinner error
     * will be generated. All three stages must be completed within the specified time limit
     * or a relevant timeout error will be generated.  */
    class TimeoutCheckingWinner : RaireError()

    /** If assertion generation (usually the slowest of the three stages of computation)
     * does not complete within the specified time limit, the TimeoutFindingAssertions error
     * will be generated. All three stages must be completed within the specified time limit
     * or a relevant timeout error will be generated.  */
    class TimeoutFindingAssertions(val difficultyAtTimeOfStopping: Double) : RaireError()

    /** After generating assertions, a filtering stage will occur in which redundant
     * assertions are removed from the final set. This stage is usually reasonably fast.
     * However, if this stage does not complete within the specified time limit, the
     * TimeoutTrimmingAssertions error will be generated. All three stages must be completed
     * within the specified time limit or a relevant timeout error will be generated. */
    class TimeoutTrimmingAssertions : RaireError()

    /** If RAIRE determines that the contest has multiple possible winners consistent with
     * the rules of IRV (i.e. there is a tie) then the TiedWinners error will be generated.
     * While the particular legislation governing the contest may have unambiguous tie
     * resolution rules, there is no way that an RLA could be helpful if the contest comes
     * down to a tie resolution.  */
    class TiedWinners(val expected: IntArray) : RaireError()

    /** If RAIRE is called with a specified winner, and upon checking RAIRE determines
     * that the provided winner does not match the votes (according to its own tabulation),
     * the WrongWinner error will be generated.  */
    class WrongWinner(val expected: IntArray) : RaireError()

    /** If RAIRE determines that it is not possible to compute a set of assertions because
     * there are no assertions that would rule out a particular elimination order, then a
     * CouldNotRuleOut error is generated.  */
    class CouldNotRuleOut(val eliminationOrder: IntArray) : RaireError()

    /** Sanity checks are conducted in various locations in raire-java to ensure that
     * the code is operating as intended. An InternalErrorRuledOutWinner error will be
     * generated if the set of generated assertions actually rule out the reported winner
     * (i.e. the assertions are invalid).  */
    class InternalErrorRuledOutWinner : RaireError()

    /** Sanity checks are conducted in various locations in raire-java to ensure that
     * the code is operating as intended. An InternalErrorDidntRuleOutLoser error is
     * generated if the set of assertions formed does not rule out at least one
     * reported loser (i.e. the assertions are invalid).  */
    class InternalErrorDidntRuleOutLoser : RaireError()

    /** Sanity checks are conducted in various locations in raire-java to ensure that
     * the code is operating as intended. An InternalErrorTrimming error is generated
     * if a problem has arisen during the filtering of redundant assertions.  */
    class InternalErrorTrimming : RaireError()
}
