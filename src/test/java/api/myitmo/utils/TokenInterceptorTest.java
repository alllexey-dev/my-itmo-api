package api.myitmo.utils;

import api.myitmo.MyItmo;
import api.myitmo.model.other.TokenResponse;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TokenInterceptorTest {
    private final MockWebServer server = new MockWebServer();
    private MyItmo myItmo;

    @BeforeEach
    void start() throws IOException {
        server.start();
        myItmo = new MyItmo(new MyItmoConfiguration.Default() {
            @Override
            public String getHost() {
                return server.getHostName();
            }

            @Override
            public String getUrl() {
                return server.url("/").toString();
            }
        });
        TokenResponse tokens = new TokenResponse();
        tokens.setAccessToken("synthetic-access-token");
        tokens.setExpiresIn(3600);
        tokens.setRefreshToken("synthetic-refresh-token");
        tokens.setRefreshExpiresIn(86400);
        myItmo.getStorage().update(tokens);
    }

    @AfterEach
    void stop() throws IOException {
        server.shutdown();
    }

    @Test
    void myItmoRequestsCarryTheBearerAndTheConfiguredLanguage() throws Exception {
        server.enqueue(new MockResponse().setBody("{}"));

        try (Response response = call(new Request.Builder().url(server.url("/api/personalities/persons?q=x")).build())) {
            assertEquals(200, response.code());
        }

        RecordedRequest recorded = server.takeRequest();
        assertEquals("Bearer synthetic-access-token", recorded.getHeader("Authorization"));
        assertEquals("ru", recorded.getHeader("Accept-Language"));
    }

    @Test
    void anExplicitLanguageOnTheRequestIsKept() throws Exception {
        server.enqueue(new MockResponse().setBody("{}"));

        call(new Request.Builder().url(server.url("/api/personalities/persons?q=x"))
                .header("Accept-Language", "en").build()).close();

        assertEquals("en", server.takeRequest().getHeader("Accept-Language"));
    }

    @Test
    void foreignHostsAreLeftUntouched() throws Exception {
        MockWebServer other = new MockWebServer();
        other.start();
        try {
            other.enqueue(new MockResponse().setBody("{}"));
            // 127.0.0.1 instead of the configured "localhost" is a different host for the interceptor.
            call(new Request.Builder().url("http://127.0.0.1:" + other.getPort() + "/anything").build()).close();

            RecordedRequest recorded = other.takeRequest();
            assertNull(recorded.getHeader("Authorization"));
            assertNull(recorded.getHeader("Accept-Language"));
        } finally {
            other.shutdown();
        }
    }

    private Response call(Request request) throws IOException {
        return myItmo.getOkHttpClient().newCall(request).execute();
    }
}
