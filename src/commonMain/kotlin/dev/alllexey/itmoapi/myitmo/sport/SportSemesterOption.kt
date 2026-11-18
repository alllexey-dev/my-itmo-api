package dev.alllexey.itmoapi.myitmo.sport

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Short entry in the sports semester selector. */
@Serializable
public data class SportSemesterOption(
    /** Sports semester identifier. */
    public val id: Long = 0,
    /** Display label, for example Spring 2025/2026. */
    public val value: String = "",
    /** Optional server comment. */
    public val comment: String? = null,
)
