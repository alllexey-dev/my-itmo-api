package dev.alllexey.itmoapi.blocking

import dev.alllexey.itmoapi.core.MyItmoException
import dev.alllexey.itmoapi.myitmo.qr.*
import kotlinx.coroutines.runBlocking

/** Blocking view of [QrApi] for Java: each method waits for the result on the calling thread.
 * Do not call from an Android main thread.
 * Mirrors the interface method by method; `BlockingParityTest` fails when the two drift apart.
 */
public class BlockingQr internal constructor(private val api: QrApi) {
    /** Blocking [QrApi.getQrCode]. */
    @Throws(MyItmoException::class)
    public fun getQrCode(): QrData = runBlocking { api.getQrCode() }
}
