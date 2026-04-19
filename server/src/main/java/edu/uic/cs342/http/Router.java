package edu.uic.cs342.http;

import edu.uic.cs342.model.AiPlayer;
import edu.uic.cs342.model.GameManager;
import edu.uic.cs342.model.Board;
import edu.uic.cs342.model.CheckersGame;
import edu.uic.cs342.model.Color;
import edu.uic.cs342.model.Opponent;
import edu.uic.cs342.model.Player;
import edu.uic.cs342.util.JsonUtil;
import edu.uic.cs342.util.PlayerRegistry;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;

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

        this.log("[" + method + "] " + path);

        try {
            if (path.equals("/auth/signup")            && method.equals("POST")) return this.signup(req);
            if (path.equals("/auth/login")             && method.equals("POST")) return this.login(req);
            if (path.equals("/auth/logout")            && method.equals("POST")) return this.logout(req);

            if (path.equals("/users/online")           && method.equals("GET"))  return this.onlineUsers();
            if (path.matches("/users/[^/]+/stats")) {
                if (method.equals("GET"))  return this.getUserStats(path);
                if (method.equals("POST")) return this.updateStats(path, req);
            }

            if (path.equals("/friends/request")        && method.equals("POST")) return this.sendFriendRequest(req);
            if (path.equals("/friends/accept")         && method.equals("POST")) return this.acceptFriendRequest(req);
            if (path.equals("/friends/decline")        && method.equals("POST")) return this.declineFriendRequest(req);
            if (path.matches("/friends/[^/]+")         && method.equals("GET"))  return this.getFriends(path);

            if (path.equals("/challenge/send")         && method.equals("POST")) return this.sendChallenge(req);
            if (path.equals("/challenge/accept")       && method.equals("POST")) return this.acceptChallenge(req);
            if (path.equals("/challenge/decline")      && method.equals("POST")) return this.declineChallenge(req);
            if (path.matches("/challenge/[^/]+")       && method.equals("GET"))  return this.getChallenge(path);

            if (path.equals("/game/create")            && method.equals("POST")) return this.createGame(req);
            if (path.matches("/game/active/[^/]+")     && method.equals("GET"))  return this.getActiveGame(path);
            if (path.matches("/game/[^/]+")            && method.equals("GET"))  return this.getGame(path);
            if (path.matches("/game/[^/]+/move")       && method.equals("POST")) return this.makeMove(path, req);
            if (path.matches("/game/[^/]+/chat")       && method.equals("POST")) return this.sendChat(path, req);
            if (path.matches("/game/[^/]+/end")        && method.equals("POST")) return this.endGame(path, req);

            if (path.equals("/logs")                   && method.equals("GET"))  return this.getLogs();

            return HttpResponse.notFound();

        } catch (Exception e) {
            Router.LOG.severe("Handler error: " + e.getMessage());
            return HttpResponse.serverError(e.getMessage() != null ? e.getMessage() : "Internal error");
        }
    }

    // ── Auth ──────────────────────────────────────────────────────────────────

    private HttpResponse signup(HttpRequest req) throws Exception {
        JsonNode body     = this.parseBody(req);
        String   username = Router.str(body, "username");
        String   password = Router.str(body, "password");

        if (username.isBlank() || password.isBlank())
            return HttpResponse.badRequest("Username and password are required.");
        if (username.length() < 3 || username.length() > 20)
            return HttpResponse.badRequest("Username must be 3-20 characters.");
        if (password.length() < 6)
            return HttpResponse.badRequest("Password must be at least 6 characters.");
        if (PlayerRegistry.getInstance().usernameExists(username))
            return HttpResponse.conflict("Username already taken.");

        PlayerRegistry.getInstance().save(new Player(username, password));
        this.log("New account registered: " + username);
        return HttpResponse.ok("{\"success\":true,\"username\":\"" + username + "\"}");
    }

    private HttpResponse login(HttpRequest req) throws Exception {
        JsonNode body     = this.parseBody(req);
        String   username = Router.str(body, "username");
        String   password = Router.str(body, "password");

        Player player = PlayerRegistry.getInstance().get(username);
        if (player == null || !player.checkPassword(password))
            return HttpResponse.unauthorized("Invalid username or password.");

        player.setStatus(Player.Status.ONLINE);
        PlayerRegistry.getInstance().setOnline(username);
        this.log("Login: " + username);

        Map<String, Object> resp = new HashMap<>();
        resp.put("success",  true);
        resp.put("username", player.getUsername());
        resp.put("stats",    Router.statsMap(player));
        return HttpResponse.ok(JsonUtil.toJson(resp));
    }

    private HttpResponse logout(HttpRequest req) throws Exception {
        String username = Router.str(this.parseBody(req), "username");
        PlayerRegistry.getInstance().setOffline(username);
        PlayerRegistry.getInstance().removeChallengeForTarget(username);
        Player player = PlayerRegistry.getInstance().get(username);
        if (player != null) player.setStatus(Player.Status.OFFLINE);
        this.log("Logout: " + username);
        return HttpResponse.ok("{\"success\":true}");
    }

    // ── Users ─────────────────────────────────────────────────────────────────

    private HttpResponse onlineUsers() throws Exception {
        Map<String, Object> resp = new HashMap<>();
        resp.put("users", PlayerRegistry.getInstance().getOnlineUsernames());
        return HttpResponse.ok(JsonUtil.toJson(resp));
    }

    private HttpResponse getUserStats(String path) throws Exception {
        String username = path.split("/")[2];
        Player player   = PlayerRegistry.getInstance().get(username);
        if (player == null) return HttpResponse.notFound();
        Map<String, Object> resp = new HashMap<>();
        resp.put("stats", Router.statsMap(player));
        return HttpResponse.ok(JsonUtil.toJson(resp));
    }

    private HttpResponse updateStats(String path, HttpRequest req) throws Exception {
        String   username = path.split("/")[2];
        String   result   = Router.str(this.parseBody(req), "result");
        Player   player   = PlayerRegistry.getInstance().get(username);
        if (player == null) return HttpResponse.notFound();
        switch (result) {
            case "win"  -> player.addWin();
            case "loss" -> player.addLoss();
            case "draw" -> player.addDraw();
        }
        PlayerRegistry.getInstance().save(player);
        return HttpResponse.ok("{\"success\":true}");
    }

    // ── Friends ───────────────────────────────────────────────────────────────

    private HttpResponse sendFriendRequest(HttpRequest req) throws Exception {
        JsonNode body       = this.parseBody(req);
        String   username   = Router.str(body, "username");
        String   targetName = Router.str(body, "targetUsername");
        Player   player     = PlayerRegistry.getInstance().get(username);
        Player   target     = PlayerRegistry.getInstance().get(targetName);

        if (player == null) return HttpResponse.notFound();
        if (target == null) return HttpResponse.badRequest("User '" + targetName + "' not found.");
        if (username.equalsIgnoreCase(targetName))
            return HttpResponse.badRequest("You cannot add yourself.");
        if (player.isFriend(targetName))
            return HttpResponse.badRequest("You are already friends.");
        if (player.hasSentRequestTo(targetName))
            return HttpResponse.badRequest("Friend request already sent.");

        if (player.hasPendingRequestFrom(targetName)) {
            player.removeIncomingRequest(targetName);
            target.removeOutgoingRequest(username);
            player.addFriend(target.getUsername());
            target.addFriend(player.getUsername());
            PlayerRegistry.getInstance().save(player);
            PlayerRegistry.getInstance().save(target);
            this.log(username + " and " + targetName + " are now friends (auto-accepted).");
            return HttpResponse.ok("{\"success\":true,\"autoAccepted\":true}");
        }

        player.addOutgoingRequest(target.getUsername());
        target.addIncomingRequest(player.getUsername());
        PlayerRegistry.getInstance().save(player);
        PlayerRegistry.getInstance().save(target);
        this.log(username + " sent friend request to " + targetName);
        return HttpResponse.ok("{\"success\":true,\"autoAccepted\":false}");
    }

    private HttpResponse acceptFriendRequest(HttpRequest req) throws Exception {
        JsonNode body          = this.parseBody(req);
        String   username      = Router.str(body, "username");
        String   requesterName = Router.str(body, "requesterUsername");
        Player   player        = PlayerRegistry.getInstance().get(username);
        Player   requester     = PlayerRegistry.getInstance().get(requesterName);

        if (player == null || requester == null) return HttpResponse.notFound();
        if (!player.hasPendingRequestFrom(requesterName))
            return HttpResponse.badRequest("No pending request from that user.");

        player.removeIncomingRequest(requesterName);
        requester.removeOutgoingRequest(username);
        player.addFriend(requester.getUsername());
        requester.addFriend(player.getUsername());
        PlayerRegistry.getInstance().save(player);
        PlayerRegistry.getInstance().save(requester);
        this.log(username + " accepted friend request from " + requesterName);
        return HttpResponse.ok("{\"success\":true}");
    }

    private HttpResponse declineFriendRequest(HttpRequest req) throws Exception {
        JsonNode body          = this.parseBody(req);
        String   username      = Router.str(body, "username");
        String   requesterName = Router.str(body, "requesterUsername");
        Player   player        = PlayerRegistry.getInstance().get(username);
        Player   requester     = PlayerRegistry.getInstance().get(requesterName);

        if (player == null || requester == null) return HttpResponse.notFound();

        player.removeIncomingRequest(requesterName);
        requester.removeOutgoingRequest(username);
        PlayerRegistry.getInstance().save(player);
        PlayerRegistry.getInstance().save(requester);
        this.log(username + " declined friend request from " + requesterName);
        return HttpResponse.ok("{\"success\":true}");
    }

    private HttpResponse getFriends(String path) throws Exception {
        String username = path.split("/")[2];
        Player player   = PlayerRegistry.getInstance().get(username);
        if (player == null) return HttpResponse.notFound();

        List<Map<String, Object>> friendList = new ArrayList<>();
        for (String f : player.getFriends()) {
            Player fp = PlayerRegistry.getInstance().get(f);
            if (fp == null) continue;
            Map<String, Object> entry = new HashMap<>();
            entry.put("username", fp.getUsername());
            entry.put("isOnline", PlayerRegistry.getInstance().isOnline(f));
            entry.put("stats",    Router.statsMap(fp));
            friendList.add(entry);
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("friends",  friendList);
        resp.put("incoming", player.getIncomingFriendRequests());
        resp.put("outgoing", player.getOutgoingFriendRequests());
        return HttpResponse.ok(JsonUtil.toJson(resp));
    }

    // ── Challenges ────────────────────────────────────────────────────────────

    private HttpResponse sendChallenge(HttpRequest req) throws Exception {
        JsonNode body       = this.parseBody(req);
        String   challenger = Router.str(body, "challenger");
        String   target     = Router.str(body, "target");

        if (PlayerRegistry.getInstance().get(challenger) == null) return HttpResponse.notFound();
        if (PlayerRegistry.getInstance().get(target) == null)
            return HttpResponse.badRequest("User '" + target + "' not found.");
        if (!PlayerRegistry.getInstance().isOnline(target))
            return HttpResponse.badRequest(target + " is not online.");

        PlayerRegistry.getInstance().sendChallenge(challenger, target);
        this.log(challenger + " challenged " + target);
        return HttpResponse.ok("{\"success\":true}");
    }

    private HttpResponse acceptChallenge(HttpRequest req) throws Exception {
        JsonNode body       = this.parseBody(req);
        String   accepter   = Router.str(body, "accepter");
        String   challenger = Router.str(body, "challenger");

        PlayerRegistry.getInstance().removeChallengeForTarget(accepter);

        CheckersGame game = GameManager.getInstance().createGame(challenger, accepter, false);
        this.log("Challenge accepted: " + game.getId() + " (" + challenger + " vs " + accepter + ")");

        Map<String, Object> resp = new HashMap<>();
        resp.put("gameId", game.getId());
        return HttpResponse.ok(JsonUtil.toJson(resp));
    }

    private HttpResponse declineChallenge(HttpRequest req) throws Exception {
        JsonNode body       = this.parseBody(req);
        String   decliner   = Router.str(body, "decliner");
        String   challenger = Router.str(body, "challenger");

        PlayerRegistry.getInstance().removeChallengeForTarget(decliner);
        this.log(decliner + " declined challenge from " + challenger);
        return HttpResponse.ok("{\"success\":true}");
    }

    private HttpResponse getChallenge(String path) throws Exception {
        String target     = path.split("/")[2];
        String challenger = PlayerRegistry.getInstance().getPendingChallenger(target);

        Map<String, Object> resp = new HashMap<>();
        resp.put("challenger", challenger);
        return HttpResponse.ok(JsonUtil.toJson(resp));
    }

    // ── Games ─────────────────────────────────────────────────────────────────

    private HttpResponse createGame(HttpRequest req) throws Exception {
        JsonNode body    = this.parseBody(req);
        String   player1 = Router.str(body, "player1");
        boolean  vsAI    = body.has("vsAI") && body.get("vsAI").asBoolean();
        String   player2 = vsAI ? "AI" : Router.str(body, "player2");
        if (player1.isBlank()) return HttpResponse.badRequest("player1 required.");
        CheckersGame game = GameManager.getInstance().createGame(player1, player2, vsAI);
        this.log("Game created: " + game.getId() + " (" + player1 + " vs " + game.getPlayer2() + ")");
        Map<String, Object> resp = new HashMap<>();
        resp.put("gameId", game.getId());
        return HttpResponse.ok(JsonUtil.toJson(resp));
    }

    private HttpResponse getActiveGame(String path) throws Exception {
        String       username = path.split("/")[3];
        CheckersGame game     = GameManager.getInstance().findActiveGameForPlayer(username);

        Map<String, Object> resp = new HashMap<>();
        if (game != null) {
            resp.put("gameId",    game.getId());
            resp.put("gameState", game);
        }
        return HttpResponse.ok(JsonUtil.toJson(resp));
    }

    private HttpResponse getGame(String path) throws Exception {
        String       id   = path.split("/")[2];
        CheckersGame game = GameManager.getInstance().getGame(id);
        if (game == null) return HttpResponse.notFound();
        Map<String, Object> resp = new HashMap<>();
        resp.put("gameState", game);
        return HttpResponse.ok(JsonUtil.toJson(resp));
    }

    private HttpResponse makeMove(String path, HttpRequest req) throws Exception {
        String       id   = path.split("/")[2];
        CheckersGame game = GameManager.getInstance().getGame(id);
        if (game == null) return HttpResponse.notFound();

        JsonNode body   = this.parseBody(req);
        String   player = Router.str(body, "player");
        JsonNode fromN  = body.get("from");
        JsonNode toN    = body.get("to");

        Board.Pos from = new Board.Pos(fromN.get("row").asInt(), fromN.get("col").asInt());
        Board.Pos to   = new Board.Pos(toN.get("row").asInt(),   toN.get("col").asInt());

        Opponent opponent = player.equals("AI")
                ? AiPlayer.getInstance()
                : PlayerRegistry.getInstance().get(player);

        if (opponent == null) return HttpResponse.badRequest("Unknown player: " + player);

        String error;
        synchronized (game) {
            error = game.takeTurn(opponent, from, to);

            if (error == null && game.isVsAI()
                    && game.getStatus() == CheckersGame.Status.ACTIVE
                    && game.getCurrentTurn() == Color.BLACK) {
                game.takeTurn(AiPlayer.getInstance(), null, null);
            }
        }

        if (error != null) return HttpResponse.badRequest(error);

        if (game.getStatus() == CheckersGame.Status.COMPLETED) {
            this.updateStatsForGame(game);
            this.log("Game " + id + " ended. Winner: " + game.getWinner());
        }
        return HttpResponse.ok("{\"success\":true}");
    }

    private HttpResponse sendChat(String path, HttpRequest req) throws Exception {
        String       id      = path.split("/")[2];
        CheckersGame game    = GameManager.getInstance().getGame(id);
        if (game == null) return HttpResponse.notFound();
        JsonNode body    = this.parseBody(req);
        String   player  = Router.str(body, "player");
        String   message = Router.str(body, "message");
        if (message.isBlank()) return HttpResponse.badRequest("Message cannot be blank.");
        game.addChat(player, message);
        return HttpResponse.ok("{\"success\":true}");
    }

    private HttpResponse endGame(String path, HttpRequest req) throws Exception {
        String       id   = path.split("/")[2];
        CheckersGame game = GameManager.getInstance().getGame(id);
        if (game == null) return HttpResponse.notFound();
        String winner = Router.str(this.parseBody(req), "winner");
        game.endGame(winner);
        this.updateStatsForGame(game);
        this.log("Game " + id + " force-ended. Winner: " + winner);
        return HttpResponse.ok("{\"success\":true}");
    }

    private HttpResponse getLogs() throws Exception {
        Map<String, Object> resp = new HashMap<>();
        resp.put("onlineCount", PlayerRegistry.getInstance().getOnlineUsernames().size());
        resp.put("gameCount",   GameManager.getInstance().getAllGames().size());
        return HttpResponse.ok(JsonUtil.toJson(resp));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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
            if (winner.equals("draw"))                    p1.addDraw();
            else if (winner.equals(Color.RED.getValue())) p1.addWin();
            else                                          p1.addLoss();
            PlayerRegistry.getInstance().save(p1);
        }
        if (p2 != null) {
            if (winner.equals("draw"))                      p2.addDraw();
            else if (winner.equals(Color.BLACK.getValue())) p2.addWin();
            else                                            p2.addLoss();
            PlayerRegistry.getInstance().save(p2);
        }
    }

    private void log(String msg) {
        Router.LOG.info(msg);
        if (this.logger != null) this.logger.accept(msg);
    }
}
