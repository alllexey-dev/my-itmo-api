package api.myitmo.model.studyplan;

import lombok.Data;

import java.util.List;

/** Набор учебных планов, доступных текущему пользователю. */
@Data
public class StudyPlanPrograms {

    /** Номер ИСУ пользователя, для которого сформирован ответ. */
    private long isu;

    /** Образовательные программы пользователя. */
    private List<StudyPlanProgram> programs;
}
