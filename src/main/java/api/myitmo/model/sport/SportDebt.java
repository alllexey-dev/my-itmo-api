package api.myitmo.model.sport;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

/** Состояние задолженности пользователя по физической культуре. */
@Data
public class SportDebt {

    /** Есть ли задолженность. */
    @SerializedName("is_having_debt")
    private boolean havingDebt;

    /** Сколько баллов необходимо набрать для её закрытия; отсутствует без задолженности. */
    @SerializedName("needed_score")
    private Double neededScore;

    /** Число доступных специальных попыток; отсутствует без задолженности. */
    @SerializedName("free_attempts")
    private Integer freeAttempts;
}
