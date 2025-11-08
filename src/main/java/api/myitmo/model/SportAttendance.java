package api.myitmo.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class SportAttendance {

    private String type;

    private String name;

    @SerializedName("evaluation_id")
    private long evaluationId;

    @SerializedName("evaluation_name")
    private String evaluationName;

    @SerializedName("section_level")
    private int sectionLevel;

    private int score;

    private OffsetDateTime date;

    private boolean isCompetition;
}
