package api.bars;

import api.bars.model.Setting;
import api.bars.model.Term;
import api.bars.model.User;
import api.bars.storage.BarsStorage;
import api.bars.storage.RuntimeBarsStorage;
import api.bars.utils.BarsApiException;
import api.bars.utils.BarsAuthHelper;
import api.bars.utils.BarsCodeSupplier;
import api.bars.utils.BarsTokenInterceptor;
import api.myitmo.storage.RuntimeCookieJar;
import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Клиент БАРС ИТМО ({@code https://bars.itmo.ru}).
 *
 * <p>Сессия — заголовок {@code Bearer ...}, который выдаёт {@code GET login} после
 * OIDC-входа через ITMO.ID. Он живёт около 30 минут, refresh token не выдаётся и
 * сервер его не продлевает. Для тихого продления задайте {@link BarsCodeSupplier}:
 * при HTTP 401 клиент один раз запросит новый код, обменяет его и повторит запрос.</p>
 *
 * <p>Каталоги и журналы читаются в контексте периода, сохранённого на сервере
 * ({@link #selectPeriod}); эта настройка общая с веб-версией БАРС.</p>
 */
@Setter
public class Bars {

    private BarsApi api;

    private Retrofit retrofit;

    private OkHttpClient okHttpClient;

    private Gson gson;

    private BarsAuthHelper authHelper;

    private BarsStorage storage;

    /** Источник кода для тихого продления сессии; без него 401 сразу становится исключением. */
    @Getter
    private BarsCodeSupplier codeSupplier;

    @Getter
    private final BarsConfiguration configuration;

    private final ReentrantLock sessionLock = new ReentrantLock();

    public Bars() {
        this(BarsConfiguration.DEFAULT);
    }

    public Bars(BarsConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * Создаёт клиент поверх существующего {@link OkHttpClient}, например из {@code MyItmo},
     * чтобы разделить cookie jar ITMO.ID и входить в БАРС по SSO без пароля.
     */
    public Bars(BarsConfiguration configuration, OkHttpClient sharedClient) {
        this.configuration = configuration;
        this.okHttpClient = sharedClient.newBuilder()
                .addInterceptor(new BarsTokenInterceptor(this))
                .followRedirects(false)
                .followSslRedirects(false)
                .build();
    }

    // region auth

    /** Обменивает authorization code на сессию и сохраняет её. */
    public void login(String code) {
        getStorage().setAuthorization(getAuthHelper().exchange(code));
    }

    /** Вход по логину и паролю ITMO.ID; учётные данные не сохраняются. */
    public void auth(String username, String password) {
        getStorage().setAuthorization(getAuthHelper().auth(username, password));
    }

    /**
     * Вход по уже существующей сессии ITMO.ID в cookie jar клиента.
     *
     * @return {@code false}, если ITMO.ID требует ввода пароля
     */
    public boolean authWithSession() {
        String code = getAuthHelper().obtainCodeFromSession(BarsAuthHelper.newState());
        if (code == null) return false;
        login(code);
        return true;
    }

    public boolean hasSession() {
        return getStorage().getAuthorization() != null;
    }

    public void logout() {
        getStorage().setAuthorization(null);
    }

    /** Заголовок сессии: одна строка {@code Bearer ...} разумной длины без переводов строк. */
    public static boolean isValidAuthorization(String value) {
        return value != null && value.startsWith("Bearer ") && value.length() >= 16 && value.length() <= 16384
                && value.indexOf('\n') < 0 && value.indexOf('\r') < 0;
    }

    // endregion auth

    // region session

    /**
     * Выполняет запрос и возвращает тело. Без сессии или при HTTP 401 пробует
     * получить новую сессию через {@link BarsCodeSupplier} и повторяет запрос один раз.
     *
     * @throws BarsApiException при HTTP-ошибке (в том числе 401 без возможности продления) или ошибке сети
     */
    public <T> T execute(Call<T> call) {
        String authorization = getStorage().getAuthorization();
        if (authorization == null && !renew(null)) throw new BarsApiException(401);
        Response<T> response = perform(call);
        if (response.code() == 401 && renew(authorization)) {
            response = perform(call.clone());
        }
        if (!response.isSuccessful()) throw new BarsApiException(response.code());
        String rotated = response.headers().get("authorization");
        if (isValidAuthorization(rotated) && !rotated.equals(getStorage().getAuthorization())) {
            getStorage().setAuthorization(rotated);
        }
        return response.body();
    }

    private <T> Response<T> perform(Call<T> call) {
        try {
            return call.execute();
        } catch (IOException e) {
            throw new BarsApiException("Network error", e);
        }
    }

    /**
     * Продлевает сессию, если задан {@link BarsCodeSupplier}. Параллельные запросы,
     * получившие 401 с одним и тем же заголовком, делят одно продление.
     */
    private boolean renew(String rejected) {
        if (codeSupplier == null) return false;
        synchronized (sessionLock) {
            String current = getStorage().getAuthorization();
            if (current != null && !current.equals(rejected)) return true;
            String code = codeSupplier.obtainCode(BarsAuthHelper.newState());
            if (code == null) return false;
            login(code);
            return true;
        }
    }

    public User getCurrentUser() {
        return execute(getApi().getCurrentUser());
    }

    /**
     * Делает {@code year}/{@code term} текущим периодом пользователя на сервере.
     * Записывает только отличающиеся значения и перечитывает пользователя после записи.
     *
     * @param year учебный год в формате {@code 2025/2026}
     * @throws BarsApiException если сервер не принял выбор
     */
    public User selectPeriod(String year, Term term) {
        if (!year.matches("\\d{4}/\\d{4}")) throw new IllegalArgumentException("Year must look like 2025/2026");
        User user = getCurrentUser();
        if (year.equals(user.getSelectedYear()) && user.getSelectedTerm() == term.getWireValue()) return user;
        if (!year.equals(user.getSelectedYear())) {
            execute(getApi().setPersonalSetting(new Setting("current_year", year)));
        }
        if (user.getSelectedTerm() != term.getWireValue()) {
            execute(getApi().setPersonalSetting(new Setting("current_term", String.valueOf(term.getWireValue()))));
        }
        user = getCurrentUser();
        if (!year.equals(user.getSelectedYear()) || user.getSelectedTerm() != term.getWireValue()) {
            throw new BarsApiException("Server did not apply the selected period", null);
        }
        return user;
    }

    /**
     * Выбирает период и выполняет {@code action}, пока другой поток этого клиента
     * не может сменить период. Чтения внутри {@code action} могут идти параллельно.
     */
    public <T> T withPeriod(String year, Term term, Callable<T> action) {
        sessionLock.lock();
        try {
            selectPeriod(year, term);
            return action.call();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new BarsApiException("Action failed", e);
        } finally {
            sessionLock.unlock();
        }
    }

    // endregion session

    public BarsApi getApi() {
        if (api == null) {
            api = getRetrofit().create(BarsApi.class);
        }
        return api;
    }

    public Retrofit getRetrofit() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(configuration.getRestUrl())
                    .client(getOkHttpClient())
                    .addConverterFactory(GsonConverterFactory.create(getGson()))
                    .build();
        }
        return retrofit;
    }

    public OkHttpClient getOkHttpClient() {
        if (okHttpClient == null) {
            okHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(new BarsTokenInterceptor(this))
                    .cookieJar(new RuntimeCookieJar()) // ITMO.ID keeps its SSO session in cookies
                    .followRedirects(false) // the authorization code arrives in a Location header
                    .followSslRedirects(false)
                    .build();
        }
        return okHttpClient;
    }

    public Gson getGson() {
        if (gson == null) {
            gson = new Gson();
        }
        return gson;
    }

    public BarsAuthHelper getAuthHelper() {
        if (authHelper == null) {
            authHelper = new BarsAuthHelper(this);
        }
        return authHelper;
    }

    /** По умолчанию {@link RuntimeBarsStorage} в памяти. */
    public BarsStorage getStorage() {
        if (storage == null) {
            storage = new RuntimeBarsStorage();
        }
        return storage;
    }
}
