package dev.alllexey.itmoapi.myitmo.sport

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Counters for sports enrollment attempts. */
@Serializable
public data class SportAttempts(
    /** Total attempts granted. */
    @SerialName("total_attempts") public val totalAttempts: Int = 0,
    /** Attempts already used. */
    @SerialName("used_attempts") public val usedAttempts: Int = 0,
    /** Attempts remaining. */
    @SerialName("free_attempts") public val freeAttempts: Int = 0,
    /** Whether the server permits enrollment now. */
    @SerialName("can_sign_in") public val canSignIn: Boolean = false,
)
