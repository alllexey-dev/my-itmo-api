package api.bars.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

/** Дисциплина внутри плана. */
@Data
public class PlanDiscipline {

    private long id;

    private String name;

    @SerializedName("course_project")
    private boolean courseProject;

    // TODO: term наблюдался только как null.
}
