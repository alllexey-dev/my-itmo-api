package api.myitmo.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class ResultResponse<T> {

    @SerializedName("error_code")
    private int errorCode;

    @SerializedName("error_message")
    private String errorMessage;

    private T result;
}
