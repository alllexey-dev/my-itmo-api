package dev.alllexey.itmoapi.myitmo.sport

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Nested payload returned by the personal health-level endpoint. Non-null defaults are client fallbacks, not evidence of wire absence. */
@Serializable
public data class SportHealthLevelResponse(
    /** Assigned medical health group; no null was documented by the legacy model. The default is a client fallback. */
    @SerialName("health_level")
    public val healthLevel: SportHealthLevel = SportHealthLevel(),
)
