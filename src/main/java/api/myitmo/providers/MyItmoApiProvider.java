package api.myitmo.providers;

import api.myitmo.MyItmoApi;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MyItmoApiProvider {

    public static final String MY_ITMO_URL = "https://my.itmo.ru";

    private Gson gson;

    private OkHttpClient okHttpClient;

    private Retrofit retrofit;

    private MyItmoApi myItmoApi;

    public MyItmoApi get() {
        if (myItmoApi == null) {
            myItmoApi = getRetrofit()
                    .create(MyItmoApi.class);
        }
        return myItmoApi;
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
                    .build();
        }
        return okHttpClient;
    }

    public Gson getGson() {
        if (gson == null) {
            gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .create();
        }
        return gson;
    }
}
