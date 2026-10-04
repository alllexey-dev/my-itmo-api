package dev.alllexey.itmoapi

import kotlinx.serialization.Serializable

/** Synthetic payload proving serialization works on every declared target. */
@Serializable
internal data class SmokePayload(val message: String)
