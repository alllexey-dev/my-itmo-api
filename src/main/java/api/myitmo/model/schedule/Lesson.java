package api.myitmo.model.schedule;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

/** Учебное занятие из личного расписания. */
@Data
public class Lesson {

    /** Уникальный идентификатор пары в расписании. */
    @SerializedName("pair_id")
    private long pairId;

    @Nullable
    private String subject;

    /** Внутренний идентификатор дисциплины. */
    @SerializedName("subject_id")
    private long subjectId;

    /** Дополнительное примечание; обычно {@code null}. */
    @Nullable
    private String note;

    private String type;

    @SerializedName("time_start")
    private String timeStart;

    @SerializedName("time_end")
    private String timeEnd;

    /** ИСУ преподавателя; может отсутствовать. */
    @Nullable
    @SerializedName("teacher_id")
    private Long teacherId;

    /** ФИО преподавателя; может отсутствовать. */
    @Nullable
    @SerializedName("teacher_name")
    private String teacherName;

    @Nullable
    private String room;

    @Nullable
    private String building;

    /** Название формата, например очный или дистанционный. */
    private String format;

    /** Название вида работы, например лекция, практика или лабораторная. */
    @SerializedName("work_type")
    private String workType;

    /**
     * Идентификатор вида работы. Наблюдаемые значения:
     * 1 — лекция, 2 — лабораторная, 3 — практика, 5 — экзамен,
     * 6 — зачёт, 10 — консультация, 11 — спорт.
     */
    @SerializedName("work_type_id")
    private int workTypeId;

    private String group;

    /**
     * Тип потока. Наблюдаемые значения:
     * 2 — учебные пары, 3 — спорт, 5 — бронирование аудиторий.
     */
    @SerializedName("flow_type_id")
    private int flowTypeId;

    @SerializedName("flow_id")
    private int flowId;

    /** Ссылка на дистанционное занятие; может отсутствовать. */
    @Nullable
    @SerializedName("zoom_url")
    private String zoomUrl;

    /** Пароль дистанционного занятия; может отсутствовать. */
    @Nullable
    @SerializedName("zoom_password")
    private String zoomPassword;

    /** Дополнительные данные для подключения к дистанционному занятию; могут отсутствовать. */
    @Nullable
    @SerializedName("zoom_info")
    private String zoomInfo;

    /** Идентификатор фактического корпуса. */
    @Nullable
    @SerializedName("bld_id")
    private Integer bldId;

    /**
     * Идентификатор формата. Наблюдаемые значения:
     * 1 — очный, 2 — очно-дистанционный, 3 — дистанционный.
     */
    @SerializedName("format_id")
    private int formatId;

    /**
     * Идентификатор основного корпуса. Частые значения:
     * 13 — Кронверкский проспект, 273 — ул. Ломоносова,
     * 5 — Вяземский переулок, 319 — виртуальные аудитории.
     */
    @Nullable
    @SerializedName("main_bld_id")
    private Integer mainBldId;

}
