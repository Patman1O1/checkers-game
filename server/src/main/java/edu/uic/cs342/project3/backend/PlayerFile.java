package edu.uic.cs342.project3.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class PlayerFile extends File {
    // ── Fields ───────────────────────────────────────────────────────────────────────────────────────────────────────
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // ── Constructors ─────────────────────────────────────────────────────────────────────────────────────────────────
    public PlayerFile(String filename) throws NullPointerException {
        super(System.getProperty("user.dir"), String.format("players/%s", filename));
    }

    // ── Methods ──────────────────────────────────────────────────────────────────────────────────────────────────────
    public Player read() throws IOException {
        return PlayerFile.OBJECT_MAPPER.readValue(this, Player.class);
    }

    public void write(Player player) throws IOException, NullPointerException {
        if (player == null) {
            throw new NullPointerException("player is null");
        }
        PlayerFile.OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(this, player);
    }
}
