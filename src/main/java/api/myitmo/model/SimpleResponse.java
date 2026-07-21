package api.myitmo.model;

import lombok.Data;

/** Простая обёртка внешних сервисов, не использующих формат MyITMO. */
@Data
public class SimpleResponse<T> {

    private T response;
}
