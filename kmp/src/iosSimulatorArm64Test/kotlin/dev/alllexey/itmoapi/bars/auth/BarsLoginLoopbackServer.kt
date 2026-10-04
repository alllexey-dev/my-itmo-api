@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package dev.alllexey.itmoapi.bars.auth

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
internal class BarsLoginLoopbackServer {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val requests = Channel<CookieRequest>(Channel.UNLIMITED)
    private val replies = Channel<CookieReply>(Channel.UNLIMITED)
    private var stopped = false
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
                    val reply = replies.receive()
                    val headers = buildList {
                        reply.location?.let { add("Location: $it") }
                        reply.setCookies.forEach { add("Set-Cookie: $it") }
                        add("Content-Type: text/html")
                        add("Content-Length: ${if (reply.unreadBody) 1048576 else 0}")
                        add("Connection: close")
                    }
                    val response = (listOf("HTTP/1.1 ${reply.status} Synthetic") + headers + listOf("", "")).joinToString("\r\n")
                    writeBytes(connection, response.encodeToByteArray())
                    if (reply.unreadBody) {
                        // Ktor Darwin exposes response metadata on its first data callback, not on headers alone.
                        // Send a small prefix, but never the advertised remainder: whole-body reads cannot finish.
                        writeBytes(connection, "x".repeat(1024).encodeToByteArray())
                        val byte = ByteArray(1)
                        byte.usePinned { recv(connection, it.addressOf(0), 1u, 0) }
                    }
                } finally {
                    close(connection)
                }
            }
        }
    }

    suspend fun enqueue(reply: CookieReply) { replies.send(reply) }

    suspend fun assertNoRequest() {
        kotlin.test.assertTrue(kotlinx.coroutines.withTimeoutOrNull(150) { requests.receive() } == null, "No redirect or extra request may occur")
    }

    suspend fun takeRequest(): CookieRequest = withTimeout(5_000) { requests.receive() }

    suspend fun stop() {
        if (stopped) return
        stopped = true
        scope.cancel()
        shutdown(listener, SHUT_RDWR)
        close(listener)
        withTimeout(5_000) { worker.join() }
        replies.close()
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

    private fun readRequest(connection: Int): CookieRequest {
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
        return CookieRequest(requestLine[0], requestLine[1], headers["cookie"]?.let { listOf(it) }.orEmpty(), headers["authorization"])
    }

    private fun writeBytes(connection: Int, bytes: ByteArray) = bytes.usePinned {
        var offset = 0
        while (offset < bytes.size) {
            val written = send(connection, it.addressOf(offset), (bytes.size - offset).convert(), 0).toInt()
            check(written > 0) { "Cannot send test response" }
            offset += written
        }
    }

}
