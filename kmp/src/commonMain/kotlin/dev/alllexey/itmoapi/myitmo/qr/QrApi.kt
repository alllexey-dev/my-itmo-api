package dev.alllexey.itmoapi.myitmo.qr

import dev.alllexey.itmoapi.core.ItmoTransport
import dev.alllexey.itmoapi.core.MyItmoException
import dev.alllexey.itmoapi.core.SimpleResponse
import io.ktor.client.request.url
import io.ktor.http.HttpMethod
import kotlinx.coroutines.CancellationException

/** Digital passes from the separate QR service, authenticated with the MyITMO bearer. */
public interface QrApi {
    /** GET https://qr.itmo.su/v1/user/pass; response contains the hexadecimal digital pass.
     * Uses the absolute QR host, independent of the configured MyITMO base URL.
     */
    @Throws(MyItmoException::class, CancellationException::class)
    public suspend fun getQrCode(): SimpleResponse<QrData>
}

internal class QrApiImpl(private val transport: ItmoTransport) : QrApi {
    override suspend fun getQrCode(): SimpleResponse<QrData> = transport.execute(
        SimpleResponse.serializer(QrData.serializer()), HttpMethod.Get, "v1/user/pass",
    ) { url("https://qr.itmo.su/v1/user/pass") }
}
