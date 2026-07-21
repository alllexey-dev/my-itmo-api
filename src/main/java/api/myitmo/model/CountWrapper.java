package api.myitmo.model;

import lombok.Data;

/** Страница результатов с общим количеством найденных элементов. */
@Data
public class CountWrapper<T> {

    private int count;

    private T data;
}
