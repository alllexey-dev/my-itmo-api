package api.myitmo.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

@Data
public class Specialization {

    @SerializedName("main_plan")
    private int mainPlan;

    @SerializedName("specialization_name")
    private String specializationName;

    private List<Semester> semesters;
}
