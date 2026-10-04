package dev.alllexey.itmoapi.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Standard MyITMO envelope. Zero error_code means success; nonzero values are API errors.
 * error_message is localized and observed null or absent on success; result may be absent on errors.
 */
@Serializable
public data class ResultResponse<T>(
    @SerialName("error_code") public val errorCode: Int = 0,
    @SerialName("error_message") public val errorMessage: String? = null,
    public val result: T? = null,
) {
    override fun toString(): String = "ResultResponse(errorCode=$errorCode, payload=[redacted])"
}

/** Legacy schedule envelope. Zero code means success; data is absent on errors.
 * message is localized and observed null or absent on success.
 */
@Serializable
public data class DataResponse<T>(
    public val code: Int = 0,
    public val data: T? = null,
    public val message: String? = null,
) {
    override fun toString(): String = "DataResponse(code=$code, payload=[redacted])"
}

/** External-service envelope, including QR. response is the payload; there is no wire error code.
 * Missing or null payloads cannot be unwrapped as successful results.
 */
@Serializable
public data class SimpleResponse<T>(public val response: T? = null) {
    override fun toString(): String = "SimpleResponse(payload=[redacted])"
}

/** Result page: count is the total number of items; data holds the page's typed payload. */
@Serializable
public data class CountWrapper<T>(public val count: Int = 0, public val data: T) {
    override fun toString(): String = "CountWrapper(count=$count, payload=[redacted])"
}

/** Reference-directory pair: numeric identifier and human-readable display value. */
@Serializable
public data class IdValuePair(public val id: Long = 0, public val value: String = "")

/** Unwraps a result or throws a typed API/decode error; status is supplied by the transport when known. */
@Throws(MyItmoException::class)
public fun <T : Any> ResultResponse<T>.requireResult(status: Int = 200): T {
    if (errorCode != 0) throw MyItmoException.Api(status, errorCode, errorMessage)
    return result ?: throw MyItmoException.Decode()
}

/** Unwraps the schedule data, honoring its own code rather than ResultResponse's error_code. */
@Throws(MyItmoException::class)
public fun <T : Any> DataResponse<T>.requireResult(status: Int = 200): T {
    if (code != 0) throw MyItmoException.Api(status, code, message)
    return data ?: throw MyItmoException.Decode()
}

/** Unwraps an external-service payload or throws Decode when response is absent/null. */
@Throws(MyItmoException::class)
public fun <T : Any> SimpleResponse<T>.requireResult(): T = response ?: throw MyItmoException.Decode()
