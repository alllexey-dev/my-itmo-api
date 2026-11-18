package dev.alllexey.itmoapi.myitmo.qr

import dev.alllexey.itmoapi.testing.mockExchange
import dev.alllexey.itmoapi.core.MyItmoException
import dev.alllexey.itmoapi.testing.fixture
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull

class QrApiTest {
    @Test
    fun passUsesAbsoluteQrHostAndPreservesBearerAndHexadecimalLeadingZeroes() = runTest {
        val pass = mockExchange("/v1/user/pass", fixture("qr/pass.json"), host = "qr.itmo.su") {
            QrApiImpl(it).getQrCode()
        }!!
        assertEquals("001122aabb", pass.qrHex)
        assertFalse(pass.toString().contains(pass.qrHex))
    }

    @Test
    fun anAnswerWithoutAPassIsNullNotAFailure() = runTest {
        for (body in listOf("""{"response":null}""", "{}", """{"response":{}}""", """{"response":{"qr_hex":""}}""")) {
            val pass = mockExchange("/v1/user/pass", body, host = "qr.itmo.su") { QrApiImpl(it).getQrCode() }
            assertNull(pass, body)
        }
    }

    @Test
    fun anUnreadableAnswerIsStillADecodeFailure() = runTest {
        assertFailsWith<MyItmoException.Decode> {
            mockExchange("/v1/user/pass", "not JSON", host = "qr.itmo.su") { QrApiImpl(it).getQrCode() }
        }
    }

    @Test
    fun externalServiceHttpFailureIsTypedBeforeDecode() = runTest {
        val error = assertFailsWith<MyItmoException.Http> {
            mockExchange("/v1/user/pass", "not JSON", host = "qr.itmo.su", status = 502) {
                QrApiImpl(it).getQrCode()
            }
        }
        assertEquals(502, error.status)
    }
}
