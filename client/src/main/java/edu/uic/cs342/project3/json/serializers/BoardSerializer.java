package edu.uic.cs342.project3.json.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import edu.uic.cs342.project3.model.Board;

import java.io.IOException;

public class BoardSerializer extends StdSerializer<Board> {
    // ── Constructors ─────────────────────────────────────────────────────────────────────────────────────────────────
    public BoardSerializer() { super(Board.class); }

    // ── Methods ──────────────────────────────────────────────────────────────────────────────────────────────────────
    @Override
    public void serialize(Board board, JsonGenerator generator, SerializerProvider provider) throws IOException {
        generator.writeStartObject();
        generator.writeArrayFieldStart("rows");

        for (int r = 0; r < 8; ++r) {
            generator.writeStartObject();
            generator.writeArrayFieldStart("columns");

            for (int c = 0; c < 8; ++c) {
                provider.defaultSerializeValue(board.grid[r][c], generator);
            }

            generator.writeEndArray();
            generator.writeEndObject();
        }
        generator.writeEndArray();
        generator.writeEndObject();
    }
}
