package api.myitmo.model.sport;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

/** Счётчики попыток записи на спортивные занятия. */
@Data
public class SportAttempts {

    /** Общее количество попыток, выданных пользователю. */
    @SerializedName("total_attempts")
    private int totalAttempts;

    /** Количество уже использованных попыток. */
    @SerializedName("used_attempts")
    private int usedAttempts;

    /** Количество оставшихся попыток. */
    @SerializedName("free_attempts")
    private int freeAttempts;

    /** Разрешает ли сервер выполнить новую запись прямо сейчас. */
    @SerializedName("can_sign_in")
    private boolean canSignIn;
}
