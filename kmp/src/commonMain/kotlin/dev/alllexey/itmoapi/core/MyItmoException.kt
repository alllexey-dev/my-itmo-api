package dev.alllexey.itmoapi.core

/** Typed failures whose diagnostics never contain remote bodies, URLs or credentials. */
public sealed class MyItmoException protected constructor(message: String, cause: Throwable? = null) :
    Exception(message, cause) {

    /** Engine I/O failure. The original cause is retained for consumer connectivity classification.
     * Do not log the cause: engine diagnostics can contain request data.
     */
    public class Network(cause: Throwable) : MyItmoException("Network request failed", cause)

    /** Unsuccessful HTTP status without a recognized API error envelope. */
    public class Http(public val status: Int) : MyItmoException("HTTP request failed (status=$status)")

    /** API error, including error envelopes returned with HTTP 200 or HTTP 400.
     * Incoming [message] is retained as [serverMessage] only for explicit domain reason classification.
     * It is untrusted wire text: never log it. Exception diagnostics remain redacted.
     */
    public class Api(public val status: Int, public val errorCode: Int, message: String? = null) :
        MyItmoException("API request failed (status=$status, errorCode=$errorCode)") {
        /** Untrusted wire reason; read explicitly for domain classification, never for logging. */
        public val serverMessage: String? = message
    }

    /** The request is unauthenticated (HTTP 401 or 403), without a more specific API error. */
    public class Auth(public val status: Int) : MyItmoException("Authentication required (status=$status)")

    /** A successful body cannot be decoded or its required payload is absent.
     * Parser causes are not attached: they can echo the response or a secret field.
     */
    public class Decode : MyItmoException("Response could not be decoded")
}
