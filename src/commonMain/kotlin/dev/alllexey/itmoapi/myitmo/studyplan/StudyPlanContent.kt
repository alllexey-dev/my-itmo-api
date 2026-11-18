package dev.alllexey.itmoapi.myitmo.studyplan

import kotlinx.serialization.Serializable

/** Discipline workload in a particular semester. */
@Serializable
public data class StudyPlanContent(
    /** Workload entry identifier. */
    public val id: Long = 0,
    /** Parent module identifier. */
    public val moduleId: Long = 0,
    /** Discipline identifier. */
    public val disciplineId: Long = 0,
    /** Position within the module. */
    public val order: Int = 0,
    /** Semester number. */
    public val semester: Int = 0,
    /** Workload in academic credits. */
    public val creditPoints: Int = 0,
    /** Contact, independent and assessment work; the empty list is a client fallback. */
    public val activities: List<StudyPlanActivity> = emptyList(),
)
