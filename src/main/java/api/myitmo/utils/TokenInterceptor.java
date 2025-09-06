package api.myitmo.utils;

import api.myitmo.MyItmo;
import api.myitmo.storage.Storage;
import okhttp3.Interceptor;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class TokenInterceptor implements Interceptor {

    private final MyItmo myItmo;

    public TokenInterceptor(MyItmo myItmo) {
        this.myItmo = myItmo;
    }

    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {
        // we intercept only MyITMO requests
        if (!chain.request().url().host().equals(MyItmo.MY_ITMO_HOST)) {
            return chain.proceed(chain.request());
        }

        Storage storage = myItmo.getStorage();
        long currentTime = System.currentTimeMillis();
        // try to update token
        if (storage.getAccessToken() == null || storage.getAccessExpiresAt() < currentTime) {
            if (storage.getRefreshToken() == null) {
                throw new RuntimeException("Cannot refresh access token: no refresh token present");
            }
            if (storage.getRefreshExpiresAt() < currentTime) {
                throw new RuntimeException("Cannot refresh access token: refresh token expired");
            }

            myItmo.refreshTokens(storage.getRefreshToken());
        }

        String accessToken = myItmo.getStorage().getAccessToken();
        // this should not happen
        if (accessToken == null) {
            throw new RuntimeException("Cannot get access token: access token is null after refresh");
        }

        return chain.proceed(chain.request().newBuilder().addHeader("Authorization", "Bearer " + accessToken).build());
    }
}
