package api.myitmo.model.other;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class QrData {

    @SerializedName("qr_hex")
    private String qrHex;
}
