package org.cryptobiotic.raireservice

import org.cryptobiotic.raire.RaireError


class RaireServiceException(message: String, val errorCode: RaireErrorCode) : Exception(message) {

    companion object {
        val ERROR_CODE_KEY: String = "error_code"

        fun makeFromError(error: RaireError, candidates: List<String>): RaireServiceException {
            val errorcode = when (error) {
                is RaireError.TiedWinners -> RaireErrorCode.TIED_WINNERS
                is RaireError.TimeoutFindingAssertions -> RaireErrorCode.TIMEOUT_FINDING_ASSERTIONS
                is RaireError.TimeoutTrimmingAssertions -> RaireErrorCode.TIMEOUT_TRIMMING_ASSERTIONS
                is RaireError.CouldNotRuleOut -> RaireErrorCode.COULD_NOT_RULE_OUT_ALTERNATIVE
                is RaireError.InvalidCandidateNumber -> RaireErrorCode.WRONG_CANDIDATE_NAMES
                else -> RaireErrorCode.INTERNAL_ERROR
            }
            return RaireServiceException(makeMessage(error, candidates), errorcode)
        }

        /**
         * Make a human-readable error message out of a raire-java error.
         *
         * @param error      the RaireError to be explained
         * @param candidates the list of candidate names as strings
         * @return a human-readable error message to be returned through the API.
         */
        private fun makeMessage(error: RaireError, candidates: List<String>): String {

            val message = when (error) {
                is RaireError.TiedWinners -> {
                    // expected are the indices into the candidate array. LOOK throws an exception if out of bounds
                    val tiedWinners = error.expected.map { candidates[it] }.joinToString { ", " }
                    "Tied winners: $tiedWinners.";
                }

                is RaireError.TimeoutFindingAssertions ->
                    "Time out finding assertions - try again with longer timeout."

                is RaireError.TimeoutTrimmingAssertions ->
                    "Time out trimming assertions - the assertions are usable, but could be reduced given more trimming time."

                is RaireError.TimeoutCheckingWinner ->
                    "Time out checking winner - the election is either tied or extremely complex."

                is RaireError.CouldNotRuleOut -> {
                    val sequenceList = error.eliminationOrder.map { candidates[it] }.joinToString { ", " }
                    "Could not rule out alternative elimination order: : $sequenceList.";
                }

                is RaireError.InvalidCandidateNumber ->
                    "Candidate list does not match database."

                else -> "Internal error"
            }
            return message;
        }

    }


}