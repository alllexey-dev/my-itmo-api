package dev.alllexey.itmoapi.myitmo

import io.ktor.http.Url
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.seconds

class MyItmoConfigurationTest {
    @Test
    fun productionAndDevelopmentUseObservedOriginsAndSeparateOAuthClients() {
        assertEquals("my.itmo.ru", MyItmoConfiguration.DEFAULT.baseUrl.host)
        assertEquals("student-personal-cabinet", MyItmoConfiguration.DEFAULT.itmoId.clientId)
        assertEquals("https://my.itmo.ru/login/callback", MyItmoConfiguration.DEFAULT.itmoId.redirectUri)
        assertEquals("dev.my.itmo.su", MyItmoConfiguration.DEV.baseUrl.host)
        assertEquals("student-personal-cabinet-dev", MyItmoConfiguration.DEV.itmoId.clientId)
        assertEquals("https://dev.my.itmo.su/login/callback", MyItmoConfiguration.DEV.itmoId.redirectUri)
        assertEquals("ru", MyItmoConfiguration.DEFAULT.acceptLanguage)
        assertEquals(30.seconds, MyItmoConfiguration.DEFAULT.clockSkew)
    }

    @Test
    fun originAndLanguageRejectUnsafeConfigurationWithoutEchoingInput() {
        for (url in listOf("http://example.invalid", "https://example.invalid:8443", "https://caller@example.invalid", "https://example.invalid/api", "https://example.invalid?query=one", "https://example.invalid/#fragment")) {
            val failure = assertFailsWith<IllegalArgumentException> { MyItmoConfiguration(baseUrl = Url(url)) }
            assertEquals("Invalid MyITMO origin", failure.message)
        }
        assertFailsWith<IllegalArgumentException> { MyItmoConfiguration(acceptLanguage = "ru\r\nInjected: value") }
        assertFailsWith<IllegalArgumentException> { MyItmoConfiguration(clockSkew = (-1).seconds) }
    }
}
