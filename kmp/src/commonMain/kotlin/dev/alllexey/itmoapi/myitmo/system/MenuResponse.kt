package dev.alllexey.itmoapi.myitmo.system

import kotlinx.serialization.Serializable

/** MyITMO navigation menu wrapper.
 * Non-null defaults are client fallbacks, not evidence of wire optionality.
 */
@Serializable
public data class MenuResponse(
    /** Ordered menu items or service cards. */
    public val menu: List<MenuItem> = emptyList(),
)
