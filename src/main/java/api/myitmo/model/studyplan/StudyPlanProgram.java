package api.myitmo.model.studyplan;

import lombok.Data;

/** Краткое описание образовательной программы и связанного учебного плана. */
@Data
public class StudyPlanProgram {

    /** Идентификатор учебного плана для {@code /api/eduPlanNew/study_plan/{plan_id}}. */
    private long planId;

    /** Идентификатор специализации; у программ без специализации равен {@code null}. */
    private Long specializationId;

    /** Название образовательной программы. */
    private String name;

    /** Признак программы, на которой пользователь учится сейчас. */
    private boolean isActive;
}
