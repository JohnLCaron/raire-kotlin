package org.cryptobiotic.raire.cli

import java.nio.file.Files
import java.nio.file.Path
import java.util.function.Predicate
import kotlin.io.path.isDirectory
import kotlin.test.Test
import kotlin.test.assertTrue

class RunSolveProblemTest {

    @Test
    fun testCallingMain() {
        // note doesnt test if we get the same result
        RunSolveRaiveProblem.main(
            arrayOf(
                "-problem",
                "/home/stormy/dev/github/rla/raire-kotlin/src/test/data/AustralianExamples/NSW Local Government/2021/Kempsey Mayoral.json",
                "-solution",
                "/home/stormy/dev/github/rla/raire-kotlin/src/test/data/AustralianExamples/NSW Local Government/2021/Kempsey Mayoral_out.json",
                "--verbose"
            )
        )
    }

    @Test
    fun testSolveOneProblem() {
        val filename = "src/test/data/AustralianExamples/NSW Local Government/2021/Kempsey Mayoral"
        val fileIn = "${filename}.json"
        val fileOut = "${filename}_out.json"

        val same = RunSolveRaiveProblem.solveProblem(fileIn, fileOut)
        println(" solveProblem is same = $same")
        if (!same) {
            RunSolveRaiveProblem.solveProblem(fileIn, fileOut, true)
        }
        assertTrue(same, "RunSolveRaiveProblem.solveProblem not the same")
    }

    @Test
    fun testSolveAllProblems() {
        val dirPath = Path.of("src/test/data/AustralianExamples/NSW Local Government/2021/")
        val files: List<Path> = dirPath.pathListNoDirs { path ->
            !path.toString().endsWith("_out.json")
        }
        println("------------------------")
        files.forEach { path ->
            print("**  '$path'")
            val filename = path.toString()
            val posDot = filename.indexOf('.')
            val filenameOut = filename.take(posDot) + "_out.json"
            val same = RunSolveRaiveProblem.solveProblem(filename, filenameOut)
            println(" solveProblem is same = $same")
            if (!same) {
                RunSolveRaiveProblem.solveProblem(filename, filenameOut, true)
            }
            assertTrue(same, "RunSolveRaiveProblem.solveProblem not the same\n $filename \n $filenameOut) ")
        }
    }
}

fun Path.pathListNoDirs(filter: Predicate<Path>?): List<Path> {
    return Files.walk(this, 1).use { fileStream ->
        fileStream.filter {
            it != this && !it.isDirectory() && (filter == null || filter.test(it))
        }.toList()
    }
}

