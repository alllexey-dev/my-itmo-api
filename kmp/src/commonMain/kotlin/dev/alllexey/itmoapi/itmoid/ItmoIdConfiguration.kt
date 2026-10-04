package dev.alllexey.itmoapi.itmoid

/** ITMO.ID realm, public OAuth client and exact HTTPS redirect URI.
 * Defaults match the observed MyITMO client; BARS supplies its client/redirect while sharing issuer.
 */
public class ItmoIdConfiguration(
    public val issuer: String = "https://id.itmo.ru/auth/realms/itmo",
    public val clientId: String = "student-personal-cabinet",
    public val redirectUri: String = "https://my.itmo.ru/login/callback",
) {
    init {
        require(StrictHttpsUrl.parse(issuer) != null && '?' !in issuer) { "Invalid ITMO.ID issuer" }
        require(StrictHttpsUrl.parse(redirectUri) != null && '?' !in redirectUri) { "Invalid ITMO.ID redirect URI" }
        require(clientId.isNotBlank() && clientId.none { it.code < 32 || it.code == 127 }) { "Invalid ITMO.ID client identifier" }
    }

    /** Authorization endpoint in the configured realm; no token state is stored here. */
    public val authorizationEndpoint: String = issuer.trimEnd('/') + "/protocol/openid-connect/auth"

    /** Code exchange and refresh endpoint in the configured realm. */
    public val tokenEndpoint: String = issuer.trimEnd('/') + "/protocol/openid-connect/token"
}
