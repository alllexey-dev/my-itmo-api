package api.myitmo.model.recordbook;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class RecordBookEntry {

    private String name;

    @SerializedName("discipline_id")
    private long disciplineId;

    @SerializedName("est_id")
    private long estId;

    @SerializedName("current_score")
    private Double currentScore;

    private String rate;

    private int attempt;

    @SerializedName("control_type")
    private String controlType;

    @SerializedName("control_type_id")
    private long controlTypeId;

    @SerializedName("exam_date")
    private OffsetDateTime examDate;

    @SerializedName("have_tree")
    private boolean haveTree;

    @SerializedName("lms_link")
    private String lmsLink;

    private RecordBookTeacher teacher;
}
