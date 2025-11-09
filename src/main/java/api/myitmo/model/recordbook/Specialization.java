package api.myitmo.model.recordbook;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

@Data
public class Specialization {

    @SerializedName("main_plan")
    private long mainPlan;

    @SerializedName("specialization_name")
    private String specializationName;

    private List<Semester> semesters;
}
