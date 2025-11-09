package api.myitmo.model.schedule;

import api.myitmo.model.sport.TimeSlot;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ExtendedTimeSlot extends TimeSlot {
    private int order;

}
