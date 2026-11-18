package dev.alllexey.itmoapi.myitmo.sport

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Sports lesson interval; start/end are HH:mm strings in Moscow time. */
@Serializable
public data class TimeSlot(
    /** Numeric slot identifier. */
    public val id: Long = 0,
    /** Start time, HH:mm, Moscow time. */
    @SerialName("time_start") public val timeStart: String = "",
    /** End time, HH:mm, Moscow time. */
    @SerialName("time_end") public val timeEnd: String = "",
)
