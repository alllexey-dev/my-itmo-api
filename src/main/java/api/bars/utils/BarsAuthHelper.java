package api.bars.utils;

import api.bars.Bars;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.net.URI;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * OIDC-вход в БАРС через ITMO.ID. У веб-клиента БАРС нет PKCE: authorization code
 * обменивает бэкенд БАРС по своему зарегистрированному callback, поэтому клиенту
 * достаточно получить {@code code} и передать его в {@code GET login}.
 */
public class BarsAuthHelper {

    private final Bars bars;

    public BarsAuthHelper(Bars bars) {
        this.bars = bars;
    }

    public static String newState() {
        return UUID.randomUUID().toString();
    }

    /** URL страницы входа ITMO.ID для клиента БАРС; {@code state} проверяется в {@link #extractCode}. */
    public String getLoginUrl(String state) {
        return HttpUrl.get(bars.getConfiguration().getIssuer() + "/protocol/openid-connect/auth")
                .newBuilder()
                .addQueryParameter("response_type", "code")
                .addQueryParameter("scope", "openid")
                .addQueryParameter("client_id", bars.getConfiguration().getClientId())
                .addQueryParameter("redirect_uri", bars.getConfiguration().getRedirectUri())
                .addQueryParameter("state", state)
                .build()
                .toString();
    }

    /** Точный HTTPS callback клиента: тот же хост и путь, без userinfo и нестандартного порта. */
    public boolean isCallback(String url) {
        URI uri = parse(url);
        if (uri == null) return false;
        URI callback = parse(bars.getConfiguration().getRedirectUri());
        return callback != null && trusted(uri, callback.getHost()) && callback.getRawPath().equals(uri.getRawPath());
    }

    /** Страницы, которые можно показывать во время входа: ITMO.ID и callback. */
    public boolean isAllowedPage(String url) {
        URI uri = parse(url);
        URI issuer = parse(bars.getConfiguration().getIssuer());
        return uri != null && issuer != null && (trusted(uri, issuer.getHost()) || isCallback(url));
    }

    /**
     * Достаёт {@code code} из callback URL. Возвращает {@code null}, если это не callback,
     * {@code state} не совпал, есть {@code error}, fragment, повторяющиеся параметры
     * или {@code iss} указывает на другой issuer.
     */
    public String extractCode(String callbackUrl, String expectedState) {
        if (expectedState == null || expectedState.isEmpty() || !isCallback(callbackUrl)) return null;
        try {
            URI uri = new URI(callbackUrl);
            if (uri.getRawFragment() != null) return null;
            Map<String, String> query = new HashMap<>();
            String rawQuery = uri.getRawQuery() == null ? "" : uri.getRawQuery();
            for (String part : rawQuery.split("&")) {
                if (part.isEmpty()) continue;
                int eq = part.indexOf('=');
                String name = URLDecoder.decode(eq < 0 ? part : part.substring(0, eq), "UTF-8");
                String value = URLDecoder.decode(eq < 0 ? "" : part.substring(eq + 1), "UTF-8");
                if (query.put(name, value) != null) return null;
            }
            if (!expectedState.equals(query.get("state")) || query.containsKey("error")) return null;
            String iss = query.get("iss");
            if (iss != null && !iss.equals(bars.getConfiguration().getIssuer())) return null;
            String code = query.get("code");
            return code == null || code.isEmpty() || code.length() > 4096 ? null : code;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Обменивает код на сессию через {@code GET login} и возвращает заголовок {@code Bearer ...}.
     *
     * @throws BarsApiException если сервер ответил ошибкой или не вернул заголовок
     */
    public String exchange(String code) {
        try {
            retrofit2.Response<Void> response = bars.getApi().login(code, bars.getConfiguration().getRedirectUri()).execute();
            if (!response.isSuccessful()) throw new BarsApiException(response.code());
            String authorization = response.headers().get("authorization");
            if (!Bars.isValidAuthorization(authorization)) throw new BarsApiException(401);
            return authorization;
        } catch (java.io.IOException e) {
            throw new BarsApiException("Network error", e);
        }
    }

    /**
     * Получает код по уже существующей сессии ITMO.ID в cookie jar клиента (SSO).
     * Работает после {@code MyItmo#auth}, если {@link Bars} создан поверх того же {@link OkHttpClient}.
     *
     * @return код или {@code null}, если ITMO.ID требует ввода пароля
     */
    public String obtainCodeFromSession(String state) {
        Request request = new Request.Builder().get().url(getLoginUrl(state)).build();
        try (Response response = bars.getOkHttpClient().newCall(request).execute()) {
            String location = response.header("Location");
            return location == null ? null : extractCode(location, state);
        } catch (java.io.IOException e) {
            throw new BarsApiException("Network error", e);
        }
    }

    /**
     * Вход по логину и паролю: форма ITMO.ID для клиента БАРС, код из redirect, обмен на сессию.
     * Логин и пароль не сохраняются.
     *
     * @throws BarsApiException если любой шаг завершился ошибкой
     */
    public String auth(String username, String password) {
        String state = newState();
        OkHttpClient client = bars.getOkHttpClient();
        String loginActionUrl;
        try (Response initial = client.newCall(new Request.Builder().get().url(getLoginUrl(state)).build()).execute()) {
            String location = initial.header("Location");
            if (location != null) {
                String code = extractCode(location, state);
                if (code != null) return exchange(code);
            }
            String body = initial.body() == null ? "" : initial.body().string();
            loginActionUrl = findLoginAction(body);
        } catch (java.io.IOException e) {
            throw new BarsApiException("Network error", e);
        }
        if (loginActionUrl == null) throw new BarsApiException("Could not find ITMO.ID login form", null);
        FormBody form = new FormBody.Builder()
                .add("username", username)
                .add("password", password)
                .add("rememberMe", "on")
                .build();
        try (Response auth = client.newCall(new Request.Builder().post(form).url(loginActionUrl).build()).execute()) {
            String location = auth.header("Location");
            String code = location == null ? null : extractCode(location, state);
            if (code == null) throw new BarsApiException("ITMO.ID did not return an authorization code", null);
            return exchange(code);
        } catch (java.io.IOException e) {
            throw new BarsApiException("Network error", e);
        }
    }

    /** Keycloak отдаёт адрес формы как {@code action="..."} на HTML-странице или {@code "loginAction": "..."} в JSON-конфиге. */
    public static String findLoginAction(String body) {
        for (String key : new String[]{"\"loginAction\": \"", "\"loginAction\":\"", "action=\""}) {
            int start = body.indexOf(key);
            if (start < 0) continue;
            start += key.length();
            int end = body.indexOf('"', start);
            if (end < 0) continue;
            return body.substring(start, end).replace("&amp;", "&");
        }
        return null;
    }

    private static boolean trusted(URI uri, String host) {
        return "https".equals(uri.getScheme()) && uri.getHost() != null && uri.getHost().equalsIgnoreCase(host)
                && uri.getRawUserInfo() == null && (uri.getPort() == -1 || uri.getPort() == 443);
    }

    private static URI parse(String url) {
        try {
            return new URI(url);
        } catch (Exception e) {
            return null;
        }
    }
}
