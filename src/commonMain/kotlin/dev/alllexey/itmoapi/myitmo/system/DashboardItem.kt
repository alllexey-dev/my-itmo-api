package dev.alllexey.itmoapi.myitmo.system

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Saved MyITMO dashboard widget position and grid constraints.
 * Non-null defaults are client fallbacks, not evidence of wire optionality.
 */
@Serializable
public data class DashboardItem(
    /** String layout item identifier. */
    public val i: String = "",
    /** Horizontal grid position. */
    public val x: Int = 0,
    /** Vertical grid position. */
    public val y: Int = 0,
    /** Width in grid units. */
    public val w: Int = 0,
    /** Height in grid units. */
    public val h: Int = 0,
    /** Widget name mapped by the consumer to a UI component. */
    public val widget: String = "",
    /** Whether the layout item is fixed. */
    @SerialName("static")
    public val isStatic: Boolean = false,
    /** Optional minimum width in grid units. */
    @SerialName("min_w")
    public val minWidth: Int? = null,
    /** Optional maximum width in grid units. */
    @SerialName("max_w")
    public val maxWidth: Int? = null,
    /** Optional minimum height in grid units. */
    @SerialName("min_h")
    public val minHeight: Int? = null,
    /** Optional maximum height in grid units. */
    @SerialName("max_h")
    public val maxHeight: Int? = null,
    /** Optional dragging permission. */
    @SerialName("is_draggable")
    public val draggable: Boolean? = null,
    /** Optional resizing permission. */
    @SerialName("is_resizable")
    public val resizable: Boolean? = null,
    /** Optional aspect-ratio constraint. */
    @SerialName("preserve_aspect_ratio")
    public val preserveAspectRatio: Boolean? = null,
    /** Optional selector allowing drag initiation. */
    @SerialName("drag_allow_from")
    public val dragAllowFrom: String? = null,
    /** Optional selector excluding drag initiation. */
    @SerialName("drag_ignore_from")
    public val dragIgnoreFrom: String? = null,
    /** Optional selector excluding resizing. */
    @SerialName("resize_ignore_from")
    public val resizeIgnoreFrom: String? = null,
)
