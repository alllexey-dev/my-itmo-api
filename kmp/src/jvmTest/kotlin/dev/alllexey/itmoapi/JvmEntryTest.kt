package dev.alllexey.itmoapi

import dev.alllexey.itmoapi.core.MyItmoException
import dev.alllexey.itmoapi.itmoid.FileTokenStorage
import dev.alllexey.itmoapi.itmoid.TokenSet
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.time.Instant

class JvmEntryTest {
    private val snapshot = TokenSet(
        "synthetic-access", Instant.parse("2026-10-12T09:05:00Z"),
        "synthetic-refresh", Instant.parse("2026-10-12T10:00:00Z"), "synthetic-id",
    )

    @Test
    fun fileStorageSurvivesANewInstanceIsOwnerOnlyAndDeletesOnLogout() = runTest {
        val directory = Files.createTempDirectory("tokens")
        val path = directory.resolve("nested/tokens.json")
        try {
            assertNull(FileTokenStorage(path).read())
            FileTokenStorage(path).write(snapshot)
            val restored = FileTokenStorage(path).read()
            assertEquals(snapshot.refreshToken, restored?.refreshToken)
            assertEquals(snapshot.accessExpiresAt, restored?.accessExpiresAt)
            assertEquals(snapshot.refreshExpiresAt, restored?.refreshExpiresAt)
            assertEquals(snapshot.idToken, restored?.idToken)
            if ("posix" in path.fileSystem.supportedFileAttributeViews()) {
                assertEquals(setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE), Files.getPosixFilePermissions(path))
            }
            assertEquals(listOf("tokens.json"), Files.list(path.parent).use { files -> files.map { it.fileName.toString() }.toList() })
            assertFalse("synthetic" in FileTokenStorage(path).toString())
            FileTokenStorage(path).write(null)
            assertFalse(Files.exists(path))
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun aDamagedFileIsDecodeNotAnEmptySession() = runTest {
        val path = Files.createTempFile("tokens", ".json")
        try {
            Files.write(path, "{\"accessToken\":\"\"}".toByteArray())
            assertFailsWith<MyItmoException.Decode> { FileTokenStorage(path).read() }
        } finally {
            Files.deleteIfExists(path)
        }
    }

    private suspend fun suspending(value: String): String {
        delay(1)
        return value
    }

    private suspend fun failing(): String {
        delay(1)
        throw MyItmoException.Http(503)
    }

    @Test
    fun blockingReturnsSuspendedResultsAndRethrowsFailures() {
        assertEquals("synthetic", Blocking.call<String> { continuation -> callSuspending(continuation) })
        assertEquals(503, assertFailsWith<MyItmoException.Http> { Blocking.call<String> { callFailing(it) } }.status)
    }

    // Java sees suspend functions as methods with a trailing continuation; these mirror that shape.
    @Suppress("UNCHECKED_CAST")
    private fun callSuspending(continuation: kotlin.coroutines.Continuation<String>): Any? =
        (::suspending as Function2<String, kotlin.coroutines.Continuation<String>, Any?>)("synthetic", continuation)

    @Suppress("UNCHECKED_CAST")
    private fun callFailing(continuation: kotlin.coroutines.Continuation<String>): Any? =
        (::failing as Function1<kotlin.coroutines.Continuation<String>, Any?>)(continuation)
}
