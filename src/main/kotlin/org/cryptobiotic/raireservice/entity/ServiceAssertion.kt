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
package org.cryptobiotic.raireservice.entity

import io.github.oshai.kotlinlogging.KotlinLogging
import org.cryptobiotic.raire.assertions.AssertionAndDifficulty
import org.cryptobiotic.raireservice.RaireErrorCode
import org.cryptobiotic.raireservice.RaireServiceException
import org.cryptobiotic.raireservice.util.CSVUtils.escapeThenJoin
import java.math.BigDecimal
import java.text.DecimalFormat

/**
 * RAIRE (raire-java) generates a set of assertions for a given IRV contest. The different types of
 * assertion that RAIRE can generate are defined as subclasses of this base Assertion class. For a
 * description of what assertions are and the role they play in an IRV audit, see the Guide to
 * RAIRE. This class has ReadOnlyProperty annotations against attributes as raire-service creates
 * assertions to be stored in the database, but never modified existing assertions that are present
 * in the database. The only type of 'modification' that the raire-service will do, if required,
 * is delete assertions from the database for a specific contest, re-generate them, and store
 * the new assertions in the database.
 */
abstract class ServiceAssertion {
    /**
     * Get the id. Used for sorting when assertions are output as csv.
     * @return the id.
     */
    val id: Long = 0

    /**
     * Version. Used for optimistic locking.
     */
    private val version: Long = 0

    /**
     * Get the name of the contest to which this assertion belongs.
     * @return the assertion's contest (name).
     */
    var contestName: String? = null
        protected set

    /**
     * Get the winner by name. Used for CSV output.
     * @return the winner.
     */
    var winner: String? = null
        protected set

    /**
     * Get the loser by name. Used for CSV output.
     * @return the loser.
     */
    var loser: String? = null
        protected set

    /**
     * Get the margin. Used for CSV output.
     * Assertion margin (note: this is not the *diluted* margin).
     */
    var margin: Int = 0
        protected set

    /**
     * Get the difficulty. Used for CSV output.
     * @return the difficulty.
     */
    /**
     * Assertion difficulty, as estimated by raire-java. (Note that raire-java has multiple ways
     * of estimating difficulty, and that these measurements are not necessarily in terms of numbers
     * of ballots. For example, one method may be: difficulty =  1 / assertion margin).
     */
    var difficulty: Double = 0.0
        protected set

    /**
     * List of candidates that the Assertion assumes are 'continuing' in the Assertion's context.
     * Note that this is always empty for NEB assertions.
     */
    var assumedContinuing: List<String?> = ArrayList()
        protected set

    /**
     * Diluted margin for the Assertion. This is equal to the assertion margin divided by the
     * number of ballots in the relevant auditing universe.
     */
    var dilutedMargin: Double = 0.0
        protected set

    /**
     * A map between CVR ID and the discrepancy recorded against that CVR for this assertion
     * in the assertions contest, if one exists. CVRs are only present in this map if
     * a discrepancy exists between it and the paper ballot in the assertions contest.
     */
    var cvrDiscrepancy: Map<Long, Int> = HashMap()
        protected set

    /**
     * The expected number of samples to audit overall for the Assertion, assuming overstatements
     * continue at the current rate experienced in the audit.
     */
    var estimatedSamplesToAudit: Int = 0
        protected set

    /**
     * The expected number of samples to audit overall for the Assertion, assuming no further
     * overstatements will be encountered in the audit.
     */
    var optimisticSamplesToAudit: Int = 0
        protected set

    /**
     * The two-vote understatements recorded against the Assertion.
     */
    var twoVoteUnderCount: Int = 0
        protected set

    /**
     * The one-vote understatements recorded against the Assertion.
     */
    var oneVoteUnderCount: Int = 0
        protected set

    /**
     * The one-vote overstatements recorded against the Assertion.
     */
    var oneVoteOverCount: Int = 0
        protected set

    /**
     * The two-vote overstatements recorded against the Assertion.
     */
    var twoVoteOverCount: Int = 0
        protected set

    /**
     * Discrepancies recorded against the Assertion that are neither understatements nor
     * overstatements.
     */
    var otherCount: Int = 0
        protected set

    /**
     * Current risk measurement recorded against the Assertion. It is initialized to 1, as prior
     * to an audit starting, and without additional information, we assume maximum risk.
     */
    var currentRisk = BigDecimal.valueOf(1)

    /**
     * Construct an Assertion for a specific contest.
     * @param contestName Contest for which the Assertion has been created.
     * @param winner Winner of the Assertion (name of a candidate in the contest).
     * @param loser Loser of the Assertion (name of a candidate in the contest).
     * @param margin Absolute margin of the Assertion.
     * @param universeSize Total number of ballots in the auditing universe of the Assertion.
     * @param difficulty Assertion difficulty, as computed by raire-java.
     * @param assumedContinuing List of candidates, by name, that the Assertion assumes is continuing.
     * @throws IllegalArgumentException if the caller supplies a non-positive universe size, invalid
     * margin, or invalid combination of winner, loser and list of assumed continuing candidates.
     */
    constructor(
        contestName: String,
        winner: String,
        loser: String,
        margin: Int,
        universeSize: Long,
        difficulty: Double,
        assumedContinuing: List<String> = emptyList(),
    ) {
        val prefix = "[all args constructor]"
        logger.debug {
            String.format(
                "%s Parameters: contest name %s; winner %s; loser %s; " +
                        "margin %d; universe size %d; difficulty %f; assumed continuing %s.", prefix,
                contestName, winner, loser, margin, universeSize, difficulty, assumedContinuing
            )
        }

        this.contestName = contestName
        this.winner = winner
        this.loser = loser
        this.margin = margin

        if (universeSize <= 0) {
            val msg = String.format(
                "%s An assertion must have a positive universe size " +
                        "(%d provided). Throwing an IllegalArgumentException.", prefix, universeSize
            )
            logger.error { msg }
            throw IllegalArgumentException(msg)
        }

        if (margin < 0 || margin > universeSize) {
            val msg = String.format(
                "%s An assertion must have a non-negative margin that is " +
                        "less than universe size (margin of %d provided with universe size %d). " +
                        "Throwing an IllegalArgumentException.", prefix, margin, universeSize
            )
            logger.error { msg }
            throw IllegalArgumentException(msg)
        }

        if (winner == loser) {
            val msg = String.format(
                "%s The winner and loser of an assertion must not be the same " +
                        "candidate (%s provided for both). Throwing an IllegalArgumentException.", prefix, winner
            )
            logger.error { msg }
            throw IllegalArgumentException(msg)
        }

        this.dilutedMargin = margin / universeSize.toDouble()

        this.difficulty = difficulty
        this.assumedContinuing = assumedContinuing

        logger.debug { "$prefix Diluted margin computed: $dilutedMargin. Construction complete." }
    }

    /**
     * Construct and return a raire-java representation of this Assertion. This utility is
     * ultimately used to construct an assertions report export in the same format that raire-java
     * exports. This report is formed by serialising a RaireSolution object which itself contains
     * assertions as AssertionAndDifficulty objects.
     * @param candidates The candidates in this assertion's contest. TODO non-null?
     * @return a representation of this Assertion as an AssertionAndDifficulty object.
     * @throws IllegalArgumentException when the provided candidate list is inconsistent with the
     * data stored in the assertion.
     */
    abstract fun convert(candidates: List<String?>): AssertionAndDifficulty // TODO changed

    /**
     * Return as a list of strings intended for a CSV row, in the same order as the csvHeaders in
     * Metadata.java.
     * Note that some of these (such as names and numbers > 999) may have commas - the receiving
     * function needs to apply escapeThenJoin.
     * Floating-point numbers are formatted to 4 d.p, except the (BigDecimal) current risk, which is
     * given to its full precision.
     * @return The assertion data, as a list of csv-escaped strings.
     * @throws RaireServiceException with error code WRONG_CANDIDATE_NAMES if the winner, loser or any of
     * the assumed_continuing candidates are not in the input candidate list.
     */
    fun asCSVRow(candidates: List<String?>): List<String> {
        val prefix = "[asCSVRow]"
        val fm = DecimalFormat("0.0###")

        if (candidates.contains(winner) && candidates.contains(loser)
            && candidates.containsAll(assumedContinuing)
        ) {
            return java.util.List.of(
                assertionType,
                winner,
                loser,
                escapeThenJoin(assumedContinuing),
                fm.format(difficulty),
                margin.toString() + "",
                fm.format(dilutedMargin),
                currentRisk.toString(),
                estimatedSamplesToAudit.toString() + "",
                optimisticSamplesToAudit.toString() + "",
                twoVoteOverCount.toString() + "",
                oneVoteOverCount.toString() + "",
                otherCount.toString() + "",
                oneVoteUnderCount.toString() + "",
                twoVoteUnderCount.toString() + ""
            )
        } else {
            val msg = "$prefix Candidate list provided as parameter is inconsistent " +
                        "with assertion (winner or loser or some continuing candidate not present)."
            logger.error { msg }
            throw RaireServiceException(msg, RaireErrorCode.WRONG_CANDIDATE_NAMES)
        }
    }

    /** Return a description of the Assertion in a human-readable format. */
    abstract val description: String

    /** Print the assertion type, either NEN or NEB. */
    abstract val assertionType: String

    companion object {
        private val logger = KotlinLogging.logger("ServiceAssertion")
    }
}