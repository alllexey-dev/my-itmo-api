package dev.alllexey.itmoapi.myitmo.sport

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Physical-education debt status. Optional score and attempts are absent when there is no debt. Non-null defaults are client fallbacks, not evidence of wire absence. */
@Serializable
public data class SportDebt(
    /** Whether physical-education debt exists. */
    @SerialName("is_having_debt")
    public val havingDebt: Boolean = false,
    /** Points required to clear the debt; absent without debt. */
    @SerialName("needed_score")
    public val neededScore: Double? = null,
    /** Available special attempts; absent without debt. */
    @SerialName("free_attempts")
    public val freeAttempts: Int? = null,
)
