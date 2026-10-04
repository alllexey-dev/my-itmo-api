package dev.alllexey.itmoapi.itmoid

/** Strict HTTPS callback allow-list, shared by ITMO.ID and BARS login.
 * No userinfo, IDN, nonstandard port, fragment, duplicate decoded query key or malformed encoding is accepted.
 * Codes and caller state are never included in diagnostics.
 */
public class CallbackUrl(redirectUri: String, private val issuer: String) {
    private val callback = requireNotNull(StrictHttpsUrl.parse(redirectUri)) { "Invalid callback configuration" }
    private val authority = requireNotNull(StrictHttpsUrl.parse(issuer)) { "Invalid issuer configuration" }

    init {
        require(callback.query.isEmpty() && authority.query.isEmpty()) { "Callback configuration must not contain query parameters" }
    }

    /** Tests exact host/path and structural rules; state/code validation belongs to extractCode. */
    public fun isCallback(url: String): Boolean = StrictHttpsUrl.parse(url)?.let {
        it.host == callback.host && it.path == callback.path
    } == true

    /** Permits trusted issuer-host pages or the exact callback, with the same strict structural rules. */
    public fun isAllowedPage(url: String): Boolean = StrictHttpsUrl.parse(url)?.let {
        it.host == authority.host || (it.host == callback.host && it.path == callback.path)
    } == true

    /** Returns a code only after origin/path, duplicate-query, state and issuer checks.
     * Issuer absence preserves the owner-approved 1.x contract; every present issuer must match exactly.
     * OAuth error callbacks and absent/empty/oversized codes are rejected; never log returned codes.
     */
    public fun extractCode(url: String, expectedState: String): String? {
        val parsed = StrictHttpsUrl.parse(url) ?: return null
        if (parsed.host != callback.host || parsed.path != callback.path || expectedState.isBlank()) return null
        val query = parsed.query
        if (query["state"] != expectedState || "error" in query) return null
        if (query["iss"] != null && query["iss"] != issuer) return null
        return query["code"]?.takeIf { it.isNotBlank() && it.length <= 4096 }
    }
}

/** Parses the restricted URI grammar before any Ktor normalization can widen the allow-list. */
internal class StrictHttpsUrl(val host: String, val path: String, val query: Map<String, String>) {
    companion object {
        fun parse(value: String): StrictHttpsUrl? {
            if (value.length > 16384 || !value.startsWith("https://", ignoreCase = true) || value.any { it.code !in 33..126 || it in "\\\"<>^`{|}#" }) return null
            val rest = value.substring(8)
            val authorityEnd = rest.indexOfFirst { it == '/' || it == '?' }.let { if (it < 0) rest.length else it }
            val authority = rest.substring(0, authorityEnd)
            if ('@' in authority || '%' in authority) return null
            val host = authority.removeSuffix(":443").lowercase()
            if (':' in host || host.isEmpty() || host.endsWith('.') || host.split('.').any {
                    it.isEmpty() || it.startsWith("xn--") || it.startsWith('-') || it.endsWith('-') || it.any { char -> char !in 'a'..'z' && char !in '0'..'9' && char != '-' }
                }) return null
            val tail = rest.substring(authorityEnd)
            val rawPath = tail.substringBefore('?').ifEmpty { "/" }
            if ('%' in rawPath || !rawPath.startsWith('/')) return null
            val query = linkedMapOf<String, String>()
            if ('?' in tail) {
                for (part in tail.substringAfter('?').split('&')) {
                    if (part.isEmpty()) return null
                    val name = decode(part.substringBefore('=')) ?: return null
                    val content = decode(part.substringAfter('=', "")) ?: return null
                    if (name.isEmpty() || name.any { it !in QUERY_KEY } || name in query) return null
                    query[name] = content
                }
            }
            return StrictHttpsUrl(host, rawPath, query)
        }

        private fun decode(value: String): String? {
            val bytes = mutableListOf<Byte>()
            var position = 0
            while (position < value.length) {
                when (val char = value[position]) {
                    '%' -> {
                        if (position + 2 >= value.length) return null
                        val high = value[position + 1].digitToIntOrNull(16) ?: return null
                        val low = value[position + 2].digitToIntOrNull(16) ?: return null
                        bytes += ((high shl 4) or low).toByte()
                        position += 3
                    }
                    else -> {
                        bytes += (if (char == '+') ' ' else char).code.toByte()
                        position++
                    }
                }
            }
            val decoded = try { bytes.toByteArray().decodeToString(throwOnInvalidSequence = true) } catch (_: CharacterCodingException) { return null }
            return decoded.takeIf { it.none { char -> char.code < 32 || char.code == 127 } }
        }

        private const val QUERY_KEY = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"
    }
}
