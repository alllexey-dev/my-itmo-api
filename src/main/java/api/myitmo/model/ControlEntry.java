package api.myitmo.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class ControlEntry {

    private long id;

    @SerializedName("control_name")
    private String controlName;

    // private ? parentId

    @SerializedName("lower_value")
    private int lowerValue;

    @SerializedName("max_value")
    private int maxValue;

    @SerializedName("min_value")
    private int minValue;

    private boolean required;

    // private ? rate

    // private ? date

    // private ? teacher
}
