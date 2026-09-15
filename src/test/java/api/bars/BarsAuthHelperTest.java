package api.bars;

import api.bars.utils.BarsAuthHelper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BarsAuthHelperTest {
    private final BarsAuthHelper helper = new Bars().getAuthHelper();
    private static final String CALLBACK = "https://bars.itmo.ru/rest/login";

    @Test
    void loginUrlTargetsTheBarsClientWithState() {
        String url = helper.getLoginUrl("s1");
        assertTrue(url.startsWith("https://id.itmo.ru/auth/realms/itmo/protocol/openid-connect/auth?"));
        assertTrue(url.contains("client_id=bars"));
        assertTrue(url.contains("redirect_uri=https%3A%2F%2Fbars.itmo.ru%2Frest%2Flogin"));
        assertTrue(url.contains("state=s1"));
        assertTrue(url.contains("response_type=code"));
    }

    @Test
    void onlyTheExactHttpsCallbackIsAccepted() {
        assertTrue(helper.isCallback(CALLBACK + "?code=x"));
        assertFalse(helper.isCallback("http://bars.itmo.ru/rest/login?code=x"));
        assertFalse(helper.isCallback("https://bars.itmo.ru.evil.example/rest/login?code=x"));
        assertFalse(helper.isCallback("https://user@bars.itmo.ru/rest/login?code=x"));
        assertFalse(helper.isCallback("https://bars.itmo.ru:8443/rest/login?code=x"));
        assertFalse(helper.isCallback("https://bars.itmo.ru/rest/login/extra?code=x"));
        assertTrue(helper.isAllowedPage("https://id.itmo.ru/auth/realms/itmo/login-actions/authenticate"));
        assertFalse(helper.isAllowedPage("https://example.com/"));
    }

    @Test
    void codeRequiresMatchingStateAndCleanQuery() {
        assertEquals("abc", helper.extractCode(CALLBACK + "?state=s1&code=abc&iss=https%3A%2F%2Fid.itmo.ru%2Fauth%2Frealms%2Fitmo", "s1"));
        assertNull(helper.extractCode(CALLBACK + "?state=other&code=abc", "s1"));
        assertNull(helper.extractCode(CALLBACK + "?state=s1&code=abc&error=access_denied", "s1"));
        assertNull(helper.extractCode(CALLBACK + "?state=s1&code=abc#fragment", "s1"));
        assertNull(helper.extractCode(CALLBACK + "?state=s1&code=abc&code=def", "s1"));
        assertNull(helper.extractCode(CALLBACK + "?state=s1&code=abc&iss=https%3A%2F%2Fevil.example", "s1"));
        assertNull(helper.extractCode(CALLBACK + "?state=s1", "s1"));
        assertNull(helper.extractCode(CALLBACK + "?state=s1&code=abc", ""));
    }

    @Test
    void loginActionIsFoundInHtmlAndJsonForms() {
        assertEquals("https://id.itmo.ru/a?x=1&y=2", BarsAuthHelper.findLoginAction("<form action=\"https://id.itmo.ru/a?x=1&amp;y=2\" method=\"post\">"));
        assertEquals("https://id.itmo.ru/b", BarsAuthHelper.findLoginAction("{\"loginAction\": \"https://id.itmo.ru/b\", \"loginUrl\": \"c\"}"));
        assertNull(BarsAuthHelper.findLoginAction("<html></html>"));
    }
}
