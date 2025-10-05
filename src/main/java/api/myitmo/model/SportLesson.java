package api.myitmo.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class SportLesson {

    private long id;

    private OffsetDateTime date;

    @SerializedName("date_end")
    private OffsetDateTime dateEnd;

    @SerializedName("section_id")
    private Long sectionId;

    @SerializedName("section_name")
    private String sectionName;

    @SerializedName("section_level")
    private Long sectionLevel;

    @SerializedName("lesson_group_id")
    private Long lessonGroupId;

    @SerializedName("lesson_level")
    private Long lessonLevel;

    @SerializedName("type_id")
    private Long typeId;

    @SerializedName("building_id")
    private Long buildingId;

    @SerializedName("room_id")
    private Long roomId;

    @SerializedName("room_name")
    private String roomName;

    private Long limit;

    private Long available;

    @Nullable
    private String comment;

    @SerializedName("time_slot_id")
    private Long timeSlotId;

    @SerializedName("time_slot_start")
    private String timeSlotStart;

    @SerializedName("time_slot_end")
    private String timeSlotEnd;

    private Boolean intersection;

    @SerializedName("can_sign_in")
    private CanSignIn canSignIn;

    @SerializedName("other_lessons")
    private List<OtherLesson> otherLessons;

    private Boolean signed;

    @SerializedName("teacher_isu")
    private Long teacherIsu;

    @SerializedName("teacher_fio")
    private String teacherFio;

    @Data
    public static class OtherLesson {

        private long id;

        private int weekday;

        @SerializedName("room_id")
        private Long roomId;

        @SerializedName("room_name")
        private String roomName;

        @SerializedName("evaluation_id")
        private Long evaluationId;

        @SerializedName("evaluation_name")
        private String evaluationName;

        @SerializedName("time_slot_id")
        private Long timeSlotId;

        @SerializedName("time_slot_start")
        private String timeSlotStart;

        @SerializedName("time_slot_end")
        private String timeSlotEnd;

        private Boolean repeatable;

        @SerializedName("teacher_isu")
        private Long teacherIsu;

        @SerializedName("teacher_fio")
        private String teacherFio;

        @SerializedName("type_id")
        private Long typeId;

        @Nullable
        private String comment;

        private Boolean intersection;
    }
}
