package api.myitmo.model.studyplan;

import lombok.Data;

/** Метаданные образовательной программы учебного плана. */
@Data
public class StudyPlanInfo {

    /** Код направления подготовки, например {@code 09.03.04}. */
    private String directionCode;

    private String directionName;

    /** Квалификация, например бакалавр или магистр. */
    private String levelQualification;

    private String planType;

    private String programName;

    /** Год начала обучения. */
    private int startYear;
}
