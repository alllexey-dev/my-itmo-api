package api.bars.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

/** План контрольных точек дисциплины ({@code headers.plan} журнала, {@code GET checkpoint_plans/{id}}). */
@Data
public class CheckpointPlan {

    /** Идентификатор плана; не совпадает с {@code est_id} MyITMO. */
    private long id;

    private String gid;

    /** Учебный год плана в формате {@code 2025/2026}. */
    private String year;

    /** Сквозные номера семестров. */
    private List<Integer> terms;

    private PlanDiscipline discipline;

    @SerializedName("regular_checkpoints")
    private List<Checkpoint> regularCheckpoints;

    /** Итоговая точка (экзамен, зачёт, диф. зачёт); может отсутствовать. */
    @SerializedName("final_checkpoint")
    private Checkpoint finalCheckpoint;

    @SerializedName("point_distribution")
    private int pointDistribution;

    @SerializedName("additional_points")
    private boolean additionalPoints;

    /** У планов с курсовым проектом структура {@code course_project_checkpoint} не наблюдалась. */
    @SerializedName("has_course_project")
    private boolean courseProject;

    // TODO: programs и components наблюдались только пустыми; alternate_methods и course_project_checkpoint только null;
    // status наблюдался как null и как string, для вывода статуса сдачи не подходит.
}
