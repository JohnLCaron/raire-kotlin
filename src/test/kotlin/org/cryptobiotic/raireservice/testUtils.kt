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
package org.cryptobiotic.raireservice

import io.github.oshai.kotlinlogging.KLogger
import org.cryptobiotic.raire.algorithm.RaireResult
import org.cryptobiotic.raire.assertions.AssertionAndDifficulty
import org.cryptobiotic.raire.assertions.NotEliminatedBefore
import org.cryptobiotic.raire.assertions.NotEliminatedNext
import org.cryptobiotic.raireservice.entity.ServiceAssertion
import org.cryptobiotic.raireservice.service.Metadata
import org.cryptobiotic.raireservice.util.DoubleComparator
import java.lang.reflect.Type
import java.math.BigDecimal
import java.util.*

/**
 * Utility methods for use in test classes.
 */
object testUtils {
    /**
     * Comparator for doubles within a specific tolerance.
     */
    private val doubleComparator = DoubleComparator()

    /**
     * Print log statement indicating that a specific test has started running.
     */
    fun log(logger: KLogger, test: String?) {
        logger.debug(String.format("RUNNING TEST: %s.", test))
    }

    /**
     * Utility to check that the response to a get assertions request contains the right metadata. Use
     * this one for API tests, where the value has been serialized & deserialized.
     *
     * @param candidates  the expected list of candidate names
     * @param contestName the expected contest name
     * @param riskLimit   the expected risk limit
     * @param metadata    the metadata from the response, in which the riskLimit is interpreted as a
     * double by the deserializer.
     * @param riskLimitClass the class in which the risk limit is expressed. Use BigDecimal for things
     * derived directly from the service, double for values that have been
     * serialized and deserialized via the API.
     * @return true if the response's metadata fields match the candidates, contestname and riskLimit.
     */
    @Throws(ClassCastException::class)
    fun correctMetadata(
        candidates: List<String>, contestName: String,
        riskLimit: BigDecimal, metadata: Map<String?, Any>, riskLimitClass: Type
    ): Boolean {
        val retrievedRiskLimit = if (riskLimitClass === Double::class.java) {
            BigDecimal.valueOf(metadata[Metadata.RISK_LIMIT] as Double)
        } else if (riskLimitClass === BigDecimal::class.java) {
            metadata[Metadata.RISK_LIMIT] as BigDecimal?
        } else {
            // We can only deal with doubles and BigDecimals.
            return false
        }

        val retrievedContestName = metadata[Metadata.CONTEST].toString()
        val retrievedCandidates = metadata[Metadata.CANDIDATES] as List<String>?

        return contestName == retrievedContestName && riskLimit.compareTo(retrievedRiskLimit) == 0 && setsNoDupesEqual(
            candidates,
            retrievedCandidates
        )
    }

    /**
     * Check that the RaireResult's solution has the expected margin and difficulty.
     * @param margin expected margin
     * @param difficulty expected difficulty
     * @param numAssertions the expected number of assertions
     * @param result the RaireResult in the body of the response
     * @return true if the result's data matches the expected values.
     */
    fun correctSolutionData(
        margin: Int, difficulty: Double, numAssertions: Int,
        result: RaireResult
    ): Boolean {
        val retrievedMargin = result.margin
        val retrievedDifficulty = result.difficulty
        return retrievedMargin == margin && doubleComparator.compare(
            retrievedDifficulty,
            difficulty
        ) == 0 && result.aandd.size == numAssertions
    }

    /**
     * Utility to check the relevant assertion attributes against expected values.
     * @param margin the expected raw margin
     * @param dilutedMargin the expected diluted margin - optional
     * @param difficulty the expected difficulty
     * @param winner the expected winner
     * @param loser the expected loser
     * @param assertion the assertion to be checked (either as an ServiceAssertion or as json)
     * @param assumedContinuing the list of candidate names expected to be in the
     * 'assumed continuing' field.
     * @return true if the assertion's data match all the expected values.
     */
    fun correctDBAssertionData(
        margin: Int, dilutedMargin: Double, difficulty: Double,
        winner: String, loser: String, assumedContinuing: List<String?>?, assertion: ServiceAssertion
    ): Boolean {
        return margin == assertion.margin && doubleComparator.compare(
            difficulty,
            assertion.difficulty
        ) == 0 && doubleComparator.compare(
            dilutedMargin,
            assertion.dilutedMargin
        ) == 0 && loser == assertion.loser && winner == assertion.winner && setsNoDupesEqual(
            assertion.assumedContinuing,
            assumedContinuing
        )
    }

    /**
     * Utility to check the relevant assertion attributes against expected values.
     * @param type the type ("NEN" or "NEB")
     * @param margin the expected raw margin
     * @param difficulty the expected difficulty
     * @param winner the expected winner
     * @param loser the expected loser
     * @param assertionAndDifficulty the (raire-java) assertionAndDifficulty to be checked
     * @param assumedContinuing the list of candidate names expected to be in the
     * 'assumed continuing' field.
     * @return true if the assertion's type and data match all the expected values.
     */
    fun correctAssertionData(
        type: String, margin: Int, difficulty: Double, winner: Int,
        loser: Int, assumedContinuing: List<Int>, risk: Double,
        assertionAndDifficulty: AssertionAndDifficulty
    ): Boolean {
        // Check for the right margin, difficulty and risk

        val rightMarginAndDifficulty =
            (assertionAndDifficulty.margin == margin
                    && (doubleComparator.compare(assertionAndDifficulty.difficulty, difficulty) == 0)
                    && (doubleComparator.compare(assertionAndDifficulty.status!!["risk"] as Double, risk) == 0)
            )

        if (assertionAndDifficulty.assertion is NotEliminatedNext) {
            val nen = assertionAndDifficulty.assertion as NotEliminatedNext
            // If it's an NEN assertion, check that that's the expected type, and that all the other data match
            val nenAssumedContinuing: List<Int> = Arrays.stream(nen.continuing).boxed().toList()
            return (type == "NEN" && nen.winner == winner && nen.loser == loser && setsNoDupesEqual<Int>(
                nenAssumedContinuing,
                assumedContinuing
            )
                    && rightMarginAndDifficulty)
        } else if (assertionAndDifficulty.assertion is NotEliminatedBefore) {
            val neb = assertionAndDifficulty.assertion as NotEliminatedBefore
            // If it's an NEB assertion, check that that's the expected type, that the assumedContinuing
            // list is empty, and that all the other data match.
            return (type == "NEB" && neb.winner == winner && neb.loser == loser && assumedContinuing.isEmpty()
                    && rightMarginAndDifficulty)
        }

        // Not an instance of a type we recognise.
        return false
    }

    /**
     * Returns true if the attributes of the given assertion are equal to those provided as input
     * to this method.
     * @param id Expected assertion id.
     * @param margin Expected assertion (raw) margin.
     * @param dilutedMargin Expected assertion diluted margin.
     * @param difficulty Expected assertion difficulty.
     * @param winner Expected assertion winner.
     * @param loser Expected assertion loser.
     * @param assumedContinuing Expected assumed continuing candidates.
     * @param cvrDiscrepancies Expected map of CVR id to assertion discrepancies.
     * @param estimatedSamplesToAudit Expected number of estimated samples to audit.
     * @param optimisticSamplesToAudit Expected number of optimistic samples to audit.
     * @param twoVoteUnderCount Expected number of two vote understatements.
     * @param oneVoteUnderCount Expected number of one vote understatements.
     * @param oneVoteOverCount Expected number of one vote overstatements.
     * @param twoVoteOverCount Expected number of two vote overstatements.
     * @param otherCount Expected number of 'other' discrepancies.
     * @param currentRisk Expected current risk.
     * @param contestName Expected name of the assertion's contest.
     * @param assertion ServiceAssertion to be checked.
     * @return True if the given assertion's attributes are as expected.
     */
    fun correctDBAssertionData(
        id: Long, margin: Int, dilutedMargin: Double,
        difficulty: Double, winner: String, loser: String, assumedContinuing: List<String?>?,
        cvrDiscrepancies: Map<Long?, Int?>, estimatedSamplesToAudit: Int, optimisticSamplesToAudit: Int,
        twoVoteUnderCount: Int, oneVoteUnderCount: Int, oneVoteOverCount: Int, twoVoteOverCount: Int,
        otherCount: Int, currentRisk: BigDecimal?, contestName: String, assertion: ServiceAssertion
    ): Boolean {
        val test = correctDBAssertionData(
            margin, dilutedMargin, difficulty, winner,
            loser, assumedContinuing, assertion
        )

        return test && assertion.estimatedSamplesToAudit == estimatedSamplesToAudit && assertion.optimisticSamplesToAudit == optimisticSamplesToAudit && assertion.oneVoteUnderCount == oneVoteUnderCount && assertion.oneVoteOverCount == oneVoteOverCount && assertion.twoVoteUnderCount == twoVoteUnderCount && assertion.twoVoteOverCount == twoVoteOverCount && assertion.otherCount == otherCount && assertion.currentRisk.compareTo(
            currentRisk
        ) == 0 && assertion.cvrDiscrepancy == cvrDiscrepancies && assertion.id == id && assertion.contestName == contestName
    }

    /**
     * Check that the max difficulty of a list of assertions matches the expected difficulty.
     * @param expectedDifficulty the expected difficulty, generated by raire-java and raire-rs directly.
     * @param assertions the assertions to be tested
     * @return true iff the maximum difficulty among the assertions equals expectedDifficulty.
     */
    fun difficultyMatchesMax(expectedDifficulty: Double, assertions: List<ServiceAssertion>): Boolean {
        val assertionDifficultyMax = assertions.map { it.difficulty }.max()
        return doubleComparator.compare(assertionDifficultyMax, expectedDifficulty) == 0
    }

    /**
     * Checks whether two lists of strings have no duplicates and the same contents, ignoring order.
     * @param list1 a list of things of type T
     * @param list2 a list of things of type T
     * @return true iff the two lists contain no duplicates and the same set of items, ignoring order.
     */
    private fun <T> setsNoDupesEqual(list1: List<T>, list2: List<T>?): Boolean {
        val list1WithoutDuplicates = list1.stream().distinct().toList()
        val list2WithoutDuplicates = list2!!.stream().distinct().toList()

        return (list1WithoutDuplicates.size == list1.size) // list1 has no duplicates
                && (list2WithoutDuplicates.size == list2.size) // list2 has no duplicates
                && (list1.size == list2.size) // they are the same size
                && list1.containsAll(list2) // and they have the same contents.
    }
}
