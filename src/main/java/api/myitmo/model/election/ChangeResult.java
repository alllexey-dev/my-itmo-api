package api.myitmo.model.election;

import lombok.Data;

import java.util.List;

@Data
public class ChangeResult {

    private Long status;

    private String name;

    private List<Long> flows;
}
