@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package dev.alllexey.itmoapi.itmoid

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.reinterpret
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.Security.SecRandomCopyBytes
import platform.Security.errSecSuccess
import platform.Security.kSecRandomDefault

internal actual fun secureRandomBytes(size: Int): ByteArray = ByteArray(size).also { bytes ->
    bytes.usePinned {
        check(SecRandomCopyBytes(kSecRandomDefault, size.toULong(), it.addressOf(0)) == errSecSuccess) {
            "Secure random generation failed"
        }
    }
}

internal actual fun sha256(bytes: ByteArray): ByteArray = ByteArray(CC_SHA256_DIGEST_LENGTH).also { digest ->
    bytes.usePinned { input ->
        digest.usePinned { output ->
            check(CC_SHA256(input.addressOf(0), bytes.size.toUInt(), output.addressOf(0).reinterpret()) != null) {
                "SHA-256 calculation failed"
            }
        }
    }
}
