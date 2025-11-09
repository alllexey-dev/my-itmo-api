package api.myitmo.model.recordbook;

import lombok.Data;

import java.util.List;

@Data
public class FlowChainsWrapper {

    // IDK what is this
    private int selectedBy;

    private List<FlowChain> flowChains;
}
