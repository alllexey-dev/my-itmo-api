package api.myitmo.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class Position {

    @SerializedName("department_name")
    private String departmentName;

    @SerializedName("department_link")
    private String departmentLink;

    @SerializedName("position_name")
    private String positionName;

    // private ? vacation

    // private ? startVacation

    // private ? endVacation
}
