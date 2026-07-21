package api.myitmo.model.recordbook;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class ControlEntry {

    private long id;

    @SerializedName("control_name")
    private String controlName;

    @SerializedName("parent_id")
    private Long parentId;

    @SerializedName("lower_value")
    private Double lowerValue;

    @SerializedName("max_value")
    private Double maxValue;

    @SerializedName("min_value")
    private Double minValue;

    private boolean required;

    private Double rate;

    private OffsetDateTime date;

    private RecordBookTeacher teacher;
}
