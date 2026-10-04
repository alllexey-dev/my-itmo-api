package dev.alllexey.itmoapi.itmoid

import kotlin.time.Instant

/** Atomic token snapshot with the same five values as 1.x Storage.
 * Lifetimes arrive in seconds and are converted to instants using the caller's injected Clock.
 * Every token is required; secrets never appear in toString or exception diagnostics.
 */
public class TokenSet(
    /** Short-lived MyITMO API bearer token. */
    public val accessToken: String,
    /** Access-token expiry instant, not epoch milliseconds or a relative lifetime. */
    public val accessExpiresAt: Instant,
    /** Refresh token; ITMO.ID may rotate it on each response. */
    public val refreshToken: String,
    /** Refresh-token expiry instant. */
    public val refreshExpiresAt: Instant,
    /** OIDC ID token carrying the caller's identity claims. */
    public val idToken: String,
) {
    init {
        require(accessToken.isNotBlank() && refreshToken.isNotBlank() && idToken.isNotBlank()) { "Incomplete token set" }
    }

    /** Redacted diagnostics disclose no token values. The observed wire session_state is intentionally
     * not retained: ML-04a preserves the five-field 1.x Storage snapshot, not every TokenResponse member.
     */
    override fun toString(): String = "TokenSet(redacted)"
}
