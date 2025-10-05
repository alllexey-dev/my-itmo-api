package api.myitmo.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

@Data
public class CanSignIn {

    @SerializedName("can_sign_in")
    private boolean canSignIn;

    @SerializedName("unavailable_reasons")
    private List<String> unavailableReasons;
}
