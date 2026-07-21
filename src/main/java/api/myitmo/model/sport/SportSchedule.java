package api.myitmo.model.sport;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/** Группа спортивных занятий на одну календарную дату. */
@Data
public class SportSchedule {

    private LocalDate date;

    /** Занятия дня; персональный календарь иногда возвращает {@code null} вместо пустого списка. */
    private List<SportLesson> lessons;
}
