package api.myitmo.model;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class SportSchedule {

    private LocalDate date;

    private List<SportLesson> lessons;
}
