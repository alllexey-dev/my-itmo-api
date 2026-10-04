package dev.alllexey.itmoapi.myitmo.studyplan

import kotlinx.serialization.Serializable

/** Complete recursive structure of the user study plan. Lists use empty client fallbacks. */
@Serializable
public data class StudyPlan(
    /** Study-plan identifier. */
    public val id: Long = 0,
    /** Current semester number in the plan. */
    public val currentSemester: Int = 0,
    /** Internal identifier of the current semester. */
    public val currentSemesterId: Long = 0,
    /** Total number of semesters in the plan. */
    public val semestersCount: Int = 0,
    /** Educational program metadata; the empty object is a client fallback. */
    public val planInfo: StudyPlanInfo = StudyPlanInfo(),
    /** Flat semester directory. */
    public val semesters: List<StudyPlanSemester> = emptyList(),
    /** Recursive structure of blocks, modules and disciplines. */
    public val structure: List<StudyPlanNode> = emptyList(),
)
