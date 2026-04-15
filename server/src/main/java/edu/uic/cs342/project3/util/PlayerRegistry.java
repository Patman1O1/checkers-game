package edu.uic.cs342.project3.util;

import edu.uic.cs342.project3.model.Player;
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

/**
 * Single source of truth for all player data.
 * Merges persistent JSON-backed player records with in-memory online/offline tracking.
 * All public methods are thread-safe.
 */
public class PlayerRegistry {

    // ── Fields ────────────────────────────────────────────────────────────────

    private static final Logger              LOG      = Logger.getLogger(PlayerRegistry.class.getName());
    private static final PlayerRegistry      INSTANCE = new PlayerRegistry();

    private final Path                 playersDir;
    private final ObjectMapper         mapper = JsonUtil.getMapper();
    private final Map<String, Player>  cache  = new ConcurrentHashMap<>();
    private final Set<String>          online = ConcurrentHashMap.newKeySet();

    // ── Constructors ──────────────────────────────────────────────────────────

    private PlayerRegistry() {
        playersDir = Paths.get(System.getProperty("user.dir"), "players");
        try {
            Files.createDirectories(playersDir);
            loadAll();
        } catch (IOException e) {
            LOG.severe("Cannot create players directory: " + e.getMessage());
        }
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public static PlayerRegistry getInstance() { return INSTANCE; }

    public boolean isOnline(String username) {
        return online.contains(username.toLowerCase());
    }

    public synchronized Player get(String username) {
        return cache.get(username.toLowerCase());
    }

    public synchronized boolean usernameExists(String username) {
        return cache.containsKey(username.toLowerCase());
    }

    public synchronized List<Player> getAll() {
        return new ArrayList<>(cache.values());
    }

    /** Returns online usernames in their original (registered) casing. */
    public List<String> getOnlineUsernames() {
        List<String> result = new ArrayList<>();
        for (String key : online) {
            Player p = cache.get(key);
            result.add(p != null ? p.getUsername() : key);
        }
        return result;
    }

    // ── Methods ───────────────────────────────────────────────────────────────

    public void setOnline(String username)  { online.add(username.toLowerCase());    }
    public void setOffline(String username) { online.remove(username.toLowerCase()); }

    public synchronized boolean save(Player player) {
        cache.put(player.getUsername().toLowerCase(), player);
        return persist(player);
    }

    private void loadAll() throws IOException {
        File   dir   = playersDir.toFile();
        File[] files = dir.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null) return;
        for (File f : files) {
            try {
                Player p = mapper.readValue(f, Player.class);
                if (p.getUsername() != null)
                    cache.put(p.getUsername().toLowerCase(), p);
            } catch (Exception e) {
                LOG.warning("Skipping corrupt player file: " + f.getName());
            }
        }
        LOG.info("Loaded " + cache.size() + " player(s) from disk.");
    }

    private boolean persist(Player player) {
        File file = playersDir.resolve(player.getUsername() + ".json").toFile();
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, player);
            return true;
        } catch (IOException e) {
            LOG.severe("Failed to persist player " + player.getUsername() + ": " + e.getMessage());
            return false;
        }
    }
}
