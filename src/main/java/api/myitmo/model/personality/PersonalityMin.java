package api.myitmo.model.personality;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class PersonalityMin {

    private long id;

    private String fio;

    private String gender;

    private String phone;

    private String email;

    private String work;

    // private ? education

    @SerializedName("photo")
    private String photoUrl;
}
