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
import org.cryptobiotic.raire.assertions.AssertionAndDifficulty
import org.cryptobiotic.raire.assertions.NotEliminatedBeforeCache
import org.cryptobiotic.raire.audittype.AuditType
import org.cryptobiotic.raire.irv.Votes
import java.util.*
import java.util.function.Predicate

/**
 * This class refers to an elimination order suffix as 'pi'. This represents a set of possible elimination orders that
 * have a particular ending (eg. pi = [2, 3, 1] is a suffix that captures all elimination orders that end with candidate
 * '1' as the winner, '3' as the runner-up and '2' as the candidate eliminated when only '2', '3' and '1' remain. RAIRE
 * needs to generate assertions that rule out all elimination orders that end in someone other than the reported
 * winner. To do this, RAIRE will generate assertions to rule out a set of elimination order suffixes, where together,
 * those suffixes capture all possible elimination orders with an alternate winner.
 *
 * This class considers a specific elimination order suffix -- a (potentially partial) branch or path in the tree of
 * alternate elimination orders that RAIRE is searching through in order to find assertions to rule out all branches.
 * For any given branch, RAIRE keeps track of the current 'best known' way of ruling out that branch with an assertion.
 * The point in the branch that the assertion attacks is called the 'best ancestor'. This class stores the assertion
 * attacking the branch at this point in the attribute 'best_assertion_for_ancestor' and indicates the point in the
 * branch being attacked in the attribute 'best_ancestor_length'.
 */
internal class SequenceAndEffort(
    /** An elimination order suffix that needs to be ruled out.  */
    val pi: IntArray, best_assertion_for_ancestor: AssertionAndDifficulty,
    /** The best ancestor for pi will be a subset of pi, in particular the last 'best_ancestor_length' elements of pi.
     * This attribute is essentially telling us the point in the elimination order suffix 'pi' we are attacking with the
     * assertion 'best_assertion_for_ancestor'.  */
    val best_ancestor_length: Int,
    /** If not null, then a dive has already been done on the specified candidate. Diving is described in A Guide
     * to RAIRE Part 2. It is an algorthmic feature used to try and ascertain the overall difficulty of an audit
     * earlier in the process of searching for assertions. As RAIRE is searching for a set of assertions that will
     * result in the easiest audit, knowing this information earlier in the process will allow RAIRE to avoid wasting
     * time searching for unnecessarily good ways of ruling out alternate outcomes.  */
    var dive_done: Int?
) : Comparable<SequenceAndEffort> {
    /** The best ancestor for the given suffix 'pi' refers to the point in the suffix that we can attack most cheaply
     * with an assertion. The best assertion we have been found to perform this attack is stored in this attribute.  */
    val best_assertion_for_ancestor: AssertionAndDifficulty = best_assertion_for_ancestor

    /**
     * RAIRE will store elimination order suffixes in a priority queue, ordered according to the difficulty of the best
     * known assertion to attack the suffix at some point. Each suffix is captured in this queue in the form of a
     * SequenceAndEffort object. We want the suffix that is currently most difficult to attack at the front of the queue.
     * (i.e. We want the inverse of the normal ordering on difficulty.)
     *
     * This method compares this suffix with 'other' returning a negative integer result if this suffix has a higher
     * difficulty than other.
     */
    override fun compareTo(other: SequenceAndEffort): Int {
        return java.lang.Double.compare(
            other.best_assertion_for_ancestor.difficulty,
            best_assertion_for_ancestor.difficulty
        )
    }


    /** Returns the difficulty of the assertion being used to attack this elimination order suffix.  */
    fun difficulty(): Double {
        return best_assertion_for_ancestor.difficulty
    }


    /** Get the best ancestor of the elimination order suffix pi, which is a subset of pi. Recall that
     * the best ancestor is the point in the suffix that we can attack most cheaply with an assertion.  */
    fun best_ancestor(): IntArray {
        return Arrays.copyOfRange(pi, pi.size - best_ancestor_length, pi.size)
    }

    /** Add a candidate to the front of the elimination order suffix 'pi', extending our search of the
     * alternate outcome space, and returning a new suffix in the form of a SequenceAndEffort object. When
     * we create a new suffix, we examine where in the suffix we can attack with the cheapest assertion. This
     * determines the best ancestor of the suffix and the assigned assertion.   */
    fun extend_by_candidate(
        c: Int,
        votes: Votes,
        audit: AuditType,
        neb_cache: NotEliminatedBeforeCache
    ): SequenceAndEffort {
        val pi_prime = IntArray(pi.size + 1) // π ′ ← [c] ++π
        pi_prime[0] = c
        System.arraycopy(pi, 0, pi_prime, 1, pi.size)
        val a: AssertionAndDifficulty =
            RaireResult.find_best_audit(pi_prime, votes, audit, neb_cache) // a in the original paper
        val best_ancestor_length = if (a.difficulty < difficulty()) pi_prime.size else this.best_ancestor_length
        val best_assertion_for_ancestor: AssertionAndDifficulty =
            if (a.difficulty < difficulty()) a else this.best_assertion_for_ancestor
        return SequenceAndEffort(pi_prime, best_assertion_for_ancestor, best_ancestor_length, null)
    }

    /** Called when we want to take the assertion attacking this elimination order suffix,
     * and add it to the list of assertions in our audit, 'assertions'. This method checks that the
     * assertion is not already in our audit. The method also looks for other suffixes in our priority queue (our
     * frontier, as described in A Guide to RAIRE Part 2) that can be obviously attacked by the assertion as
     * the suffix ruled out by this assertion is a suffix of the element of the frontier. Those
     * suffixes will be removed from the frontier.
     *
     * There may be other elements of the frontier that would be ruled out by the assertion,
     * but checking for these would likely take longer than just leaving them in the frontier.
     * Leaving them in is not a serious problem as they will be processed as soon as they come up
     * without any further expansion as there will exist at least one assertion (i.e. this one) with
     * a difficulty no higher than the highest seen so far (which includes this one).  */
    fun just_take_assertion(
        assertions: MutableList<AssertionAndDifficulty>,
        frontier: PriorityQueue<SequenceAndEffort>
    ) {
        // If the assertion is already in the list, don't bother adding it again. Could be faster if a hash map is used, but complicates the Assertion classes, and is not a significant time sink.
        for (a in assertions) {
            if (a.assertion.equals(best_assertion_for_ancestor.assertion)) {
                return  // don't add assertion as it was already there.
            }
        }
        // 15 F ← F \ {π ′ ∈ F | ba[π] is a suffix of π ′ }
        // This step is just an optimization.
        //  * 503, 482, 511 ms to run TestNSW with this,
        //  * 508, 533, 510ms to run TestNSW without this. It is probably marginally useful.
        val best_ancestor_pi = best_ancestor()
        val isASuffixOfBestAncestorPi: Predicate<SequenceAndEffort> = Predicate<SequenceAndEffort> { s ->
            val pi = s.pi
            val offset = pi.size - best_ancestor_pi.size
            if (offset < 0) return@Predicate false
            for (i in best_ancestor_pi.indices) if (pi[offset + i] != best_ancestor_pi[i]) return@Predicate false
            true
        }
        frontier.removeIf(isASuffixOfBestAncestorPi)
        // 14 A ← A ∪ {asr[ba[π]]}
        assertions.add(best_assertion_for_ancestor)
    }

    /** Called when a sequence has gone as far as it can - i.e. we have reached a 'leaf', where all candidates are in
     * the exclusion order list 'pi'. Returns a new lower bound (on the cost of the overall audit), or throws an
     * exception if we could not rule out the alternate outcome defined by the candidate sequence.  */
    fun contains_all_candidates(
        assertions: MutableList<AssertionAndDifficulty>,
        frontier: PriorityQueue<SequenceAndEffort>,
        lower_bound: Double
    ): Double {
        var lowerBound = lower_bound
        if (java.lang.Double.isInfinite(difficulty())) { // 23 if (ASN (asr[ba[π ′ ]]) = ∞):
            //println!("Couldn't deal with {:?}",new_sequence.pi);
            throw RaireException(RaireError.CouldNotRuleOut(pi)) // 24 terminate algorithm, full recount necessary
        } else {
            if (lowerBound < difficulty()) {
                lowerBound = difficulty() // 27 LB ← max(LB, ASN (asr[ba[π′]]))
                // log::trace!("Found bound {} on elimination sequence {:?}",*bound,self.pi)
            }
            just_take_assertion(assertions, frontier) // Steps 26 and 28 are same as 14 and 15.
            return lowerBound
        }
    }
}
