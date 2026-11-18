package dev.alllexey.itmoapi.bars.auth

import io.ktor.client.engine.mock.MockEngine
import io.ktor.http.Url
import kotlin.test.*

/** Three applicable legacy helper cases; findLoginAction is deliberately not ported. */
class BarsAuthHelperTest {
    private val engine = MockEngine { error("No network request expected") }
    private val login = BarsLogin(engine)
    private val callback = login.configuration.redirectUri

    private fun withLogin(block: () -> Unit) {
        try { block() } finally { login.close(); engine.close() }
    }

    @Test
    fun loginUrlTargetsTheBarsClientWithState() = withLogin {
        val state = "s1&+ encoded"
        val url = Url(login.loginUrl(state))
        assertEquals("https", url.protocol.name)
        assertEquals("id.itmo.ru", url.host)
        assertEquals("/auth/realms/itmo/protocol/openid-connect/auth", url.encodedPath)
        assertTrue(url.parameters.entries().associate { it.key to it.value } == mapOf(
            "response_type" to listOf("code"), "scope" to listOf("openid"), "client_id" to listOf("bars"),
            "redirect_uri" to listOf(callback), "state" to listOf(state),
        ), "Authorization parameters must match the BARS contract")
    }

    @Test
    fun onlyTheExactHttpsCallbackIsAccepted() = withLogin {
        assertTrue(login.isCallback("$callback?code=x"))
        assertTrue(login.isCallback("https://BARS.ITMO.RU:443/rest/login?code=x"))
        listOf(
            "http://bars.itmo.ru/rest/login?code=x", "https://bars.itmo.ru.evil.example/rest/login?code=x",
            "https://user@bars.itmo.ru/rest/login?code=x", "https://bars.itmo.ru:8443/rest/login?code=x",
            "https://bars.itmo.ru/rest/login/extra?code=x",
        ).forEach { assertFalse(login.isCallback(it), "Untrusted callback must be rejected") }
        assertTrue(login.isAllowedPage("https://id.itmo.ru/auth/realms/itmo/login-actions/authenticate"))
        assertFalse(login.isAllowedPage("https://example.com/"))
    }

    @Test
    fun codeRequiresMatchingStateAndCleanQuery() = withLogin {
        val code = login.newState()
        val prefix = "$callback?state=s1&code=$code"
        assertTrue(login.extractCode("$prefix&iss=https%3A%2F%2Fid.itmo.ru%2Fauth%2Frealms%2Fitmo", "s1") == code,
            "Exact issuer must be accepted")
        assertTrue(login.extractCode(prefix, "s1") == code, "Issuer absence preserves the 1.x contract")
        listOf(
            "$callback?state=other&code=$code", "$prefix&error=access_denied", "$prefix#fragment",
            "$prefix&code=other", "$prefix&iss=https%3A%2F%2Fevil.example", "$callback?state=s1",
            "$prefix&iss=", "$prefix&%73tate=s1", "$prefix&session_state=one&session_state=two",
            "$callback?state=s1&code=", "$callback?state=s1&code=${"a".repeat(4097)}",
        ).forEach { assertTrue(login.extractCode(it, "s1") == null, "Invalid callback must not yield a code") }
        assertTrue(login.extractCode(prefix, "") == null)
        assertTrue(login.extractCode(prefix, " ") == null)
    }

    @Test
    fun malformedIdnAndEncodedCallbackAttacksAreRejectedAndStateIsFresh() = withLogin {
        listOf(
            "not a URL", "$callback?state=s1&code=%", "$callback?state=s1&code=%GG",
            "$callback?state=s1&code=%C0%AF", "$callback?state=s1&code=%00",
            "$callback?state=s1&code=x&", "$callback?state=s1&code=x#",
            "https://bаrs.itmo.ru/rest/login?state=s1&code=x",
            "https://xn--bars-123.itmo.ru/rest/login?state=s1&code=x",
            "https://bars.itmo.ru/rest/%6Cogin?state=s1&code=x",
            "https://bars.itmo.ru./rest/login?state=s1&code=x",
            "https://id.itmo.ru@evil.example/rest/login?state=s1&code=x",
            "https://id.itmo.ru:8443/login", "https://id.itmo.ru/login#fragment",
        ).forEach {
            assertFalse(login.isCallback(it), "Malformed callback must be rejected")
            assertFalse(login.isAllowedPage(it), "Malformed page must be rejected")
            assertTrue(login.extractCode(it, "s1") == null, "Malformed callback must not yield a code")
        }
        val states = List(20) { login.newState() }
        assertEquals(20, states.toSet().size)
        assertTrue(states.all { it.length == 36 })
        val invalid = assertFailsWith<IllegalArgumentException> { login.loginUrl(" ") }
        assertEquals("Invalid authorization state", invalid.message)
    }
}
