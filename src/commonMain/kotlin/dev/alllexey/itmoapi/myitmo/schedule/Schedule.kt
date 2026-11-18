package dev.alllexey.itmoapi.myitmo.schedule

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.datetime.LocalDate

/** Personal schedule for one calendar day. Empty lessons are represented by an empty list.
 * note is observed null; date is an ISO calendar date without a timezone. */
@Serializable
public data class Schedule(
    /** Day number inside the response range. */
    @SerialName("day_number") public val dayNumber: Int = 0,
    /** Academic week number. */
    @SerialName("week_number") public val weekNumber: Int = 0,
    /** Calendar day in yyyy-MM-dd wire form. */
    public val date: LocalDate = LocalDate(1970, 1, 1),
    /** Whole-day note, usually null. */
    public val note: String? = null,
    /** Lessons for the day; no lessons means an empty collection. */
    public val lessons: List<Lesson> = emptyList(),
) {
    // TODO: type exists, but observed values do not establish its complete wire type.
    // val type: ?
    // TODO: intersections exists, but the structure of nonempty elements is unconfirmed.
    // val intersections: List<?>
}
