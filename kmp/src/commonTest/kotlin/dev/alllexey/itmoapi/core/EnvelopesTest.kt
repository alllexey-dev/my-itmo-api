package dev.alllexey.itmoapi.core

import dev.alllexey.itmoapi.testing.fixture
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EnvelopesTest {
    @Test
    fun resultResponseUnwrapsAndUsesSnakeCaseNames() {
        val response = ItmoApiJson.decodeFromString<ResultResponse<Int>>("""{"error_code":0,"result":7}""")
        assertEquals(7, response.requireResult())
        assertNull(response.errorMessage)
        assertEquals("""{"error_code":0,"result":7}""", ItmoApiJson.encodeToString(response))
    }

    @Test
    fun resultResponseMapsHttp200ApiFailure() {
        val response = ItmoApiJson.decodeFromString<ResultResponse<Int>>(fixture("errors/error-code-200.json"))
        val failure = assertFailsWith<MyItmoException.Api> { response.requireResult() }
        assertEquals(200, failure.status)
        assertEquals(3, failure.errorCode)
        assertEquals(response.errorMessage, failure.serverMessage)
    }

    @Test
    fun resultResponsePreserves400AndError100() {
        val response = ItmoApiJson.decodeFromString<ResultResponse<Int>>(fixture("errors/error-envelope-400.json"))
        val failure = assertFailsWith<MyItmoException.Api> { response.requireResult(400) }
        assertEquals(400, failure.status)
        assertEquals(100, failure.errorCode)
        assertNull(failure.serverMessage)
    }

    @Test
    fun dataResponseUnwrapsItsOwnEnvelope() {
        val response = ItmoApiJson.decodeFromString<DataResponse<String>>("""{"code":0,"data":"example"}""")
        assertEquals("example", response.requireResult())
        assertNull(response.message)
    }

    @Test
    fun dataResponseMapsApiCode() {
        val response = DataResponse<String>(code = 5, message = "untrusted marker")
        val failure = assertFailsWith<MyItmoException.Api> { response.requireResult(400) }
        assertEquals(400, failure.status)
        assertEquals(5, failure.errorCode)
        assertEquals(response.message, failure.serverMessage)
    }

    @Test
    fun simpleResponseUnwrapsItsOwnPayload() {
        val response = ItmoApiJson.decodeFromString<SimpleResponse<Int>>("""{"response":9}""")
        assertEquals(9, response.requireResult())
    }

    @Test
    fun missingAndNullSuccessPayloadsAreDecodeFailures() {
        assertFailsWith<MyItmoException.Decode> { ResultResponse<String>().requireResult() }
        assertFailsWith<MyItmoException.Decode> { DataResponse<String>().requireResult() }
        assertFailsWith<MyItmoException.Decode> { SimpleResponse<String>().requireResult() }
    }

    @Test
    fun countAndIdValueWrappersRoundTrip() {
        val response = CountWrapper(2, listOf(IdValuePair(7, "first"), IdValuePair(8, "second")))
        assertEquals(response, ItmoApiJson.decodeFromString<CountWrapper<List<IdValuePair>>>(ItmoApiJson.encodeToString(response)))
    }

    @Test
    fun envelopesAndApiExceptionsNeverRenderRemoteMessagesOrPayloads() {
        val marker = "untrusted marker"
        val failure = MyItmoException.Api(400, 100, marker)
        assertEquals(marker, failure.serverMessage)
        val renderings = listOf(
            ResultResponse(3, marker, marker).toString(), DataResponse(3, marker, marker).toString(),
            SimpleResponse(marker).toString(), CountWrapper(1, marker).toString(),
            failure.message.orEmpty(), failure.toString(),
        )
        renderings.forEach { assertTrue(marker !in it) }
        assertNull(failure.cause)
    }
}
