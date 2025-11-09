package api.myitmo.model.sport;

import api.myitmo.model.IdValuePair;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

@Data
public class SportFilters {

    @SerializedName("building_id")
    private List<IdValuePair> buildingId;

    @SerializedName("section_id")
    private List<IdValuePair> sectionId;

    @SerializedName("sport_type_id")
    private List<IdValuePair> sportTypeId;

    @SerializedName("teacher_isu")
    private List<IdValuePair> teacherIsu;
}
