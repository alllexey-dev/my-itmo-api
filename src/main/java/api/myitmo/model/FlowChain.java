package api.myitmo.model;

import lombok.Data;

import java.util.List;

@Data
public class FlowChain {

    private int disciplineId;

    private String discName;

    private List<Flow> flows;
}
