package api.myitmo.model.sport;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

@Data
public class SportLessonGroup {

    private long id;

    private int level;

    @SerializedName("level_name")
    private String levelName;

    @SerializedName("has_future_lessons")
    private boolean hasFutureLessons;

    private List<ChosenSportLesson> lessons;

    private List<ChosenSportWeekday> weekdays;
}
