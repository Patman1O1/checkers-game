package edu.uic.cs342.project3.json.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import edu.uic.cs342.project3.model.Player;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PlayerSerializer extends StdSerializer<Player> {
    // ── Constructors ─────────────────────────────────────────────────────────────────────────────────────────────────
    public PlayerSerializer() { super(Player.class); }

    // ── Methods ──────────────────────────────────────────────────────────────────────────────────────────────────────
    @Override
    public void serialize(Player player, JsonGenerator generator, SerializerProvider provider) throws IOException {
        generator.writeStartObject();

        generator.writeStringField("username", player.getUsername());
        generator.writeStringField("password", player.getPassword());

        provider.defaultSerializeField("color", player.getColor(), generator);

        generator.writeNumberField("wins", player.getWins());
        generator.writeNumberField("losses", player.getLosses());
        generator.writeNumberField("draws", player.getDraws());

        generator.writeStringField("status", player.getStatus().toString());

        List<String> friends = player.getFriends();
        generator.writeArrayFieldStart("friends");
        generator.writeArray(friends.toArray(new String[0]), friends.size(), friends.size());
        generator.writeEndArray();

        generator.writeEndObject();
    }
}
