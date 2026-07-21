package api.myitmo.model.sport;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

/** Решение сервера о возможности записаться на занятие. */
@Data
public class CanSignIn {

    @SerializedName("can_sign_in")
    private boolean canSignIn;

    /** Локализованные причины запрета; пустой список при разрешённой записи. */
    @SerializedName("unavailable_reasons")
    private List<String> unavailableReasons;
}
