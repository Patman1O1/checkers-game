package edu.uic.cs342.project3.json.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import edu.uic.cs342.project3.model.ChatEntry;

import java.io.IOException;

public class ChatEntrySerializer extends StdSerializer<ChatEntry> {
    // ── Constructors ─────────────────────────────────────────────────────────────────────────────────────────────────
    public ChatEntrySerializer() { super(ChatEntry.class); }

    // ── Methods ──────────────────────────────────────────────────────────────────────────────────────────────────────
    @Override
    public void serialize(ChatEntry entry, JsonGenerator generator, SerializerProvider provider) throws IOException {
        generator.writeStartObject();

        generator.writeStringField("timestamp", entry.getTimestamp().toString());
        generator.writeStringField("playerName", entry.getPlayerName());
        generator.writeStringField("message", entry.getMessage());

        generator.writeEndObject();
    }
}
