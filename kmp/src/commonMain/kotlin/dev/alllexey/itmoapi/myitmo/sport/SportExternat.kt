package dev.alllexey.itmoapi.myitmo.sport

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Participation in the sports externship format. Non-null defaults are client fallbacks, not evidence of wire absence. */
@Serializable
public data class SportExternat(
    /** Whether enrollment in the externship format was accepted. */
    public val signed: Boolean = false,
    /** Server application status identifier; absent when no application was made. Values are server-managed. */
    @SerialName("externat_status_id")
    public val externatStatusId: Long? = null,
    /** Refusal reason; absent when the application was not refused. */
    @SerialName("decline_reason")
    public val declineReason: String? = null,
)
