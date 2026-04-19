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

public class Router {
    // ── Fields ───────────────────────────────────────────────────────────────────────────────────────────────────────
    private static final Logger LOGGER = Logger.getLogger(Router.class.getName());

    private final Consumer<String> callback;

    // ── Constructors ─────────────────────────────────────────────────────────────────────────────────────────────────
    public Router(Consumer<String> callback) { this.callback = callback; }

    // ── Methods ──────────────────────────────────────────────────────────────────────────────────────────────────────
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

    private HttpResponse signup(HttpRequest request, long sequenceId) {
        JsonNode body = this.parseBody(request);
        String username = str(body, "username");
        String password = str(body, "password");

        if (username.isBlank() || password.isBlank())
            return HttpResponse.badRequest(sequenceId, "Username and password are required.");
        if (username.length() < 3 || username.length() > 20)
            return HttpResponse.badRequest(sequenceId, "Username must be 3-20 characters.");
        if (password.length() < 6)
            return HttpResponse.badRequest(sequenceId, "Password must be at least 6 characters.");
        if (PlayerRegistry.getInstance().usernameExists(username))
            return HttpResponse.conflict(sequenceId, "Username already taken.");

        PlayerRegistry.getInstance().save(new Player(username, password));
        this.log("New account registered: " + username);
        return HttpResponse.ok(sequenceId, "{\"success\":true,\"username\":\"" + username + "\"}");
    }

    private HttpResponse login(HttpRequest request, long sequenceId) {
        JsonNode body     = parseBody(request);
        String   username = str(body, "username");
        String   password = str(body, "password");

        Player player = PlayerRegistry.getInstance().get(username);
        if (player == null || !player.checkPassword(password))
            return HttpResponse.unauthorized(sequenceId, "Invalid username or password.");

        player.setStatus(Player.Status.ONLINE);
        PlayerRegistry.getInstance().setOnline(username);
        log("Login: " + username);

        Map<String, Object> resp = new HashMap<>();
        resp.put("success",  true);
        resp.put("username", player.getUsername());
        resp.put("stats",    statsMap(player));
        return HttpResponse.ok(sequenceId, JsonUtil.toJson(resp));
    }

    private HttpResponse logout(HttpRequest request, long sequenceId) {
        JsonNode body     = parseBody(request);
        String   username = str(body, "username");
        PlayerRegistry.getInstance().setOffline(username);
        Player player = PlayerRegistry.getInstance().get(username);
        if (player != null) player.setStatus(Player.Status.OFFLINE);
        log("Logout: " + username);
        return HttpResponse.ok(sequenceId, "{\"success\":true}");
    }

    private HttpResponse onlineUsers(long sequenceId) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("users", PlayerRegistry.getInstance().getOnlineUsernames());
        return HttpResponse.ok(sequenceId, JsonUtil.toJson(resp));
    }

    private HttpResponse getUserStats(String path, long sequenceId) {
        String username = path.split("/")[2];
        Player player   = PlayerRegistry.getInstance().get(username);
        if (player == null) return HttpResponse.notFound(sequenceId);
        Map<String, Object> resp = new HashMap<>();
        resp.put("stats", statsMap(player));
        return HttpResponse.ok(sequenceId, JsonUtil.toJson(resp));
    }

    private HttpResponse updateStats(String path, HttpRequest request, long sequenceId) {
        String   username = path.split("/")[2];
        JsonNode body     = parseBody(request);
        String   result   = str(body, "result");
        Player   player   = PlayerRegistry.getInstance().get(username);
        if (player == null) return HttpResponse.notFound(sequenceId);
        switch (result) {
            case "win"  -> player.addWin();
            case "loss" -> player.addLoss();
            case "draw" -> player.addDraw();
        }
        PlayerRegistry.getInstance().save(player);
        return HttpResponse.ok(sequenceId, "{\"success\":true}");
    }

    private HttpResponse addFriend(HttpRequest request, long sequenceId) {
        JsonNode body       = parseBody(request);
        String   username   = str(body, "username");
        String   friendName = str(body, "friendUsername");
        Player   player     = PlayerRegistry.getInstance().get(username);
        Player   friend     = PlayerRegistry.getInstance().get(friendName);
        if (player == null) return HttpResponse.notFound(sequenceId);
        if (friend == null) return HttpResponse.badRequest(sequenceId, "User '" + friendName + "' not found.");
        if (username.equalsIgnoreCase(friendName))
            return HttpResponse.badRequest(sequenceId, "You cannot add yourself.");
        player.addFriend(friend.getUsername());
        PlayerRegistry.getInstance().save(player);
        log(username + " added friend: " + friendName);
        return HttpResponse.ok(sequenceId, "{\"success\":true}");
    }

    private HttpResponse getFriends(String path, long sequenceId) {
        String username = path.split("/")[2];
        Player player   = PlayerRegistry.getInstance().get(username);
        if (player == null) return HttpResponse.notFound(sequenceId);

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
        return HttpResponse.ok(sequenceId, JsonUtil.toJson(resp));
    }

    private HttpResponse createGame(HttpRequest request, long sequenceId) {
        JsonNode body    = parseBody(request);
        String   player1 = str(body, "player1");
        boolean  vsAI    = body.has("vsAI") && body.get("vsAI").asBoolean();
        String   player2 = vsAI ? "AI" : str(body, "player2");
        if (player1.isBlank()) return HttpResponse.badRequest(sequenceId, "player1 required.");
        CheckersGame game = GameManager.getInstance().createGame(player1, player2, vsAI);
        log("Game created: " + game.getId() + " (" + player1 + " vs " + game.getPlayer2() + ")");
        Map<String, Object> resp = new HashMap<>();
        resp.put("gameId", game.getId());
        return HttpResponse.ok(sequenceId, JsonUtil.toJson(resp));
    }

    private HttpResponse getGame(String path, long sequenceId) {
        String       id   = path.split("/")[2];
        CheckersGame game = GameManager.getInstance().getGame(id);
        if (game == null) return HttpResponse.notFound(sequenceId);
        Map<String, Object> resp = new HashMap<>();
        resp.put("gameState", game);
        return HttpResponse.ok(sequenceId, JsonUtil.toJson(resp));
    }

    private HttpResponse makeMove(String path, HttpRequest request, long sequenceId) {
        String       id   = path.split("/")[2];
        CheckersGame game = GameManager.getInstance().getGame(id);
        if (game == null) return HttpResponse.notFound(sequenceId);

        JsonNode body   = parseBody(request);
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

        if (error != null) return HttpResponse.badRequest(sequenceId, error);

        if (game.getStatus() == CheckersGame.Status.COMPLETED) {
            updateStatsForGame(game);
            log("Game " + id + " ended. Winner: " + game.getWinner());
        }
        return HttpResponse.ok(sequenceId, "{\"success\":true}");
    }

    private HttpResponse sendChat(String path, HttpRequest request, long sequenceId) {
        String       id      = path.split("/")[2];
        CheckersGame game    = GameManager.getInstance().getGame(id);
        if (game == null) return HttpResponse.notFound(sequenceId);
        JsonNode body    = parseBody(request);
        String   player  = str(body, "player");
        String   message = str(body, "message");
        if (message.isBlank()) return HttpResponse.badRequest(sequenceId, "Message cannot be blank.");
        game.addChat(player, message);
        return HttpResponse.ok(sequenceId, "{\"success\":true}");
    }

    private HttpResponse endGame(String path, HttpRequest request, long sequenceId) {
        String       id   = path.split("/")[2];
        CheckersGame game = GameManager.getInstance().getGame(id);
        if (game == null) return HttpResponse.notFound(sequenceId);
        String winner = str(parseBody(request), "winner");
        game.endGame(winner);
        updateStatsForGame(game);
        log("Game " + id + " force-ended. Winner: " + winner);
        return HttpResponse.ok(sequenceId, "{\"success\":true}");
    }

    private HttpResponse getLogs(long sequenceId) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("onlineCount", PlayerRegistry.getInstance().getOnlineUsernames().size());
        resp.put("gameCount",   GameManager.getInstance().getAllGames().size());
        return HttpResponse.ok(sequenceId, JsonUtil.toJson(resp));
    }

    private JsonNode parseBody(HttpRequest request) {
        String body = request.getBody();
        if (body == null || body.isBlank()) return JsonUtil.getMapper().createObjectNode();
        try { return JsonUtil.getMapper().readTree(body); }
        catch (Exception e) { return JsonUtil.getMapper().createObjectNode(); }
    }

    private void updateStatsForGame(CheckersGame game) {
        String winner = game.getWinner();
        if (winner == null) return;
        Player p1 = PlayerRegistry.getInstance().get(game.getPlayer1());
        Player p2 = game.isVsAI() ? null : PlayerRegistry.getInstance().get(game.getPlayer2());
        if (p1 != null) {
            if (winner.equals("draw"))     p1.addDraw();
            else if (winner.equals(Color.RED.getName())) p1.addWin();
            else                           p1.addLoss();
            PlayerRegistry.getInstance().save(p1);
        }
        if (p2 != null) {
            if (winner.equals("draw"))       p2.addDraw();
            else if (winner.equals(Color.BLACK.getName())) p2.addWin();
            else                             p2.addLoss();
            PlayerRegistry.getInstance().save(p2);
        }
    }

    private void log(String message) {
        Router.LOGGER.info(message);
        if (callback != null) callback.accept(message);
    }

    public HttpResponse handleRequest(HttpRequest request) {
        HttpMethod method = request.getMethod();
        String path = request.getPath();
        long sequenceId = request.getSequenceId();

        this.log(String.format("[%s] %s", method.toString(), path));

        try {
            if (path.equals("/auth/signup") && method == HttpMethod.POST) {
                return this.signup(request, sequenceId);
            }

            if (path.equals("/auth/login") && method == HttpMethod.POST) {
                return this.login(request, sequenceId);
            }

            if (path.equals("/auth/logout") && method == HttpMethod.POST) {
                return this.logout(request, sequenceId);
            }

            if (path.equals("/users/online") && method == HttpMethod.GET) {
                return this.onlineUsers(sequenceId);
            }

            if (path.matches("/users/[^/]+/stats")) {
                if (method == HttpMethod.GET) {
                    return this.getUserStats(path, sequenceId);
                }

                if (method == HttpMethod.POST) {
                    return this.updateStats(path, request, sequenceId);
                }
            }

            if (path.equals("/friends/add") && method == HttpMethod.POST) {
                return this.addFriend(request, sequenceId);
            }

            if (path.matches("/friends/[^/]+") && method == HttpMethod.GET) {
                return this.getFriends(path, sequenceId);
            }

            if (path.equals("/game/create") && method == HttpMethod.POST) {
                return this.createGame(request, sequenceId);
            }

            if (path.matches("/game/[^/]+") && method == HttpMethod.GET) {
                return this.getGame(path, sequenceId);
            }

            if (path.matches("/game/[^/]+/move") && method == HttpMethod.POST) {
                return this.makeMove(path, request, sequenceId);
            }
            if (path.matches("/game/[^/]+/chat") && method == HttpMethod.POST) {
                return this.sendChat(path, request, sequenceId);
            }

            if (path.matches("/game/[^/]+/end") && method == HttpMethod.POST) {
                return this.endGame(path, request, sequenceId);
            }

            if (path.equals("/logs") && method == HttpMethod.GET) {
                return this.getLogs(sequenceId);
            }

            return HttpResponse.notFound(sequenceId);

        } catch (Exception e) {
            LOGGER.severe("Handler error: " + e.getMessage());
            return HttpResponse.serverError(sequenceId, e.getMessage() != null ? e.getMessage() : "Internal error");
        }
    }

}
