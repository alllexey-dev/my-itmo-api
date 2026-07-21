package api.myitmo.model.requests;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.time.OffsetDateTime;

/** Краткая информация о пользовательской заявке. */
@Data
public class RequestSummary {

    private long id;

    private String name;

    /** Дополнительное уведомление или пояснение к заявке. */
    private String notice;

    /** Числовой статус; значения зависят от типа заявки и могут расширяться сервером. */
    private int status;

    /** Локализованное название статуса, которое следует показывать пользователю. */
    @SerializedName("status_name")
    private String statusName;

    /** Момент создания заявки. */
    @SerializedName("created_at")
    private OffsetDateTime createdAt;

    /** Момент последнего изменения заявки. */
    @SerializedName("updated_at")
    private OffsetDateTime updatedAt;
}
