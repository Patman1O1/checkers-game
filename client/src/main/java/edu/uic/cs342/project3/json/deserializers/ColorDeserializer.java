package edu.uic.cs342.project3.json.deserializers;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import edu.uic.cs342.project3.model.Color;

import java.io.IOException;

public class ColorDeserializer extends StdDeserializer<Color> {
    // ── Constructors ─────────────────────────────────────────────────────────────────────────────────────────────────
    public ColorDeserializer() { super(Color.class); }

    // ── Methods ──────────────────────────────────────────────────────────────────────────────────────────────────────
    @Override
    public Color deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonNode rootNode = parser.getCodec().readTree(parser);
        String colorName = parser.getCodec().treeToValue(rootNode.get("color"), String.class);

        switch (colorName) {
            case "red": {
                return Color.RED;
            }

            case "black": {
                return Color.BLACK;
            }

            default: {
                throw new JsonParseException(String.format("Invalid color name: %s", colorName));
            }
        }
    }
}
