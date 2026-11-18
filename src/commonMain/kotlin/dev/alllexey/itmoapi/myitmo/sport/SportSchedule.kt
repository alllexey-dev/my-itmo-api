package dev.alllexey.itmoapi.myitmo.sport

import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Sports lessons grouped by calendar date. */
@Serializable
public data class SportSchedule(
    /** Calendar date of this group. */
    public val date: LocalDate = LocalDate(1970, 1, 1),
    /** Lessons; the personal calendar has also returned null rather than an empty list. */
    public val lessons: List<SportLesson>? = null,
)
