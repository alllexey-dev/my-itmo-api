package api.myitmo.model.personality;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

/** Аудитория или кабинет, связанный с персоналией. */
@Data
public class Room {

    /** Номер аудитории. */
    @SerializedName("room_number")
    private String roomNumber;

    /** Название или адрес корпуса. */
    @SerializedName("bld_name")
    private String bldName;
}
