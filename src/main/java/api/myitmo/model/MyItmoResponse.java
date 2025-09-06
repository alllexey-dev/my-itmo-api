package api.myitmo.model;

import lombok.Data;

@Data
public class MyItmoResponse<T> {

    private int code;

    private T data;

    private String message;
}
