package api.myitmo.adapters;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.OffsetDateTime;

public class OffsetDateTimeAdapter extends TypeAdapter<OffsetDateTime> {
    @Override
    public void write(JsonWriter jsonWriter, OffsetDateTime offsetDateTime) throws IOException {
        jsonWriter.value(offsetDateTime.toString());
    }

    @Override
    public OffsetDateTime read(JsonReader in) throws IOException {
        if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
            in.nextNull();
            return null;
        }

        String dateString = in.nextString();
        return OffsetDateTime.parse(dateString);
    }
}
