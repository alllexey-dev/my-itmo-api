package api.myitmo.model.sport;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

/** Вложенная обёртка ответа о медицинской группе. */
@Data
public class SportHealthLevelResponse {

    @SerializedName("health_level")
    private SportHealthLevel healthLevel;
}
