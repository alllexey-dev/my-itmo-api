package dev.alllexey.itmoapi.itmoid

import dev.alllexey.itmoapi.core.ItmoApiJson
import dev.alllexey.itmoapi.core.MyItmoException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.time.Instant

/** Keeps the session in a JSON file so a script or a server survives restarts.
 * The file holds the refresh token in plain text: keep it out of repositories and backups; on POSIX
 * systems it is created readable by the owner only. Writes replace the file atomically; null deletes it.
 * One process per file: several processes need a shared [TokenRefreshGuard] or their own files.
 * For Android use encrypted storage (Keystore) instead.
 */
public class FileTokenStorage(private val path: Path) : TokenStorage {
    private val mutex = Mutex()

    @Throws(MyItmoException::class, CancellationException::class)
    override suspend fun read(): TokenSet? = io {
        if (!Files.exists(path)) return@io null
        val stored = try {
            ItmoApiJson.decodeFromString(StoredTokens.serializer(), String(Files.readAllBytes(path), Charsets.UTF_8))
        } catch (_: SerializationException) {
            throw MyItmoException.Decode()
        } catch (_: IllegalArgumentException) {
            throw MyItmoException.Decode()
        }
        try {
            TokenSet(stored.accessToken, stored.accessExpiresAt, stored.refreshToken, stored.refreshExpiresAt, stored.idToken)
        } catch (_: IllegalArgumentException) {
            throw MyItmoException.Decode()
        }
    }

    @Throws(MyItmoException::class, CancellationException::class)
    override suspend fun write(tokens: TokenSet?): Unit = io {
        if (tokens == null) {
            Files.deleteIfExists(path)
            return@io
        }
        val json = ItmoApiJson.encodeToString(
            StoredTokens.serializer(),
            StoredTokens(tokens.accessToken, tokens.accessExpiresAt, tokens.refreshToken, tokens.refreshExpiresAt, tokens.idToken),
        )
        val directory = path.toAbsolutePath().parent
        Files.createDirectories(directory)
        // createTempFile makes owner-only files on POSIX; the move keeps those permissions.
        val temporary = Files.createTempFile(directory, ".tokens", ".tmp")
        try {
            Files.write(temporary, json.toByteArray(Charsets.UTF_8))
            try {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    private suspend fun <T> io(action: () -> T): T = mutex.withLock { withContext(Dispatchers.IO) { action() } }

    override fun toString(): String = "FileTokenStorage(redacted)"

    @Serializable
    private class StoredTokens(
        val accessToken: String,
        val accessExpiresAt: Instant,
        val refreshToken: String,
        val refreshExpiresAt: Instant,
        val idToken: String,
    )
}
