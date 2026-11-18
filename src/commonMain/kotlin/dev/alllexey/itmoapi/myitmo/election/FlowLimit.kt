package dev.alllexey.itmoapi.myitmo.election

import kotlinx.serialization.Serializable

/** Current capacity of an elective flow, measured in students.
 * Non-null defaults are client fallbacks, not evidence of wire optionality.
 */
@Serializable
public data class FlowLimit(
    /** Maximum student capacity. */
    public val limitMax: Long = 0,
    /** Occupied student places. */
    public val occupied: Long = 0,
    /** Remaining student places. */
    public val free: Long = 0,
)
