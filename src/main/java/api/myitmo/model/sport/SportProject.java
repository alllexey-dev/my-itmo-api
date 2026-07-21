package api.myitmo.model.sport;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

/** Специальный формат получения спортивного зачёта. */
@Data
public class SportProject {

    private long id;

    private String name;

    private String description;

    /** Записан ли пользователь на проект. */
    private boolean signed;

    /** Общий лимит участников. */
    private int limit;

    /** Количество доступных мест по представлению сервера. */
    private int available;

    /** Ссылка на правила или инструкцию. */
    @SerializedName("instruction_link")
    private String instructionLink;

    /** Текст подтверждения ознакомления с правилами. */
    @SerializedName("instruction_description")
    private String instructionDescription;

    /** Выполнены ли предварительные требования для записи. */
    @SerializedName("requisite_available")
    private boolean requisiteAvailable;

    /** Пользовательская ссылка на подтверждающий документ; может отсутствовать. */
    private String link;
}
