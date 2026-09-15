package api.bars.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

/** Серверные флаги прав в журнале; все наблюдались как boolean. */
@Data
public class StudentAccessibility {

    @SerializedName("can_edit_current_marks")
    private boolean canEditCurrentMarks;

    @SerializedName("can_edit_additional_marks")
    private boolean canEditAdditionalMarks;

    @SerializedName("can_edit_course_marks")
    private boolean canEditCourseMarks;

    @SerializedName("can_edit_final_marks")
    private boolean canEditFinalMarks;

    @SerializedName("can_approve_marks")
    private boolean canApproveMarks;

    @SerializedName("can_approve_retry_marks")
    private boolean canApproveRetryMarks;

    @SerializedName("has_unfilled_key_checkpoints")
    private boolean hasUnfilledKeyCheckpoints;

    @SerializedName("course_project_theme_not_approved")
    private boolean courseProjectThemeNotApproved;
}
