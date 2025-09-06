package api.myitmo.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class RecordBookEntry {

    private String name;

    @SerializedName("discipline_id")
    private int disciplineId;

    @SerializedName("est_id")
    private int estId;

    // private ? currentScore

    // private ? rate

    // private ? attempt

    @SerializedName("control_type")
    private String controlType;

    @SerializedName("control_type_id")
    private int controlTypeId;

    // private ? examDate

    @SerializedName("have_tree")
    private boolean haveTree;

    // private ? lmsLink

    // private ? teacher
}
