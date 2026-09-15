package api.bars.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

/** Поток или группа ({@code GET journal/groups-and-flows}); пара {@code type}+{@code identifier} адресует журнал. */
@Data
public class GroupOrFlow {

    /** Наблюдалось только {@code flow}. */
    private String type;

    private String name;

    /** Строка, даже если выглядит как число; передаётся в URL журнала как есть. */
    private String identifier;

    @SerializedName("checkpoint_plan_ids")
    private List<Long> checkpointPlanIds;
}
