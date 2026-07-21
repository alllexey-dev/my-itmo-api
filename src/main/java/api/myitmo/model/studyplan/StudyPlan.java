package api.myitmo.model.studyplan;

import lombok.Data;

import java.util.List;

/** Полная структура учебного плана пользователя. */
@Data
public class StudyPlan {

    private long id;

    /** Номер текущего семестра в учебном плане. */
    private int currentSemester;

    /** Внутренний идентификатор текущего семестра. */
    private long currentSemesterId;

    /** Общее количество семестров в плане. */
    private int semestersCount;

    private StudyPlanInfo planInfo;

    /** Плоский справочник семестров. */
    private List<StudyPlanSemester> semesters;

    /** Рекурсивная структура блоков, модулей и дисциплин. */
    private List<StudyPlanNode> structure;
}
