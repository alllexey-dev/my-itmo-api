package api.myitmo.model.schedule;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;
import java.util.List;

/** Расписание пользователя на один календарный день. */
@Data
public class Schedule {

    /** Номер дня в серверном диапазоне ответа. */
    @SerializedName("day_number")
    private int dayNumber;

    /** Номер учебной недели. */
    @SerializedName("week_number")
    private int weekNumber;

    private LocalDate date;

    /** Примечание ко всему дню; обычно {@code null}. */
    @Nullable
    private String note;

    /** Занятия этого дня. */
    private List<Lesson> lessons;

    // Поле присутствует в ответе API, но наблюдавшихся значений недостаточно для определения типа.
    // private ? type;

    // Поле присутствует в ответе API, но структура элементов пока не подтверждена.
    // private List<?> intersections;
}
