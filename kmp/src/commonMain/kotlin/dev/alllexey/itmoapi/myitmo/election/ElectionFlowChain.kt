package dev.alllexey.itmoapi.myitmo.election

import kotlinx.serialization.Serializable

/** Selected discipline and its recursive tree of available flows.
 * Non-null defaults are client fallbacks, not evidence of wire optionality.
 */
@Serializable
public data class ElectionFlowChain(
    /** Opaque selected discipline group-flow identifier. */
    public val groupFlow: String = "",
    /** Discipline identifier. */
    public val disciplineId: Long = 0,
    /** Discipline name. */
    public val disciplineName: String = "",
    /** Available flow trees. */
    public val flows: List<ElectionFlow> = emptyList(),
)
