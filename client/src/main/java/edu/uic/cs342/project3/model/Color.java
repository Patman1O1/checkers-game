package edu.uic.cs342.project3.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import edu.uic.cs342.project3.json.deserializers.ColorDeserializer;
import edu.uic.cs342.project3.json.serializers.ColorSerializer;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize(using = ColorSerializer.class)
@JsonDeserialize(using = ColorDeserializer.class)
public enum Color {
    // ── Constants ────────────────────────────────────────────────────────────────────────────────────────────────────
    RED("red"), BLACK("black");

    // ── Fields ───────────────────────────────────────────────────────────────────────────────────────────────────────
    @JsonProperty("name")
    private final String name;

    // ── Constructors ─────────────────────────────────────────────────────────────────────────────────────────────────
    private Color(String name) { this.name = name; }

    // ── Methods ──────────────────────────────────────────────────────────────────────────────────────────────────────
    @Override
    public String toString() { return this.name; }
}
