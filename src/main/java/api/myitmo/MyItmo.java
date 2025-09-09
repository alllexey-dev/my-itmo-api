package api.myitmo;

import api.myitmo.adapters.LocalDateAdapter;
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

    public void refreshTokens(String refreshToken) {
        TokenResponse response = getAuthHelper().refreshTokens(refreshToken);
        getStorage().update(response);
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
