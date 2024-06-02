/*
  Copyright 2023 Democracy Developers
  This is a Java re-implementation of raire-rs https://github.com/DemocracyDevelopers/raire-rs
  It attempts to copy the design, API, and naming as much as possible subject to being idiomatic and efficient Java.

  This file is part of raire-java.
  raire-java is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
  raire-java is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details.
  You should have received a copy of the GNU Affero General Public License along with ConcreteSTV.  If not, see <https://www.gnu.org/licenses/>.

 */
package org.cryptobiotic.raire.util


import org.cryptobiotic.raire.irv.Vote
import java.util.*

/**
 * A utility class for building an array of Vote[] structures
 * from provided preference lists. The main purpose is to convert
 * a large number of weight votes, possibly the same, into a
 * set of unique votes with multiplicities.
 *
 * It is also (optionally) capable of converting a preference list of
 * strings into the array of integer preferences used by Raire.
 */
class VoteConsolidator {
    /** The map from candidate names to indices. The argument should never be null  */
    private val candidateNameToIndex = HashMap<String, Int>()

    /** The thing being built up. The key is a preference list, the argument is a non-null multiplicity  */
    private val multiplicityByPreferenceList = HashMap<HashableIntArray, Int>()

    /** Use this constructor if you are providing preference lists as an array of integers  */
    constructor()

    /** Use this constructor if you are providing preference lists as an array of integers  */
    constructor(candidateNames: Array<String>) {
        for (i in candidateNames.indices) candidateNameToIndex[candidateNames[i]] = i
    }

    constructor(candidateNames: List<String>) {
        for (i in candidateNames.indices) candidateNameToIndex[candidateNames[i]] = i
    }

    /** Call addVote({0,5,2}) to add a vote first for candidate 0, second for candidate 5, third for candidate 2  */
    fun addVote(preferences: IntArray) {
        val key = HashableIntArray(preferences)
        multiplicityByPreferenceList[key] = multiplicityByPreferenceList.getOrDefault(key, 0) + 1
    }

    @Throws(InvalidCandidateName::class)
    private fun candidateIndex(candidateName: String): Int {
        val res = candidateNameToIndex[candidateName] ?: throw InvalidCandidateName(candidateName)
        return res
    }

    /**
     * Call addVote({"A","B","C"}) to add a vote first for candidate A, second for candidate B, third for candidate C.
     * Uses the order given in the VoteConsolidator(String[] candidateNames) constructor.  */
    @Throws(InvalidCandidateName::class)
    fun addVoteNames(preferences: Array<String>?) {
        val intPreferences =
            Arrays.stream(preferences).mapToInt { candidateName: String -> this.candidateIndex(candidateName) }
                .toArray()
        addVote(intPreferences)
    }

    val votes: List<Vote> // LOOK
        /** Get the votes with appropriate multiplicities  */
        get() = multiplicityByPreferenceList.entries
            .map { entry: Map.Entry<HashableIntArray, Int> -> Vote(entry.value, entry.key.array) }

    /** An error indicating that the provided name was not a listed candidate  */
    class InvalidCandidateName(val candidateName: String) :
        IllegalArgumentException("Candidate $candidateName was not on the list of candidates")

    /** A wrapper around int[] that works as a key in a hash map  */
    private class HashableIntArray(val array: IntArray) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || javaClass != other.javaClass) return false
            val that = other as HashableIntArray
            return array.contentEquals(that.array)
        }

        override fun hashCode(): Int {
            return array.contentHashCode()
        }
    }
}

inline fun <reified T> toArray(list: List<*>): Array<T> {
    return (list as List<T>).toTypedArray()
}
