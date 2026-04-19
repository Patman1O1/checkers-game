package edu.uic.cs342.project3.json.deserializers;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import edu.uic.cs342.project3.model.Piece;
import edu.uic.cs342.project3.model.Color;

import java.io.IOException;

public class PieceDeserializer extends StdDeserializer<Piece> {
    // ── Constructors ─────────────────────────────────────────────────────────────────────────────────────────────────
    public PieceDeserializer() { super(Piece.class); }

    // ── Methods ──────────────────────────────────────────────────────────────────────────────────────────────────────
    @Override
    public Piece deserialize(JsonParser parser, DeserializationContext context)
            throws IOException, NullPointerException {
        JsonNode rootNode = parser.getCodec().readTree(parser);

        Color color = parser.getCodec().treeToValue(rootNode.get("color"), Color.class);

        String typeName = rootNode.get("type").asText();
        switch (typeName) {
            case "regular": {
                return new Piece(color, Piece.Type.REGULAR);
            }

            case "king": {
                return new Piece(color, Piece.Type.KING);
            }

            default: {
                throw new JsonParseException(parser, String.format("Invalid type: %s", typeName));
            }
        }
    }
}
