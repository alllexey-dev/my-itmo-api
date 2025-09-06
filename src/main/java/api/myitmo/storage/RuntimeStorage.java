package api.myitmo.storage;

import lombok.Data;

@Data
public class RuntimeStorage implements Storage {

    private String accessToken;

    private long accessExpiresAt;

    private String refreshToken;

    private long refreshExpiresAt;

    private String idToken;
}
