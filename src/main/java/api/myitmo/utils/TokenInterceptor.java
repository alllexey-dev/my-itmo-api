package api.myitmo.utils;

import api.myitmo.MyItmo;
import api.myitmo.storage.Storage;
import okhttp3.Interceptor;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class TokenInterceptor implements Interceptor {

    private final List<String> hostsToIntercept = Arrays.asList(MyItmo.MY_ITMO_HOST, "qr.itmo.su");

    private final MyItmo myItmo;

    public TokenInterceptor(MyItmo myItmo) {
        this.myItmo = myItmo;
    }

    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {
        // we intercept only MyITMO (and some other) requests
        if (!hostsToIntercept.contains(chain.request().url().host())) {
            return chain.proceed(chain.request());
        }

        Storage storage = myItmo.getStorage();
        long currentTime = System.currentTimeMillis();

        // try to update token
        // if accessTokenExpiration time is not set, ignore it
        if (storage.getAccessToken() == null || (storage.getAccessExpiresAt() != 0 && storage.getAccessExpiresAt() < currentTime)) {
            if (storage.getRefreshToken() == null) {
                throw new IOException("Cannot refresh access token: no refresh token present");
            }

            // if refreshTokenExpiration time is not set, ignore it
            if (storage.getRefreshExpiresAt() != 0 && storage.getRefreshExpiresAt() < currentTime) {
                throw new IOException("Cannot refresh access token: refresh token expired");
            }

            myItmo.refreshTokens(storage.getRefreshToken());
        }

        String accessToken = myItmo.getStorage().getAccessToken();
        // this should not happen
        if (accessToken == null) {
            throw new IOException("Cannot get access token: access token is null after refresh");
        }

        return chain.proceed(chain.request().newBuilder().addHeader("Authorization", "Bearer " + accessToken).build());
    }
}
