package edu.uic.cs342.project3.util;

import edu.uic.cs342.project3.models.Player;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PlayerRegistry {
    // ── Fields ───────────────────────────────────────────────────────────────────────────────────────────────────────
    private static final Logger LOGGER = Logger.getLogger(PlayerRegistry.class.getName());

    private static final PlayerRegistry INSTANCE = new PlayerRegistry();

    private final Path playersDir;

    private final ObjectMapper objectMapper;

    private final Map<String, Player> cache;

    private final Set<String> online;

    private final Map<String, String> challenges;

    // ── Constructors ─────────────────────────────────────────────────────────────────────────────────────────────────
    private PlayerRegistry() {
        this.playersDir = Paths.get(System.getProperty("user.dir"), "players");
        this.objectMapper = JsonUtil.getObjectMapper();
        this.cache = new ConcurrentHashMap<>();
        this.online = ConcurrentHashMap.newKeySet();
        this.challenges = new ConcurrentHashMap<>();
        try {
            Files.createDirectories(this.playersDir);
            this.loadAll();
        } catch (IOException exception) {
            PlayerRegistry.LOGGER.log(Level.SEVERE, String.format("Cannot create players directory: %s", exception.getMessage()), exception);
        }
    }

    // ── Getters ──────────────────────────────────────────────────────────────────────────────────────────────────────
    public static PlayerRegistry getInstance() { return PlayerRegistry.INSTANCE; }

    public synchronized Player get(String username) { return this.cache.get(username.toLowerCase()); }

    public synchronized boolean usernameExists(String username) { return this.cache.containsKey(username.toLowerCase()); }

    public synchronized List<Player> getAll() { return new ArrayList<>(this.cache.values()); }

    public List<String> getOnlineUsernames() {
        List<String> result = new ArrayList<>();
        for (String key : this.online) {
            Player player = this.cache.get(key);
            result.add(player != null ? player.getUsername() : key);
        }
        return result;
    }

    public String getPendingChallenger(String target) {
        String key = target.toLowerCase();
        for (Map.Entry<String, String> entry : this.challenges.entrySet()) {
            if (entry.getValue().equals(key)) {
                return entry.getKey();
            }
        }
        return null;
    }

    // ── Setters ──────────────────────────────────────────────────────────────────────────────────────────────────────
    public void setOnline(String username)  { this.online.add(username.toLowerCase()); }

    public void setOffline(String username) { this.online.remove(username.toLowerCase()); }

    // ── Methods ──────────────────────────────────────────────────────────────────────────────────────────────────────
    private void loadAll() throws IOException {
        File[] files = this.playersDir.toFile().listFiles((directory, file) -> file.endsWith(".json"));
        if (files == null) {
            return;
        }

        for (File file : files) {
            try {
                Player player = this.objectMapper.readValue(file, Player.class);
                if (player.getUsername() != null) {
                    this.cache.put(player.getUsername().toLowerCase(), player);
                }
            } catch (IOException exception) {
                PlayerRegistry.LOGGER.log(Level.WARNING, String.format("Skipping corrupt player file: %s", file.getName()));
            }
        }
        PlayerRegistry.LOGGER.log(Level.INFO, String.format("Loaded %d player(s) from disk.", this.cache.size()));
    }

    private boolean persist(Player player) {
        File file = this.playersDir.resolve(player.getUsername() + ".json").toFile();
        try {
            this.objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, player);
            return true;
        } catch (IOException exception) {
            PlayerRegistry.LOGGER.log(Level.SEVERE, String.format("Failed to persist player %s: %s", player.getUsername(), exception.getMessage()), exception);
            return false;
        }
    }

    public boolean isOnline(String username) { return this.online.contains(username.toLowerCase()); }

    public synchronized boolean save(Player player) {
        this.cache.put(player.getUsername().toLowerCase(), player);
        return this.persist(player);
    }

    public void sendChallenge(String challenger, String target) {
        this.challenges.put(challenger.toLowerCase(), target.toLowerCase());
    }

    public void removeChallenge(String challenger) { this.challenges.remove(challenger.toLowerCase()); }

    public void removeChallengeForTarget(String target) {
        String key = target.toLowerCase();
        this.challenges.entrySet().removeIf(e -> e.getValue().equals(key));
    }
}
