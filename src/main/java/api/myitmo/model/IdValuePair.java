package api.myitmo.model;

import lombok.Data;

/** Универсальная пара идентификатора и отображаемого значения справочника. */
@Data
public class IdValuePair {

    private long id;

    private String value;
}
