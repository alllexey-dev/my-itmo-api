package api.myitmo.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

@Data
public class Personality {

    private int isu;

    private String fio;

    private String gender;

    @SerializedName("photo")
    private String photoUrl;

    private List<Contact> contacts;

    private List<Room> rooms;

    private List<Position> positions;

    // private ? powers

    // private ? levels

    // private ? education

    // private ? activities

    @SerializedName("exchange_training")
    private boolean exchangeTraining;
}
