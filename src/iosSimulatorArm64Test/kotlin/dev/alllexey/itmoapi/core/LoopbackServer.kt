@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package dev.alllexey.itmoapi.core

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import platform.posix.AF_INET
import platform.posix.SHUT_RDWR
import platform.posix.SOCK_STREAM
import platform.posix.SOL_SOCKET
import platform.posix.SO_NOSIGPIPE
import platform.posix.SO_RCVTIMEO
import platform.posix.SO_SNDTIMEO
import platform.posix.accept
import platform.posix.bind
import platform.posix.close
import platform.posix.getsockname
import platform.posix.listen
import platform.posix.recv
import platform.posix.send
import platform.posix.setsockopt
import platform.posix.shutdown
import platform.posix.sockaddr_in
import platform.posix.socket
import platform.posix.socklen_tVar
import platform.posix.timeval
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.get
import kotlinx.cinterop.set

/** Synthetic loopback-only HTTP/1.1 test server. No separate process, fixed port or workflow setup is needed. */
internal class LoopbackServer {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val requests = Channel<WireRequest>(Channel.UNLIMITED)
    private val listener = socket(AF_INET, SOCK_STREAM, 0).also { check(it >= 0) { "Cannot create test socket" } }
    val baseUrl: String
    private val worker: kotlinx.coroutines.Job

    init {
        baseUrl = memScoped {
            val address = alloc<sockaddr_in>()
            address.sin_len = sizeOf<sockaddr_in>().convert()
            address.sin_family = AF_INET.convert()
            address.sin_port = 0u
            // Native interop does not expose arpa/inet macros here; assign network-order IPv4 bytes.
            val ip = address.sin_addr.ptr.reinterpret<UByteVar>()
            ip[0] = 127u; ip[1] = 0u; ip[2] = 0u; ip[3] = 1u
            check(bind(listener, address.ptr.reinterpret(), sizeOf<sockaddr_in>().convert()) == 0) { "Cannot bind test socket" }
            check(listen(listener, 8) == 0) { "Cannot listen on test socket" }
            val length = alloc<socklen_tVar>()
            length.value = sizeOf<sockaddr_in>().convert()
            check(getsockname(listener, address.ptr.reinterpret(), length.ptr) == 0) { "Cannot read test port" }
            // Darwin sockaddr_in starts with len/family, then the two network-order port bytes.
            val raw = address.ptr.reinterpret<UByteVar>()
            val port = raw[2].toInt() * 256 + raw[3].toInt()
            "http://127.0.0.1:$port"
        }
        worker = scope.launch {
            while (isActive) {
                val connection = accept(listener, null, null)
                if (connection < 0) break
                try {
                    configureConnection(connection)
                    val request = readRequest(connection)
                    requests.send(request)
                    val response = when (request.path) {
                        "/redirect" -> response(302, "", listOf("Location: /landing") + cookieHeaders())
                        "/cookies" -> response(200, "ok", cookieHeaders())
                        "/cacheable" -> response(200, "ok", listOf("Cache-Control: public, max-age=3600"))
                        else -> response(200, "ok")
                    }
                    writeBytes(connection, response.encodeToByteArray())
                } finally {
                    close(connection)
                }
            }
        }
    }

    suspend fun takeRequest(): WireRequest = withTimeout(5_000) { requests.receive() }

    suspend fun stop() {
        scope.cancel()
        shutdown(listener, SHUT_RDWR)
        close(listener)
        withTimeout(5_000) { worker.join() }
        requests.close()
    }

    private fun configureConnection(connection: Int) = memScoped {
        val timeout = alloc<timeval>()
        timeout.tv_sec = 3
        timeout.tv_usec = 0
        setsockopt(connection, SOL_SOCKET, SO_RCVTIMEO, timeout.ptr, sizeOf<timeval>().convert())
        setsockopt(connection, SOL_SOCKET, SO_SNDTIMEO, timeout.ptr, sizeOf<timeval>().convert())
        val one = alloc<IntVar>()
        one.value = 1
        setsockopt(connection, SOL_SOCKET, SO_NOSIGPIPE, one.ptr, sizeOf<IntVar>().convert())
    }

    private fun readRequest(connection: Int): WireRequest {
        val bytes = mutableListOf<Byte>()
        val single = ByteArray(1)
        fun readByte(): Byte = single.usePinned {
            check(recv(connection, it.addressOf(0), 1u, 0) == 1L) { "Incomplete test request" }
            single[0]
        }
        fun readLine(): String {
            val line = mutableListOf<Byte>()
            while (true) {
                line += readByte()
                check(line.size <= 8192) { "Test request line is too large" }
                if (line.size >= 2 && line.takeLast(2) == listOf(13.toByte(), 10.toByte())) {
                    return line.dropLast(2).toByteArray().decodeToString()
                }
            }
        }
        val requestLine = readLine().split(' ')
        val headers = mutableMapOf<String, String>()
        while (true) {
            val line = readLine()
            if (line.isEmpty()) break
            headers[line.substringBefore(':').lowercase()] = line.substringAfter(':').trim()
        }
        if (headers["transfer-encoding"]?.lowercase() == "chunked") {
            while (true) {
                val size = readLine().substringBefore(';').toInt(16)
                if (size == 0) { readLine(); break }
                repeat(size) { bytes += readByte() }
                check(readByte() == 13.toByte() && readByte() == 10.toByte()) { "Invalid test chunk" }
            }
        } else {
            repeat(headers["content-length"]?.toInt() ?: 0) { bytes += readByte() }
        }
        return WireRequest(requestLine[0], requestLine[1], headers, bytes.toByteArray().decodeToString())
    }

    private fun writeBytes(connection: Int, bytes: ByteArray) = bytes.usePinned {
        var offset = 0
        while (offset < bytes.size) {
            val written = send(connection, it.addressOf(offset), (bytes.size - offset).convert(), 0).toInt()
            check(written > 0) { "Cannot send test response" }
            offset += written
        }
    }

    private fun cookieHeaders(): List<String> = SYNTHETIC_COOKIES.map { "Set-Cookie: $it" }

    private fun response(status: Int, body: String, headers: List<String> = emptyList()): String =
        (listOf("HTTP/1.1 $status ${if (status == 302) "Found" else "OK"}") + headers + listOf(
            "Content-Type: text/plain", "Content-Length: ${body.encodeToByteArray().size}", "Connection: close", "", body,
        )).joinToString("\r\n")
}

internal class WireRequest(val method: String, val path: String, val headers: Map<String, String>, val body: String) {
    override fun toString(): String = "WireRequest(method=$method, contents=[redacted])"
}

internal val SYNTHETIC_COOKIES: List<String> = listOf(
    "synthetic_session=a; Path=/; HttpOnly; SameSite=Lax",
    "synthetic_expires=b; Expires=Wed, 21 Oct 2026 07:28:00 GMT; Path=/realms/itmo/; HttpOnly",
    "synthetic_maxage=c; Max-Age=3600; Path=/; Secure; SameSite=None",
)
