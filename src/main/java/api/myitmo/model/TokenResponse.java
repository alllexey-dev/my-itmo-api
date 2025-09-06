package api.myitmo.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class TokenResponse {

    @SerializedName("access_token")
    private String accessToken;

    @SerializedName("expires_in")
    private long expiresIn;

    @SerializedName("refresh_token")
    private String refreshToken;

    @SerializedName("refresh_expires_in")
    private long refreshExpiresIn;

    @SerializedName("id_token")
    private String idToken;

    @SerializedName("session_state")
    private String sessionState;

}
