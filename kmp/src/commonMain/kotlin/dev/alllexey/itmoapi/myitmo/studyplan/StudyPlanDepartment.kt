package dev.alllexey.itmoapi.myitmo.studyplan

import kotlinx.serialization.Serializable

/** Department responsible for a discipline or module. */
@Serializable
public data class StudyPlanDepartment(
    /** Department identifier. */
    public val id: Long = 0,
    /** Full department name. */
    public val name: String = "",
    /** Short department name. */
    public val shortName: String = "",
)
