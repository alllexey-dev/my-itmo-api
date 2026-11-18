package dev.alllexey.itmoapi.bars.auth

/** Result of replaying caller-owned ITMO.ID cookies for the BARS public OIDC client.
 * A live session returns a 302 callback; a login page means a session is required.
 * HTTP failures are distinct from an ended session. No code or cookie is included in diagnostics.
 */
public class BarsSessionCode internal constructor(
    /** Classification; no storage changes are performed for any outcome. */
    public val outcome: Outcome,
    /** Opaque authorization code for CODE only; absent for every other outcome. Never log this value. */
    public val code: String?,
    /** HTTP response status, or zero when caller cookies were absent and no request was made. */
    public val httpCode: Int,
    setCookies: List<String>,
) {
    /** Headers in response order, copied without storing them in any cookie jar.
     * Darwin normalizes attribute spelling/order while preserving cookie semantics and Expires commas.
     * Treat returned values as secrets; consumers decide whether to update their own storage.
     */
    public val setCookies: List<String> = setCookies.toList()

    /** Four outcomes mapped individually by consumers; transport failures are exceptions instead. */
    public enum class Outcome {
        /** Exact callback with a matching state and usable code. */
        CODE,
        /** Missing caller cookies, a trusted issuer page or a 2xx login page. */
        LOGIN_REQUIRED,
        /** Foreign redirect or callback that fails code/state/issuer validation. */
        REJECTED,
        /** Non-2xx/non-redirect response, or a redirect with no Location header. */
        HTTP_ERROR,
    }

    /** Redacted diagnostic form: status is zero when no request was made. */
    override public fun toString(): String = "BarsSessionCode(outcome=$outcome, httpCode=$httpCode)"
}
