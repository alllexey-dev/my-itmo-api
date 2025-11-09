package api.myitmo.model.sport;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

@Data
public class ChosenSportSection {

    private long id;

    @SerializedName("section_name")
    private String sectionName;

    private int level;

    @SerializedName("lesson_groups")
    private List<SportLessonGroup> lessonGroups;
}
