package edu.uic.cs342.project3.backend;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.List;

public class PlayerSerializer extends JsonSerializer<Player> {
    // ── Constructors ─────────────────────────────────────────────────────────────────────────────────────────────────
    public PlayerSerializer() {}

    // ── Methods ──────────────────────────────────────────────────────────────────────────────────────────────────────
    private void serializePlayer(Player player, JsonGenerator jsonGenerator) throws IOException {
        // Begin writing to the .json file
        jsonGenerator.writeStartObject();

        // Create the username field
        jsonGenerator.writeStringField("username", player.getUsername());

        // Create the password field
        jsonGenerator.writeStringField("password", player.getPassword());

        // Create the wins field
        jsonGenerator.writeNumberField("wins", player.getWins());

        // Create the losses field
        jsonGenerator.writeNumberField("losses", player.getLosses());

        // Create the draws field
        jsonGenerator.writeNumberField("draws", player.getDraws());
    }

    @Override
    public void serialize(Player player, JsonGenerator jsonGenerator, SerializerProvider serializers)
            throws IOException, NullPointerException {
        // Begin writing to the .json file
        jsonGenerator.writeStartObject();

        // Create the username field
        jsonGenerator.writeStringField("username", player.getUsername());

        // Create the password field
        jsonGenerator.writeStringField("password", player.getPassword());

        // Create the wins field
        jsonGenerator.writeNumberField("wins", player.getWins());

        // Create the losses field
        jsonGenerator.writeNumberField("losses", player.getLosses());

        // Create the draws field
        jsonGenerator.writeNumberField("draws", player.getDraws());

        // Create the friends field
        List<String> friends = player.getFriends();
        jsonGenerator.writeArrayFieldStart("friends");
        if (!friends.isEmpty()) {
            for (String friendName : player.getFriends()) {
                if (friendName == null) {
                    jsonGenerator.writeNull();
                    continue;
                }
                jsonGenerator.writeString(friendName);
            }
        }
        jsonGenerator.writeEndArray();

        // Stop writing to the .json file
        jsonGenerator.writeEndObject();
    }
}
