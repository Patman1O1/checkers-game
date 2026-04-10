package edu.uic.cs342.project3.backend;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PlayerDeserializer extends StdDeserializer<Player> {
    public PlayerDeserializer() { super(Player.class); }

    @Override
    public Player deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException, NullPointerException {
        if (jsonParser == null) {
            throw new NullPointerException("jsonParser is null");
        }

        if (deserializationContext == null) {
            throw new NullPointerException("deserializationContext is null");
        }

        JsonNode rootNode = jsonParser.getCodec().readTree(jsonParser);

        String username = rootNode.get("username").asText();

        String password = rootNode.get("password").asText();

        int wins = rootNode.get("wins").asInt();

        int losses = rootNode.get("losses").asInt();

        int draws = rootNode.get("draws").asInt();

        List<String> friends = new ArrayList<>();
        JsonNode friendsNode = rootNode.get("friends");
        if (!friendsNode.isEmpty()) {
            for (JsonNode friendNode : friendsNode) {
                friends.add(friendNode.asText());
            }
        }

        return new Player(username, password, wins, losses, draws, Player.Status.OFFLINE, friends);
    }
}
