package dev.alllexey.itmoapi.myitmo.election

import kotlinx.serialization.Serializable

/** Known shape of an order change payload; order endpoints retain arbitrary JSON results.
 * Non-null defaults are client fallbacks, not evidence of wire optionality.
 */
@Serializable
public data class ChangeResult(
    /** Operation status; may be absent, exact values are undocumented. */
    public val status: Long? = null,
    /** Changed item name; may be absent. */
    public val name: String? = null,
    /** Resulting selected flow identifiers. */
    public val flows: List<Long> = emptyList(),
)
