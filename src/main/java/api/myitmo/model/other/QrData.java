package api.myitmo.model.other;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

/** Данные цифрового пропуска, полученные от {@code qr.itmo.su}. */
@Data
public class QrData {

    /** QR-код в шестнадцатеричном представлении. */
    @SerializedName("qr_hex")
    private String qrHex;
}
