@file:OptIn(ExperimentalSerializationApi::class)

package org.cryptobiotic.raire.json

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import org.cryptobiotic.raire.RaireMetadata
import org.cryptobiotic.raire.RaireProblem
import org.cryptobiotic.raire.irv.Vote
import org.cryptobiotic.raire.pruning.TrimAlgorithm
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

//{
// "metadata":
//   {"candidates":
//     ["HUNTER Alan","CLARKE Bruce","COOREY Cate","ANDERSON John","MCILRATH Christopher","LYON Michael","DEY Duncan","PUGH Asren","SWIVEL Mark"],
//    "contest":"2021 NSW Local Government election for Byron Mayoral."
//   },
//  "num_candidates":9,
//  "votes":
//       [
//         {"n":1,"prefs":[4,6,7,5,8,2,3,1,0]},
//         {"n":1,"prefs":[3,6,2]},
//         {"n":1,"prefs":[0,1,6]},
//         {"n":1,"prefs":[8,6]},
//         {"n":1,"prefs":[6,7,8,1,2]},
//         {"n":1,"prefs":[6,2,5]},{"n":5, ...
//         {"n":1,"prefs":[5,0,1,8,2,7,6,4,3]},
//         {"n":1,"prefs":[6,3]},
//         {"n":1,"prefs":[6,5,8,2,3,4,7,0,1]}
//       ],
//   "winner":5,
//   "audit":{"type":"OneOnMargin","total_auditable_ballots":18165}
//}

// class RaireProblem(
//    val metadata: Map<String, Any>,
//    val votes: Array<Vote>,
//    val num_candidates: Int,
//    val winner: Int? = null,
//    val audit: AuditType,
//    val trim_algorithm: TrimAlgorithm?,
//    val difficulty_estimate: Double?,
//    val time_limit_seconds: Double?,
//)

@Serializable
data class RaireProblemJson(
    val metadata: RaireMetadataJson?, // Map<String, Any> : Any is hard to serialize hahaha
    val votes: List<VoteJson>,
    val num_candidates: Int,
    val winner: Int?,
    val audit: AuditTypeJson,
    val trim_algorithm: String?,
    val difficulty_estimate: Double?,
    val time_limit_seconds: Double?,
) {

    fun import(): RaireProblem {
        val trim_algorithm = safeEnumValueOf(this.trim_algorithm) ?: TrimAlgorithm.MinimizeTree
        val votes = this.votes.map { it.import() }
        val metadata = this.metadata?.import()

        return RaireProblem(
            metadata,
            votes,
            this.num_candidates,
            this.winner,
            this.audit.import(),
            trim_algorithm,
            this.difficulty_estimate,
            this.time_limit_seconds,
        )
    }

    override fun toString() = buildString {
        appendLine("num_candidates=$num_candidates, winner=$winner, audit=$audit")
        votes.forEach { appendLine("  $it") }
    }
}

fun RaireProblem.publishJson(): RaireProblemJson {
    val votes = this.votes.map { it.publishJson() }

    return RaireProblemJson(
        this.metadata?.publishJson() ?: RaireMetadataJson(emptyList(), null),
        votes,
        this.num_candidates,
        this.winner,
        this.audit.publishJson(),
        this.trim_algorithm?.name ?: TrimAlgorithm.None.name,
        this.difficulty_estimate,
        this.time_limit_seconds,
    )
}

@Serializable
data class RaireMetadataJson(val candidates: List<String>, val contest: String?) {
    fun import() = RaireMetadata(candidates, contest)
}

fun RaireMetadata.publishJson(): RaireMetadataJson {
    return RaireMetadataJson(this.candidates, this.contest)
}

@Serializable
data class VoteJson(val n: Int, val prefs: List<Int>) {
    fun import() = Vote(n, IntArray(prefs.size) { prefs[it] })
}

fun Vote.publishJson(): VoteJson {
    return VoteJson(this.n, this.prefs.toList())
}

// TODO error handling
fun readRaireProblemFromFile(filename: String): RaireProblem {
    val filepath = Path.of(filename)
    if (!Files.exists(filepath)) {
        throw RuntimeException("file does not exist")
    }
    val jsonReader = Json { explicitNulls = false; ignoreUnknownKeys = true }

    Files.newInputStream(filepath, StandardOpenOption.READ).use { inp ->
        val jsonObject:RaireProblemJson =  jsonReader.decodeFromStream<RaireProblemJson>(inp)
        return jsonObject.import()
    }
}

fun readRaireProblemFromString(json: String): RaireProblem {
    val jsonReader = Json { explicitNulls = false; ignoreUnknownKeys = true }
    val jsonObject:RaireProblemJson = jsonReader.decodeFromString<RaireProblemJson>(json)
    return jsonObject.import()
}

fun RaireProblem.writeToFile(filename: String) {
    val jsonReader = Json { explicitNulls = false; ignoreUnknownKeys = true; prettyPrint = true }
    val jsonObject : RaireProblemJson =  this.publishJson()
    FileOutputStream(filename).use { out ->
        jsonReader.encodeToStream(jsonObject, out)
    }
}

//////////////////////////////////////////////////////////////////////////

inline fun <reified T : Enum<T>> safeEnumValueOf(name: String?): T? {
    if (name == null) {
        return null
    }

    return try {
        enumValueOf<T>(name)
    } catch (e: IllegalArgumentException) {
        null
    }
}

fun inverseListToString(s: String): List<String> {
    val chop = s.substring(1, s.length - 1); // chop off brackets
    val tokes = chop.split(", ");
    return tokes.map { it.trim()}
}
