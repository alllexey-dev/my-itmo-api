package api.myitmo.model.election;

import lombok.Data;

/** Доступность дисциплины в одном учебном семестре. */
@Data
public class AvailableDisciplineSemester {

    private int semester;

    /** Серверный статус; значение {@code 1} означает доступную дисциплину. */
    private int statusId;

    /** Непрозрачный идентификатор, принимаемый endpoint-ом {@code /order/}. */
    private String groupFlow;

    private String statusName;
}
