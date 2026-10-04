package dev.alllexey.itmoapi.bars.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Discipline embedded in a checkpoint plan.
 * Non-null defaults are client fallbacks, not claims of wire optionality.
 */
@Serializable
public data class PlanDiscipline(
    /** Discipline identifier. */
    public val id: Long = 0,
    /** Discipline name. */
    public val name: String = "",
    /** Whether this discipline has a course project. */
    @SerialName("course_project")
    public val courseProject: Boolean = false,
) {
    // TODO: term: unknown; observed only null.
}
