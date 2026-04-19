package edu.uic.cs342.project3.json.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import edu.uic.cs342.project3.model.ChatEntry;

import java.io.IOException;
import java.time.LocalTime;

public class ChatEntryDeserializer extends StdDeserializer<ChatEntry> {
    public ChatEntryDeserializer() { super(ChatEntry.class); }

    @Override
    public ChatEntry deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonNode rootNode = parser.getCodec().readTree(parser);

        return new ChatEntry(rootNode.get("playerName").asText(),
                             rootNode.get("message").asText(),
                             LocalTime.parse(rootNode.get("timestamp").asText()));
    }
}
