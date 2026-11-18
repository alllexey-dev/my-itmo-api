package dev.alllexey.itmoapi.bars

import dev.alllexey.itmoapi.itmoid.ItmoIdConfiguration
import io.ktor.http.Url

/** Observed BARS REST and public OIDC client addresses. Issuer is shared with ITMO.ID. */
public class BarsConfiguration(
    public val restUrl: Url = Url("https://bars.itmo.ru/backend/rest/"),
    public val clientId: String = "bars",
    public val redirectUri: String = "https://bars.itmo.ru/rest/login",
    itmoId: ItmoIdConfiguration = ItmoIdConfiguration(),
) {
    /** ITMO.ID issuer supplied by the shared configuration, not a separate BARS realm. */
    public val issuer: String = itmoId.issuer
}
