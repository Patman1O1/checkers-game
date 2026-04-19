package edu.uic.cs342.project3.json.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;

import edu.uic.cs342.project3.model.Board;
import edu.uic.cs342.project3.model.Piece;

import java.io.IOException;

public class BoardDeserializer extends StdDeserializer<Board> {
    // ── Constructors ─────────────────────────────────────────────────────────────────────────────────────────────────
    public BoardDeserializer() { super(Board.class); }

    // ── Methods ──────────────────────────────────────────────────────────────────────────────────────────────────────
    @Override
    public Board deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonNode root = parser.getCodec().readTree(parser);
        ArrayNode rowsNode = (ArrayNode) root.get("rows");

        Board board = new Board();
        for (int r = 0; r < 8; ++r) {
            ArrayNode columnsNode = (ArrayNode) rowsNode.get(r).get("columns");
            for (int c = 0; c < 8; ++c) {
                board.grid[r][c] = parser.getCodec().treeToValue(columnsNode.get(c), Piece.class);
            }
        }
        return board;
    }
}
