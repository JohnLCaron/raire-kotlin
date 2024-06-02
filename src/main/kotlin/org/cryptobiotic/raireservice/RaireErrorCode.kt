package org.cryptobiotic.raireservice

/**
 * Error codes describing what went wrong, for returning via http to colorado-rla.
 */
enum class RaireErrorCode {
    // Errors that the user can do something about.
    /**
     * Tied winners - the contest is a tie and therefore cannot be audited.
     */
    TIED_WINNERS,

    /**
     * The total number of auditable ballots for a contest is less than the number of CVRs in
     * the database that contain the contest.
     */
    INVALID_TOTAL_AUDITABLE_BALLOTS,

    /**
     * Time out checking winners - can happen if the contest is tied, or if it is complicated and
     * can't be distinguished from a tie.
     */
    TIMEOUT_CHECKING_WINNER,

    /**
     * Raire timed out trying to find assertions. It may succeed if given more time.
     */
    TIMEOUT_FINDING_ASSERTIONS,

    /**
     * Raire timed out trimming assertions. The assertions have been generated, but the audit could
     * be more efficient if the trimming step is re-run with more time allowed.
     */
    TIMEOUT_TRIMMING_ASSERTIONS,

    /**
     * RAIRE couldn't rule out some alternative winner.
     */
    COULD_NOT_RULE_OUT_ALTERNATIVE,

    /**
     * The list of candidate names in the request didn't match the database.
     */
    WRONG_CANDIDATE_NAMES,

    /**
     * The user has requested to retrieve assertions for a contest for which no assertions have
     * been generated.
     */
    NO_ASSERTIONS_PRESENT,

    /**
     * The user has requested to generate assertions for a contest for which no votes are present.
     */
    NO_VOTES_PRESENT,  // Internal errors (that the user can do nothing about)

    /**
     * A catch-all for various kinds of errors that indicate a programming error: invalid
     * input errors such as InvalidNumberOfCandidates, InvalidTimeout, InvalidCandidateNumber -
     * these should all be caught before being sent to raire-java. Also database errors.
     * These are errors that the user can't do anything about.
     */
    INTERNAL_ERROR,
}