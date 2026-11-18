package dev.alllexey.itmoapi.bars.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Journal header containing the full plan and requested group/flow address.
 * Non-null defaults are client fallbacks, not claims of wire optionality.
 */
@Serializable
public data class JournalHeaders(
    /** Complete checkpoint plan. */
    public val plan: CheckpointPlan = CheckpointPlan(),
    /** Matches the URL type segment. */
    public val type: String = "",
    /** Matches the URL identifier segment. */
    public val identifier: String = "",
    /** Group or flow name. */
    public val name: String = "",
) {
    // TODO: deadlines: List<unknown>; observed only empty.
}
