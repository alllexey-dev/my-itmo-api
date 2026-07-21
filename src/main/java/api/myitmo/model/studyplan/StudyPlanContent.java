package api.myitmo.model.studyplan;

import lombok.Data;

import java.util.List;

/** Нагрузка дисциплины в конкретном семестре. */
@Data
public class StudyPlanContent {

    private long id;

    private long moduleId;

    private long disciplineId;

    /** Порядок элемента внутри модуля. */
    private int order;

    private int semester;

    /** Трудоёмкость в зачётных единицах. */
    private int creditPoints;

    /** Аудиторные, самостоятельные и контрольные виды работ. */
    private List<StudyPlanActivity> activities;
}
