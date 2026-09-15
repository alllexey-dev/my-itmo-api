package api.bars.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

/** Дисциплина каталога журнала ({@code GET journal/disciplines}) в контексте выбранного периода. */
@Data
public class Discipline {

    /** Идентификатор дисциплины БАРС; не совпадает с {@code discipline_id} MyITMO. */
    private long id;

    private String name;

    /** Сквозные номера семестров, в которых план применим, например {@code [2, 4, 6]}. */
    private List<Integer> terms;

    /** Планы контрольных точек дисциплины; журнал читается по плану, а не по дисциплине. */
    @SerializedName("checkpoint_plan_ids")
    private List<Long> checkpointPlanIds;
}
