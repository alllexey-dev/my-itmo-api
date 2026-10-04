package dev.alllexey.itmoapi.bars.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Checkpoint plan from journal headers.plan; its id is not MyITMO est_id.
 * Non-null defaults are client fallbacks, not claims of wire optionality.
 */
@Serializable
public data class CheckpointPlan(
    /** BARS plan identifier. */
    public val id: Long = 0,
    /** External plan identifier. */
    public val gid: String = "",
    /** Academic year in yyyy/yyyy form. */
    public val year: String = "",
    /** Continuous semester numbers. */
    public val terms: List<Int> = emptyList(),
    /** Plan discipline definition. */
    public val discipline: PlanDiscipline = PlanDiscipline(),
    /** Regular checkpoint definitions. */
    @SerialName("regular_checkpoints")
    public val regularCheckpoints: List<Checkpoint> = emptyList(),
    /** Final exam/pass checkpoint; may be absent. */
    @SerialName("final_checkpoint")
    public val finalCheckpoint: Checkpoint? = null,
    /** Server point distribution selector. */
    @SerialName("point_distribution")
    public val pointDistribution: Int = 0,
    /** Whether additional points are allowed. */
    @SerialName("additional_points")
    public val additionalPoints: Boolean = false,
    /** Whether a course project exists; its checkpoint shape is unobserved. */
    @SerialName("has_course_project")
    public val courseProject: Boolean = false,
) {
    // TODO: programs: List<unknown>; observed only empty.
    // TODO: components: List<unknown>; observed only empty.
    // TODO: alternate_methods: unknown; observed only null.
    // TODO: course_project_checkpoint: unknown; observed only null.
    // TODO: status: String?; observed null and string, not suitable for submission status.
}
