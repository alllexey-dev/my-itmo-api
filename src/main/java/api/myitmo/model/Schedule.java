package api.myitmo.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.time.LocalDate;

@Data
public class Schedule {

    @SerializedName("day_number")
    private int dayNumber;

    @SerializedName("week_number")
    private int weekNumber;

    private LocalDate date;

    private String note;

    private String type;

    // todo: intersections
    // private List<?> intersections
}
