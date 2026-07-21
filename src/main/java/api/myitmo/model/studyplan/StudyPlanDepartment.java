package api.myitmo.model.studyplan;

import lombok.Data;

/** Подразделение, ответственное за дисциплину или модуль. */
@Data
public class StudyPlanDepartment {

    private long id;

    private String name;

    private String shortName;
}
