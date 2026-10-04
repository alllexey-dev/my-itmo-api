package dev.alllexey.itmoapi.bars.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Checkpoint definition; student scores reference its id through Mark.
 * Non-null defaults are client fallbacks, not claims of wire optionality.
 */
@Serializable
public data class Checkpoint(
    /** Checkpoint identifier. */
    public val id: Long = 0,
    /** External checkpoint identifier. */
    public val gid: String = "",
    /** Null for the final checkpoint; use type as its description. */
    public val name: String? = null,
    /** Work kind, e.g. Тест, Лабораторная работа, Экзамен, Зачет. */
    public val type: String = "",
    /** Work kind identifier. */
    @SerialName("type_id")
    public val typeId: Long = 0,
    /** Semester week; null for a final checkpoint. */
    public val week: Int? = null,
    /** Whether this checkpoint is a group. */
    public val group: Boolean = false,
    /** Key checkpoint flag. */
    public val key: Boolean = false,
    /** Minimum points. */
    @SerialName("min_grade")
    public val minGrade: Double = 0.0,
    /** Maximum points. */
    @SerialName("max_grade")
    public val maxGrade: Double = 0.0,
    /** Recursive nested checkpoints; observed lists were empty. */
    @SerialName("sub_checkpoints")
    public val subCheckpoints: List<Checkpoint> = emptyList(),
    /** Parent checkpoint identifier; may be absent. */
    @SerialName("parent_checkpoint_id")
    public val parentCheckpointId: Long? = null,
) {
    // TODO: test_id: unknown; observed only null.
    // TODO: test_name: unknown; observed only null.
    // TODO: max_sub_checkpoints_fillable: unknown; observed only null.
}
