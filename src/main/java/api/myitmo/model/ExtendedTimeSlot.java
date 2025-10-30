package api.myitmo.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ExtendedTimeSlot extends TimeSlot {
    private int order;

}
