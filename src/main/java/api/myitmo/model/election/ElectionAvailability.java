package api.myitmo.model.election;

import lombok.Data;

import java.time.OffsetDateTime;

/** Состояние и сроки кампании записи по выбору. */
@Data
public class ElectionAvailability {

    /** Серверный статус кампании; значение {@code 1} означает открытую выборность. */
    private int id;

    private String status;

    private OffsetDateTime semesterStart;

    private OffsetDateTime semesterEnd;

    private OffsetDateTime dateStart;

    private OffsetDateTime dateEnd;

    private String timeStart;

    private String timeEnd;

    private String studyYear;

    private long semesterId;

    private int semester;
}
