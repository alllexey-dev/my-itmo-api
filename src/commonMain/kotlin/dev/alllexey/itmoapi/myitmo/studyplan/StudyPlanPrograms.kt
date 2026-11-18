package dev.alllexey.itmoapi.myitmo.studyplan

import kotlinx.serialization.Serializable

/** Study plans available to the current user. */
@Serializable
public data class StudyPlanPrograms(
    /** ISU identifier of the user for whom this response was formed. */
    public val isu: Long = 0,
    /** Educational programs of the user. The empty list is a client fallback. */
    public val programs: List<StudyPlanProgram> = emptyList(),
)
