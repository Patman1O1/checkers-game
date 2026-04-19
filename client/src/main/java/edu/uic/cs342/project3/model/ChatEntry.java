package edu.uic.cs342.project3.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import edu.uic.cs342.project3.json.deserializers.ChatEntryDeserializer;
import edu.uic.cs342.project3.json.serializers.ChatEntrySerializer;

import java.time.LocalTime;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize(using = ChatEntrySerializer.class)
@JsonDeserialize(using = ChatEntryDeserializer.class)
public class ChatEntry {
    // ── Fields ───────────────────────────────────────────────────────────────────────────────────────────────────────
    @JsonProperty("playerName")
    private final String playerName;

    @JsonProperty("message")
    private final String message;

    @JsonProperty("timestamp")
    private final LocalTime timestamp;

    // ── Constructors ─────────────────────────────────────────────────────────────────────────────────────────────────
    public ChatEntry(String playerName, String message, LocalTime timestamp) throws NullPointerException {
        if (playerName == null) {
            throw new NullPointerException("playerName is null");
        }

        if (message == null) {
            throw new NullPointerException("message is null");
        }

        this.playerName = playerName;
        this.message = message;
        this.timestamp = timestamp;
    }

    // ── Getters ──────────────────────────────────────────────────────────────────────────────────────────────────────
    public String getPlayerName() { return this.playerName; }

    public String getMessage() { return this.message; }

    public LocalTime getTimestamp() { return this.timestamp; }
}
