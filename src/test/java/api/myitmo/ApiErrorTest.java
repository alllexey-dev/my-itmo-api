package api.myitmo;

import api.myitmo.utils.ApiException;
import api.myitmo.utils.MyItmoConfiguration;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiErrorTest {
    private final MockWebServer server = new MockWebServer();
    private MyItmo myItmo;

    @BeforeEach
    void start() throws IOException {
        server.start();
        myItmo = new MyItmo(new MyItmoConfiguration.Default() {
            @Override
            public String getUrl() {
                return server.url("/").toString();
            }
        });
        myItmo.setOkHttpClient(new OkHttpClient());
    }

    @AfterEach
    void stop() throws IOException {
        server.shutdown();
    }

    @Test
    void htmlGatewayErrorBecomesApiExceptionWithoutEchoingTheBody() {
        server.enqueue(new MockResponse().setResponseCode(502)
                .setBody("<html>synthetic-body-secret</html>"));

        ApiException failure = assertThrows(ApiException.class,
                () -> myItmo.execute(myItmo.getApi().getPersonality(123456)));

        assertTrue(failure.getMessage().contains("HTTP 502"));
        assertFalse(failure.getMessage().contains("synthetic-body-secret"));
        assertNull(failure.getCause());
    }

    @Test
    void jsonErrorKeepsTheApiErrorCodeAndMessage() {
        server.enqueue(new MockResponse().setResponseCode(400)
                .setBody("{\"error_code\":100,\"error_message\":\"Missing synthetic person\"}"));

        ApiException failure = assertThrows(ApiException.class,
                () -> myItmo.execute(myItmo.getApi().getPersonality(123456)));

        assertEquals(100, failure.getErrorCode());
        assertEquals("Missing synthetic person", failure.getErrorMessage());
    }

    @Test
    void nullJsonErrorBecomesApiException() {
        server.enqueue(new MockResponse().setResponseCode(502).setBody("null"));

        ApiException failure = assertThrows(ApiException.class,
                () -> myItmo.execute(myItmo.getApi().getPersonality(123456)));

        assertTrue(failure.getMessage().contains("HTTP 502"));
    }
}
