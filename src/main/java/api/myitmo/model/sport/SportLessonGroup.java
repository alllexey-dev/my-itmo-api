package api.myitmo.model.sport;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

/** Группа регулярных занятий внутри выбранной спортивной секции. */
@Data
public class SportLessonGroup {

    private long id;

    /**
     * Уровень: 1 — свободное посещение, 2 — обучение,
     * 3 — средний уровень, 4 — сборная. Набор может расширяться.
     */
    private int level;

    @SerializedName("level_name")
    private String levelName;

    /** Есть ли в группе будущие разовые занятия. */
    @SerializedName("has_future_lessons")
    private boolean hasFutureLessons;

    /** Конкретные занятия с календарными датами. */
    private List<ChosenSportLesson> lessons;

    /** Регулярные занятия, описанные днём недели. */
    private List<ChosenSportWeekday> weekdays;
}
