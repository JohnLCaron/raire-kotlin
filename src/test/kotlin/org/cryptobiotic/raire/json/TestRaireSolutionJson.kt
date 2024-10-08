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

import com.github.michaelbull.result.unwrap
import org.cryptobiotic.raire.RaireSolution
import org.cryptobiotic.raire.algorithm.RaireResult
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class TestRaireSolutionJson {

    @Test
    fun testSolutionSerialization() {
        val solution: RaireSolution = readRaireSolutionFromString(demoJson)
        val result: RaireResult = solution.solution.unwrap()

        val tempFilename = "/home/stormy/temp/test/testSolutionSerialization.json"
        solution.writeToFile(tempFilename)

        val solutionRoundtrip: RaireSolution = readRaireSolutionFromFile(tempFilename)
        val resultRoundtrip: RaireResult = solutionRoundtrip.solution.unwrap()

        assertEquals(result, resultRoundtrip)
        assertEquals(solution, solutionRoundtrip)
    }

    @Test
    fun testExistingSolutionSerialization() {
        val existingFile = "src/test/data/AustralianExamples/NSW Local Government/2021/Kempsey Mayoral_out.json"

        val solution: RaireSolution = readRaireSolutionFromFile(existingFile)
        println("solution file $existingFile\n $solution")

        val tempFilename = "/home/stormy/temp/test/testExistingSolutionSerialization.json"
        solution.writeToFile(tempFilename)

        val solutionRoundtrip: RaireSolution = readRaireSolutionFromFile(tempFilename)
        assertEquals(solution.metadata, solutionRoundtrip.metadata)
        assertEquals(solution.solution, solutionRoundtrip.solution)
        assertEquals(solution, solutionRoundtrip)
    }

}

private val demoJson = """
{
  "metadata": {
    "candidates": [
      "HUNTER Alan",
      "CLARKE Bruce",
      "COOREY Cate",
      "ANDERSON John",
      "MCILRATH Christopher",
      "LYON Michael",
      "DEY Duncan",
      "PUGH Asren",
      "SWIVEL Mark"
    ],
    "contest": "2021 NSW Local Government election for Byron Mayoral."
  },
  "solution": {
    "Ok": {
      "assertions": [
        {
          "assertion": {
            "type": "NEB",
            "winner": 5,
            "loser": 0
          },
          "margin": 2098,
          "difficulty": 8.658245948522403
        },
        {
          "assertion": {
            "type": "NEB",
            "winner": 5,
            "loser": 1
          },
          "margin": 1866,
          "difficulty": 9.734726688102894
        },
        {
          "assertion": {
            "type": "NEB",
            "winner": 5,
            "loser": 3
          },
          "margin": 2808,
          "difficulty": 6.469017094017094
        },
        {
          "assertion": {
            "type": "NEB",
            "winner": 5,
            "loser": 4
          },
          "margin": 3345,
          "difficulty": 5.430493273542601
        },
        {
          "assertion": {
            "type": "NEN",
            "winner": 2,
            "loser": 6,
            "continuing": [
              2,
              6
            ]
          },
          "margin": 2344,
          "difficulty": 7.749573378839591
        },
        {
          "assertion": {
            "type": "NEN",
            "winner": 5,
            "loser": 2,
            "continuing": [
              2,
              5
            ]
          },
          "margin": 2302,
          "difficulty": 7.890964378801042
        },
        {
          "assertion": {
            "type": "NEN",
            "winner": 5,
            "loser": 6,
            "continuing": [
              5,
              6
            ]
          },
          "margin": 2914,
          "difficulty": 6.233699382292381
        },
        {
          "assertion": {
            "type": "NEN",
            "winner": 5,
            "loser": 7,
            "continuing": [
              5,
              7
            ]
          },
          "margin": 2284,
          "difficulty": 7.953152364273205
        },
        {
          "assertion": {
            "type": "NEN",
            "winner": 5,
            "loser": 8,
            "continuing": [
              5,
              8
            ]
          },
          "margin": 1060,
          "difficulty": 17.13679245283019
        },
        {
          "assertion": {
            "type": "NEN",
            "winner": 8,
            "loser": 6,
            "continuing": [
              6,
              8
            ]
          },
          "margin": 1351,
          "difficulty": 13.44559585492228
        },
        {
          "assertion": {
            "type": "NEN",
            "winner": 8,
            "loser": 7,
            "continuing": [
              7,
              8
            ]
          },
          "margin": 1571,
          "difficulty": 11.562698917886696
        },
        {
          "assertion": {
            "type": "NEN",
            "winner": 2,
            "loser": 7,
            "continuing": [
              2,
              7,
              8
            ]
          },
          "margin": 1190,
          "difficulty": 15.264705882352942
        },
        {
          "assertion": {
            "type": "NEN",
            "winner": 5,
            "loser": 2,
            "continuing": [
              2,
              5,
              8
            ]
          },
          "margin": 1235,
          "difficulty": 14.708502024291498
        },
        {
          "assertion": {
            "type": "NEN",
            "winner": 5,
            "loser": 6,
            "continuing": [
              2,
              5,
              6
            ]
          },
          "margin": 3289,
          "difficulty": 5.522955305564001
        },
        {
          "assertion": {
            "type": "NEN",
            "winner": 5,
            "loser": 6,
            "continuing": [
              5,
              6,
              8
            ]
          },
          "margin": 1459,
          "difficulty": 12.450308430431802
        },
        {
          "assertion": {
            "type": "NEN",
            "winner": 5,
            "loser": 7,
            "continuing": [
              2,
              5,
              7
            ]
          },
          "margin": 1803,
          "difficulty": 10.07487520798669
        },
        {
          "assertion": {
            "type": "NEN",
            "winner": 5,
            "loser": 7,
            "continuing": [
              5,
              6,
              7
            ]
          },
          "margin": 2350,
          "difficulty": 7.729787234042553
        },
        {
          "assertion": {
            "type": "NEN",
            "winner": 5,
            "loser": 7,
            "continuing": [
              5,
              7,
              8
            ]
          },
          "margin": 2995,
          "difficulty": 6.065108514190317
        },
        {
          "assertion": {
            "type": "NEN",
            "winner": 6,
            "loser": 7,
            "continuing": [
              6,
              7,
              8
            ]
          },
          "margin": 1257,
          "difficulty": 14.45107398568019
        },
        {
          "assertion": {
            "type": "NEN",
            "winner": 8,
            "loser": 6,
            "continuing": [
              2,
              6,
              8
            ]
          },
          "margin": 1920,
          "difficulty": 9.4609375
        },
        {
          "assertion": {
            "type": "NEN",
            "winner": 8,
            "loser": 7,
            "continuing": [
              6,
              7,
              8
            ]
          },
          "margin": 1556,
          "difficulty": 11.674164524421593
        },
        {
          "assertion": {
            "type": "NEN",
            "winner": 5,
            "loser": 2,
            "continuing": [
              2,
              5,
              6,
              8
            ]
          },
          "margin": 2037,
          "difficulty": 8.917525773195877
        },
        {
          "assertion": {
            "type": "NEN",
            "winner": 5,
            "loser": 6,
            "continuing": [
              2,
              5,
              6,
              7
            ]
          },
          "margin": 2279,
          "difficulty": 7.970601140851251
        },
        {
          "assertion": {
            "type": "NEN",
            "winner": 5,
            "loser": 7,
            "continuing": [
              2,
              5,
              7,
              8
            ]
          },
          "margin": 2478,
          "difficulty": 7.330508474576271
        },
        {
          "assertion": {
            "type": "NEN",
            "winner": 5,
            "loser": 7,
            "continuing": [
              5,
              6,
              7,
              8
            ]
          },
          "margin": 2933,
          "difficulty": 6.193317422434368
        },
        {
          "assertion": {
            "type": "NEN",
            "winner": 8,
            "loser": 7,
            "continuing": [
              2,
              6,
              7,
              8
            ]
          },
          "margin": 1230,
          "difficulty": 14.768292682926829
        },
        {
          "assertion": {
            "type": "NEN",
            "winner": 5,
            "loser": 7,
            "continuing": [
              2,
              5,
              6,
              7,
              8
            ]
          },
          "margin": 2661,
          "difficulty": 6.826381059751973
        }
      ],
      "difficulty": 17.13679245283019,
      "margin": 1060,
      "winner": 5,
      "num_candidates": 9,
      "time_to_determine_winners": {
        "work": 9,
        "seconds": 0.000231217
      },
      "time_to_find_assertions": {
        "work": 187,
        "seconds": 0.016607343
      },
      "time_to_trim_assertions": {
        "work": 442,
        "seconds": 0.00008264299999999913
      }
    }
  }
}
"""
