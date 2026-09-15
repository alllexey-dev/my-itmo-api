package api.bars.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.Locale;

/**
 * Подтверждённый результат одной попытки. Оценка приходит строкой в формате БАРС:
 * {@code Отл., A}, {@code Хор., C}, {@code Удвл., E}, {@code Неуд., FX}, {@code Зачет}.
 */
@Data
public class Approval {

    private long id;

    @SerializedName("student_id")
    private long studentId;

    @SerializedName("student_login")
    private String studentLogin;

    @SerializedName("checkpoint_plan_id")
    private long checkpointPlanId;

    /** Номер попытки, начиная с 1; пересдача даёт новую запись с большим номером. */
    private int attempt;

    /** Сумма баллов на момент подтверждения. */
    @SerializedName("marks_sum")
    private Double marksSum;

    /** Оценка словами БАРС; см. {@link #getGradeCode()}. */
    @SerializedName("mark_string")
    private String markString;

    @SerializedName("is_active")
    private boolean active;

    /** Аннулированное подтверждение; наблюдалось у неудачной первой попытки. */
    @SerializedName("is_invalid")
    private boolean invalid;

    @SerializedName("is_absent")
    private boolean absent;

    @SerializedName("was_recalculated")
    private boolean recalculated;

    /** Подтверждение курсового проекта, а не дисциплины. */
    private boolean course;

    @SerializedName("created_at")
    private Long createdAt;

    @SerializedName("updated_at")
    private Long updatedAt;

    /**
     * Оценка в формате MyITMO: {@code 5/A}, {@code 4/C}, {@code 3/E}, {@code 2/FX}.
     * Зачёт, незачёт и незнакомые строки возвращаются как есть (без пробелов по краям).
     */
    public String getGradeCode() {
        if (markString == null) return null;
        String text = markString.trim();
        int comma = text.indexOf(',');
        if (comma < 0) return text;
        String word = text.substring(0, comma).trim().toLowerCase(Locale.ROOT).replace('ё', 'е');
        while (word.endsWith(".")) word = word.substring(0, word.length() - 1);
        String letter = text.substring(comma + 1).trim().toUpperCase(Locale.ROOT);
        if (!letter.matches("[A-FX]{1,2}")) return text;
        switch (word) {
            case "отл": return "5/" + letter;
            case "хор": return "4/" + letter;
            case "удвл":
            case "удовл": return "3/" + letter;
            case "неуд": return "2/" + letter;
            default: return text;
        }
    }
}
