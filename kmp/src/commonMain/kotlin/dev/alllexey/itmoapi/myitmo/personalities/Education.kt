package dev.alllexey.itmoapi.myitmo.personalities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Brief education record. Student samples contain all three fields as strings without nulls.
 * Employee and service profiles have an empty outer education list. */
@Serializable
public data class Education(
    /** Course number as a JSON string (e.g. "3"), not a JSON number. */
    public val course: String = "",
    /** Faculty or institute name. */
    @SerialName("faculty_name") public val facultyName: String = "",
    /** Academic group label. */
    public val group: String = "",
)
