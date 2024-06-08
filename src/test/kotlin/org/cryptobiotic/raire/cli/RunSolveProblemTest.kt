package org.cryptobiotic.raire.cli

import java.nio.file.Files
import java.nio.file.Path
import java.util.function.Predicate
import kotlin.io.path.isDirectory
import kotlin.test.Test
import kotlin.test.assertTrue

class RunSolveProblemTest {

    @Test
    fun testSolveProblem() {
        RunSolveRaiveProblem.main(
            arrayOf(
                "-problem", "/home/stormy/dev/github/rla/raire-kotlin/src/test/data/AustralianExamples/NSW Local Government/2021/Byron Mayoral.json",
                "-solution", "/home/stormy/dev/github/rla/raire-kotlin/src/test/data/AustralianExamples/NSW Local Government/2021/Byron Mayoral_out.json",
                "--verbose"
            )
        )
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
            val success = RunSolveRaiveProblem.solveProblem(
                filename,
                filenameOut,
            )
            println(" solution is the same = $success")
            assertTrue(success)
        }
    }


}

fun Path.pathListNoDirs(filter: Predicate<Path>?): List<Path> {
    return Files.walk(this, 1).use { fileStream ->
        fileStream.filter {
            it != this && !it.isDirectory() &&  (filter == null || filter.test(it)) }.toList()
    }
}

