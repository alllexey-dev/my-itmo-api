package api.myitmo.model.sport;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

/** Состояние участия пользователя в спортивном экстернате. */
@Data
public class SportExternat {

    /** Подана и принята ли запись в формате экстерната. */
    private boolean signed;

    /** Серверный статус заявки; отсутствует, если заявка не подавалась. */
    @SerializedName("externat_status_id")
    private Long externatStatusId;

    /** Причина отказа; отсутствует при отсутствии отказа. */
    @SerializedName("decline_reason")
    private String declineReason;
}
