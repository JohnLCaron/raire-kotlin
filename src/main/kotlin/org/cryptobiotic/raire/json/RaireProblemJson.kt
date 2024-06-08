@file:OptIn(ExperimentalSerializationApi::class)

package org.cryptobiotic.raire.json

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import org.cryptobiotic.raire.RaireProblem
import org.cryptobiotic.raire.audittype.AuditType
import org.cryptobiotic.raire.audittype.BallotComparisonOneOnDilutedMargin
import org.cryptobiotic.raire.irv.Vote
import org.cryptobiotic.raire.util.toArray
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


@Serializable
data class RaireProblemJson(
    val metadata: ProblemMetadata,
    val num_candidates: Int,
    val votes: List<VoteJson>,
    val winner: Int,
    val audit: Audit,
    ) {

    @Serializable
    data class ProblemMetadata(
        val candidates: List<String>,
        val contest: String,
    )

    @Serializable
    data class VoteJson(
        val n: Int,
        val prefs: List<Int>,
    )  {
        fun import() = Vote(n, IntArray(prefs.size) { prefs[it] } )
    }

    @Serializable
    data class Audit(
        val type: String,
        val total_auditable_ballots: Int,
    ) {
        fun import(): AuditType = BallotComparisonOneOnDilutedMargin(total_auditable_ballots)
    }

    override fun toString() = buildString {
        appendLine("candidates=${metadata.candidates} (${metadata.candidates.size})")
        appendLine("contest=${metadata.contest}")
        appendLine("num_candidates=$num_candidates, winner=$winner, audit=$audit")
        votes.forEach { appendLine("  $it") }
    }

    fun import(): RaireProblem {
        return RaireProblem(emptyMap(), toArray(votes.map{ it.import() }), num_candidates,
            null, audit.import(), null, null, null)
    }

}

fun readRaireProblemFromFile(filename: String): RaireProblemJson? {
    val filepath = Path.of(filename)
    if (!Files.exists(filepath)) {
        println("file does not exist")
        return null
    }
    val jsonReader = Json { explicitNulls = false; ignoreUnknownKeys = true }

    Files.newInputStream(filepath, StandardOpenOption.READ).use { inp ->
        return jsonReader.decodeFromStream<RaireProblemJson>(inp)
    }
}

fun writeRaireProblem(problem: RaireProblemJson) {
    val jsonReader = Json { explicitNulls = false; ignoreUnknownKeys = true; prettyPrint = true }
    jsonReader.encodeToStream(problem, System.out)
}
