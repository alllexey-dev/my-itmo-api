package api.myitmo.model.personality;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

/**
 * Аудитория или кабинет, связанный с персоналией.
 * В проверенных профилях rooms был пустым массивом: типы значений, nullability
 * и наличие полей отдельной аудитории этой пробой не подтверждены.
 */
@Data
public class Room {

    /** Номер аудитории; null и отсутствие поля в непустом rooms не проверены. */
    @SerializedName("room_number")
    private String roomNumber;

    /** Название или адрес корпуса; null и отсутствие поля в непустом rooms не проверены. */
    @SerializedName("bld_name")
    private String bldName;
}
