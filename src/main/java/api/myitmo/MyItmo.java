package api.myitmo;

import api.myitmo.adapters.LocalDateAdapter;
import api.myitmo.adapters.OffsetDateTimeAdapter;
import api.myitmo.model.TokenResponse;
import api.myitmo.storage.RuntimeCookieJar;
import api.myitmo.storage.RuntimeStorage;
import api.myitmo.storage.Storage;
import api.myitmo.utils.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Setter;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Setter
public class MyItmo {

    public static final String MY_ITMO_HOST = "my.itmo.ru";

    public static final String MY_ITMO_URL = "https://" +  MY_ITMO_HOST;

    private MyItmoApi api;

    private Retrofit retrofit;

    private OkHttpClient okHttpClient;

    private Gson gson;

    private AuthHelper authHelper;

    private Storage storage;

    public void auth(String username, String password) {
        TokenResponse response = getAuthHelper().auth(username, password);
        getStorage().update(response);
    }

    public TokenResponse forceRefreshTokens() {
        try {
            TokenResponse response = getAuthHelper().refreshTokens(storage.getRefreshToken());
            getStorage().update(response);
            return response;
        } catch (Exception e) {
            throw new TokenRefreshException("Could not force refresh tokens", e);
        }
    }

    private final Object tokenLock = new Object();

    public TokenResponse getOrRefreshTokens() {
        long currentTime = System.currentTimeMillis();

        if (isAccessTokenExpired()) {
            synchronized (tokenLock) {
                if (isAccessTokenExpired()) {
                    if (storage.getRefreshToken() == null) {
                        throw new TokenRefreshException("Cannot refresh access token: no refresh token present");
                    }

                    // if refreshTokenExpiration time is not set, ignore it
                    if (storage.getRefreshExpiresAt() != 0 && storage.getRefreshExpiresAt() < currentTime) {
                        throw new TokenRefreshException("Cannot refresh access token: refresh token expired");
                    }

                    return forceRefreshTokens();
                }
            }
            return forceRefreshTokens();
        }

        return storage.toTokenResponse();
    }

    private boolean isAccessTokenExpired() {
        long currentTime = System.currentTimeMillis();

        // if accessTokenExpiration time is not set, ignore it
        return storage.getAccessToken() == null || (storage.getAccessExpiresAt() != 0 && storage.getAccessExpiresAt() < currentTime);
    }

    // default getters
    public MyItmoApi api() {
        if (api == null) {
            api = getRetrofit()
                    .create(MyItmoApi.class);
        }
        return api;
    }

    public Retrofit getRetrofit() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(MY_ITMO_URL)
                    .client(getOkHttpClient())
                    .addConverterFactory(GsonConverterFactory.create(getGson()))
                    .build();
        }
        return retrofit;
    }

    public OkHttpClient getOkHttpClient() {
        if (okHttpClient == null) {
            okHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(new TokenInterceptor(this))
                    .cookieJar(new RuntimeCookieJar()) // needed for authentication
                    .followRedirects(false) // needed for authentication
                    .build();
        }
        return okHttpClient;
    }

    public Gson getGson() {
        if (gson == null) {
            gson = new GsonBuilder()
//                    .setPrettyPrinting()
                    .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                    .registerTypeAdapter(OffsetDateTime.class, new OffsetDateTimeAdapter())
                    .create();
        }
        return gson;
    }

    public AuthHelper getAuthHelper() {
        if (authHelper == null) {
            authHelper = new AuthHelper(this);
        }
        return authHelper;
    }

    public Storage getStorage() {
        if (storage == null) {
            storage = new RuntimeStorage(); // store in memory by default
        }
        return storage;
    }
}
