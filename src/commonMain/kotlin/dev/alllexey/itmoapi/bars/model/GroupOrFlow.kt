package dev.alllexey.itmoapi.bars.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Journal catalog group or flow; type plus identifier addresses its journal.
 * Non-null defaults are client fallbacks, not claims of wire optionality.
 */
@Serializable
public data class GroupOrFlow(
    /** Only flow has been observed. */
    public val type: String = "",
    /** Group or flow name. */
    public val name: String = "",
    /** Opaque string even when it looks numeric; passed to the journal URL. */
    public val identifier: String = "",
    /** Applicable checkpoint plans. */
    @SerialName("checkpoint_plan_ids")
    public val checkpointPlanIds: List<Long> = emptyList(),
) {
}
