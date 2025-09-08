package api.myitmo.model;

import lombok.Data;

@Data
public class SimpleResponse<T> {

    private T response;
}
