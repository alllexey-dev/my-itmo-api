package api.myitmo.model.sport;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class TimeSlot {
    private long id;

    @SerializedName("time_start")
    private String timeStart;

    @SerializedName("time_end")
    private String timeEnd;
}
