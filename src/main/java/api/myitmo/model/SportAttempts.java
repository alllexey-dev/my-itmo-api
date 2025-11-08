package api.myitmo.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class SportAttempts {

    @SerializedName("total_attempts")
    private int totalAttempts;

    @SerializedName("used_attempts")
    private int usedAttempts;

    @SerializedName("free_attempts")
    private int freeAttempts;

    @SerializedName("can_sign_in")
    private String canSignIn;
}
