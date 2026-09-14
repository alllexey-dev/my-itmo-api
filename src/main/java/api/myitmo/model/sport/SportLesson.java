package api.myitmo.model.sport;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

import java.time.OffsetDateTime;
import java.util.List;

/** Спортивное занятие, доступное для записи или присутствующее в персональном календаре. */
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

    /**
     * Уровень секции. Наблюдаемые значения:
     * 1 — свободное посещение, 2 — секция с отбором.
     */
    @SerializedName("section_level")
    private Long sectionLevel;

    @SerializedName("lesson_group_id")
    private Long lessonGroupId;

    /**
     * Уровень занятия. Наблюдаемые значения:
     * 1 — открытое или свободное занятие, 2 — обучение,
     * 3 — средний уровень, 4 — сборная.
     */
    @SerializedName("lesson_level")
    private Long lessonLevel;

    /**
     * Тип занятия. Наблюдаемые значения:
     * 1 — открытое занятие, 2 — свободное посещение, 5 — задолженность,
     * 6 — нормативы, 7 — экстернат, 8 — дополнительное занятие.
     */
    @SerializedName("type_id")
    private Long typeId;

    /**
     * Real venue ID, not necessarily present in the building filter options.
     * Observed 2026-09-09: off-site venues have positive IDs missing from filters;
     * online lessons have null here and room_id=-1. Preserve null and the raw ID.
     */
    @Nullable
    @SerializedName("building_id")
    private Long buildingId;

    /** Real room ID; -1 explicitly denotes Online (observed 2026-09-09). */
    @SerializedName("room_id")
    private Long roomId;

    @SerializedName("room_name")
    private String roomName;

    /** Общее число мест. */
    private Long limit;

    /** Число доступных мест. */
    private Long available;

    @Nullable
    private String comment;

    @SerializedName("time_slot_id")
    private Long timeSlotId;

    @SerializedName("time_slot_start")
    private String timeSlotStart;

    @SerializedName("time_slot_end")
    private String timeSlotEnd;

    /** Пересекается ли занятие с другим событием пользователя. */
    private Boolean intersection;

    /** Серверное решение о возможности записи и причины запрета. */
    @SerializedName("can_sign_in")
    private CanSignIn canSignIn;

    /** Альтернативные связанные занятия. */
    @SerializedName("other_lessons")
    private List<OtherLesson> otherLessons;

    /** Записан ли пользователь на занятие. */
    private Boolean signed;

    @SerializedName("teacher_isu")
    private Long teacherIsu;

    @SerializedName("teacher_fio")
    private String teacherFio;

    /** Краткое описание связанного альтернативного занятия. */
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
