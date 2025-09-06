package api.myitmo.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

@Data
public class Lesson {

    @SerializedName("pair_id")
    private long pairId;

    @Nullable
    private String subject;

    @SerializedName("subject_id")
    private long subjectId;

    @Nullable
    private String note;

    private String type;

    @SerializedName("time_start")
    private String timeStart;

    @SerializedName("time_end")
    private String timeEnd;

    @Nullable
    @SerializedName("teacher_id")
    private Long teacherId;

    @Nullable
    @SerializedName("teacher_name")
    private String teacherName;

    @Nullable
    private String room;

    @Nullable
    private String building;

    private String format;

    @SerializedName("work_type")
    private String workType;

    /**
     * 1 - Lectures
     * 2 - Laboratory classes
     * 3 - Practical classes
     */
    @SerializedName("work_type_id")
    private int workTypeId;

    private String group;

    @SerializedName("flow_type_id")
    private int flowTypeId;

    @SerializedName("flow_id")
    private int flowId;

    @Nullable
    @SerializedName("zoom_url")
    private String zoomUrl;

    @Nullable
    @SerializedName("zoom_password")
    private String zoomPassword;

    @Nullable
    @SerializedName("zoom_info")
    private String zoomInfo;

    @Nullable
    @SerializedName("bld_id")
    private Integer bldId;

    @SerializedName("format_id")
    private int formatId;

    @Nullable
    @SerializedName("main_bld_id")
    private Integer mainBldId;

}
