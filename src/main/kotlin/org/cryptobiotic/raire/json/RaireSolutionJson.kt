@file:OptIn(ExperimentalSerializationApi::class)

package org.cryptobiotic.raire.json

import com.github.michaelbull.result.*

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream

import org.cryptobiotic.raire.RaireError
import org.cryptobiotic.raire.RaireError.*
import org.cryptobiotic.raire.RaireSolution
import org.cryptobiotic.raire.algorithm.RaireResult
import org.cryptobiotic.raire.assertions.Assertion
import org.cryptobiotic.raire.assertions.AssertionAndDifficulty
import org.cryptobiotic.raire.assertions.NotEliminatedBefore
import org.cryptobiotic.raire.assertions.NotEliminatedNext
import org.cryptobiotic.raire.time.TimeTaken
import org.cryptobiotic.raire.util.toArray
import org.cryptobiotic.raire.util.toIntArray
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.collections.map

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
    val metadata: RaireMetadataJson?,
    val solution: OkOrErrorJson,  // not my favorite design
) {
    fun import() = RaireSolution(this.metadata?.import(), this.solution.import())
}
fun RaireSolution.publishJson(): RaireSolutionJson {
    val sj: OkOrErrorJson = publishRRaireResultJson(this.solution)
    return RaireSolutionJson(this.metadata?.publishJson(), sj)
}

@Serializable
data class OkOrErrorJson(
    val Ok: RaireResultJson?,
    val Error: RaireErrorJson?,
) {
    fun import(): Result<RaireResult, RaireError> {
        return if (this.Ok != null) Ok(this.Ok.import())
        else if (this.Error != null) Err(this.Error.import())
        else throw RuntimeException("RaireSolutionJson must have result or error")
    }
}

fun publishRRaireResultJson(rr: Result<RaireResult, RaireError>): OkOrErrorJson {
    var resultj: RaireResultJson? = null
    var errorj: RaireErrorJson? = null

    if (rr is Ok) {
        val result = rr.value
        resultj = result.publishJson()
    } else if (rr is Err) {
        val error = rr.unwrapError()
        errorj = error.publishJson()
    }

    return OkOrErrorJson(resultj, errorj)
}

@Serializable
data class RaireResultJson(
    val assertions: List<AssertionAndDifficultyJson>,
    val difficulty: Double,
    val margin: Int,
    val winner: Int,
    val num_candidates: Int,
    val time_to_determine_winners: TimeTakenJson,
    val time_to_find_assertions: TimeTakenJson,
    val time_to_trim_assertions: TimeTakenJson,
) {

    fun import(): RaireResult {
        val aand: List<AssertionAndDifficulty> = this.assertions.map { it.import() }
        return RaireResult(
            aand,
            this.difficulty,
            this.margin,
            this.winner,
            this.num_candidates,
            this.time_to_determine_winners.import(),
            this.time_to_find_assertions.import(),
            this.time_to_trim_assertions.import(),
            false
        )
    }
}

fun RaireResult.publishJson(): RaireResultJson {
    val aand: List<AssertionAndDifficultyJson> = this.assertAndDiff.map { it.publishJson() }
    return RaireResultJson(
        aand,
        this.difficulty,
        this.margin,
        this.winner,
        this.num_candidates,
        this.time_to_determine_winners.publishJson(),
        this.time_to_find_assertions.publishJson(),
        this.time_to_trim_assertions.publishJson(),
    )
}

@Serializable
data class RaireErrorJson(
    val type: String,
    val intArrayParam: List<Int>? = null,
    val doubleParam: Double? = null,
) {
    fun import(): RaireError {
        return when (type) {
            "InvalidNumberOfCandidates" -> InvalidNumberOfCandidates()
            "InvalidTimeout" -> InvalidTimeout()
            "InvalidCandidateNumber" -> InvalidCandidateNumber()
            "TimeoutCheckingWinner" -> TimeoutCheckingWinner()
            "TimeoutFindingAssertions" -> TimeoutFindingAssertions(doubleParam!!)
            "TimeoutTrimmingAssertions" -> TimeoutTrimmingAssertions()
            "TiedWinners" -> TiedWinners(toIntArray(intArrayParam!!))
            "WrongWinner" -> WrongWinner(toIntArray(intArrayParam!!))
            "CouldNotRuleOut" -> CouldNotRuleOut(toIntArray(intArrayParam!!))
            "InternalErrorRuledOutWinner" -> InternalErrorRuledOutWinner()
            "InternalErrorDidntRuleOutLoser" -> InternalErrorDidntRuleOutLoser()
            "InternalErrorTrimming" -> InternalErrorTrimming()
            else -> throw RuntimeException("unknown RaireError $type")
        }
    }
}

fun RaireError.publishJson(): RaireErrorJson {
    return when (this) {
        is InvalidNumberOfCandidates -> RaireErrorJson("InvalidNumberOfCandidates", null)
        is InvalidTimeout -> RaireErrorJson("InvalidTimeout", null)
        is InvalidCandidateNumber -> RaireErrorJson("InvalidCandidateNumber", null)
        is TimeoutCheckingWinner -> RaireErrorJson("TimeoutCheckingWinner", null)
        is TimeoutFindingAssertions -> RaireErrorJson("TimeoutFindingAssertions", doubleParam=this.difficultyAtTimeOfStopping)
        is TimeoutTrimmingAssertions -> RaireErrorJson("TimeoutTrimmingAssertions", null)
        is TiedWinners -> RaireErrorJson("TiedWinners", this.expected.toList())
        is WrongWinner -> RaireErrorJson("WrongWinner", this.expected.toList())
        is CouldNotRuleOut -> RaireErrorJson("CouldNotRuleOut", this.eliminationOrder.toList())
        is InternalErrorRuledOutWinner -> RaireErrorJson("InternalErrorRuledOutWinner", null)
        is InternalErrorTrimming -> RaireErrorJson("InternalErrorTrimming", null)
        else -> throw RuntimeException("unknown RaireError ${this}")
    }
}

// data class AssertionAndDifficulty(
//    val assertion: Assertion,
//    val difficulty: Double,
//    val margin: Int,
//    val status: MutableMap<String, Any> = mutableMapOf(), // TODO mutable ??

@Serializable
data class AssertionAndDifficultyJson(
    val assertion: AssertionJson,
    val difficulty: Double,
    val margin: Int,
    val status: Map<String, String>? = null, // oh jeez
) {
    fun import() = AssertionAndDifficulty(assertion.import(), difficulty, margin)
}

fun AssertionAndDifficulty.publishJson(): AssertionAndDifficultyJson {
    val status = this.status.mapValues { it.toString() }
    return AssertionAndDifficultyJson(assertion.publishJson(), difficulty, margin, status)
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

fun Assertion.publishJson(): AssertionJson {
    return if (this is NotEliminatedBefore)
        AssertionJson("NEB", winner, loser, null)
    else if (this is NotEliminatedNext)
        AssertionJson("NEN", winner, loser, continuing.toList())
    else throw RuntimeException("unknown Assertion type $this")
}

@Serializable
data class TimeTakenJson(
    val work: Long,
    val seconds: Double,
) {
    fun import() = TimeTaken(work, seconds)
}

fun TimeTaken.publishJson() = TimeTakenJson(work, seconds)


/////////////////////////////////////////////////////////////////////////

// TODO error handling
fun readRaireSolutionFromFile(filename: String): RaireSolution {
    val filepath = Path.of(filename)
    if (!Files.exists(filepath)) {
        throw RuntimeException("file does not exist")
    }
    val jsonReader = Json { explicitNulls = false; ignoreUnknownKeys = true }

    Files.newInputStream(filepath, StandardOpenOption.READ).use { inp ->
        val jsonObject:RaireSolutionJson =  jsonReader.decodeFromStream<RaireSolutionJson>(inp)
        return jsonObject.import()
    }
}

fun readRaireSolutionFromString(json: String): RaireSolution {
    val jsonReader = Json { explicitNulls = false; ignoreUnknownKeys = true }
    val jsonObject:RaireSolutionJson = jsonReader.decodeFromString<RaireSolutionJson>(json)
    return jsonObject.import()
}

fun RaireSolution.writeToFile(filename: String) {
    val jsonReader = Json { explicitNulls = false; ignoreUnknownKeys = true; prettyPrint = true }
    val jsonObject : RaireSolutionJson =  this.publishJson()
    FileOutputStream(filename).use { out ->
        jsonReader.encodeToStream(jsonObject, out)
    }
}
