package dev.alllexey.itmoapi.bars.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Score for a checkpoint. A missing record means not marked, not zero.
 * Non-null defaults are client fallbacks, not claims of wire optionality.
 */
@Serializable
public data class Mark(
    /** Score record identifier, not the work identifier. */
    public val id: Long = 0,
    /** References Checkpoint.id; null for additional points. */
    @SerialName("checkpoint_id")
    public val checkpointId: Long? = null,
    /** Checkpoint plan identifier. */
    @SerialName("checkpoint_plan_id")
    public val checkpointPlanId: Long = 0,
    /** Points earned, including fractional values; may be absent. */
    public val mark: Double? = null,
    /** current for regular work, final for the final score, not checkpoint kind. */
    public val type: String = "",
    /** Explicit absence, never inferred from zero points. */
    @SerialName("is_absent")
    public val absent: Boolean = false,
    /** Server maximum-score validation flag. */
    @SerialName("is_not_bigger_than_max")
    public val notBiggerThanMax: Boolean = false,
    /** Unix epoch time in milliseconds. */
    @SerialName("created_at")
    public val createdAt: Long? = null,
    /** Unix epoch time in milliseconds. */
    @SerialName("updated_at")
    public val updatedAt: Long? = null,
    /** Record author, not necessarily the discipline teacher. */
    @SerialName("created_by_name")
    public val createdByName: String = "",
    /** Record updater, not necessarily the discipline teacher. */
    @SerialName("updated_by_name")
    public val updatedByName: String = "",
) {
}
