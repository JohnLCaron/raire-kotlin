package org.cryptobiotic.raire.json

import kotlinx.serialization.Serializable
import org.cryptobiotic.raire.audittype.AuditType
import org.cryptobiotic.raire.audittype.BallotComparisonMACRO
import org.cryptobiotic.raire.audittype.BallotComparisonOneOnDilutedMargin
import org.cryptobiotic.raire.audittype.BallotComparisonOneOnDilutedMarginSquared
import org.cryptobiotic.raire.audittype.BallotPollingBRAVO

@Serializable
data class AuditTypeJson(
    val type: String,
    val confidence: Double?,
    val error_inflation_factor: Double?,
    val total_auditable_ballots: Int,
) {
    //         @JsonSubTypes.Type(value = BallotComparisonMACRO.class, name = "MACRO"),
    //        @JsonSubTypes.Type(value = BallotPollingBRAVO.class, name = "BRAVO"),
    //        @JsonSubTypes.Type(value = BallotComparisonOneOnDilutedMargin.class, name = "OneOnMargin"),
    //        @JsonSubTypes.Type(value = BallotComparisonOneOnDilutedMarginSquared.class, name = "OneOnMarginSq")
    fun import(): AuditType {
        return when (type) {
            "MACRO" -> BallotComparisonMACRO(confidence!!, error_inflation_factor!!, total_auditable_ballots)
            "OneOnMargin" -> BallotComparisonOneOnDilutedMargin(total_auditable_ballots)
            "OneOnMarginSq" -> BallotComparisonOneOnDilutedMarginSquared(total_auditable_ballots)
            "BRAVO" -> BallotPollingBRAVO(confidence!!, total_auditable_ballots)
            else -> {
                throw RuntimeException()
            }
        }
    }
}

fun AuditType.publishJson(): AuditTypeJson =
    when (this) {
        is BallotComparisonMACRO -> AuditTypeJson("MACRO", this.confidence, this.error_inflation_factor, this.total_auditable_ballots)
        is BallotComparisonOneOnDilutedMargin -> AuditTypeJson("OneOnMargin", 0.0, 0.0, this.total_auditable_ballots)
        is BallotComparisonOneOnDilutedMarginSquared -> AuditTypeJson("OneOnMarginSq", 0.0, 0.0, this.total_auditable_ballots)
        is BallotPollingBRAVO -> AuditTypeJson("BRAVO", this.confidence, 0.0, this.total_auditable_ballots)
        else -> {
            throw RuntimeException()
        }
    }