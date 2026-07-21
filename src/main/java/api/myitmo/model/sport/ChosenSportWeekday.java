package api.myitmo.model.sport;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** Регулярный спортивный слот, заданный днём недели вместо конкретной даты. */
@EqualsAndHashCode(callSuper = true)
@Data
public class ChosenSportWeekday extends ChosenSportLesson {

    /** Локализованное или серверное название дня недели. */
    private String weekday;
}
