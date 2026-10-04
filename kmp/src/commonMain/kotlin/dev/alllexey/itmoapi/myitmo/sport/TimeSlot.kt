package dev.alllexey.itmoapi.myitmo.sport

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Sports lesson interval; start/end are local HH:mm strings. */
@Serializable
public data class TimeSlot(
    /** Numeric slot identifier. */
    public val id: Long = 0,
    /** Local start time, HH:mm. */
    @SerialName("time_start") public val timeStart: String = "",
    /** Local end time, HH:mm. */
    @SerialName("time_end") public val timeEnd: String = "",
)
