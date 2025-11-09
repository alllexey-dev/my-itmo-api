package api.myitmo.model.recordbook;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class RecordBookEntry {

    private String name;

    @SerializedName("discipline_id")
    private long disciplineId;

    @SerializedName("est_id")
    private long estId;

    // private ? currentScore

    // private ? rate

    // private ? attempt

    @SerializedName("control_type")
    private String controlType;

    @SerializedName("control_type_id")
    private long controlTypeId;

    // private ? examDate

    @SerializedName("have_tree")
    private boolean haveTree;

    // private ? lmsLink

    // private ? teacher
}
