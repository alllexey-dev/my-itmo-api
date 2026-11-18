package dev.alllexey.itmoapi.myitmo.studyplan

import kotlinx.serialization.Serializable

/** One kind of educational work in a discipline workload. */
@Serializable
public data class StudyPlanActivity(
    /** Activity identifier. */
    public val id: Long = 0,
    /** Parent workload entry identifier. */
    public val contentId: Long = 0,
    /** Work name, for example Лекции, Практические занятия or Экзамен. */
    public val name: String = "",
    /** Volume in academic hours; may be null for an assessment. */
    public val volume: Double? = null,
    /** Open server directory: observed 1 lectures, 3 practice, 4 independent work, 5 exam, 6 pass/fail. */
    public val workTypeId: Long = 0,
)
