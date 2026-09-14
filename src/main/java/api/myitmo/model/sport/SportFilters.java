package api.myitmo.model.sport;

import api.myitmo.model.IdValuePair;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

/**
 * Значения фильтров, а не полный каталог объектов расписания.
 * На 2026-09-09 building_id содержит -1 (Online), 0 (Другие объекты) и два корпуса.
 * Занятия на других площадках содержат собственные положительные building_id,
 * отсутствующие в этом списке; 0 нельзя использовать вместо реального ID площадки.
 */
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
