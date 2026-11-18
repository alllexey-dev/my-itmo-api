package dev.alllexey.itmoapi.bars.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Journal catalog discipline in the server-selected period.
 * Non-null defaults are client fallbacks, not claims of wire optionality.
 */
@Serializable
public data class Discipline(
    /** BARS discipline identifier, not MyITMO discipline_id. */
    public val id: Long = 0,
    /** Discipline name. */
    public val name: String = "",
    /** Continuous semesters where the plan applies, e.g. [2, 4, 6]. */
    public val terms: List<Int> = emptyList(),
    /** Plans used to address journals, rather than the discipline id. */
    @SerialName("checkpoint_plan_ids")
    public val checkpointPlanIds: List<Long> = emptyList(),
) {
}
