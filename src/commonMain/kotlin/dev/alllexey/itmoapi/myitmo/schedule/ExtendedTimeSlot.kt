package dev.alllexey.itmoapi.myitmo.schedule

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Academic lesson interval, with display order. Times remain HH:mm strings in Moscow time;
 * no timezone or null variant is documented. This flattens the legacy TimeSlot superclass. */
@Serializable
public data class ExtendedTimeSlot(
    /** Numeric slot identifier. */
    public val id: Long = 0,
    /** Lesson start, HH:mm, Moscow time. */
    @SerialName("time_start") public val timeStart: String = "",
    /** Lesson end, HH:mm, Moscow time. */
    @SerialName("time_end") public val timeEnd: String = "",
    /** Slot display order within the academic day. */
    public val order: Int = 0,
)
