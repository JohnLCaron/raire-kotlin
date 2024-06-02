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
package org.cryptobiotic.raireservice.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.cryptobiotic.raireservice.RaireErrorCode
import org.cryptobiotic.raireservice.RaireServiceException
import org.cryptobiotic.raireservice.entity.ServiceAssertion
import org.cryptobiotic.raireservice.repository.AssertionRepository
import org.cryptobiotic.raireservice.request.GetAssertionsRequest
import org.cryptobiotic.raireservice.service.Metadata.CANDIDATES_HEADER
import org.cryptobiotic.raireservice.service.Metadata.CONTEST_NAME_HEADER
import org.cryptobiotic.raireservice.service.Metadata.CURRENT_RISK
import org.cryptobiotic.raireservice.service.Metadata.DIFFICULTY
import org.cryptobiotic.raireservice.service.Metadata.DILUTED_MARGIN
import org.cryptobiotic.raireservice.service.Metadata.ESTIMATED_SAMPLES
import org.cryptobiotic.raireservice.service.Metadata.MARGIN
import org.cryptobiotic.raireservice.service.Metadata.OPTIMISTIC_SAMPLES
import org.cryptobiotic.raireservice.service.Metadata.csvHeaders
import org.cryptobiotic.raireservice.service.Metadata.extremumHeaders
import org.cryptobiotic.raireservice.util.CSVUtils.escapeCsv
import org.cryptobiotic.raireservice.util.CSVUtils.escapeThenJoin
import org.cryptobiotic.raireservice.util.CSVUtils.intListToString
import org.cryptobiotic.raireservice.util.DoubleComparator
import java.math.BigDecimal
import java.util.function.Function

/**
 * Convert a contest's assertions, along with associated metadata, into a csv file for return via
 * the /get-assertions-csv endpoint.
 */
class GetAssertionsCsvService(assertionRepository: AssertionRepository) {
    private val assertionRepository: AssertionRepository = assertionRepository

    /**
     * Generate CSV, main function. The csv includes:
     * - a preface, with metadata about the contest and the assertions
     * - the header row for the csv columns
     * - the actual csv data, one row for each assertion
     * @param request the GetAssertionsRequest, which contains the contest name and candidates.
     * @return the csv as a string.
     * @throws RaireServiceException if assertion retrieval fails, or some other database-related error
     * occurs.
     */
    fun generateCSV(request: GetAssertionsRequest): String {
        val prefix = "[generateCSV]"
        try {
            logger.debug {
                java.lang.String.format(
                    "%s Preparing to export assertions as CSV for contest %s.",
                    prefix, request.contestName
                )
            }

            // Retrieve the assertions.
            val assertions: List<ServiceAssertion> = assertionRepository.getAssertionsThrowError(request.contestName)

            // Sort the assertions by ID. This may be redundant, but it guarantees that they are arranged
            // in a consistent order over multiple csv requests.
            val sortedAssertions
                    : List<ServiceAssertion> =
                assertions.stream().sorted(Comparator.comparingLong<ServiceAssertion>(ServiceAssertion::id)).toList()

            // Write metadata/summary at the top of the file, then the extrema data, then the csv header
            // row, then the assertion data.
            logger.debug {
                String.format(
                    "%s Converting %d assertions into csv format.", prefix,
                    assertions.size
                )
            }
            val preface = makePreface(request)
            val extrema = findExtrema(sortedAssertions)
            val headers: String = escapeThenJoin(csvHeaders)
            val contents = makeContents(sortedAssertions, request.candidates)

            logger.debug { String.format("%s %d assertions translated to csv.", prefix, assertions.size) }
            return """
                $preface$extrema
                
                $headers
                $contents
                """.trimIndent()
        } catch (ex: RaireServiceException) {
            logger.error {
                java.lang.String.format(
                    "%s RaireServiceException caught. Passing to caller: %s",
                    prefix, ex.message
                )
            }
            throw ex
        } catch (e: Exception) {
            logger.error {
                String.format(
                    "%s Generic exception caught. Passing to caller: %s",
                    prefix, e.message
                )
            }
            throw RaireServiceException(e.message ?: "no message", RaireErrorCode.INTERNAL_ERROR)
        }
    }

    /**
     * Find a single extremum (min or max) and return the appropriate extremumResult structure
     * (detailing the value of the extremum and the indices of the assertions that attain it).
     * @param sortedAssertions the assertions, in the sorted order that the indices will reference.
     * These must be non-empty.
     * @param statisticName e.g. margin, diluted margin, estimated samples to audit, current risk.
     * @param type MAX or MIN
     * @param getter the getter for extracting the relevant statistic from a particular assertion.
     * @param comparator an appropriate comparator for the type.
     * @return the extremumResult, including all the data required to make the csv row.
     * @param <T> the type of the statistic. Can be anything on which an appropriate comparator can
     * be defined, but for all the known examples it's a numeric type.
     * @throws NoSuchElementException if called on an empty list of assertions.
    </T> */
    @Throws(NoSuchElementException::class)
    private fun <T> findExtremum(
        sortedAssertions: List<ServiceAssertion>, statisticName: String,
        type: extremumType, getter: Function<ServiceAssertion, T>, comparator: Comparator<T>
    ): extremumResult<T> {
        // Now we know there is at least one assertion. Initialize the extremum with the first
        // assertion's values. Throws NoSuchElementException if the assertion list is empty.

        var extremalAssertions: MutableList<Int> = ArrayList(listOf(1))
        var extremalValue = getter.apply(sortedAssertions.first())

        // Starting from the second assertion, compare each assertion's statistics to the current
        // extremum.
        for (i in 1 until sortedAssertions.size) {
            // Is this assertion's value for this statistic more extremal than our current extremum?
            val statistic = getter.apply(sortedAssertions[i])
            val comparison = comparator.compare(statistic, extremalValue)

            // If this assertion has the same value as the current minimum.
            if (comparison == 0) {
                // Add it to the list of extremal assertions.
                // The human-readable indices start at 1, so we have to add 1.
                extremalAssertions.add(i + 1)

                // If we're looking for MAX and this assertion has a higher value than the current max,
                // or we're looking for MIM and this assertion has a lower value than the current min,
            } else if ((type == extremumType.MAX && comparison > 0) ||
                (type == extremumType.MIN && comparison < 0)
            ) {
                // replace the extremal value and the list of extremal assertions with this one (only) -
                // it is (so far) the unique extremum.
                // The human-readable indices start at 1, so we have to add 1.

                extremalValue = statistic
                extremalAssertions = ArrayList(java.util.List.of(i + 1))
            }
        }

        return extremumResult(statisticName, type, extremalValue, extremalAssertions)
    }

    /**
     * The result of finding the extremum, for one statistic.
     * @param statisticName the name of the statistic.
     * @param type whether this is a max or min.
     * @param value the value of the extremum (max or min).
     * @param indices the indices of the assertions that attain the extremum.
     */
    @JvmRecord
    private data class extremumResult<T>(
        val statisticName: String,
        val type: extremumType,
        val value: T,
        val indices: List<Int>
    ) {
        /**
         * Make the appropriate CSV row for the extremum result: comma-separated statistic name, value,
         * and indices of assertions that produced the extremum.
         * @return a CSV row with the relevant data, as a string.
         */
           fun toCSVRow(): String {
            return escapeThenJoin(java.util.List.of(statisticName, value.toString())) + "," + intListToString(indices)
        }

    }

    /**
     * Extremum type: max or min, so we know whether to search for the biggest or smallest thing.
     */
    private enum class extremumType {
        MAX, MIN
    }

    /**
     * Find the maximum or minimum (whichever is meaningful) for the important statistics: margin,
     * diluted margin, (raire-java estimated) difficulty, optimistic samples to audit, estimated
     * samples to audit.
     *
     * @param sortedAssertions The assertions, assumed to be sorted by ID.
     * @return the CSV rows for all the extrema: margin, diluted margin, difficulty, etc, along with
     * all relevant data (the extremal value and the indices of assertions that attain it).
     */
    private fun findExtrema(sortedAssertions: List<ServiceAssertion>): String {
        val csvRows: MutableList<String> = ArrayList()
        val doubleComparator: DoubleComparator = DoubleComparator()

        // Minimum margin.
        csvRows.add(findExtremum(sortedAssertions,
            MARGIN, extremumType.MIN, ServiceAssertion::margin, Comparator<Int> { x: Int?, y: Int? ->
                Integer.compare(
                    x!!, y!!
                )
            }
        ).toCSVRow())

        // Minimum diluted margin.
        csvRows.add(
            findExtremum(
                sortedAssertions,
                DILUTED_MARGIN, extremumType.MIN, ServiceAssertion::dilutedMargin, doubleComparator
            ).toCSVRow()
        )

        // Maximum difficulty.
        csvRows.add(
            findExtremum(
                sortedAssertions,
                DIFFICULTY, extremumType.MAX, ServiceAssertion::difficulty, doubleComparator
            ).toCSVRow()
        )

        // Maximum current risk.
        csvRows.add(findExtremum(sortedAssertions,
            CURRENT_RISK,
            extremumType.MAX,
            ServiceAssertion::currentRisk,
            Comparator<BigDecimal> { obj: BigDecimal, `val`: BigDecimal? -> obj.compareTo(`val`) }
        ).toCSVRow())

        // Maximum optimistic samples to audit.
        csvRows.add(findExtremum(sortedAssertions,
            OPTIMISTIC_SAMPLES,
            extremumType.MAX,
            ServiceAssertion::optimisticSamplesToAudit,
            Comparator<Int> { x: Int?, y: Int? ->
                Integer.compare(
                    x!!, y!!
                )
            }
        ).toCSVRow())

        // Maximum estimated samples to audit.
        csvRows.add(findExtremum<Int>(sortedAssertions,
            ESTIMATED_SAMPLES,
            extremumType.MAX,
            ServiceAssertion::estimatedSamplesToAudit,
            Comparator<Int> { x: Int?, y: Int? ->
                Integer.compare(
                    x!!, y!!
                )
            }
        ).toCSVRow())

        return java.lang.String.join("\n", csvRows)
    }

    /**
     * Construct the preface of the csv file, including contest metadata (contest name and list of
     * candidates), and headers for the extreme values.
     * @param request the GetAssertionsRequest, used for contest name and candidate list.
     * @return a preface to the CSV file.
     */
    private fun makePreface(request: GetAssertionsRequest): String {
        return ((escapeThenJoin( // The contest name. This gets escaped just in case it contains commas.
            java.util.List.of(CONTEST_NAME_HEADER, escapeCsv(request.contestName))
        ) + "\n"
                + escapeThenJoin( // The list of candidates.
            java.util.List.of(CANDIDATES_HEADER, escapeThenJoin(request.candidates))
        )).toString() + "\n\n"
                + escapeThenJoin(extremumHeaders)).toString() + "\n"
    }

    /**
     * Generate the actual csv data rows for a list of assertions. Each row is prepended with an index
     * number (not related to the database's index) that begins at 1 and increments by 1 with each row.
     * @param assertions a list of assertions
     * @return their concatenated csv rows.
     * @throws RaireServiceException if the candidate names in any of the assertions are inconsistent
     * with the request's candidate list.
     */
    private fun makeContents(assertions: List<ServiceAssertion>, candidates: List<String>): String {
        var index = 1
        val rows: MutableList<String> = ArrayList()

        for (assertion in assertions) {
            rows.add(index++.toString() + "," + escapeThenJoin(assertion.asCSVRow(candidates)))
        }

        return java.lang.String.join("\n", rows) + "\n"
    }

    companion object {
        private val logger = KotlinLogging.logger("GetAssertionsCsvService")
    }
}
