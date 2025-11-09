package api.myitmo.model.sport;

import lombok.Data;

import java.util.List;

@Data
public class SportScore {

    private Sum sum;

    private List<SportAttendance> attendances;

    @Data
    public static class Sum {
        private long attendances;

        private long other;
    }
}
