package api.myitmo.model.sport;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.time.OffsetDateTime;

/** Начисление спортивных баллов за занятие, соревнование или другое действие. */
@Data
public class SportAttendance {

    /** Наблюдаемые значения: {@code lesson}, {@code competition}; набор может расширяться. */
    private String type;

    private String name;

    @SerializedName("evaluation_id")
    private long evaluationId;

    @SerializedName("evaluation_name")
    private String evaluationName;

    @SerializedName("section_level")
    private int sectionLevel;

    /** Баллы, начисленные за событие. */
    private int score;

    private OffsetDateTime date;

    /** Признак начисления за соревнование. */
    @SerializedName("is_competition")
    private boolean isCompetition;

    /** Вид спорта для соревнования; у обычного занятия может отсутствовать. */
    @SerializedName("discipline_name")
    private String disciplineName;

    /** Название соревнования; у обычного занятия отсутствует. */
    @SerializedName("competition_name")
    private String competitionName;

    /** Результат или место на соревновании, например {@code Участие}. */
    private String place;
}
