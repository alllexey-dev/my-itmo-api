package api.myitmo.model;

import lombok.Data;

@Data
public class CountWrapper<T> {

    private int count;

    private T data;
}
