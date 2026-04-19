package edu.uic.cs342.model;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GameManager {

    // ── Fields ────────────────────────────────────────────────────────────────

    private static final GameManager         INSTANCE = new GameManager();
    private final Map<String, CheckersGame>  sessions = new ConcurrentHashMap<>();

    // ── Constructors ──────────────────────────────────────────────────────────

    private GameManager() {}

    // ── Getters ───────────────────────────────────────────────────────────────

    public static GameManager getInstance() { return GameManager.INSTANCE; }

    public CheckersGame getGame(String id) {
        return this.sessions.get(id);
    }

    public Collection<CheckersGame> getAllGames() {
        return this.sessions.values();
    }

    // ── Methods ───────────────────────────────────────────────────────────────

    public synchronized CheckersGame createGame(String player1, String player2, boolean vsAI) {
        CheckersGame game = new CheckersGame(player1, player2, vsAI);
        this.sessions.put(game.getId(), game);
        return game;
    }

    public CheckersGame findActiveGameForPlayer(String username) {
        return this.sessions.values().stream()
                .filter(g -> g.getStatus() == CheckersGame.Status.ACTIVE && g.hasPlayer(username))
                .findFirst().orElse(null);
    }
}
