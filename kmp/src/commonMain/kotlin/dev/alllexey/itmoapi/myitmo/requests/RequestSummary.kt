package dev.alllexey.itmoapi.myitmo.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import dev.alllexey.itmoapi.core.WireInstantSerializer
import kotlin.time.Instant

/** Summary of a request belonging to the current user.
 * Non-null defaults are client fallbacks, not evidence of wire optionality.
 */
@Serializable
public data class RequestSummary(
    /** Request identifier. */
    public val id: Long = 0,
    /** Request name. */
    public val name: String = "",
    /** Additional request notice or explanation. */
    public val notice: String = "",
    /** Numeric status; values vary by request type and may expand. */
    public val status: Int = 0,
    /** Localized server status label intended for display. */
    @SerialName("status_name")
    public val statusName: String = "",
    /** Creation instant from an offset date-time. */
    @SerialName("created_at")
    @Serializable(with = WireInstantSerializer::class)
    public val createdAt: Instant = Instant.fromEpochSeconds(0),
    /** Last update instant from an offset date-time. */
    @SerialName("updated_at")
    @Serializable(with = WireInstantSerializer::class)
    public val updatedAt: Instant = Instant.fromEpochSeconds(0),
)
