package api.myitmo.model.sport;

import lombok.Data;

/** Краткий элемент списка спортивных семестров. */
@Data
public class SportSemesterOption {

    private long id;

    /** Отображаемая подпись, например {@code Весна 2025/2026}. */
    private String value;

    /** Необязательный комментарий сервера. */
    private String comment;
}
