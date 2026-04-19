package edu.uic.cs342.project3.json.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import edu.uic.cs342.project3.model.Color;

import java.io.IOException;

public class ColorSerializer extends StdSerializer<Color> {
    // ── Constructors ─────────────────────────────────────────────────────────────────────────────────────────────────
    public ColorSerializer() { super(Color.class); }

    // ── Methods ──────────────────────────────────────────────────────────────────────────────────────────────────────
    @Override
    public void serialize(Color color, JsonGenerator generator, SerializerProvider provider) throws IOException {
        generator.writeStartObject();
        generator.writeStringField("color", color.toString());
        generator.writeEndObject();
    }
}
