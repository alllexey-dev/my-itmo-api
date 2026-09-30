package api.bars;

import api.bars.utils.BarsApiException;
import api.bars.utils.BarsSessionCode;
import api.bars.utils.BarsSessionCode.Outcome;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class BarsCookieSessionTest {
    private static final String COOKIE = "SYNTHETIC_SESSION=synthetic-value; SYNTHETIC_ID=synthetic-id";
    private static final String CALLBACK = "https://bars.itmo.ru/rest/login";

    private final MockWebServer server = new MockWebServer();
    private Bars bars;

    @BeforeEach
    void start() throws IOException {
        server.start();
        bars = new Bars(new BarsConfiguration.Default() {
            @Override
            public String getIssuer() {
                return server.url("/auth/realms/itmo").toString();
            }
        });
    }

    @AfterEach
    void stop() throws IOException {
        server.shutdown();
    }

    private static MockResponse redirect(String location) {
        return new MockResponse().setResponseCode(302).setHeader("Location", location);
    }

    @Test
    void callbackWithTheSameStateIsACodeAndIsNotFollowed() throws Exception {
        server.enqueue(redirect(CALLBACK + "?state=s1&code=abc"));
        BarsSessionCode result = bars.getAuthHelper().requestCodeWithCookies("s1", COOKIE);
        assertEquals(Outcome.CODE, result.getOutcome());
        assertEquals("abc", result.getCode());
        assertEquals(302, result.getHttpCode());
        RecordedRequest request = server.takeRequest();
        assertEquals(Arrays.asList(COOKIE), request.getHeaders().values("Cookie"));
        assertTrue(request.getPath().startsWith("/auth/realms/itmo/protocol/openid-connect/auth?"));
        assertTrue(request.getPath().contains("state=s1"));
        assertEquals(1, server.getRequestCount());
    }

    @Test
    void callbackWithoutAUsableCodeIsRejected() {
        server.enqueue(redirect(CALLBACK + "?state=other&code=abc"));
        server.enqueue(redirect(CALLBACK + "?state=s1&code=abc&error=access_denied"));
        server.enqueue(redirect(CALLBACK + "?state=s1&code=abc&code=def"));
        for (int i = 0; i < 3; i++) {
            BarsSessionCode result = bars.getAuthHelper().requestCodeWithCookies("s1", COOKIE);
            assertEquals(Outcome.REJECTED, result.getOutcome());
            assertNull(result.getCode());
        }
    }

    @Test
    void itmoIdPageMeansLoginIsRequired() {
        server.enqueue(redirect("https://" + server.getHostName() + "/auth/realms/itmo/login-actions/authenticate"));
        assertEquals(Outcome.LOGIN_REQUIRED, bars.getAuthHelper().requestCodeWithCookies("s1", COOKIE).getOutcome());
    }

    @Test
    void relativeLocationIsResolvedFromTheRequestUrl() throws Exception {
        // isAllowedPage accepts only HTTPS on the default port, so the issuer is an HTTPS address
        // on the server's host, and the test client routes it to MockWebServer.
        OkHttpClient routed = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    HttpUrl url = chain.request().url().newBuilder().scheme("http").port(server.getPort()).build();
                    return chain.proceed(chain.request().newBuilder().url(url).build());
                })
                .build();
        Bars httpsBars = new Bars(new BarsConfiguration.Default() {
            @Override
            public String getIssuer() {
                return "https://" + server.getHostName() + "/auth/realms/itmo";
            }
        }, routed);
        server.enqueue(redirect("/auth/realms/itmo/login-actions/authenticate?client_id=bars"));
        assertEquals(Outcome.LOGIN_REQUIRED, httpsBars.getAuthHelper().requestCodeWithCookies("s1", COOKIE).getOutcome());
        assertEquals(COOKIE, server.takeRequest().getHeader("Cookie"));
    }

    @Test
    void foreignRedirectIsRejectedAndRedirectWithoutLocationIsAnHttpError() {
        server.enqueue(redirect("https://example.com/"));
        server.enqueue(new MockResponse().setResponseCode(302));
        assertEquals(Outcome.REJECTED, bars.getAuthHelper().requestCodeWithCookies("s1", COOKIE).getOutcome());
        BarsSessionCode noLocation = bars.getAuthHelper().requestCodeWithCookies("s1", COOKIE);
        assertEquals(Outcome.HTTP_ERROR, noLocation.getOutcome());
        assertEquals(302, noLocation.getHttpCode());
    }

    @Test
    void loginPageIsLoginRequiredWithoutReadingTheBody() {
        Buffer page = new Buffer();
        for (int i = 0; i < 64 * 1024; i++) page.writeUtf8("<p>synthetic login form</p>");
        server.enqueue(new MockResponse().setHeader("Content-Type", "text/html").setBody(page)
                .throttleBody(1024, 1, TimeUnit.SECONDS));
        BarsSessionCode result = assertTimeoutPreemptively(Duration.ofSeconds(10),
                () -> bars.getAuthHelper().requestCodeWithCookies("s1", COOKIE));
        assertEquals(Outcome.LOGIN_REQUIRED, result.getOutcome());
        assertEquals(200, result.getHttpCode());
    }

    @Test
    void serverErrorIsAnHttpError() {
        server.enqueue(new MockResponse().setResponseCode(503));
        BarsSessionCode result = bars.getAuthHelper().requestCodeWithCookies("s1", COOKIE);
        assertEquals(Outcome.HTTP_ERROR, result.getOutcome());
        assertEquals(503, result.getHttpCode());
        assertNull(result.getCode());
    }

    @Test
    void missingCookiesNeedLoginWithoutARequest() {
        for (String cookie : new String[]{"", "  ", null}) {
            BarsSessionCode result = bars.getAuthHelper().requestCodeWithCookies("s1", cookie);
            assertEquals(Outcome.LOGIN_REQUIRED, result.getOutcome());
            assertEquals(0, result.getHttpCode());
            assertTrue(result.getSetCookies().isEmpty());
        }
        assertEquals(0, server.getRequestCount());
    }

    @Test
    void setCookiesAreReturnedInOrderAndNotStoredInTheClientJar() {
        server.enqueue(redirect(CALLBACK + "?state=s1&code=abc")
                .addHeader("Set-Cookie", "SYNTHETIC_A=one; Path=/auth/realms/itmo/")
                .addHeader("Set-Cookie", "SYNTHETIC_B=two; Path=/auth/realms/itmo/"));
        BarsSessionCode result = bars.getAuthHelper().requestCodeWithCookies("s1", COOKIE);
        assertEquals(Arrays.asList("SYNTHETIC_A=one; Path=/auth/realms/itmo/", "SYNTHETIC_B=two; Path=/auth/realms/itmo/"),
                result.getSetCookies());
        assertThrows(UnsupportedOperationException.class, () -> result.getSetCookies().add("x"));
        HttpUrl itmoId = HttpUrl.get(bars.getAuthHelper().getLoginUrl("s1"));
        assertTrue(bars.getOkHttpClient().cookieJar().loadForRequest(itmoId).isEmpty());
    }

    @Test
    void textFormHidesTheCodeAndCookies() {
        server.enqueue(redirect(CALLBACK + "?state=s1&code=synthetic-code-value")
                .addHeader("Set-Cookie", "SYNTHETIC_A=synthetic-cookie-value"));
        BarsSessionCode result = bars.getAuthHelper().requestCodeWithCookies("s1", COOKIE);
        assertEquals(Outcome.CODE, result.getOutcome());
        assertFalse(result.toString().contains("synthetic-code-value"));
        assertFalse(result.toString().contains("synthetic-cookie-value"));
        assertTrue(result.toString().contains("CODE"));
    }

    @Test
    void stoppedServerIsANetworkError() throws Exception {
        String issuer = server.url("/auth/realms/itmo").toString();
        server.shutdown();
        Bars offline = new Bars(new BarsConfiguration.Default() {
            @Override
            public String getIssuer() {
                return issuer;
            }
        });
        BarsApiException error = assertThrows(BarsApiException.class,
                () -> offline.getAuthHelper().requestCodeWithCookies("s1", COOKIE));
        assertNull(error.getHttpCode());
        assertInstanceOf(IOException.class, error.getCause());
        assertFalse(error.getMessage().contains("synthetic"));
    }
}
