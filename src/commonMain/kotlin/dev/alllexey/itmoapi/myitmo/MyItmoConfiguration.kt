package dev.alllexey.itmoapi.myitmo

import dev.alllexey.itmoapi.itmoid.ItmoIdConfiguration
import dev.alllexey.itmoapi.itmoid.StrictHttpsUrl
import io.ktor.http.Url
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** Immutable MyITMO origin, ITMO.ID OAuth configuration and localized payload language.
 * Language defaults to ru: personalities otherwise returns transliterated names.
 * [clockSkew] refreshes access tokens before expiry; no global settings or session state exist.
 */
public class MyItmoConfiguration(
    public val baseUrl: Url = Url("https://my.itmo.ru"),
    public val itmoId: ItmoIdConfiguration = ItmoIdConfiguration(),
    public val acceptLanguage: String = "ru",
    public val clockSkew: Duration = 30.seconds,
) {
    init {
        require(StrictHttpsUrl.parse(baseUrl.toString())?.let { it.path == "/" && it.query.isEmpty() } == true) {
            "Invalid MyITMO origin"
        }
        require(acceptLanguage.isNotBlank() && acceptLanguage.all { it.code in 32..126 }) { "Invalid payload language" }
        require(clockSkew.isFinite() && !clockSkew.isNegative()) { "Invalid token clock skew" }
    }

    public companion object {
        /** Official production MyITMO and its observed ITMO.ID public client. */
        public val DEFAULT: MyItmoConfiguration = MyItmoConfiguration()

        /** Official development MyITMO with its separate ITMO.ID public client and redirect. */
        public val DEV: MyItmoConfiguration = MyItmoConfiguration(
            baseUrl = Url("https://dev.my.itmo.su"),
            itmoId = ItmoIdConfiguration(
                clientId = "student-personal-cabinet-dev",
                redirectUri = "https://dev.my.itmo.su/login/callback",
            ),
        )
    }
}
