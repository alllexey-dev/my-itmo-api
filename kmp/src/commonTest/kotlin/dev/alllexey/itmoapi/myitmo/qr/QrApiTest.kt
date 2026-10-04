package dev.alllexey.itmoapi.myitmo.qr

import dev.alllexey.itmoapi.core.MyItmoException
import dev.alllexey.itmoapi.core.requireResult
import dev.alllexey.itmoapi.testing.fixture
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class QrApiTest {
    @Test
    fun passUsesAbsoluteQrHostAndPreservesBearerAndHexadecimalLeadingZeroes() = runTest {
        val pass = areaExchange("/v1/user/pass", fixture("qr/pass.json"), host = "qr.itmo.su") {
            QrApiImpl(it).getQrCode().requireResult()
        }
        assertEquals("001122aabb", pass.qrHex)
        assertFalse(pass.toString().contains(pass.qrHex))
    }

    @Test
    fun nullPassCannotBeUnwrappedAsSuccess() = runTest {
        assertFailsWith<MyItmoException.Decode> {
            areaExchange("/v1/user/pass", """{"response":null}""", host = "qr.itmo.su") {
                QrApiImpl(it).getQrCode().requireResult()
            }
        }
    }

    @Test
    fun externalServiceHttpFailureIsTypedBeforeDecode() = runTest {
        val error = assertFailsWith<MyItmoException.Http> {
            areaExchange("/v1/user/pass", "not JSON", host = "qr.itmo.su", status = 502) {
                QrApiImpl(it).getQrCode()
            }
        }
        assertEquals(502, error.status)
    }
}
