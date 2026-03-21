package api.myitmo.utils;

import api.myitmo.MyItmo;
import api.myitmo.model.other.TokenResponse;
import okhttp3.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class AuthHelper {

    private final MyItmo myItmo;

    public AuthHelper(MyItmo myItmo) {
        this.myItmo = myItmo;
    }

    // region auth

    /**
     * Выполняет полный OAuth2 Authorization Code Flow с PKCE.
     *
     * <p>Шаги:</p>
     * <ol>
     *     <li>Получение loginAction URL</li>
     *     <li>Отправка логина и пароля</li>
     *     <li>Получение authorization code из redirect</li>
     *     <li>Обмен code на access и refresh токены</li>
     * </ol>
     *
     * @param username логин пользователя
     * @param password пароль пользователя
     * @return {@link TokenResponse} с access и refresh токенами
     *
     * @throws RuntimeException если любой шаг аутентификации завершился ошибкой
     */

    public TokenResponse auth(String username, String password) {
        OkHttpClient client = myItmo.getOkHttpClient();

        String codeVerifier = generateCodeVerifier();
        String codeChallenge = getCodeChallenge(codeVerifier);

        Request initialRequest = getInitialRequest(codeChallenge);

        String loginActionUrl;

        try (Response initialResponse = client.newCall(initialRequest).execute()) {
            String body = initialResponse.body().string();
            String loginActionKey = "\"loginAction\": ";
            String subBody = body.substring(body.indexOf(loginActionKey) + loginActionKey.length());
            String loginActionEscaped = subBody.substring(0, subBody.indexOf("\"loginUrl\": ")).trim();
            loginActionUrl = loginActionEscaped.substring(1, loginActionEscaped.length() - 2);
        } catch (Exception e) {
            throw new RuntimeException("Could not get loginAction", e);
        }

        Request authRequest = getAuthRequest(loginActionUrl, username, password);

        String code;
        try (Response authResponse = client.newCall(authRequest).execute()) {
            String locationString = authResponse.header("Location");
            if (locationString == null) {
                throw new RuntimeException("Not found location header");
            }
            HttpUrl locationUrl = HttpUrl.get(locationString);
            code = locationUrl.queryParameter("code");
            if (code == null) {
                throw new RuntimeException("Not found code parameter");
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not get authorize", e);
        }

        Request tokenRequest = getTokenRequest(code, codeVerifier);
        try (Response tokenResponse = client.newCall(tokenRequest).execute()) {
            String body = tokenResponse.body().string();
            return myItmo.getGson().fromJson(body, TokenResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("Could not get tokens", e);
        }
    }

    /**
     * Формирует initial OAuth2 запрос (authorization endpoint).
     *
     * @param codeChallenge PKCE code challenge
     * @return HTTP GET запрос
     */
    public Request getInitialRequest(String codeChallenge) {
        HttpUrl url = HttpUrl.get("https://id.itmo.ru/auth/realms/itmo/protocol/openid-connect/auth")
                .newBuilder()
                .addEncodedQueryParameter("protocol", "oauth2")
                .addEncodedQueryParameter("response_type", "code")
                .addEncodedQueryParameter("client_id", myItmo.getConfiguration().getAuthClientId())
                .addEncodedQueryParameter("redirect_uri", myItmo.getConfiguration().getAuthRedirectUri())
                .addEncodedQueryParameter("scope", "openid profile")
                .addEncodedQueryParameter("state", "im_not_a_browser")
                .addEncodedQueryParameter("code_challenge_method", "S256")
                .addEncodedQueryParameter("code_challenge", codeChallenge)
                .build();

        return new Request.Builder()
                .get()
                .url(url)
                .build();
    }

    /**
     * Формирует POST-запрос с учётными данными пользователя.
     *
     * @param loginActionUrl URL формы логина
     * @param username логин
     * @param password пароль
     * @return HTTP POST запрос
     */
    public Request getAuthRequest(String loginActionUrl, String username, String password) {
        FormBody formBody = new FormBody.Builder()
                .add("username", username)
                .add("password", password)
                .add("rememberMe", "on")
                .build();

        return new Request.Builder()
                .post(formBody)
                .url(loginActionUrl)
                .build();
    }

    /**
     * Формирует запрос для обмена authorization code на токены.
     *
     * @param code authorization code
     * @param codeVerifier PKCE verifier
     * @return HTTP POST запрос
     */
    public Request getTokenRequest(String code, String codeVerifier) {
        FormBody formBody2 = new FormBody.Builder()
                .add("code", code)
                .add("client_id", myItmo.getConfiguration().getAuthClientId())
                .add("redirect_uri", myItmo.getConfiguration().getAuthRedirectUri())
                .add("response_type", "code")
                .add("grant_type", "authorization_code")
                .add("code_verifier", codeVerifier)
                .build();

        return new Request.Builder()
                .post(formBody2)
                .url( "https://id.itmo.ru/auth/realms/itmo/protocol/openid-connect/token")
                .build();
    }

    /**
     * Генерирует случайный PKCE code verifier.
     *
     * @return base64url строка без padding
     */
    public static String generateCodeVerifier() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] codeVerifier = new byte[32];
        secureRandom.nextBytes(codeVerifier);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(codeVerifier);
    }

    /**
     * Вычисляет PKCE code challenge (SHA-256).
     *
     * @param codeVerifier исходный verifier
     * @return base64url SHA-256 hash
     */
    public static String getCodeChallenge(String codeVerifier) {
        byte[] bytes = codeVerifier.getBytes(StandardCharsets.UTF_8);
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        byte[] digest = messageDigest.digest(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }

    // endregion auth

    /**
     * Обновляет токены с использованием refresh token.
     *
     * @param refreshToken refresh token
     * @return новый {@link TokenResponse}
     *
     * @throws RuntimeException если не удалось выполнить запрос или распарсить ответ
     */
    public TokenResponse refreshTokens(String refreshToken) {
        FormBody formBody =  new FormBody.Builder()
                .add("refresh_token", refreshToken)
                .add("scopes", "openid profile")
                .add("client_id", myItmo.getConfiguration().getAuthClientId())
                .add("grant_type", "refresh_token")
                .build();

        Request request = new Request.Builder()
                .post(formBody)
                .url("https://id.itmo.ru/auth/realms/itmo/protocol/openid-connect/token")
                .build();

        try (Response response = myItmo.getOkHttpClient().newCall(request).execute()) {
            ResponseBody body = response.body();
            if (body == null) throw new NullPointerException("Response body is null");
            String bodyString = body.string();
            return myItmo.getGson().fromJson(bodyString, TokenResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("Could not refresh tokens", e);
        }
    }
}
