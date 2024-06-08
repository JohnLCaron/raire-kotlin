@file:OptIn(ExperimentalSerializationApi::class)

package org.cryptobiotic.raire.json

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import org.cryptobiotic.raire.algorithm.RaireResult
import org.cryptobiotic.raire.assertions.Assertion
import org.cryptobiotic.raire.assertions.AssertionAndDifficulty
import org.cryptobiotic.raire.assertions.NotEliminatedBefore
import org.cryptobiotic.raire.assertions.NotEliminatedNext
import org.cryptobiotic.raire.time.TimeTaken
import org.cryptobiotic.raire.util.toArray
import org.cryptobiotic.raire.util.toIntArray
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

//{
//  "metadata":
//   {"candidates":["HAGARTY Nathan","MORSHED Asm","ANDJELKOVIC Milomir (Michael)","HARLE Peter","MANNOUN Ned"],
//    "contest":"2021 NSW Local Government election for City of Liverpool Mayoral."
//   },
//  "solution":
//    {"Ok":
//      {"assertions": [
//        {"assertion":{"type":"NEB","winner":4,"loser":1},"margin":32364,"difficulty":3.5587999011247065},
//        {"assertion":{"type":"NEB","winner":4,"loser":2},"margin":28000,"difficulty":4.113464285714286},
//        {"assertion":{"type":"NEB","winner":4,"loser":3},"margin":28713,"difficulty":4.011318914777279},
//        {"assertion":{"type":"NEN","winner":4,"loser":0,"continuing":[0,4]},"margin":2536,"difficulty":45.416798107255524}
//        ],
//       "difficulty":45.416798107255524,
//       "margin":2536,
//       "winner":4,
//       "num_candidates":5,
//       "time_to_determine_winners":{"work":5,"seconds":0.000027951},
//       "time_to_find_assertions":{"work":7,"seconds":0.000310609},
//       "time_to_trim_assertions":{"work":16,"seconds":2.5699999999999873e-6}
//      }
//    }
//}


@Serializable
data class RaireSolutionJson(
    val metadata: SolutionMetadata,
    val solution: Solution
) {

    @Serializable
    data class SolutionMetadata(
        val candidates: List<String>,
        val contest: String,
    )

    @Serializable
    data class Solution(
        val Ok: Ok,
    )

    @Serializable
    data class Ok(
        val assertions: List<Assertions>,
        val difficulty: Double,
        val margin: Int,
        val winner: Int,
        val num_candidates: Int,
        val time_to_determine_winners: Time,
        val time_to_find_assertions: Time,
        val time_to_trim_assertions: Time,
    )

    @Serializable
    data class Assertions(
        val assertion: AssertionJson,
        val margin: Int,
        val difficulty: Double,
    ) {
        //     val assertion: Assertion,
        //
        //    /** A measure of how hard this assertion will be to audit. Assertions with a higher difficulty
        //     * will require more ballot samples to check in an audit.  */
        //    val difficulty: Double,
        //
        //    /** Each assertion has a winner, a loser, and a context which determines whether a given
        //     * votes falls into the winner's pile or the loser's. The margin of the assertion is equal to
        //     * the difference in these tallies.  */
        //    val margin: Int,
        fun import() = AssertionAndDifficulty(assertion.import(), difficulty, margin)
    }

    @Serializable
    data class AssertionJson(
        val type: String,
        val winner: Int,
        val loser: Int,
        val continuing: List<Int>?,
    ) {
        fun import(): Assertion {
            return if (type == "NEB")
                NotEliminatedBefore(winner, loser)
            else
                NotEliminatedNext(winner, loser, toIntArray(continuing!!))
        }

    }

    @Serializable
    data class Time(
        val work: Long,
        val seconds: Double,
    ) {
        fun import() = TimeTaken(work, seconds)

    }

    //         assertions: Array<AssertionAndDifficulty>,
    //        difficulty: Double,
    //        margin: Int,
    //        winner: Int,
    //        num_candidates: Int,
    //        time_to_determine_winners: TimeTaken,
    //        time_to_find_assertions: TimeTaken,
    //        time_to_trim_assertions: TimeTaken,
    //        warning_trim_timed_out: Boolean

    fun import(): RaireResult {
        val ok = this.solution.Ok
        val aand: List<AssertionAndDifficulty> = ok.assertions.map{ it.import() }
        return RaireResult(
            toArray(aand),
            ok.difficulty,
            ok.margin,
            ok.winner,
            ok.num_candidates,
            ok.time_to_determine_winners.import(),
            ok.time_to_find_assertions.import(),
            ok.time_to_trim_assertions.import(),
            false
        )
    }
}

fun readRaireSolutionFromFile(filename: String): RaireSolutionJson? {
    val filepath = Path.of(filename)
    if (!Files.exists(filepath)) {
        println("file does not exist")
        return null
    }
    val jsonReader = Json { explicitNulls = false; ignoreUnknownKeys = true }

    Files.newInputStream(filepath, StandardOpenOption.READ).use { inp ->
        val raireSolution = jsonReader.decodeFromStream<RaireSolutionJson>(inp)
        // writeRaireSolution(raireSolution)
        return raireSolution
    }
}

fun writeRaireSolution(raireSolution: RaireSolutionJson) {
    val jsonReader = Json { explicitNulls = false; ignoreUnknownKeys = true; prettyPrint = true }
    jsonReader.encodeToStream(raireSolution, System.out)
}
