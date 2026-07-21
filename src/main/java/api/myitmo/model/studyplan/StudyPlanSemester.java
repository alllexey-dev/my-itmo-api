package api.myitmo.model.studyplan;

import lombok.Data;

/** Семестр в учебном плане. */
@Data
public class StudyPlanSemester {

    private int semester;

    private long semesterId;

    /** Чётность семестра по классификации сервера; наблюдаемые значения: 0 и 1. */
    private int semesterParity;

    /** Учебный год в формате {@code YYYY/YYYY}. */
    private String studyYear;
}
