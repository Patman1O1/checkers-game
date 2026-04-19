package edu.uic.cs342.util;

import edu.uic.cs342.model.Player;
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
import java.util.logging.Logger;

public class PlayerRegistry {

    // ── Fields ────────────────────────────────────────────────────────────────

    private static final Logger         LOG      = Logger.getLogger(PlayerRegistry.class.getName());
    private static final PlayerRegistry INSTANCE = new PlayerRegistry();

    private final Path                    playersDir;
    private final ObjectMapper            mapper     = JsonUtil.getMapper();
    private final Map<String, Player>     cache      = new ConcurrentHashMap<>();
    private final Set<String>             online     = ConcurrentHashMap.newKeySet();
    private final Map<String, String>     challenges = new ConcurrentHashMap<>();

    // ── Constructors ──────────────────────────────────────────────────────────

    private PlayerRegistry() {
        this.playersDir = Paths.get(System.getProperty("user.dir"), "players");
        try {
            Files.createDirectories(this.playersDir);
            this.loadAll();
        } catch (IOException e) {
            PlayerRegistry.LOG.severe("Cannot create players directory: " + e.getMessage());
        }
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public static PlayerRegistry getInstance() { return PlayerRegistry.INSTANCE; }

    public boolean isOnline(String username) {
        return this.online.contains(username.toLowerCase());
    }

    public synchronized Player get(String username) {
        return this.cache.get(username.toLowerCase());
    }

    public synchronized boolean usernameExists(String username) {
        return this.cache.containsKey(username.toLowerCase());
    }

    public synchronized List<Player> getAll() {
        return new ArrayList<>(this.cache.values());
    }

    public List<String> getOnlineUsernames() {
        List<String> result = new ArrayList<>();
        for (String key : this.online) {
            Player p = this.cache.get(key);
            result.add(p != null ? p.getUsername() : key);
        }
        return result;
    }

    public String getPendingChallenger(String target) {
        String key = target.toLowerCase();
        for (Map.Entry<String, String> e : this.challenges.entrySet()) {
            if (e.getValue().equals(key)) return e.getKey();
        }
        return null;
    }

    // ── Methods ───────────────────────────────────────────────────────────────

    public void setOnline(String username)  { this.online.add(username.toLowerCase());    }
    public void setOffline(String username) { this.online.remove(username.toLowerCase()); }

    public synchronized boolean save(Player player) {
        this.cache.put(player.getUsername().toLowerCase(), player);
        return this.persist(player);
    }

    public void sendChallenge(String challenger, String target) {
        this.challenges.put(challenger.toLowerCase(), target.toLowerCase());
    }

    public void removeChallenge(String challenger) {
        this.challenges.remove(challenger.toLowerCase());
    }

    public void removeChallengeForTarget(String target) {
        String key = target.toLowerCase();
        this.challenges.entrySet().removeIf(e -> e.getValue().equals(key));
    }

    private void loadAll() throws IOException {
        File   dir   = this.playersDir.toFile();
        File[] files = dir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try {
                Player p = this.mapper.readValue(f, Player.class);
                if (p.getUsername() != null)
                    this.cache.put(p.getUsername().toLowerCase(), p);
            } catch (Exception e) {
                PlayerRegistry.LOG.warning("Skipping corrupt player file: " + f.getName());
            }
        }
        PlayerRegistry.LOG.info("Loaded " + this.cache.size() + " player(s) from disk.");
    }

    private boolean persist(Player player) {
        File file = this.playersDir.resolve(player.getUsername() + ".json").toFile();
        try {
            this.mapper.writerWithDefaultPrettyPrinter().writeValue(file, player);
            return true;
        } catch (IOException e) {
            PlayerRegistry.LOG.severe("Failed to persist player " + player.getUsername() + ": " + e.getMessage());
            return false;
        }
    }
}
