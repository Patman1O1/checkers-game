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
        this.serializePlayer(player, jsonGenerator);

        // Create the friends field
        jsonGenerator.writeArrayFieldStart("friends");
        List<Player> friends = player.getFriends();
        if (!friends.isEmpty()) {
            for (Player friend : player.getFriends()) {
                if (friend == null) {
                    continue;
                }
                this.serializePlayer(friend, jsonGenerator);
            }
        }
        jsonGenerator.writeEndArray();

        // Stop writing to the .json file
        jsonGenerator.writeEndObject();
    }
}
