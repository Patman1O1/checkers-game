package edu.uic.cs342.project3.json.deserializers;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import com.fasterxml.jackson.databind.node.ArrayNode;
import edu.uic.cs342.project3.model.Color;
import edu.uic.cs342.project3.model.Player;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PlayerDeserializer extends StdDeserializer<Player> {
    // ── Constructors ─────────────────────────────────────────────────────────────────────────────────────────────────
    public PlayerDeserializer() { super(Player.class); }

    // ── Methods ──────────────────────────────────────────────────────────────────────────────────────────────────────
    @Override
    public Player deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonNode rootNode = parser.getCodec().readTree(parser);

        String username = rootNode.get("username").asText();
        String password = rootNode.get("password").asText();

        Color color = parser.getCodec().treeToValue(rootNode.get("color"), Color.class);

        int wins = rootNode.get("wins").asInt();
        int losses = rootNode.get("losses").asInt();
        int draws = rootNode.get("draws").asInt();

        String statusValue = rootNode.get("status").asText();

        List<String> friends = new ArrayList<>();
        ArrayNode arrayNode = (ArrayNode) rootNode.get("friends");
        if (arrayNode != null) {
            for (JsonNode jsonNode : arrayNode) {
                friends.add(parser.getCodec().treeToValue(jsonNode, String.class));
            }
        }

        switch (statusValue) {
            case "online": {
                return new Player(
                        username,
                        password,
                        color,
                        wins,
                        losses,
                        draws,
                        Player.Status.ONLINE,
                        friends
                );
            }

            case "offline": {
                return new Player(
                        username,
                        password,
                        color,
                        wins,
                        losses,
                        draws,
                        Player.Status.OFFLINE,
                        friends
                );
            }

            default: {
                throw new JsonParseException(parser, String.format("Invalid status: %s", statusValue));
            }
        }
    }
}
