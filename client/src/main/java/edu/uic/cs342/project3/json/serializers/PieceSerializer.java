package edu.uic.cs342.project3.json.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import edu.uic.cs342.project3.model.Piece;

import java.io.IOException;

public class PieceSerializer extends StdSerializer<Piece> {
    // ── Constructors ─────────────────────────────────────────────────────────────────────────────────────────────────
    public PieceSerializer() { super(Piece.class); }

    // ── Methods ──────────────────────────────────────────────────────────────────────────────────────────────────────
    @Override
    public void serialize(Piece piece, JsonGenerator generator, SerializerProvider provider) throws IOException {
        generator.writeStartObject();

        // Serialize the color
        provider.defaultSerializeField("color", piece.getColor(), generator);

        // Serialize the type
        generator.writeStringField("type", piece.getType().toString());

        generator.writeEndObject();
    }
}
