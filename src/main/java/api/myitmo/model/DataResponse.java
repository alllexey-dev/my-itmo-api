package api.myitmo.model;

import lombok.Data;

/** Обёртка старых сервисов расписания. */
@Data
public class DataResponse<T> {

    /** {@code 0} при успехе. */
    private int code;

    /** Полезная нагрузка. */
    private T data;

    /** Сообщение сервера; обычно {@code null} при успехе. */
    private String message;
}
