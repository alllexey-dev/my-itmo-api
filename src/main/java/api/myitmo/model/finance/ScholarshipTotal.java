package api.myitmo.model.finance;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

/** Сумма начислений одной финансовой категории. */
@Data
public class ScholarshipTotal {

    @SerializedName("category_id")
    private long categoryId;

    /** Название категории; сервер может возвращать служебные пробелы и переводы строки по краям. */
    @SerializedName("category_name")
    private String categoryName;

    /** Сумма в рублях, а не в копейках. */
    private long sum;
}
