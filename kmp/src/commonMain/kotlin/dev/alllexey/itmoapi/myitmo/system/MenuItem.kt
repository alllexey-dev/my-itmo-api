package dev.alllexey.itmoapi.myitmo.system

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Navigation section or service card; nested children form a menu tree.
 * Non-null defaults are client fallbacks, not evidence of wire optionality.
 */
@Serializable
public data class MenuItem(
    /** Menu or service title. */
    public val title: String = "",
    /** Menu or service description. */
    public val description: String = "",
    /** Internal route or external link. */
    public val to: String = "",
    /** Monochrome design-system icon name. */
    public val icon: String = "",
    /** Multicolor icon name; may be empty. */
    @SerialName("multicolor_icon")
    public val multicolorIcon: String = "",
    /** Whether the active route requires an exact match. */
    public val exact: Boolean = false,
    /** Service-card color; can be absent from main menu items. */
    public val color: String? = null,
    /** Nested items; absent on a terminal route. */
    public val children: List<MenuItem>? = null,
)
