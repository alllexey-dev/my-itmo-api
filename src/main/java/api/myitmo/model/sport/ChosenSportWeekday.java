package api.myitmo.model.sport;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ChosenSportWeekday extends ChosenSportLesson {

    private String weekday;
}
