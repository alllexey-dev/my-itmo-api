package dev.alllexey.itmoapi.myitmo.sport

import dev.alllexey.itmoapi.core.MyItmoException

/**
 * What MyITMO decided about an enrollment in one lesson ([SportApi.signInLesson]).
 *
 * Only MyITMO's own refusal on a successful HTTP exchange (an error envelope with HTTP 2xx) decides about the
 * lesson: it is [Full] or [Rejected]. Anything else is [RetryLater].
 */
public sealed interface SportSignResult {

    /** Enrolled: `error_code` 0 with a non-null `result` list of enrolled lesson IDs. */
    public data object Booked : SportSignResult

    /**
     * The lesson has no free places: the legacy refusal `нельзя записать студента: нет свободных мест на занятии`
     * followed by the server's student and lesson context. The error code is not relied on. Another attempt may
     * succeed once a place frees up.
     */
    public data object Full : SportSignResult

    /**
     * MyITMO refused by another rule, for example code 137 with bracketed reasons such as
     * `[cannot enroll student: [daily limit], [overlapping enrollment], [weekly limit]]` (localized on the wire).
     * Another attempt will not succeed. [refusal] keeps the code and the untrusted `serverMessage`: never log it.
     */
    public class Rejected(public val refusal: MyItmoException.Api) : SportSignResult

    /**
     * No decision about the lesson: a network failure, a non-2xx status (an error envelope on HTTP 400 included),
     * a session or access envelope code (401, 403, 404), or a success without `result`. [cause] tells which;
     * [MyItmoException.Auth] means the user must sign in again.
     */
    public class RetryLater(public val cause: MyItmoException) : SportSignResult
}

/** The legacy capacity refusal; the server appends the student and lesson context to it. */
private const val NO_PLACES_MESSAGE = "нельзя записать студента: нет свободных мест на занятии"

/** Envelope codes that are about the session or access, not the lesson. */
private val NOT_A_REFUSAL = setOf(401, 403, 404)

internal fun sportSignResultOf(failure: MyItmoException): SportSignResult {
    if (failure !is MyItmoException.Api || failure.status !in 200..299 || failure.errorCode in NOT_A_REFUSAL) {
        return SportSignResult.RetryLater(failure)
    }
    val message = failure.serverMessage.orEmpty()
    // The legacy rule: the signature alone, without more than 20 characters of context, is another refusal.
    val full = message.contains(NO_PLACES_MESSAGE) && message.replace(NO_PLACES_MESSAGE, "").length > 20
    return if (full) SportSignResult.Full else SportSignResult.Rejected(failure)
}
