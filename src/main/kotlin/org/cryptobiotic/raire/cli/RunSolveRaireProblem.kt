package org.cryptobiotic.raire.cli

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.unwrap
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required
import org.cryptobiotic.raire.RaireSolution
import org.cryptobiotic.raire.algorithm.RaireResult
import org.cryptobiotic.raire.assertions.NotEliminatedNext
import org.cryptobiotic.raire.json.readRaireProblemFromFile
import org.cryptobiotic.raire.json.readRaireSolutionFromFile
import org.cryptobiotic.raireservice.util.DoubleComparator

// I think this replaces (and improves on) TestNSW from raire-java
class RunSolveRaiveProblem {

    companion object {
        val logger = KotlinLogging.logger("RunSolveProblem")

        @JvmStatic
        fun main(args: Array<String>) {
            val parser = ArgParser("RunMakeRepository")
            val problem by parser.option(
                ArgType.String,
                shortName = "problem",
                description = "directory containing input json files"
            ).required()
            val solution by parser.option(
                ArgType.String,
                shortName = "solution",
                description = "output directory (default is publicDir)"
            )
            val verbose by parser.option(
                ArgType.Boolean,
                shortName = "verbose",
                description = "show results"
            ).default(false)

            parser.parse(args)
            val info = "RunSolveProblem problem= $problem\n solution= $solution\n"
            logger.info { info }
            solveProblem(problem, solution, verbose)
        }

        fun solveProblem(
            problemFile: String,
            solutionFile: String?,
            verbose: Boolean = false
        ) : Boolean  {
            val raireProblem = readRaireProblemFromFile(problemFile)

            val solution: RaireSolution = raireProblem.solve()
            val solvedResult: RaireResult = solution.solution.unwrap()
            if (verbose) println("solution = $solution")

            if (solutionFile != null) {
                val expectedSolution: RaireSolution = readRaireSolutionFromFile(solutionFile)

                if (expectedSolution.solution is Ok) {
                    val expectedResult: RaireResult = expectedSolution.solution.unwrap()
                    if (verbose) println("expectedSolution = $expectedSolution")
                    return compare(expectedResult, solvedResult)
                } else {
                    println("expectedSolution error = ${expectedSolution.solution}")
                    logger.error { expectedSolution.solution }
                    return false
                }
            }
            return true
        }
    }

}

fun compare(r1: RaireResult, r2: RaireResult): Boolean {
    if (r1.winner != r2.winner) return false
    if (r1.margin != r2.margin) return false
    if (DoubleComparator().compare(r1.difficulty, r2.difficulty) != 0) return false
    if (r1.num_candidates != r2.num_candidates) return false
    r1.assertAndDiff.forEachIndexed { idx, a1 ->
        val a2 = r2.assertAndDiff[idx]
        if (a1.margin != a2.margin) return false
        // if (a1.assertion.javaClass.name != a2.assertion.javaClass.name) return false
    }
    r1.assertAndDiff.forEachIndexed { idx, a1 ->
        if (!a1.assertion.isNEB) {
            val a1n = a1.assertion as NotEliminatedNext
            val a2 = r2.assertAndDiff[idx].assertion
            val a2n = a2 as NotEliminatedNext
            if (a1n.winner != a2n.winner) return false
            if (a1n.loser != a2n.loser) return false
            if (!a1n.continuing.contentEquals(a2n.continuing)) {
                return false
            }
            // if (a1.assertion.javaClass.name != a2.assertion.javaClass.name) return false
        }
    }
    return true
}
