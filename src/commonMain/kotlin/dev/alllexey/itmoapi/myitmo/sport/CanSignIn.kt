package dev.alllexey.itmoapi.myitmo.sport

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Server decision about enrollment, with localized rejection reasons. */
@Serializable
public data class CanSignIn(
    /** Whether enrollment is currently permitted. */
    @SerialName("can_sign_in") public val canSignIn: Boolean = false,
    /** Reasons for refusal; empty when enrollment is allowed. */
    @SerialName("unavailable_reasons") public val unavailableReasons: List<String> = emptyList(),
)
