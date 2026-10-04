package dev.alllexey.itmoapi.myitmo.qr

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Digital pass payload from qr.itmo.su; qr_hex is a hexadecimal string, not an integer.
 * No null variant is documented; absent fields use an empty default. Never log the pass.
 */
@Serializable
public data class QrData(
    /** Hexadecimal representation of the user's QR pass. */
    @SerialName("qr_hex") public val qrHex: String = "",
) {
    override fun toString(): String = "QrData(pass=[redacted])"
}
