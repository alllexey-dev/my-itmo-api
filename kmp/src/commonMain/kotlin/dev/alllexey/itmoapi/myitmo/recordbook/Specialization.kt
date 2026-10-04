package dev.alllexey.itmoapi.myitmo.recordbook

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Educational program for which the user has a recordbook. */
@Serializable
public data class Specialization(
    /** Main study-plan identifier used as specializationId in getRecordBook. */
    @SerialName("main_plan")
    public val mainPlan: Long = 0,
    /** Display name of the educational program. */
    @SerialName("specialization_name")
    public val specializationName: String = "",
    /** Periods the server permits the user to request. The empty list is a client fallback. */
    public val semesters: List<Semester> = emptyList(),
)
