package api.myitmo.model.election;

import lombok.Data;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Data
public class ChangeResult {

    @Nullable
    private Long status;

    @Nullable
    private String name;

    private List<Long> flows;
}
