package api.myitmo.model.schedule;

import api.myitmo.model.sport.TimeSlot;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Учебный временной слот с порядком отображения в расписании. */
@EqualsAndHashCode(callSuper = true)
@Data
public class ExtendedTimeSlot extends TimeSlot {
    /** Порядковый номер слота в учебном дне. */
    private int order;

}
