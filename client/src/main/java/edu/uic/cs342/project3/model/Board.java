package edu.uic.cs342.project3.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import edu.uic.cs342.project3.json.deserializers.BoardDeserializer;
import edu.uic.cs342.project3.json.serializers.BoardSerializer;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize(using = BoardSerializer.class)
@JsonDeserialize(using = BoardDeserializer.class)
public class Board {
    // ── Fields ───────────────────────────────────────────────────────────────────────────────────────────────────────
    @JsonProperty("grid")
    public final Piece[][] grid;

    // ── Constructors ─────────────────────────────────────────────────────────────────────────────────────────────────
    public Board() { this.grid = new Piece[8][8]; }
}
