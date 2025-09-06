package api.myitmo.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class Semester {

    @SerializedName("study_year")
    private String studyYear;

    private int semester;

    private int course;

    private boolean actual;
}
