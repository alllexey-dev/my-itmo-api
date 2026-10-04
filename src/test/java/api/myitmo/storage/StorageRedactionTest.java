package api.myitmo.storage;

import api.bars.storage.RuntimeBarsStorage;
import api.myitmo.model.other.TokenResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StorageRedactionTest {
    @Test
    void tokenResponseStringRedactsEveryCredential() {
        TokenResponse tokens = new TokenResponse();
        tokens.setAccessToken("synthetic-access");
        tokens.setRefreshToken("synthetic-refresh");
        tokens.setIdToken("synthetic-id");
        tokens.setSessionState("synthetic-session");

        assertEquals("TokenResponse(redacted)", tokens.toString());
    }

    @Test
    void runtimeStorageStringRedactsEveryCredential() {
        RuntimeStorage storage = new RuntimeStorage();
        storage.setAccessToken("synthetic-access");
        storage.setRefreshToken("synthetic-refresh");
        storage.setIdToken("synthetic-id");

        assertEquals("RuntimeStorage(redacted)", storage.toString());
    }

    @Test
    void barsStorageStringRedactsAuthorization() {
        RuntimeBarsStorage storage = new RuntimeBarsStorage();
        storage.setAuthorization("Bearer synthetic-bars");

        assertEquals("RuntimeBarsStorage(redacted)", storage.toString());
    }
}
