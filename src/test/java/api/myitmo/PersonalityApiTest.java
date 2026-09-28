package api.myitmo;

import api.myitmo.model.ResultResponse;
import api.myitmo.model.other.TokenResponse;
import api.myitmo.model.personality.Personality;
import api.myitmo.utils.MyItmoConfiguration;
import com.google.gson.JsonObject;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import retrofit2.Response;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersonalityApiTest {
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
    void personalityRequestUsesTheIsuPathAndRussianLanguage() throws Exception {
        server.enqueue(new MockResponse().setHeader("Content-Type", "application/json")
                .setBody("{\"error_code\":0,\"result\":{\"isu\":123456,\"fio\":\"Тестовый Профиль\","
                        + "\"gender\":\"male\",\"photo\":null,\"contacts\":[],\"rooms\":[],\"positions\":[],"
                        + "\"powers\":[],\"levels\":null,\"education\":[],\"activities\":null,\"exchange_training\":false}}"));

        Response<ResultResponse<Personality>> response = myItmo.getApi().getPersonality(123456).execute();

        assertEquals(200, response.code());
        assertTrue(response.isSuccessful());
        assertNotNull(response.body());
        assertEquals(0, response.body().getErrorCode());
        assertEquals(123456L, response.body().getResult().getIsu());
        assertNull(response.errorBody());
        RecordedRequest request = server.takeRequest();
        assertEquals("GET", request.getMethod());
        assertEquals("/api/personalities/persons/123456", request.getPath());
        assertEquals("ru", request.getHeader("Accept-Language"));
        assertEquals("Bearer synthetic-access-token", request.getHeader("Authorization"));
    }

    @Test
    void missingPersonalityPreservesObservedErrorInErrorBody() throws Exception {
        String errorJson = "{\"error_code\":100,\"result\":null}";
        server.enqueue(new MockResponse().setResponseCode(400)
                .setHeader("Content-Type", "application/json").setBody(errorJson));

        Response<ResultResponse<Personality>> response = myItmo.getApi().getPersonality(123457).execute();

        assertEquals(400, response.code());
        assertFalse(response.isSuccessful());
        assertNull(response.body());
        assertNotNull(response.errorBody());
        String errorBody = response.errorBody().string();
        assertEquals(errorJson, errorBody);
        JsonObject error = myItmo.getGson().fromJson(errorBody, JsonObject.class);
        assertTrue(error.getAsJsonPrimitive("error_code").isNumber());
        assertEquals(100, error.get("error_code").getAsInt());
        assertTrue(error.has("result"));
        assertTrue(error.get("result").isJsonNull());
    }
}
