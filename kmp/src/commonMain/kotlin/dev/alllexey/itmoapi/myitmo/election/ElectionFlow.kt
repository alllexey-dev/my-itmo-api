package dev.alllexey.itmoapi.myitmo.election

import kotlinx.serialization.Serializable

/** Recursive flow node in the current elective campaign, not the deprecated Flow model.
 * Non-null defaults are client fallbacks, not evidence of wire optionality.
 */
@Serializable
public data class ElectionFlow(
    /** Flow identifier. */
    public val id: Long = 0,
    /** Flow name. */
    public val name: String = "",
    /** Maximum capacity; the legacy nullable value is retained. */
    public val limitMax: Long? = null,
    /** Teacher names. */
    public val teachers: List<String> = emptyList(),
    /** Nested alternative flows. */
    public val variants: List<ElectionFlow> = emptyList(),
    /** Server work-type identifier; values are not enumerated. */
    public val workType: Int = 0,
    /** Whether enrollment is currently available. */
    public val available: Boolean = false,
    /** Server selection markers, not a client-defined enum. */
    public val selections: List<Int> = emptyList(),
)
