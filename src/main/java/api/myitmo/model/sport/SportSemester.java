package api.myitmo.model.sport;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.time.OffsetDateTime;

/** Полное описание спортивного семестра и контрольных периодов записи. */
@Data
public class SportSemester {

    private long id;

    /** Учебный год в формате {@code YYYY/YYYY}. */
    @SerializedName("study_year")
    private String studyYear;

    /** Номер семестра в серверной классификации; наблюдалось значение 0. */
    private int semester;

    @SerializedName("date_start")
    private OffsetDateTime dateStart;

    /** Мягкая дата окончания основного периода. */
    @SerializedName("date_end")
    private OffsetDateTime dateEnd;

    /** Жёсткая дата, после которой семестр полностью закрыт. */
    @SerializedName("hard_date_end")
    private OffsetDateTime hardDateEnd;

    /** Признак текущего спортивного семестра. */
    private boolean current;

    /** Начало периода выбора; сервер может вернуть техническую минимальную дату. */
    @SerializedName("choice_start")
    private OffsetDateTime choiceStart;

    /** Граница для бакалавриата; сервер может вернуть техническую минимальную дату. */
    @SerializedName("bachelor_bound")
    private OffsetDateTime bachelorBound;

    /** Начало первого периода ППА. */
    @SerializedName("ppa1_start")
    private OffsetDateTime ppa1Start;

    /** Конец первого периода ППА. */
    @SerializedName("ppa1_end")
    private OffsetDateTime ppa1End;

    /** Начало второго периода ППА. */
    @SerializedName("ppa2_start")
    private OffsetDateTime ppa2Start;

    /** Конец второго периода ППА. */
    @SerializedName("ppa2_end")
    private OffsetDateTime ppa2End;

    /** Допустимая продолжительность записи в днях. */
    @SerializedName("sign_duration")
    private int signDuration;
}
