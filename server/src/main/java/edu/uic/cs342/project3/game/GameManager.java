package edu.uic.cs342.project3.game;

import edu.uic.cs342.project3.model.CheckersGame;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry of all active and completed CheckersGame instances.
 */
public class GameManager {

    // ── Fields ────────────────────────────────────────────────────────────────

    private static final GameManager              INSTANCE = new GameManager();
    private final Map<String, CheckersGame>        sessions = new ConcurrentHashMap<>();

    // ── Constructors ──────────────────────────────────────────────────────────

    private GameManager() {}

    // ── Getters ───────────────────────────────────────────────────────────────

    public static GameManager getInstance() { return INSTANCE; }

    public CheckersGame getGame(String id) {
        return sessions.get(id);
    }

    public Collection<CheckersGame> getAllGames() {
        return sessions.values();
    }

    // ── Methods ───────────────────────────────────────────────────────────────

    public synchronized CheckersGame createGame(String player1, String player2, boolean vsAI) {
        CheckersGame game = new CheckersGame(player1, player2, vsAI);
        sessions.put(game.getId(), game);
        return game;
    }

    public CheckersGame findActiveGameForPlayer(String username) {
        return sessions.values().stream()
                .filter(g -> g.getStatus() == CheckersGame.Status.ACTIVE && g.hasPlayer(username))
                .findFirst().orElse(null);
    }
}
