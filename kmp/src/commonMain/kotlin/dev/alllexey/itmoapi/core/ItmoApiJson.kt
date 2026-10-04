package dev.alllexey.itmoapi.core

import kotlinx.serialization.json.Json

/** MyITMO wire JSON: unknown keys are ignored and observed nulls use declared defaults.
 * Quoted numbers and booleans are supported without lenient JSON syntax (SP-02, config B).
 */
public val ItmoApiJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
    coerceInputValues = true
    isLenient = false
}
