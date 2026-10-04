package dev.alllexey.itmoapi.myitmo.recordbook

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Academic period available in the recordbook. */
@Serializable
public data class Semester(
    /** Academic year in YYYY/YYYY form, for example 2025/2026. */
    @SerialName("study_year")
    public val studyYear: String = "",
    /** Sequential semester number in the plan, starting at 1. */
    public val semester: Int = 0,
    /** Year of study, starting at 1. */
    public val course: Int = 0,
    /** Whether this is the current academic period for the user. */
    public val actual: Boolean = false,
)
