package dev.alllexey.itmoapi.myitmo.studyplan

import kotlinx.serialization.Serializable

/** Semester directory entry in the study plan. */
@Serializable
public data class StudyPlanSemester(
    /** Sequential semester number. */
    public val semester: Int = 0,
    /** Internal semester identifier. */
    public val semesterId: Long = 0,
    /** Server semester parity classification; observed values 0 and 1. */
    public val semesterParity: Int = 0,
    /** Academic year in YYYY/YYYY form. */
    public val studyYear: String = "",
)
