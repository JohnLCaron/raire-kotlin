package org.cryptobiotic.raire.cli

import kotlin.test.Test

class RunSolveProblemTest {

    @Test
    fun testSolveProblem() {
        RunSolveProblem.main(
            arrayOf(
                "-problem", "/home/stormy/dev/github/rla/raire-kotlin/src/test/data/AustralianExamples/NSW Local Government/2021/Byron Mayoral.json",
                "-solution", "/home/stormy/dev/github/rla/raire-kotlin/src/test/data/AustralianExamples/NSW Local Government/2021/Byron Mayoral_out.json",
            )
        )
    }


}

