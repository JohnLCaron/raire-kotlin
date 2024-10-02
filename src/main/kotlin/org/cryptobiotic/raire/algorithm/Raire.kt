/*
  Copyright 2023 Democracy Developers
  This is a Java re-implementation of raire-rs https://github.com/DemocracyDevelopers/raire-rs
  It attempts to copy the design, API, and naming as much as possible subject to being idiomatic and efficient Java.

  This file is part of raire-java.
  raire-java is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
  raire-java is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details.
  You should have received a copy of the GNU Affero General Public License along with ConcreteSTV.  If not, see <https://www.gnu.org/licenses/>.

 */
package org.cryptobiotic.raire.algorithm

import org.cryptobiotic.raire.RaireError
import org.cryptobiotic.raire.RaireException
import org.cryptobiotic.raire.assertions.*
import org.cryptobiotic.raire.assertions.AssertionAndDifficulty
import org.cryptobiotic.raire.audittype.AuditType
import org.cryptobiotic.raire.irv.IRVResult
import org.cryptobiotic.raire.irv.Votes
import org.cryptobiotic.raire.pruning.HeuristicWorkOutWhichAssertionsAreUsed
import org.cryptobiotic.raire.pruning.TrimAlgorithm
import org.cryptobiotic.raire.time.TimeOut
import org.cryptobiotic.raire.time.TimeTaken
import java.util.*
import java.util.function.ToIntFunction
import kotlin.math.max

/** The main result of the RAIRE algorithm. This class stores the set of assertions
 * generated by RAIRE for a given contest, alongside information on the time required
 * in various stages of computation and overall statistics indicating the audit difficulty.
 *
 * This class also implements the core RAIRE assertion generation algorithm.
 *
 * @property assertAndDiff Set of AssertionAndDifficulty generated by RAIRE.
 * @property difficulty Overall difficulty of the audit formed for a given contest.
 * @property margin The smallest margin in votes associated with one of the assertions in 'assertions'.
 * @property winner Winner of the contest for which assertions are generated.
 * @property num_candidates Number of candidates in the contest for which assertions are generated.
 * @property time_to_determine_winners The time taken in the first stage of computation: determining the contest winner.
 * @property time_to_find_assertions The time taken in the second stage of computation: generating the assertions.
 * @property time_to_trim_assertions The time taken in the final stage of computation: filtering redundant assertions.
 * @property warning_trim_timed_out A flag indicating if a timeout was experienced in the final computation stage.
 */
class RaireResult(
    val assertAndDiff: List<AssertionAndDifficulty>,
    val difficulty: Double,
    val margin: Int,
    val winner: Int,
    val num_candidates: Int,
    val time_to_determine_winners: TimeTaken,
    val time_to_find_assertions: TimeTaken,
    val time_to_trim_assertions: TimeTaken,
    val warning_trim_timed_out: Boolean = false,
) {

    constructor(
        assertions: List<AssertionAndDifficulty>,
        difficulty: Double,
        margin: Int,
        num_candidates: Int,
    ) : this(
        assertions,
        difficulty,
        margin,
        -1,
        num_candidates,
        TimeTaken(0, 0.0),
        TimeTaken(0, 0.0),
        TimeTaken(0, 0.0),
        false
    )

    override fun toString() = buildString {
        appendLine("difficulty=$difficulty, margin=$margin, winner=$winner, num_candidates=$num_candidates\n  time_to_determine_winners=$time_to_determine_winners, time_to_find_assertions=$time_to_find_assertions, time_to_trim_assertions=$time_to_trim_assertions, warning_trim_timed_out=$warning_trim_timed_out")
        assertAndDiff.forEach { appendLine("  $it") }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RaireResult

        if (!assertAndDiff.equals(other.assertAndDiff)) return false
        if (difficulty != other.difficulty) return false
        if (margin != other.margin) return false
        if (winner != other.winner) return false
        if (num_candidates != other.num_candidates) return false

        return true
    }

    override fun hashCode(): Int {
        var result = assertAndDiff.hashCode()
        result = 31 * result + difficulty.hashCode()
        result = 31 * result + margin
        result = 31 * result + winner
        result = 31 * result + num_candidates
        return result
    }

    companion object {
        /** Finds the easiest to audit assertion that will rule out elimination orders ending in the sequence of candidates
         * 'pi', based on: the cast votes 'votes'; the approach being used to determine assertion difficulty 'audit'; and
         * a cache of difficulty and margins for possible NEB assertions.   */
        fun find_best_audit(
            pi: IntArray,
            votes: Votes,
            audit: AuditType,
            neb_cache: NotEliminatedBeforeCache,
        ): AssertionAndDifficulty {
            val c = pi[0]
            var res: AssertionAndDifficulty = AssertionAndDifficulty(
                NotEliminatedBefore(c, c),
                Double.POSITIVE_INFINITY,
                0
            ) // dummy infinitely bad assertion
            // consider WO contests
            val remaining_pi = Arrays.copyOfRange(pi, 1, pi.size)
            val bestNEB: AssertionAndDifficulty? =
                NotEliminatedBefore.findBestAssertionUsingCache(c, remaining_pi, votes, neb_cache)
            if (bestNEB != null && bestNEB.difficulty < res.difficulty) res = bestNEB
            // consider IRV(c,c′,{c′′ | c′′ ∈ π}): Assertion that c beats some c′ != c ∈ π
            val bestNEN: AssertionAndDifficulty? = NotEliminatedNext.findBestDifficulty(votes, audit, pi, c)
            if (bestNEN != null && bestNEN.difficulty < res.difficulty) res = bestNEN
            return res
        }

        /** A modification to the RAIRE algorithm that searches likely nasty paths first. In practice seems to speed the
         * algorithm up 20-30%  */
        const val USE_DIVING = true
    }
}

/** This is the main RAIRE algorithm... equivalent of the raire() function in rust-rs. The details of the RAIRE
 * algorithm can be found in A Guide to RAIRE Part 2.
 *
 * @param votes Consolidated set of votes cast in the contest.
 * @param claimed_winner Reported winner of the contest, may be null.
 * @param audit Approach being used to measure the difficulty of an assertion.
 * @param trim_algorithm Approach to be used to filter redundant assertions.
 * @param timeout Time limits to be applied on all stages of computation by RAIRE.
 * @throws RaireException If it was impossible to create a suitable set of assertions.
 *      See RaireError for a list of possible causes.
 */
fun runRaire(
    votes: Votes,
    claimed_winner: Int?,
    audit: AuditType,
    trim_algorithm: TrimAlgorithm,
    timeout: TimeOut,
): RaireResult {
    val irv_result: IRVResult = votes.runElection(timeout)
    val time_to_determine_winners = timeout.timeTaken()
    if (irv_result.possibleWinners.size != 1) throw RaireException(RaireError.TiedWinners(irv_result.possibleWinners))

    val winner = irv_result.possibleWinners.get(0)
    if (claimed_winner != null && claimed_winner != winner) throw RaireException(RaireError.WrongWinner(irv_result.possibleWinners))

    val neb_cache = NotEliminatedBeforeCache(votes, audit)
    val massertions = mutableListOf<AssertionAndDifficulty>() // A in the original paper
    var lower_bound = 0.0 // LB in the original paper. A lower bound on the difficulty of the problem.
    val frontier = PriorityQueue<SequenceAndEffort>() // F in the original paper
    var last_difficulty = Double.POSITIVE_INFINITY
    val num_candidates = votes.numCandidates()

    // Populate F with single-candidate sequences
    for (c in 0 until votes.numCandidates()) if (c != winner) { // 4 for each(c ∈ C \ {c w }):
        val pi = intArrayOf(c)
        //  asr[π] ← a ⊲ Record best assertion for π
        val best_assertion_for_pi: AssertionAndDifficulty =
            RaireResult.find_best_audit(pi, votes, audit, neb_cache) // a in the original paper
        //  ba[π] ← π ⊲ Record best ancestor sequence for π
        val best_ancestor_length = pi.size
        frontier.add(
            SequenceAndEffort(
                pi,
                best_assertion_for_pi,
                best_ancestor_length,
                null
            )
        ) // difficulty comes from asr[π].
    }

    // Repeatedly expand the sequence with largest ASN in F
    var sequence_being_considered = frontier.poll()
    while (sequence_being_considered != null) {
        if (timeout.quickCheckTimeout()) throw RaireException(
            RaireError.TimeoutFindingAssertions(
                max(
                    sequence_being_considered.difficulty(),
                    lower_bound
                )
            )
        )
        if (sequence_being_considered.difficulty() != last_difficulty) {
            last_difficulty = sequence_being_considered.difficulty()
            // log::trace!("Difficulty reduced to {}{}",last_difficulty,if last_difficulty<= lower_bound {" OK"} else {""});
        }

        if (sequence_being_considered.difficulty() <= lower_bound) { // may as well just include.
            sequence_being_considered.just_take_assertion(massertions, frontier)
        } else {
            if (RaireResult.USE_DIVING && sequence_being_considered.dive_done == null) {
                var last: SequenceAndEffort? = null
                assert(irv_result.eliminationOrder.size == num_candidates)
                for (i in irv_result.eliminationOrder.size - 1 downTo 0) { // iterate c over candidates in irv_result.eliminationOrder in reverse
                    val c: Int = irv_result.eliminationOrder.get(i)
                    if (Arrays.stream(sequence_being_considered.pi).noneMatch { e: Int -> e == c }) {
                        var new_sequence: SequenceAndEffort
                        if (last != null) { // don't repeat work! Mark that this path has already been dealt with.
                            last.dive_done = c // automatically boxed.
                            frontier.add(last)
                            new_sequence = last.extend_by_candidate(c, votes, audit, neb_cache)
                            last = null
                        } else {
                            sequence_being_considered.dive_done = c
                            new_sequence = sequence_being_considered.extend_by_candidate(c, votes, audit, neb_cache)
                        }
                        if (new_sequence.difficulty() <= lower_bound) {
                            new_sequence.just_take_assertion(massertions, frontier)
                            break
                        } else {
                            last = new_sequence
                        }
                    }
                }
                if (last != null) {
                    assert(last.pi.size == num_candidates)
                    lower_bound = last.contains_all_candidates(massertions, frontier, lower_bound)
                    if (sequence_being_considered.difficulty() <= lower_bound) { // the lower bound may have changed in such a way that there is no point continuing this assertion.
                        sequence_being_considered.just_take_assertion(massertions, frontier)
                        sequence_being_considered = frontier.poll()
                        continue
                    }
                }
            }

            for (c in 0 until num_candidates) { // for each(c ∈ C \ π):
                val finalC = c
                if (!(Arrays.stream(sequence_being_considered.pi)
                        .anyMatch { pc: Int -> pc == finalC } || c == sequence_being_considered.dive_done)
                ) {
                    val new_sequence = sequence_being_considered.extend_by_candidate(c, votes, audit, neb_cache)
                    if (new_sequence.pi.size == num_candidates) { // 22 if (|π′| = |C|):
                        lower_bound = new_sequence.contains_all_candidates(massertions, frontier, lower_bound)
                    } else {
                        frontier.add(new_sequence) // 31 F ← F ∪ {π ′ }
                    }
                }
            }
        }
        sequence_being_considered = frontier.poll()
    }

    val difficulty = lower_bound
    var warning_trim_timed_out = false

    val time_to_find_assertions = timeout.timeTaken().minus(time_to_determine_winners)
    try {
        HeuristicWorkOutWhichAssertionsAreUsed.order_assertions_and_remove_unnecessary(
            massertions,
            winner,
            num_candidates,
            trim_algorithm,
            timeout
        )
    } catch (e: RaireException) {
        if (e.error is RaireError.TimeoutTrimmingAssertions) warning_trim_timed_out = true
        else throw e
    }

    val time_to_trim_assertions =
        timeout.timeTaken().minus(time_to_find_assertions).minus(time_to_determine_winners)
    val margin = massertions.stream()
        .mapToInt(ToIntFunction<AssertionAndDifficulty> { a: AssertionAndDifficulty -> a.margin }).min().orElse(0)
    // simple fast consistency check - make sure that the ostensible elimination order is consistent with all the assertions. If so, then the winner is not ruled out, and all is good.
    massertions.forEach { mass ->
        if (mass.assertion.okEliminationOrderSuffix(irv_result.eliminationOrder) !== EffectOfAssertionOnEliminationOrderSuffix.Ok) throw RaireException(
            RaireError.InternalErrorRuledOutWinner()
        )
    }
    return RaireResult(
        massertions,
        difficulty,
        margin,
        winner,
        num_candidates,
        time_to_determine_winners,
        time_to_find_assertions,
        time_to_trim_assertions,
        warning_trim_timed_out,
    )
}

