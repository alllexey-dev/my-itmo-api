package dev.alllexey.itmoapi.bars

import dev.alllexey.itmoapi.core.ItmoTransport
import dev.alllexey.itmoapi.core.createItmoHttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.http.Url

/** Injectable bars client shell. Operations and configuration belong to ML-08a.
 * No HTTP client is constructed until an operation needs the internal transport.
 */
public class BarsClient(engine: HttpClientEngine, baseUrl: Url = Url("https://bars.itmo.ru/backend/rest/")) {
    internal val transport: Lazy<ItmoTransport> = lazy { ItmoTransport(createItmoHttpClient(engine), baseUrl) }

    /** Releases the initialized client without forcing initialization or closing the caller's engine. */
    public fun close(): Unit {
        if (transport.isInitialized()) transport.value.client.close()
    }
}
