package edu.uic.cs342.project3.http;

import edu.uic.cs342.project3.game.GameManager;
import edu.uic.cs342.project3.model.Board;
import edu.uic.cs342.project3.model.Color;
import edu.uic.cs342.project3.model.CheckersGame;
import edu.uic.cs342.project3.model.Player;
import edu.uic.cs342.project3.util.JsonUtil;
import edu.uic.cs342.project3.util.PlayerRegistry;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Routes every incoming request to the appropriate handler.
 * handle() is pure logic: receives an HttpRequest POJO, returns an HttpResponse POJO.
 * All stream I/O lives in ServerThread, not here.
 */
public class Router {

    // ── Fields ────────────────────────────────────────────────────────────────

    private static final Logger LOG = Logger.getLogger(Router.class.getName());

    private final Consumer<String> logger;

    // ── Constructors ──────────────────────────────────────────────────────────

    public Router(Consumer<String> logger) {
        this.logger = logger;
    }

    // ── Methods ───────────────────────────────────────────────────────────────

    public HttpResponse handle(HttpRequest req) {
        String method = req.getMethod();
        String path   = req.getPath();
        long   seq    = req.getSequenceId();

        log("[" + method + "] " + path);

        try {
            if (path.equals("/auth/signup")          && method.equals("POST")) return signup(req, seq);
            if (path.equals("/auth/login")           && method.equals("POST")) return login(req, seq);
            if (path.equals("/auth/logout")          && method.equals("POST")) return logout(req, seq);

            if (path.equals("/users/online")         && method.equals("GET"))  return onlineUsers(seq);
            if (path.matches("/users/[^/]+/stats")) {
                if (method.equals("GET"))  return getUserStats(path, seq);
                if (method.equals("POST")) return updateStats(path, req, seq);
            }

            if (path.equals("/friends/add")          && method.equals("POST")) return addFriend(req, seq);
            if (path.matches("/friends/[^/]+")       && method.equals("GET"))  return getFriends(path, seq);

            if (path.equals("/game/create")          && method.equals("POST")) return createGame(req, seq);
            if (path.matches("/game/[^/]+")          && method.equals("GET"))  return getGame(path, seq);
            if (path.matches("/game/[^/]+/move")     && method.equals("POST")) return makeMove(path, req, seq);
            if (path.matches("/game/[^/]+/chat")     && method.equals("POST")) return sendChat(path, req, seq);
            if (path.matches("/game/[^/]+/end")      && method.equals("POST")) return endGame(path, req, seq);

            if (path.equals("/logs")                 && method.equals("GET"))  return getLogs(seq);

            return HttpResponse.notFound(seq);

        } catch (Exception e) {
            LOG.severe("Handler error: " + e.getMessage());
            return HttpResponse.serverError(seq, e.getMessage() != null ? e.getMessage() : "Internal error");
        }
    }

    private HttpResponse signup(HttpRequest req, long seq) throws Exception {
        JsonNode body     = parseBody(req);
        String   username = str(body, "username");
        String   password = str(body, "password");

        if (username.isBlank() || password.isBlank())
            return HttpResponse.badRequest(seq, "Username and password are required.");
        if (username.length() < 3 || username.length() > 20)
            return HttpResponse.badRequest(seq, "Username must be 3-20 characters.");
        if (password.length() < 6)
            return HttpResponse.badRequest(seq, "Password must be at least 6 characters.");
        if (PlayerRegistry.getInstance().usernameExists(username))
            return HttpResponse.conflict(seq, "Username already taken.");

        PlayerRegistry.getInstance().save(new Player(username, password));
        log("New account registered: " + username);
        return HttpResponse.ok(seq, "{\"success\":true,\"username\":\"" + username + "\"}");
    }

    private HttpResponse login(HttpRequest req, long seq) throws Exception {
        JsonNode body     = parseBody(req);
        String   username = str(body, "username");
        String   password = str(body, "password");

        Player player = PlayerRegistry.getInstance().get(username);
        if (player == null || !player.checkPassword(password))
            return HttpResponse.unauthorized(seq, "Invalid username or password.");

        player.setStatus(Player.Status.ONLINE);
        PlayerRegistry.getInstance().setOnline(username);
        log("Login: " + username);

        Map<String, Object> resp = new HashMap<>();
        resp.put("success",  true);
        resp.put("username", player.getUsername());
        resp.put("stats",    statsMap(player));
        return HttpResponse.ok(seq, JsonUtil.toJson(resp));
    }

    private HttpResponse logout(HttpRequest req, long seq) throws Exception {
        JsonNode body     = parseBody(req);
        String   username = str(body, "username");
        PlayerRegistry.getInstance().setOffline(username);
        Player player = PlayerRegistry.getInstance().get(username);
        if (player != null) player.setStatus(Player.Status.OFFLINE);
        log("Logout: " + username);
        return HttpResponse.ok(seq, "{\"success\":true}");
    }

    private HttpResponse onlineUsers(long seq) throws Exception {
        Map<String, Object> resp = new HashMap<>();
        resp.put("users", PlayerRegistry.getInstance().getOnlineUsernames());
        return HttpResponse.ok(seq, JsonUtil.toJson(resp));
    }

    private HttpResponse getUserStats(String path, long seq) throws Exception {
        String username = path.split("/")[2];
        Player player   = PlayerRegistry.getInstance().get(username);
        if (player == null) return HttpResponse.notFound(seq);
        Map<String, Object> resp = new HashMap<>();
        resp.put("stats", statsMap(player));
        return HttpResponse.ok(seq, JsonUtil.toJson(resp));
    }

    private HttpResponse updateStats(String path, HttpRequest req, long seq) throws Exception {
        String   username = path.split("/")[2];
        JsonNode body     = parseBody(req);
        String   result   = str(body, "result");
        Player   player   = PlayerRegistry.getInstance().get(username);
        if (player == null) return HttpResponse.notFound(seq);
        switch (result) {
            case "win"  -> player.addWin();
            case "loss" -> player.addLoss();
            case "draw" -> player.addDraw();
        }
        PlayerRegistry.getInstance().save(player);
        return HttpResponse.ok(seq, "{\"success\":true}");
    }

    private HttpResponse addFriend(HttpRequest req, long seq) throws Exception {
        JsonNode body       = parseBody(req);
        String   username   = str(body, "username");
        String   friendName = str(body, "friendUsername");
        Player   player     = PlayerRegistry.getInstance().get(username);
        Player   friend     = PlayerRegistry.getInstance().get(friendName);
        if (player == null) return HttpResponse.notFound(seq);
        if (friend == null) return HttpResponse.badRequest(seq, "User '" + friendName + "' not found.");
        if (username.equalsIgnoreCase(friendName))
            return HttpResponse.badRequest(seq, "You cannot add yourself.");
        player.addFriend(friend.getUsername());
        PlayerRegistry.getInstance().save(player);
        log(username + " added friend: " + friendName);
        return HttpResponse.ok(seq, "{\"success\":true}");
    }

    private HttpResponse getFriends(String path, long seq) throws Exception {
        String username = path.split("/")[2];
        Player player   = PlayerRegistry.getInstance().get(username);
        if (player == null) return HttpResponse.notFound(seq);

        List<Map<String, Object>> list = new ArrayList<>();
        for (String f : player.getFriends()) {
            Player fp = PlayerRegistry.getInstance().get(f);
            if (fp == null) continue;
            Map<String, Object> entry = new HashMap<>();
            entry.put("username", fp.getUsername());
            entry.put("isOnline", PlayerRegistry.getInstance().isOnline(f));
            entry.put("stats",    statsMap(fp));
            list.add(entry);
        }
        Map<String, Object> resp = new HashMap<>();
        resp.put("friends", list);
        return HttpResponse.ok(seq, JsonUtil.toJson(resp));
    }

    private HttpResponse createGame(HttpRequest req, long seq) throws Exception {
        JsonNode body    = parseBody(req);
        String   player1 = str(body, "player1");
        boolean  vsAI    = body.has("vsAI") && body.get("vsAI").asBoolean();
        String   player2 = vsAI ? "AI" : str(body, "player2");
        if (player1.isBlank()) return HttpResponse.badRequest(seq, "player1 required.");
        CheckersGame game = GameManager.getInstance().createGame(player1, player2, vsAI);
        log("Game created: " + game.getId() + " (" + player1 + " vs " + game.getPlayer2() + ")");
        Map<String, Object> resp = new HashMap<>();
        resp.put("gameId", game.getId());
        return HttpResponse.ok(seq, JsonUtil.toJson(resp));
    }

    private HttpResponse getGame(String path, long seq) throws Exception {
        String       id   = path.split("/")[2];
        CheckersGame game = GameManager.getInstance().getGame(id);
        if (game == null) return HttpResponse.notFound(seq);
        Map<String, Object> resp = new HashMap<>();
        resp.put("gameState", game);
        return HttpResponse.ok(seq, JsonUtil.toJson(resp));
    }

    private HttpResponse makeMove(String path, HttpRequest req, long seq) throws Exception {
        String       id   = path.split("/")[2];
        CheckersGame game = GameManager.getInstance().getGame(id);
        if (game == null) return HttpResponse.notFound(seq);

        JsonNode body   = parseBody(req);
        String   player = str(body, "player");
        JsonNode fromN  = body.get("from");
        JsonNode toN    = body.get("to");

        Board.Pos from = new Board.Pos(fromN.get("row").asInt(), fromN.get("col").asInt());
        Board.Pos to   = new Board.Pos(toN.get("row").asInt(),   toN.get("col").asInt());

        String error;
        synchronized (game) {
            error = player.equals("AI") ? game.applyAiMove() : game.applyMove(from, to, player);

            if (error == null && game.isVsAI()
                    && game.getStatus() == CheckersGame.Status.ACTIVE
                    && game.getCurrentTurn() == Color.BLACK) {
                game.applyAiMove();
            }
        }

        if (error != null) return HttpResponse.badRequest(seq, error);

        if (game.getStatus() == CheckersGame.Status.COMPLETED) {
            updateStatsForGame(game);
            log("Game " + id + " ended. Winner: " + game.getWinner());
        }
        return HttpResponse.ok(seq, "{\"success\":true}");
    }

    private HttpResponse sendChat(String path, HttpRequest req, long seq) throws Exception {
        String       id      = path.split("/")[2];
        CheckersGame game    = GameManager.getInstance().getGame(id);
        if (game == null) return HttpResponse.notFound(seq);
        JsonNode body    = parseBody(req);
        String   player  = str(body, "player");
        String   message = str(body, "message");
        if (message.isBlank()) return HttpResponse.badRequest(seq, "Message cannot be blank.");
        game.addChat(player, message);
        return HttpResponse.ok(seq, "{\"success\":true}");
    }

    private HttpResponse endGame(String path, HttpRequest req, long seq) throws Exception {
        String       id   = path.split("/")[2];
        CheckersGame game = GameManager.getInstance().getGame(id);
        if (game == null) return HttpResponse.notFound(seq);
        String winner = str(parseBody(req), "winner");
        game.endGame(winner);
        updateStatsForGame(game);
        log("Game " + id + " force-ended. Winner: " + winner);
        return HttpResponse.ok(seq, "{\"success\":true}");
    }

    private HttpResponse getLogs(long seq) throws Exception {
        Map<String, Object> resp = new HashMap<>();
        resp.put("onlineCount", PlayerRegistry.getInstance().getOnlineUsernames().size());
        resp.put("gameCount",   GameManager.getInstance().getAllGames().size());
        return HttpResponse.ok(seq, JsonUtil.toJson(resp));
    }

    private JsonNode parseBody(HttpRequest req) throws Exception {
        String body = req.getBody();
        if (body == null || body.isBlank()) return JsonUtil.getMapper().createObjectNode();
        try { return JsonUtil.getMapper().readTree(body); }
        catch (Exception e) { return JsonUtil.getMapper().createObjectNode(); }
    }

    private static String str(JsonNode node, String field) {
        JsonNode n = node.get(field);
        return (n != null && !n.isNull()) ? n.asText("").trim() : "";
    }

    private static Map<String, Object> statsMap(Player player) {
        Map<String, Object> m = new HashMap<>();
        m.put("wins",   player.getWins());
        m.put("losses", player.getLosses());
        m.put("draws",  player.getDraws());
        return m;
    }

    private void updateStatsForGame(CheckersGame game) {
        String winner = game.getWinner();
        if (winner == null) return;
        Player p1 = PlayerRegistry.getInstance().get(game.getPlayer1());
        Player p2 = game.isVsAI() ? null : PlayerRegistry.getInstance().get(game.getPlayer2());
        if (p1 != null) {
            if (winner.equals("draw"))     p1.addDraw();
            else if (winner.equals(Color.RED.getValue())) p1.addWin();
            else                           p1.addLoss();
            PlayerRegistry.getInstance().save(p1);
        }
        if (p2 != null) {
            if (winner.equals("draw"))       p2.addDraw();
            else if (winner.equals(Color.BLACK.getValue())) p2.addWin();
            else                             p2.addLoss();
            PlayerRegistry.getInstance().save(p2);
        }
    }

    private void log(String msg) {
        LOG.info(msg);
        if (logger != null) logger.accept(msg);
    }
}
