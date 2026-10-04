package dev.alllexey.itmoapi.myitmo.sport

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Enrollment limit and remaining places for a server-defined restriction group. */
@Serializable
public data class SportSignLimit(
    /** Maximum allowed enrollments. */
    public val limit: Int = 0,
    /** Remaining enrollments. */
    public val available: Int = 0,
)
