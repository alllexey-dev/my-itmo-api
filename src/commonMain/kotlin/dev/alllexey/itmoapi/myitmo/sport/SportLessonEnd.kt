package dev.alllexey.itmoapi.myitmo.sport

import dev.alllexey.itmoapi.core.MOSCOW
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

private val SANE_LESSON_LENGTH = 15.minutes..6.hours

/**
 * The end of a sport lesson starting at [start], with My ITMO's inconsistent `date_end` repaired.
 *
 * My ITMO sometimes sends a `date_end` on the wrong calendar date with a plausible time of day
 * (observed in 2026-10: a weekly series ending two days before it starts). A [rawEnd] after
 * [start] on the same Moscow date is returned unchanged. Otherwise the end is rebuilt on the
 * start's Moscow date from [slotEnd] (`HH:mm`, Moscow time) or, when that does not parse, from
 * [rawEnd]'s Moscow time of day. A rebuilt end must be 15 minutes to 6 hours after [start];
 * if it is not, there is no sane end and the result is null.
 */
public fun normalizedSportLessonEnd(start: Instant, rawEnd: Instant, slotEnd: String): Instant? {
    val day = start.toLocalDateTime(MOSCOW).date
    val raw = rawEnd.toLocalDateTime(MOSCOW)
    if (rawEnd > start && raw.date == day) return rawEnd
    val time = runCatching { LocalTime.parse(slotEnd.trim()) }.getOrNull() ?: raw.time
    return LocalDateTime(day, time).toInstant(MOSCOW).takeIf { it - start in SANE_LESSON_LENGTH }
}

/** [SportLesson.dateEnd] repaired by [normalizedSportLessonEnd] with the lesson's time slot; null when no sane end exists. */
public val SportLesson.normalizedDateEnd: Instant?
    get() = normalizedSportLessonEnd(date, dateEnd, timeSlotEnd)

/** [ChosenSportLesson.dateEnd] repaired by [normalizedSportLessonEnd]; null when a date is absent or no sane end exists. */
public val ChosenSportLesson.normalizedDateEnd: Instant?
    get() {
        val start = dateStart ?: return null
        return normalizedSportLessonEnd(start, dateEnd ?: return null, timeEnd)
    }
