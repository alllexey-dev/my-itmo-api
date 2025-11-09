package api.myitmo.model.sport;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

import java.time.OffsetDateTime;

@Data
public class ChosenSportLesson {

    private long id;

    @SerializedName("date_start")
    private OffsetDateTime dateStart;

    @SerializedName("date_end")
    private OffsetDateTime dateEnd;

    @SerializedName("time_slot_id")
    private Long timeSlotId;

    @SerializedName("time_start")
    private String timeStart;

    @SerializedName("time_end")
    private String timeEnd;

    @SerializedName("room_id")
    private Long roomId;

    @SerializedName("room_name")
    private String roomName;

    @SerializedName("teacher_isu")
    private Long teacherIsu;

    @SerializedName("teacher_fio")
    private String teacherFio;

    @SerializedName("type_id")
    private Long typeId;

    @SerializedName("link_url")
    private String linkUrl;

    @Nullable
    private String comment;

    private boolean intersection;
}
