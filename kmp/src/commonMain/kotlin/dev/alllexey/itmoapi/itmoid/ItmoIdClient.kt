package dev.alllexey.itmoapi.itmoid

import dev.alllexey.itmoapi.core.ItmoTransport
import dev.alllexey.itmoapi.core.createItmoHttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.http.Url

/** Injectable itmoid client shell. Operations and configuration belong to ML-04a.
 * No HTTP client is constructed until an operation needs the internal transport.
 */
public class ItmoIdClient(engine: HttpClientEngine, baseUrl: Url = Url("https://id.itmo.ru/")) {
    internal val transport: Lazy<ItmoTransport> = lazy { ItmoTransport(createItmoHttpClient(engine), baseUrl) }

    /** Releases the initialized client without forcing initialization or closing the caller's engine. */
    public fun close(): Unit {
        if (transport.isInitialized()) transport.value.client.close()
    }
}
