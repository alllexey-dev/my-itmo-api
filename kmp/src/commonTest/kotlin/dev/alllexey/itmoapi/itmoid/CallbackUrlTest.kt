package dev.alllexey.itmoapi.itmoid

import io.ktor.http.URLBuilder
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class CallbackUrlTest {
    private val redirect = "https://my.itmo.ru/login/callback"
    private val issuer = "https://id.itmo.ru/auth/realms/itmo"
    private val callback = CallbackUrl(redirect, issuer)
    private val state = Pkce.newState()
    private val code = Uuid.random().toString()

    private fun validUrl(): String = URLBuilder(redirect).apply {
        parameters.append("state", state)
        parameters.append("iss", issuer)
        parameters.append("code", code)
    }.buildString()

    @Test
    fun exactCallbackWithMatchingStateAndIssuerReturnsCode() {
        assertTrue(callback.isCallback(validUrl()))
        assertTrue(callback.extractCode(validUrl(), state) == code, "Valid callback code")
    }

    @Test
    fun explicit443AndDnsCaseHaveEquivalentTrustedOrigin() {
        val url = validUrl().replace("my.itmo.ru", "MY.ITMO.RU:443")
        assertTrue(callback.extractCode(url, state) == code, "HTTPS default port")
    }

    @Test
    fun lookAlikeHostsIdnAndHostEncodingAreRejected() {
        for (host in listOf("my.itmo.ru.example.invalid", "myitmo.ru", "my.itmо.ru", "xn--myitmo-q6a.ru", "my%2Eitmo.ru", "my.itmo.ru.")) {
            assertTrue(callback.extractCode(validUrl().replace("my.itmo.ru", host), state) == null, "Untrusted host rejected")
        }
    }

    @Test
    fun userinfoNonstandardPortAndFragmentAreRejected() {
        val variants = listOf(validUrl().replace("my.itmo.ru", "caller@my.itmo.ru"), validUrl().replace("my.itmo.ru", "my.itmo.ru:8443"), validUrl() + "#fragment")
        variants.forEach { assertTrue(callback.extractCode(it, state) == null, "Unsafe authority/fragment rejected") }
    }

    @Test
    fun wrongSchemePathAndEncodedPathAreRejected() {
        val variants = listOf(validUrl().replace("https:", "http:"), validUrl().replace("/login/callback", "/login/callback/"), validUrl().replace("/login/callback", "/login/%63allback"), validUrl().replace("/login/callback", "/login/../callback"))
        variants.forEach { assertTrue(callback.extractCode(it, state) == null, "Exact HTTPS path required") }
    }

    @Test
    fun duplicatesAreRejectedAfterPercentDecodingEveryParameterName() {
        for (key in listOf("code", "co%64e", "state", "iss", "unused")) {
            val initial = if (key == "unused") validUrl() + "&unused=one" else validUrl()
            assertTrue(callback.extractCode(initial + "&$key=two", state) == null, "Duplicate query rejected")
        }
    }

    @Test
    fun stateMismatchAndMissingOrBlankStateAreRejected() {
        assertTrue(callback.extractCode(validUrl(), Pkce.newState()) == null, "State mismatch")
        assertTrue(callback.extractCode(validUrl(), "") == null, "Blank expected state")
        assertTrue(callback.extractCode(validUrl().replace("state=$state", "state="), state) == null, "Blank received state")
        assertTrue(callback.extractCode(validUrl().replace("state=$state&", ""), state) == null, "Missing state")
    }

    @Test
    fun absentIssuerPreservesTheOwnerApprovedOneXContract() {
        val url = URLBuilder(redirect).apply {
            parameters.append("state", state); parameters.append("code", code)
        }.buildString()
        assertTrue(callback.extractCode(url, state) == code, "Optional issuer remains compatible")
    }

    @Test
    fun mismatchedBlankAndDuplicateIssuerAreRejected() {
        val variants = listOf(validUrl().replace("id.itmo.ru", "untrusted.invalid"), URLBuilder(redirect).apply {
            parameters.append("state", state); parameters.append("code", code); parameters.append("iss", "")
        }.buildString(), validUrl() + "&iss=untrusted")
        variants.forEach { assertTrue(callback.extractCode(it, state) == null, "Issuer mismatch") }
    }

    @Test
    fun errorCallbacksMissingBlankAndOversizedCodesAreRejected() {
        val variants = listOf(validUrl() + "&error=access_denied", validUrl().replace("code=$code", "code="), validUrl().replace("code=$code", "unused=one"), validUrl().replace(code, "a".repeat(4097)))
        variants.forEach { assertTrue(callback.extractCode(it, state) == null, "No successful code") }
    }

    @Test
    fun malformedPercentUtf8ControlsBackslashesAndWhitespaceAreRejected() {
        for (suffix in listOf("&unused=%", "&unused=%GG", "&unused=%FF", "&unused=%00", "&unused=%0A", "&unused=\\", "&unused= ", "&&unused=a", "&=a")) {
            assertTrue(callback.extractCode(validUrl() + suffix, state) == null, "Malformed URI rejected")
        }
    }

    @Test
    fun formQueryDecodingPreservesPlusAndEncodedPlusWithoutNormalizingIssuer() {
        val url = validUrl().replace(code, "part%2Bpart+tail")
        assertTrue(callback.extractCode(url, state) == "part+part tail", "Form query decoding")
        assertTrue(callback.extractCode(validUrl().replace("%2Fitmo", "%2Fitmo%2F"), state) == null, "Exact issuer string")
    }

    @Test
    fun navigationAllowsIssuerHostAndExactCallbackOnly() {
        assertTrue(callback.isAllowedPage(issuer + "/login-actions/authenticate"))
        assertTrue(callback.isAllowedPage(validUrl()))
        assertTrue(!callback.isAllowedPage("https://my.itmo.ru/other"))
        assertTrue(!callback.isAllowedPage("https://id.itmo.ru:8443/"))
    }
}
