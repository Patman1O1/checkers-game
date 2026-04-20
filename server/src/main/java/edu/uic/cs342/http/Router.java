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
import java.util.logging.Level;
import java.util.logging.Logger;

public class Router {
    // ── Fields ───────────────────────────────────────────────────────────────────────────────────────────────────────
    private static final Logger LOG = Logger.getLogger(Router.class.getName());

    private final Consumer<String> callback;

    // ── Constructors ─────────────────────────────────────────────────────────────────────────────────────────────────
    public Router(Consumer<String> callback) { this.callback = callback; }

    // ── Methods ──────────────────────────────────────────────────────────────────────────────────────────────────────
    private HttpResponse signup(HttpRequest request) {
        JsonNode body = this.parseBody(request);

        String username = Router.str(body, "username");
        String password = Router.str(body, "password");

        if (username.isBlank() || password.isBlank()) {
            return HttpResponse.badRequest("Username and password are required.");
        }

        if (username.length() < 3 || username.length() > 20) {
            return HttpResponse.badRequest("Username must be 3-20 characters.");
        }

        if (username.length() < 6) {
            return HttpResponse.badRequest("Password must be at least 6 characters.");

        }

        if (PlayerRegistry.getInstance().usernameExists(username)) {
            return HttpResponse.conflict("Username already taken.");
        }

        PlayerRegistry.getInstance().save(new Player(username, password));
        this.log(String.format("New account registered: %s", username));
        return HttpResponse.ok(String.format("{\"success\":true,\"username\":\"%s\"}", username));
    }

    private HttpResponse login(HttpRequest request) {
        JsonNode body = this.parseBody(request);
        String username = Router.str(body, "username");
        String password = Router.str(body, "password");

        Player player = PlayerRegistry.getInstance().get(username);
        if (player == null || !player.checkPassword(password)) {
            return HttpResponse.unauthorized("Invalid username or password.");
        }

        player.setStatus(Player.Status.ONLINE);
        PlayerRegistry.getInstance().setOnline(username);
        this.log(String.format("Login: %s", username));

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("username", player.getUsername());
        response.put("stats", Router.statsMap(player));
        return HttpResponse.ok(JsonUtil.toJson(response));
    }

    private HttpResponse logout(HttpRequest request) {
        String username = Router.str(this.parseBody(request), "username");
        PlayerRegistry.getInstance().setOffline(username);
        PlayerRegistry.getInstance().removeChallengeForTarget(username);
        Player player = PlayerRegistry.getInstance().get(username);
        if (player != null) {
            player.setStatus(Player.Status.OFFLINE);
        }
        this.log(String.format("Logout: %s", username));
        return HttpResponse.ok("{\"success\":true}");
    }

    private HttpResponse onlineUsers() {
        Map<String, Object> response = new HashMap<>();
        response.put("users", PlayerRegistry.getInstance().getOnlineUsernames());
        return HttpResponse.ok(JsonUtil.toJson(response));
    }

    private HttpResponse getUserStats(String path) {
        String username = path.split("/")[2];
        Player player = PlayerRegistry.getInstance().get(username);
        if (player == null) {
            return HttpResponse.notFound();
        }
        Map<String, Object> response = new HashMap<>();
        response.put("stats", Router.statsMap(player));
        return HttpResponse.ok(JsonUtil.toJson(response));
    }

    private HttpResponse updateStats(String path, HttpRequest req) {
        String username = path.split("/")[2];
        String result = Router.str(this.parseBody(req), "result");
        Player player = PlayerRegistry.getInstance().get(username);
        if (player == null) {
            return HttpResponse.notFound();
        }
        switch (result) {
            case "win": {
                player.addWin();
                break;
            }

            case "loss": {
                player.addLoss();
                break;
            }

            case "draw": {
                player.addDraw();
                break;
            }
        }
        PlayerRegistry.getInstance().save(player);
        return HttpResponse.ok("{\"success\":true}");
    }

    private HttpResponse sendFriendRequest(HttpRequest request) {
        JsonNode body = this.parseBody(request);
        String username  = Router.str(body, "username");
        String targetName = Router.str(body, "targetUsername");
        Player player = PlayerRegistry.getInstance().get(username);
        Player target = PlayerRegistry.getInstance().get(targetName);

        if (player == null) {
            return HttpResponse.notFound();
        }

        if (target == null) {
            return HttpResponse.badRequest(String.format("User: '%s' not found.", targetName));
        }

        if (username.equalsIgnoreCase(targetName)) {
            return HttpResponse.badRequest("You cannot add yourself.");
        }

        if (player.isFriend(targetName)) {
            return HttpResponse.badRequest("You are already friends.");
        }

        if (player.hasSentRequestTo(targetName)) {
            return HttpResponse.badRequest("Friend request already sent.");
        }

        if (player.hasPendingRequestFrom(targetName)) {
            player.removeIncomingRequest(targetName);
            target.removeOutgoingRequest(username);
            player.addFriend(target.getUsername());
            target.addFriend(player.getUsername());
            PlayerRegistry.getInstance().save(player);
            PlayerRegistry.getInstance().save(target);
            this.log(String.format("%s and %s are now friends (auto-accepted).", username, targetName));
            return HttpResponse.ok("{\"success\":true,\"autoAccepted\":true}");
        }

        player.addOutgoingRequest(target.getUsername());
        target.addIncomingRequest(player.getUsername());
        PlayerRegistry.getInstance().save(player);
        PlayerRegistry.getInstance().save(target);
        this.log(String.format("%s sent friend request to %s", username, targetName));
        return HttpResponse.ok("{\"success\":true,\"autoAccepted\":false}");
    }

    private HttpResponse acceptFriendRequest(HttpRequest request) {
        JsonNode body = this.parseBody(request);
        String username = Router.str(body, "username");
        String requesterName = Router.str(body, "requesterUsername");
        Player player = PlayerRegistry.getInstance().get(username);
        Player requester = PlayerRegistry.getInstance().get(requesterName);

        if (player == null || requester == null) {
            return HttpResponse.notFound();
        }

        if (!player.hasPendingRequestFrom(requesterName)) {
            return HttpResponse.badRequest("No pending request from that user.");
        }


        player.removeIncomingRequest(requesterName);
        requester.removeOutgoingRequest(username);
        player.addFriend(requester.getUsername());
        requester.addFriend(player.getUsername());
        PlayerRegistry.getInstance().save(player);
        PlayerRegistry.getInstance().save(requester);

        this.log(String.format("%s accepted friend request from %s", username, requesterName));
        return HttpResponse.ok("{\"success\":true}");
    }

    private HttpResponse declineFriendRequest(HttpRequest request) {
        JsonNode body = this.parseBody(request);
        String username = Router.str(body, "username");
        String requesterName = Router.str(body, "requesterUsername");
        Player player = PlayerRegistry.getInstance().get(username);
        Player requester = PlayerRegistry.getInstance().get(requesterName);

        if (player == null || requester == null) {
            return HttpResponse.notFound();
        }

        player.removeIncomingRequest(requesterName);
        requester.removeOutgoingRequest(username);
        PlayerRegistry.getInstance().save(player);
        PlayerRegistry.getInstance().save(requester);
        this.log(String.format("%s declined friend request from %s", username, requesterName));
        return HttpResponse.ok("{\"success\":true}");
    }

    private HttpResponse getFriends(String path) {
        String username = path.split("/")[2];
        Player player = PlayerRegistry.getInstance().get(username);
        if (player == null) {
            return HttpResponse.notFound();
        }

        List<Map<String, Object>> friendList = new ArrayList<>();
        for (String friendName : player.getFriends()) {
            Player friend = PlayerRegistry.getInstance().get(friendName);
            if (friend == null) {
                continue;
            }

            Map<String, Object> entry = new HashMap<>();
            entry.put("username", friend.getUsername());
            entry.put("isOnline", PlayerRegistry.getInstance().isOnline(friendName));
            entry.put("stats", Router.statsMap(friend));
            friendList.add(entry);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("friends", friendList);
        response.put("incoming", player.getIncomingFriendRequests());
        response.put("outgoing", player.getOutgoingFriendRequests());
        return HttpResponse.ok(JsonUtil.toJson(response));
    }

    private HttpResponse sendChallenge(HttpRequest request) {
        JsonNode body = this.parseBody(request);
        String challenger = Router.str(body, "challenger");
        String target = Router.str(body, "target");

        if (PlayerRegistry.getInstance().get(challenger) == null) {
            return HttpResponse.notFound();
        }



        if (PlayerRegistry.getInstance().get(target) == null) {
            return HttpResponse.badRequest(String.format("User '%s' not found.", target));
        }

        if (!PlayerRegistry.getInstance().isOnline(target)) {
            return HttpResponse.badRequest(String.format("%s is not online.", target));
        }

        PlayerRegistry.getInstance().sendChallenge(challenger, target);
        this.log(String.format("%s challenged %s", challenger, target));
        return HttpResponse.ok("{\"success\":true}");
    }

    private HttpResponse acceptChallenge(HttpRequest request) {
        JsonNode body = this.parseBody(request);
        String accepter = Router.str(body, "accepter");
        String challenger = Router.str(body, "challenger");

        PlayerRegistry.getInstance().removeChallengeForTarget(accepter);

        CheckersGame game = GameManager.getInstance().createGame(challenger, accepter, false);
        this.log(String.format("Challenge accepted: %s (%s vs %s)", game.getId(), challenger, accepter));

        Map<String, Object> response = new HashMap<>();
        response.put("gameId", game.getId());
        return HttpResponse.ok(JsonUtil.toJson(response));
    }

    private HttpResponse declineChallenge(HttpRequest request) {
        JsonNode body = this.parseBody(request);
        String decliner = Router.str(body, "decliner");
        String challenger = Router.str(body, "challenger");

        PlayerRegistry.getInstance().removeChallengeForTarget(decliner);
        this.log(String.format("%s declined challenge from %s", decliner, challenger));
        return HttpResponse.ok("{\"success\":true}");
    }

    private HttpResponse getChallenge(String path) {
        String target = path.split("/")[2];
        String challenger = PlayerRegistry.getInstance().getPendingChallenger(target);

        Map<String, Object> response = new HashMap<>();
        response.put("challenger", challenger);
        return HttpResponse.ok(JsonUtil.toJson(response));
    }

    private HttpResponse createGame(HttpRequest request) {
        JsonNode body = this.parseBody(request);

        String player1 = Router.str(body, "player1");
        boolean vsAI = body.has("vsAI") && body.get("vsAI").asBoolean();
        String player2 = vsAI ? "AI" : Router.str(body, "player2");

        if (player1.isBlank()) {
            return HttpResponse.badRequest("player1 required.");
        }

        CheckersGame game = GameManager.getInstance().createGame(player1, player2, vsAI);
        this.log(String.format("Game created: %s (%s vs %s)", game.getId(), player1, game.getPlayer2()));
        Map<String, Object> response = new HashMap<>();
        response.put("gameId", game.getId());
        return HttpResponse.ok(JsonUtil.toJson(response));
    }

    private HttpResponse getActiveGame(String path) {
        String username = path.split("/")[3];
        CheckersGame game = GameManager.getInstance().findActiveGameForPlayer(username);

        Map<String, Object> response = new HashMap<>();
        if (game != null) {
            response.put("gameId", game.getId());
            response.put("gameState", game);
        }
        return HttpResponse.ok(JsonUtil.toJson(response));
    }

    private HttpResponse getGame(String path) {
        String id   = path.split("/")[2];
        CheckersGame game = GameManager.getInstance().getGame(id);
        if (game == null) {
            return HttpResponse.notFound();
        }
        Map<String, Object> response = new HashMap<>();
        response.put("gameState", game);
        return HttpResponse.ok(JsonUtil.toJson(response));
    }

    private HttpResponse makeMove(String path, HttpRequest request) {
        String id = path.split("/")[2];
        CheckersGame game = GameManager.getInstance().getGame(id);
        if (game == null) {
            return HttpResponse.notFound();
        }

        JsonNode body = this.parseBody(request);
        String player = Router.str(body, "player");
        JsonNode fromN  = body.get("from");
        JsonNode toN = body.get("to");

        Board.Pos from = new Board.Pos(fromN.get("row").asInt(), fromN.get("col").asInt());
        Board.Pos to = new Board.Pos(toN.get("row").asInt(), toN.get("col").asInt());

        Opponent opponent = player.equals("AI")
                ? AiPlayer.getInstance()
                : PlayerRegistry.getInstance().get(player);

        if (opponent == null) {
            return HttpResponse.badRequest("Unknown player: " + player);
        }

        String error;
        synchronized (game) {
            error = game.takeTurn(opponent, from, to);

            if (error == null && game.isVsAI()
                    && game.getStatus() == CheckersGame.Status.ACTIVE
                    && game.getCurrentTurn() == Color.BLACK) {
                game.takeTurn(AiPlayer.getInstance(), null, null);
            }
        }

        if (error != null) {
            return HttpResponse.badRequest(error);
        }

        if (game.getStatus() == CheckersGame.Status.COMPLETED) {
            this.updateStatsForGame(game);
            this.log(String.format("Game %s ended. Winner: %s", id, game.getWinner()));
        }
        return HttpResponse.ok("{\"success\":true}");
    }

    private HttpResponse sendChat(String path, HttpRequest request) {
        String id = path.split("/")[2];
        CheckersGame game = GameManager.getInstance().getGame(id);
        if (game == null) {
            return HttpResponse.notFound();
        }
        JsonNode body = this.parseBody(request);
        String player = Router.str(body, "player");
        String message = Router.str(body, "message");
        if (message.isBlank()) {
            return HttpResponse.badRequest("Message cannot be blank.");
        }
        game.addChat(player, message);
        return HttpResponse.ok("{\"success\":true}");
    }

    private HttpResponse endGame(String path, HttpRequest request) {
        String id = path.split("/")[2];
        CheckersGame game = GameManager.getInstance().getGame(id);
        if (game == null) {
            return HttpResponse.notFound();
        }
        String winner = Router.str(this.parseBody(request), "winner");
        game.endGame(winner);
        this.updateStatsForGame(game);
        this.log(String.format("Game %s force-ended. Winner: %s", id, winner));
        return HttpResponse.ok("{\"success\":true}");
    }

    private HttpResponse getLogs() {
        Map<String, Object> response = new HashMap<>();
        response.put("onlineCount", PlayerRegistry.getInstance().getOnlineUsernames().size());
        response.put("gameCount", GameManager.getInstance().getAllGames().size());
        return HttpResponse.ok(JsonUtil.toJson(response));
    }

    private JsonNode parseBody(HttpRequest request) {
        String body = request.getBody();
        if (body == null || body.isBlank()) {
            return JsonUtil.getMapper().createObjectNode();
        }

        try {
            return JsonUtil.getMapper().readTree(body);
        } catch (Exception exception) {
            return JsonUtil.getMapper().createObjectNode();
        }
    }

    private static String str(JsonNode node, String field) {
        JsonNode n = node.get(field);
        return (n != null && !n.isNull()) ? n.asText("").trim() : "";
    }

    private static Map<String, Object> statsMap(Player player) {
        Map<String, Object> m = new HashMap<>();
        m.put("wins", player.getWins());
        m.put("losses", player.getLosses());
        m.put("draws", player.getDraws());
        return m;
    }

    private void updateStatsForGame(CheckersGame game) {
        String winner = game.getWinner();
        if (winner == null) {
            return;
        }
        Player p1 = PlayerRegistry.getInstance().get(game.getPlayer1());
        Player p2 = game.isVsAI() ? null : PlayerRegistry.getInstance().get(game.getPlayer2());
        if (p1 != null) {
            if (winner.equals("draw")) {
                p1.addDraw();
            } else if (winner.equals(Color.RED.getValue())) {
                p1.addWin();
            } else {
                p1.addLoss();
            }
            PlayerRegistry.getInstance().save(p1);
        }
        if (p2 != null) {
            if (winner.equals("draw")) {
                p2.addDraw();
            } else if (winner.equals(Color.BLACK.getValue())) {
                p2.addWin();
            } else {
                p2.addLoss();
            }
            PlayerRegistry.getInstance().save(p2);
        }
    }

    private void log(String message) {
        Router.LOG.info(message);
        if (this.callback != null) {
            this.callback.accept(message);
        }
    }

    public HttpResponse handle(HttpRequest request) {
        String method = request.getMethod();
        String path = request.getPath();

        this.log(String.format("[%s] %s", method, path));

        try {
            if (path.equals("/auth/signup") && method.equals("POST")) {
                return this.signup(request);
            }
            if (path.equals("/auth/login") && method.equals("POST")) {
                return this.login(request);
            }
            if (path.equals("/auth/logout") && method.equals("POST")) {
                return this.logout(request);
            }

            if (path.equals("/users/online") && method.equals("GET")) {
                return this.onlineUsers();
            }

            if (path.matches("/users/[^/]+/stats")) {
                if (method.equals("GET")) {
                    return this.getUserStats(path);
                }
                if (method.equals("POST")) {
                    return this.updateStats(path, request);
                }
            }

            if (path.equals("/friends/request") && method.equals("POST")) {
                return this.sendFriendRequest(request);
            }

            if (path.equals("/friends/accept") && method.equals("POST")) {
                return this.acceptFriendRequest(request);
            }

            if (path.equals("/friends/decline") && method.equals("POST")) {
                return this.declineFriendRequest(request);
            }

            if (path.matches("/friends/[^/]+") && method.equals("GET")) {
                return this.getFriends(path);
            }

            if (path.equals("/challenge/send") && method.equals("POST")) {
                return this.sendChallenge(request);
            }

            if (path.equals("/challenge/accept") && method.equals("POST")) {
                return this.acceptChallenge(request);
            }

            if (path.equals("/challenge/decline") && method.equals("POST")) {
                return this.declineChallenge(request);
            }

            if (path.matches("/challenge/[^/]+") && method.equals("GET")) {
                return this.getChallenge(path);
            }

            if (path.equals("/game/create") && method.equals("POST")) {
                return this.createGame(request);
            }

            if (path.matches("/game/active/[^/]+") && method.equals("GET")) {
                return this.getActiveGame(path);
            }

            if (path.matches("/game/[^/]+") && method.equals("GET")) {
                return this.getGame(path);
            }

            if (path.matches("/game/[^/]+/move") && method.equals("POST")) {
                return this.makeMove(path, request);
            }

            if (path.matches("/game/[^/]+/chat") && method.equals("POST")) {
                return this.sendChat(path, request);
            }

            if (path.matches("/game/[^/]+/end") && method.equals("POST")) {
                return this.endGame(path, request);
            }

            if (path.equals("/logs") && method.equals("GET")) {
                return this.getLogs();
            }

            return HttpResponse.notFound();

        } catch (Exception exception) {
            Router.LOG.log(Level.SEVERE, exception.getMessage(), exception);
            return HttpResponse.serverError(exception.getMessage() != null ? exception.getMessage() : "Internal error");
        }
    }
}
