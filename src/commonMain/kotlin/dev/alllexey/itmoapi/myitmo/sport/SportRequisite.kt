package dev.alllexey.itmoapi.myitmo.sport

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A specific level or qualification standard within a sports selection. Non-null defaults are client fallbacks, not evidence of wire absence. */
@Serializable
public data class SportRequisite(
    /** Qualification identifier. */
    public val id: Long = 0,
    /** Level display name, for example Сборная команда; names are server-managed. */
    @SerialName("level_name")
    public val levelName: String = "",
    /** Full sports discipline and level name. */
    public val name: String = "",
)
