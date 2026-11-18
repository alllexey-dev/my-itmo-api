package dev.alllexey.itmoapi.myitmo.studyplan

import kotlinx.serialization.Serializable

/** Brief educational program and its associated study plan. */
@Serializable
public data class StudyPlanProgram(
    /** Study-plan identifier for /api/eduPlanNew/study_plan/{plan_id}. */
    public val planId: Long = 0,
    /** Specialization identifier; null for programs without a specialization. */
    public val specializationId: Long? = null,
    /** Educational program name. */
    public val name: String = "",
    /** Whether the user currently studies on this program. */
    public val isActive: Boolean = false,
)
