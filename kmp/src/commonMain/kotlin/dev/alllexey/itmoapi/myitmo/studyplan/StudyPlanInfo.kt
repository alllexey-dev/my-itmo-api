package dev.alllexey.itmoapi.myitmo.studyplan

import kotlinx.serialization.Serializable

/** Educational program metadata associated with a study plan. */
@Serializable
public data class StudyPlanInfo(
    /** Training direction code, for example 09.03.04. */
    public val directionCode: String = "",
    /** Training direction name. */
    public val directionName: String = "",
    /** Qualification level, for example bachelor or master. */
    public val levelQualification: String = "",
    /** Server study-plan type label. */
    public val planType: String = "",
    /** Educational program name. */
    public val programName: String = "",
    /** Year when study started. */
    public val startYear: Int = 0,
)
