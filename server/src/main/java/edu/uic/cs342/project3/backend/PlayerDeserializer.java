package edu.uic.cs342.project3.backend;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PlayerDeserializer extends JsonDeserializer<Player> {
    @Override
    public Player deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException, NullPointerException {
        if (jsonParser == null) {
            throw new NullPointerException("jsonParser is null");
        }

        if (deserializationContext == null) {
            throw new NullPointerException("deserializationContext is null");
        }

        JsonNode jsonNode = jsonParser.getCodec().readTree(jsonParser);
        ArrayNode arrayNode;
        ObjectMapper objectMapper = (ObjectMapper) jsonParser.getCodec();

        // Read the player's username
        String username = jsonNode.get("username").asText();

        // Read the player's password
        String password = jsonNode.get("password").asText();

        // Read the player's wins
        int wins = jsonNode.get("wins").asInt();

        // Read the player's losses
        int losses = jsonNode.get("losses").asInt();

        // Read the player's draws
        int draws = jsonNode.get("draws").asInt();

        // Read the player's friends
        List<Player> friends = new ArrayList<>();
        arrayNode = (ArrayNode) jsonNode.get("items");
        if (arrayNode != null) {
            for (JsonNode itemNode : arrayNode) {
                friends.add(objectMapper.treeToValue(itemNode, Player.class));
            }
        }

        // Create and return a new instance of Player with the deserialized fields
        return new Player(username, password, wins, losses, draws, Player.Status.ONLINE, friends);
    }
}
