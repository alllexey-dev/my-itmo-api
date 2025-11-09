package api.myitmo.model.personality;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

@Data
public class Contact {

    private List<String> contact;

    @SerializedName("contact_alias")
    private String contactAlias;
}
