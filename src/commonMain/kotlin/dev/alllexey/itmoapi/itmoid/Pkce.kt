package dev.alllexey.itmoapi.itmoid

import kotlin.io.encoding.Base64
import kotlin.uuid.Uuid

/** RFC 7636 S256 helpers. Verifiers are caller-owned secrets; never log or persist them as fixtures. */
public object Pkce {
    /** Generates a 43-character base64url verifier from 32 cryptographically random bytes. */
    public fun newVerifier(): String = base64Url(secureRandomBytes(32))

    /** Computes BASE64URL(SHA256(ASCII(verifier))) without padding.
     * RFC 7636 permits 43–128 unreserved ASCII characters; invalid input is never echoed.
     */
    public fun challenge(verifier: String): String {
        require(verifier.length in 43..128 && verifier.all { it in UNRESERVED }) { "Invalid PKCE verifier" }
        return base64Url(sha256(verifier.encodeToByteArray()))
    }

    /** Returns a fresh state to bind the caller's authorization request and callback. */
    public fun newState(): String = Uuid.random().toString()

    private fun base64Url(bytes: ByteArray): String = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT).encode(bytes)
    private const val UNRESERVED = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"
}

internal expect fun secureRandomBytes(size: Int): ByteArray
internal expect fun sha256(bytes: ByteArray): ByteArray
