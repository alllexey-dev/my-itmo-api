package api.myitmo.model.election;

import lombok.Data;

/** Актуальная вместимость потока. */
@Data
public class FlowLimit {

    private long limitMax;

    private long occupied;

    private long free;
}
