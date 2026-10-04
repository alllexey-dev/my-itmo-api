package dev.alllexey.itmoapi.myitmo.sport

import kotlinx.serialization.Serializable

/** Medical health group assigned for sports lessons. Non-null defaults are client fallbacks, not evidence of wire absence. */
@Serializable
public data class SportHealthLevel(
    /** Health group identifier. */
    public val id: Long = 0,
    /** Student ISU identifier. */
    public val isu: Long = 0,
    /** Localized group name, for example Основная группа здоровья; names are server-managed. */
    public val name: String = "",
)
