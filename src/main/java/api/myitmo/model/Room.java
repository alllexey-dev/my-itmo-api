package api.myitmo.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class Room {

    @SerializedName("room_number")
    private String roomNumber;

    @SerializedName("bld_name")
    private String bldName;
}
