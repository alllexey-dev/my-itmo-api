package api.myitmo;

import api.myitmo.adapters.LocalDateAdapter;
import api.myitmo.adapters.OffsetDateTimeAdapter;
import api.myitmo.model.ResultResponse;
import api.myitmo.model.other.TokenResponse;
import api.myitmo.storage.RuntimeCookieJar;
import api.myitmo.storage.RuntimeStorage;
import api.myitmo.storage.Storage;
import api.myitmo.utils.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.Setter;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Setter
public class MyItmo {

    private MyItmoApi api;

    private Retrofit retrofit;

    private OkHttpClient okHttpClient;

    private Gson gson;

    private AuthHelper authHelper;

    private Storage storage;

    @Getter
    private MyItmoConfiguration configuration;

    /**
     * Создаёт экземпляр клиента MyITMO с конфигурацией по умолчанию.
     *
     * <p>Использует {@link MyItmoConfiguration#DEFAULT}.</p>
     */
    public MyItmo() {
        this(MyItmoConfiguration.DEFAULT);
    }

    /**
     * Создаёт экземпляр клиента MyITMO с заданной конфигурацией.
     *
     * @param configuration конфигурация клиента (URL, OAuth параметры и т.д.)
     */
    public MyItmo(MyItmoConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * Выполняет аутентификацию пользователя через OAuth2 (PKCE flow).
     *
     * <p>После успешной аутентификации токены сохраняются в {@link Storage}.</p>
     *
     * @param username логин пользователя
     * @param password пароль пользователя
     *
     * @throws RuntimeException если не удалось пройти процесс аутентификации
     */
    public void auth(String username, String password) {
        TokenResponse response = getAuthHelper().auth(username, password);
        getStorage().update(response);
    }

    /**
     * Принудительно обновляет access и refresh токены.
     *
     * <p>Использует текущий refresh token из {@link Storage}.</p>
     *
     * @return новый {@link TokenResponse}
     *
     * @throws TokenRefreshException если:
     * <ul>
     *     <li>refresh token отсутствует</li>
     *     <li>не удалось обновить токены</li>
     * </ul>
     */
    public TokenResponse forceRefreshTokens() {
        final String refreshToken = storage.getRefreshToken();

        if (refreshToken == null) {
            throw new TokenRefreshException("Cannot force refresh: no refresh token is available in storage.");
        }

        try {
            TokenResponse response = getAuthHelper().refreshTokens(refreshToken);
            getStorage().update(response);
            return response;
        } catch (Exception e) {
            throw new TokenRefreshException("Could not force refresh tokens", e);
        }
    }

    private final Object tokenLock = new Object();

    /**
     * Возвращает валидные токены, автоматически обновляя их при необходимости.
     *
     * <p>Обновление выполняется потокобезопасно.</p>
     *
     * @return актуальный {@link TokenResponse}
     *
     * @throws TokenRefreshException если:
     * <ul>
     *     <li>нет refresh token</li>
     *     <li>refresh token истёк</li>
     *     <li>не удалось обновить токены</li>
     * </ul>
     */
    public TokenResponse getValidTokens() {
        if (needsRefresh()) {
            synchronized (tokenLock) {
                if (needsRefresh()) {
                    if (!hasRefreshToken()) {
                        throw new TokenRefreshException("Cannot refresh access token: no refresh token present");
                    }

                    if (isRefreshTokenExpired()) {
                        throw new TokenRefreshException("Cannot refresh access token: refresh token expired");
                    }

                    return forceRefreshTokens();
                }
            }
        }

        return storage.toTokenResponse();
    }

    public boolean needsRefresh() {
        return !hasAccessToken() || isAccessTokenExpired();
    }

    public boolean hasAccessToken() {
        return storage.getAccessToken() != null;
    }

    public boolean hasRefreshToken() {
        return storage.getRefreshToken() != null;
    }

    public boolean isAccessTokenExpired() {
        long currentTime = System.currentTimeMillis();
        return storage.getAccessExpiresAt() < currentTime;
    }

    public boolean isRefreshTokenExpired() {
        long currentTime = System.currentTimeMillis();
        return storage.getRefreshExpiresAt() < currentTime;
    }

    public MyItmoApi getApi() {
        if (api == null) {
            api = getRetrofit()
                    .create(MyItmoApi.class);
        }
        return api;
    }

    public Retrofit getRetrofit() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(configuration.getUrl())
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

    /**
     * Возвращает хранилище токенов.
     *
     * <p>По умолчанию используется {@link RuntimeStorage} (в памяти).</p>
     *
     * @return {@link Storage}
     */
    public Storage getStorage() {
        if (storage == null) {
            storage = new RuntimeStorage();
        }
        return storage;
    }

    /**
     * Выполняет синхронный HTTP-запрос к API MyITMO и обрабатывает ответ.
     *
     * <p>Поведение:</p>
     * <ul>
     *     <li>При HTTP 2xx возвращает десериализованный {@link ResultResponse}.</li>
     *     <li>При HTTP 4xx/5xx пытается распарсить {@code errorBody} и выбрасывает {@link ApiException}.</li>
     *     <li>При ошибках сети (IOException) выбрасывает {@link ApiException}.</li>
     * </ul>
     *
     * <p>Важно: метод не проверяет {@code error_code} внутри {@link ResultResponse}.
     * Если API возвращает ошибку с HTTP 200, её нужно обрабатывать отдельно.</p>
     *
     * @param call HTTP-вызов, созданный через {@link MyItmoApi}
     * @param <T> тип данных в поле {@code result} ответа
     * @return десериализованный {@link ResultResponse}
     *
     * @throws ApiException если:
     * <ul>
     *     <li>сервер вернул HTTP-ошибку (4xx/5xx)</li>
     *     <li>не удалось распарсить тело ошибки</li>
     *     <li>произошла ошибка сети</li>
     * </ul>
     */

    public <T> ResultResponse<T> execute(Call<ResultResponse<T>> call) {
        try {
            Response<ResultResponse<T>> response = call.execute();

            if (response.isSuccessful()) {
                return response.body();
            }

            try (ResponseBody errorBody = response.errorBody()) {
                String errorJson = errorBody != null
                        ? errorBody.string()
                        : null;

                if (errorJson != null && !errorJson.isEmpty()) {
                    ResultResponse<?> error = gson.fromJson(errorJson, ResultResponse.class);

                    throw new ApiException(
                            error.getErrorCode(),
                            error.getErrorMessage()
                    );
                }

                throw new ApiException("HTTP " + response.code() + " without body", null);
            }
        } catch (IOException e) {
            throw new ApiException("Network error", e);
        }
    }
}
