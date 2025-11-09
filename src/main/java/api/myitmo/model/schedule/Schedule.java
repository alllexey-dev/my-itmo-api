package api.myitmo.model.schedule;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;
import java.util.List;

@Data
public class Schedule {

    @SerializedName("day_number")
    private int dayNumber;

    @SerializedName("week_number")
    private int weekNumber;

    private LocalDate date;

    @Nullable
    private String note;

    private List<Lesson> lessons;

    // private ? type;

    // private List<?> intersections
}
