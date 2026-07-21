package api.myitmo.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

/** Стандартная обёртка большинства API MyITMO. */
@Data
public class ResultResponse<T> {

    /** {@code 0} при успехе; остальные коды описывают серверную ошибку. */
    @SerializedName("error_code")
    private int errorCode;

    /** Локализованное сообщение об ошибке; обычно {@code null} при успехе. */
    @SerializedName("error_message")
    private String errorMessage;

    /** Полезная нагрузка; при ошибке может отсутствовать. */
    private T result;
}
