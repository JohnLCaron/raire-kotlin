package org.cryptobiotic.raire.cli

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.required
import org.cryptobiotic.raire.algorithm.RaireResult
import org.cryptobiotic.raire.json.readRaireProblemFromFile
import org.cryptobiotic.raire.json.readRaireSolutionFromFile
import org.cryptobiotic.raireservice.util.DoubleComparator

class RunSolveProblem {

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

            parser.parse(args)

            val info = "RunSolveProblem problem= $problem\n" +
                    " solution= $solution\n"
            logger.info { info }
            runSolveProblem(problem, solution)
        }

        fun runSolveProblem(
            problemFile: String,
            solutionFile: String?,
        ) {
            val raireProblem = readRaireProblemFromFile(problemFile)!!.import()

            val solution = raireProblem.solve()
            println("solution = $solution")

            if (solutionFile != null) {
                val expected = readRaireSolutionFromFile(solutionFile)!!.import()
                println("expected = $expected")

                println("equivalent= ${compare(expected,solution.solution.Ok!!)}")

            }

        }
    }

}

fun compare(r1: RaireResult, r2: RaireResult): Boolean {
    if (r1.winner != r2.winner) return false
    if (r1.margin != r2.margin) return false
    if (DoubleComparator().compare(r1.difficulty, r2.difficulty) != 0) return false
    if (r1.num_candidates != r2.num_candidates) return false
    r1.assertions.forEachIndexed { idx, a1 ->
        val a2 = r2.assertions[idx]
        if (a1.margin != a2.margin) return false
        // if (a1.assertion.javaClass.name != a2.assertion.javaClass.name) return false
    }
    return true
}
