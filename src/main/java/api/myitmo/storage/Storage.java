package api.myitmo.storage;

import api.myitmo.model.TokenResponse;

public interface Storage {

    String getAccessToken();

    long getAccessExpiresAt();

    String getRefreshToken();

    long getRefreshExpiresAt();

    String getIdToken();

    void setAccessToken(String accessToken);

    void setAccessExpiresAt(long accessExpiresAt);

    void setRefreshToken(String refreshToken);

    void setRefreshExpiresAt(long refreshExpiresAt);

    void setIdToken(String idToken);

    default void update(TokenResponse response) {
        long currentMillis = System.currentTimeMillis();
        setAccessToken(response.getAccessToken());
        setAccessExpiresAt(currentMillis + response.getExpiresIn() * 1000);
        setRefreshToken(response.getRefreshToken());
        setRefreshExpiresAt(currentMillis + response.getRefreshExpiresIn() * 1000);
        setIdToken(response.getIdToken());
    }
}
