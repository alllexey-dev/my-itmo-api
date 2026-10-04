package api.myitmo.utils;

import api.myitmo.MyItmo;
import api.myitmo.model.other.TokenResponse;
import api.myitmo.storage.RuntimeStorage;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthHelperTest {
    private final MockWebServer server = new MockWebServer();
    private final TrackingStorage storage = new TrackingStorage();
    private MyItmo myItmo;

    @BeforeEach
    void start() throws IOException {
        server.start();
        myItmo = new MyItmo();
        myItmo.setStorage(storage);
        storage.setAccessToken("synthetic-old-access");
        storage.setRefreshToken("synthetic-old-refresh");
        storage.setIdToken("synthetic-old-id");
        storage.setAccessExpiresAt(123L);
        storage.setRefreshExpiresAt(456L);
        myItmo.setOkHttpClient(new OkHttpClient.Builder()
                .followRedirects(false)
                .retryOnConnectionFailure(false)
                .readTimeout(2, TimeUnit.SECONDS)
                .addInterceptor(chain -> chain.proceed(chain.request().newBuilder()
                        .url(server.url(chain.request().url().encodedPath()))
                        .build()))
                .build());
    }

    @AfterEach
    void stop() throws IOException {
        server.shutdown();
    }

    @Test
    void refreshHttpFailureDoesNotReplaceStoredTokens() {
        server.enqueue(new MockResponse().setResponseCode(400)
                .setBody("{\"error\":\"invalid_grant\",\"error_description\":\"synthetic-body-secret\"}"));

        TokenRefreshException failure = assertThrows(TokenRefreshException.class, myItmo::forceRefreshTokens);

        assertStoredTokensUnchanged();
        assertRedactedCauses(failure);
        assertTrue(failure.getCause().getCause().getMessage().contains("HTTP 400"));
    }

    @Test
    void refreshOAuthErrorWithHttpSuccessDoesNotReplaceStoredTokens() {
        server.enqueue(new MockResponse().setBody("{\"error\":\"invalid_grant\"}"));

        assertThrows(TokenRefreshException.class, myItmo::forceRefreshTokens);

        assertStoredTokensUnchanged();
    }

    @Test
    void refreshNonJsonSuccessDoesNotReplaceStoredTokens() {
        server.enqueue(new MockResponse().setBody("<html>synthetic-body-secret</html>"));

        TokenRefreshException failure = assertThrows(TokenRefreshException.class, myItmo::forceRefreshTokens);

        assertStoredTokensUnchanged();
        assertRedactedCauses(failure);
    }

    @Test
    void refreshMissingTokensDoesNotReplaceStoredTokens() {
        server.enqueue(new MockResponse().setBody("{\"expires_in\":3600}"));

        assertThrows(TokenRefreshException.class, myItmo::forceRefreshTokens);

        assertStoredTokensUnchanged();
    }

    @Test
    void refreshMalformedTokenFieldDoesNotExposeTheParserCause() {
        server.enqueue(new MockResponse().setBody("{\"access_token\":\"synthetic-new-access\","
                + "\"refresh_token\":\"synthetic-new-refresh\",\"expires_in\":\"synthetic-body-secret\"}"));

        TokenRefreshException failure = assertThrows(TokenRefreshException.class, myItmo::forceRefreshTokens);

        assertStoredTokensUnchanged();
        assertRedactedCauses(failure);
    }

    @Test
    void refreshTransportFailurePreservesAnIOExceptionCause() {
        server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));

        TokenRefreshException failure = assertThrows(TokenRefreshException.class, myItmo::forceRefreshTokens);

        assertInstanceOf(IOException.class, failure.getCause().getCause());
        assertStoredTokensUnchanged();
    }

    @Test
    void successfulRefreshReplacesStoredTokensOnce() {
        server.enqueue(new MockResponse().setBody("{\"access_token\":\"synthetic-new-access\","
                + "\"refresh_token\":\"synthetic-new-refresh\",\"expires_in\":3600,"
                + "\"refresh_expires_in\":86400,\"id_token\":\"synthetic-new-id\"}"));

        TokenResponse tokens = myItmo.forceRefreshTokens();

        assertEquals("synthetic-new-access", tokens.getAccessToken());
        assertEquals("synthetic-new-refresh", storage.getRefreshToken());
        assertEquals("synthetic-new-id", storage.getIdToken());
        assertEquals(1, storage.updates);
    }

    @Test
    void codeExchangeHttpFailureDoesNotReplaceStoredTokens() {
        enqueueLogin(new MockResponse().setResponseCode(400)
                .setBody("{\"error\":\"invalid_grant\"}"));

        assertThrows(RuntimeException.class, () -> myItmo.auth("synthetic-user", "synthetic-password"));

        assertStoredTokensUnchanged();
        assertEquals(3, server.getRequestCount());
    }

    @Test
    void codeExchangeOAuthErrorWithHttpSuccessDoesNotReplaceStoredTokens() {
        enqueueLogin(new MockResponse().setBody("{\"error\":\"invalid_grant\"}"));

        assertThrows(RuntimeException.class, () -> myItmo.auth("synthetic-user", "synthetic-password"));

        assertStoredTokensUnchanged();
        assertEquals(3, server.getRequestCount());
    }

    @Test
    void codeExchangeNonJsonSuccessDoesNotReplaceStoredTokens() {
        enqueueLogin(new MockResponse().setBody("<html>synthetic-body-secret</html>"));

        RuntimeException failure = assertThrows(RuntimeException.class,
                () -> myItmo.auth("synthetic-user", "synthetic-password"));

        assertStoredTokensUnchanged();
        assertRedactedCauses(failure);
        assertEquals(3, server.getRequestCount());
    }

    @Test
    void successfulCodeExchangeReplacesStoredTokensOnce() {
        enqueueLogin(new MockResponse().setBody("{\"access_token\":\"synthetic-new-access\","
                + "\"refresh_token\":\"synthetic-new-refresh\",\"expires_in\":3600,"
                + "\"refresh_expires_in\":86400}"));

        myItmo.auth("synthetic-user", "synthetic-password");

        assertEquals("synthetic-new-access", storage.getAccessToken());
        assertEquals("synthetic-new-refresh", storage.getRefreshToken());
        assertEquals(1, storage.updates);
        assertEquals(3, server.getRequestCount());
    }

    private void enqueueLogin(MockResponse tokenResponse) {
        myItmo.setAuthHelper(new AuthHelper(myItmo) {
            @Override
            public Request getInitialRequest(String challenge) {
                return new Request.Builder().url(server.url("/authorize")).build();
            }
        });
        server.enqueue(new MockResponse().setBody("{\"loginAction\": \"" + server.url("/login")
                + "\", \"loginUrl\": \"synthetic\"}"));
        server.enqueue(new MockResponse().setResponseCode(302)
                .setHeader("Location", server.url("/callback?code=synthetic-code")));
        server.enqueue(tokenResponse);
    }

    private void assertStoredTokensUnchanged() {
        assertEquals(0, storage.updates);
        assertEquals("synthetic-old-access", storage.getAccessToken());
        assertEquals("synthetic-old-refresh", storage.getRefreshToken());
        assertEquals("synthetic-old-id", storage.getIdToken());
        assertEquals(123L, storage.getAccessExpiresAt());
        assertEquals(456L, storage.getRefreshExpiresAt());
    }

    private void assertRedactedCauses(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            assertFalse(cause.toString().contains("synthetic-body-secret"));
            assertFalse(cause.toString().contains("synthetic-new-access"));
            assertFalse(cause.toString().contains("synthetic-new-refresh"));
        }
    }

    private static class TrackingStorage extends RuntimeStorage {
        private int updates;

        @Override
        public void update(TokenResponse tokens) {
            updates++;
            super.update(tokens);
        }
    }
}
