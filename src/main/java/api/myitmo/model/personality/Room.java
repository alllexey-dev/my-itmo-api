package api.myitmo.model.personality;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class Room {

    @SerializedName("room_number")
    private String roomNumber;

    @SerializedName("bld_name")
    private String bldName;
}
