package api.myitmo.model.sport;

import lombok.Data;

import java.util.List;

/** Баллы по физической культуре за выбранный спортивный семестр. */
@Data
public class SportScore {

    private Sum sum;

    private List<SportAttendance> attendances;

    /** Разбиение итоговой суммы по источнику начисления. */
    @Data
    public static class Sum {
        /** Баллы за посещения занятий. */
        private long attendances;

        /** Бонусные баллы и другие начисления. */
        private long other;
    }
}
