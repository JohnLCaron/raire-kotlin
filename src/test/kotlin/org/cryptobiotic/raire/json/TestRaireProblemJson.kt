/*
  Copyright 2023 Democracy Developers
  This is a Java re-implementation of raire-rs https://github.com/DemocracyDevelopers/raire-rs
  It attempts to copy the design, API, and naming as much as possible subject to being idiomatic and efficient Java.

  This file is part of raire-java.
  raire-java is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
  raire-java is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details.
  You should have received a copy of the GNU Affero General Public License along with ConcreteSTV.  If not, see <https://www.gnu.org/licenses/>.

 */
package org.cryptobiotic.raire.json

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.unwrap
import org.cryptobiotic.raire.RaireProblem
import org.cryptobiotic.raire.RaireSolution
import org.cryptobiotic.raire.algorithm.RaireResult
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class TestRaireProblemJson {

    @Test
    fun testSerialization() {
        val demoJson = """{
  "metadata": {
    "candidates": ["Alice", "Bob", "Chuan","Diego" ],
    "note" : "Anything can go in the metadata section. Candidates names are used below if present. "
  },
  "num_candidates": 4,
  "votes": [
    { "n": 5000, "prefs": [ 2, 1, 0 ] },
    { "n": 1000, "prefs": [ 1, 2, 3 ] },
    { "n": 1500, "prefs": [ 3, 0 ] },
    { "n": 4000, "prefs": [ 0, 3 ] },
    { "n": 2000, "prefs": [ 3 ]  }
  ],
  
  "winner": 2,
  "trim_algorithm": "MinimizeTree",
  "audit": { "type": "OneOnMargin", "total_auditable_ballots": 13500  }
}"""
        val problem: RaireProblem = readRaireProblemFromString(demoJson)
        val solution: RaireSolution = problem.solve()
        assertTrue(solution.solution is Ok)
        val result: RaireResult = solution.solution.unwrap()
        assertEquals(27.0, result.difficulty, 1e-6)

        val tempFilename = "/home/stormy/temp/test/testSerialization.json"
        problem.writeToFile(tempFilename)

        val problemRoundtrip: RaireProblem = readRaireProblemFromFile(tempFilename)
        assertEquals(problem.metadata, problemRoundtrip.metadata)
        assertEquals(problem.audit, problemRoundtrip.audit)
        assertEquals(problem.votes, problemRoundtrip.votes)
        assertEquals(problem.trim_algorithm, problemRoundtrip.trim_algorithm)
        assertEquals(problem, problemRoundtrip)
    }

    @Test
    fun testExistingSerialization() {
        val existingFile = "/home/stormy/dev/github/rla/raire-kotlin/src/test/data/AustralianExamples/NSW Local Government/2021/Ballina Mayoral.json"

        val problem: RaireProblem = readRaireProblemFromFile(existingFile)
        val tempFilename = "/home/stormy/temp/test/testExistingSerialization.json"
        problem.writeToFile(tempFilename)

        val problemRoundtrip: RaireProblem = readRaireProblemFromFile(tempFilename)
        assertEquals(problem.metadata, problemRoundtrip.metadata)
        assertEquals(problem.audit, problemRoundtrip.audit)
        assertEquals(problem.votes, problemRoundtrip.votes)
        assertEquals(problem.trim_algorithm, problemRoundtrip.trim_algorithm)
        assertEquals(problem, problemRoundtrip)
    }

}
