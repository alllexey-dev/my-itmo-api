package api.myitmo.model.other;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

/**
 * Набор OAuth2/OIDC-токенов ITMO.ID.
 * Значения являются секретами и не должны попадать в логи, аналитику или сообщения об ошибках.
 */
@Data
public class TokenResponse {

    /** Короткоживущий токен доступа к API. */
    @SerializedName("access_token")
    private String accessToken;

    /** Время жизни access token в секундах с момента выдачи. */
    @SerializedName("expires_in")
    private long expiresIn;

    /** Токен обновления; сервер может ротировать его при каждом обновлении. */
    @SerializedName("refresh_token")
    private String refreshToken;

    /** Время жизни refresh token в секундах с момента выдачи. */
    @SerializedName("refresh_expires_in")
    private long refreshExpiresIn;

    /** OIDC ID token с идентификационными claims пользователя. */
    @SerializedName("id_token")
    private String idToken;

    /** Идентификатор серверной OIDC-сессии. */
    @SerializedName("session_state")
    private String sessionState;
}
