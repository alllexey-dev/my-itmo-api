package api.myitmo.model.sport;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

/** Конкретный уровень или норматив спортивного отбора. */
@Data
public class SportRequisite {

    private long id;

    /** Название уровня, например {@code Сборная команда}. */
    @SerializedName("level_name")
    private String levelName;

    /** Полное название вида спорта и уровня. */
    private String name;
}
