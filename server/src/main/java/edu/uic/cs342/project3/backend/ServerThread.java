package edu.uic.cs342.project3.backend;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ServerThread extends Thread {
    // ── ClientThread ──────────────────────────────────────────────────────────
    private class ClientThread extends Thread {
        private final Socket socket;
        private final int id;
        private String username;
        private ObjectOutputStream out;
        private ObjectInputStream in;

        private ClientThread(Socket socket, int id) {
            if (socket == null) throw new NullPointerException("socket is null");
            this.socket = socket;
            this.id = id;
            super.setDaemon(true);
        }

        private void send(Message msg) {
            try {
                out.writeObject(msg);
                out.flush();
            } catch (Exception e) {
                //log(Message.error(username, "Failed to send to client #" + id));
            }
        }

        private void cleanup() {
            removeClient(this);
            if (username != null) {
                //Message leave = Message.leave(username);
                //log(leave);
                //broadcast(leave);
            }
            try { socket.close(); } catch (Exception ignored) {}
        }

        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                in  = new ObjectInputStream(socket.getInputStream());
                socket.setTcpNoDelay(true);
            } catch (IOException e) {
                //log(Message.error(null, String.format("Could not open streams for client #%d", id)));
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                return;
            }

            try {
                // Username handshake
                while (username == null) {

                }

                // Message loop
                while (!Thread.currentThread().isInterrupted()) {
                    try {

                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, e.getMessage(), e);
                        break;
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.INFO, e.getMessage(), e);
            }

            cleanup();
        }
    }

    // ── Fields ───────────────────────────────────────────────────────────────────────────────────────────────────────
    private static final int DEFAULT_PORT = 5555;

    private static final Logger LOGGER = Logger.getLogger(ServerThread.class.getName());

    private final Consumer<Serializable> callback;

    private final List<ClientThread> clientThreads = new ArrayList<>();

    private final Map<String, List<String>> groups = new HashMap<>();

    private int clientCount = 0;

    // ── Constructors ─────────────────────────────────────────────────────────────────────────────────────────────────
    public ServerThread(Consumer<Serializable> callback) throws NullPointerException {
        if (callback == null) {
            throw new NullPointerException("callback is null");
        }
        this.callback = callback;

        // Make this thread a daemon thread
        super.setDaemon(true);
    }

    // ── Registry methods ──────────────────────────────────────────────────────
    private synchronized void addClient(ClientThread ct) {
        clientThreads.add(ct);
    }

    private synchronized void removeClient(ClientThread ct) {
        clientThreads.remove(ct);
    }

    private synchronized boolean isUsernameTaken(String name) {
        return clientThreads.stream().anyMatch(ct -> name.equalsIgnoreCase(ct.username));
    }

    private synchronized List<String> getUsernameList() {
        return clientThreads.stream()
                .filter(ct -> ct.username != null)
                .map(ct -> ct.username)
                .collect(Collectors.toList());
    }

    private synchronized void broadcast(Message message) {
        List<ClientThread> dead = new ArrayList<>();
        for (ClientThread ct : clientThreads) {
            try {
                ct.out.writeObject(message);
                ct.out.flush();
            } catch (Exception e) {
                dead.add(ct);
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }
        }
        clientThreads.removeAll(dead);
    }

    private synchronized void sendTo(String username, Message message) {
        ClientThread recipient = clientThreads.stream()
                .filter(ct -> username.equalsIgnoreCase(ct.username))
                .findFirst().orElse(null);
        if (recipient == null) return;
        try {
            recipient.out.writeObject(message);
            recipient.out.flush();
        } catch (Exception e) {
            clientThreads.remove(recipient);
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    private synchronized void sendToGroup(String groupName, Message message) {
        List<String> members = groups.get(groupName);
        if (members == null) return;
        for (String member : members) sendTo(member, message);
    }

    private synchronized boolean groupExists(String groupName) {
        return groupName != null && groups.containsKey(groupName);
    }

    private synchronized void createGroup(String groupName, List<String> members, String creator) {
        List<String> copy = new ArrayList<>(members);
        if (!copy.contains(creator)) copy.add(creator);
        groups.put(groupName, copy);
    }

    private void log(Message message) {
        callback.accept(message);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private static List<String> parseCsv(String csv) {
        List<String> result = new ArrayList<>();
        if (csv == null || csv.isBlank()) return result;
        for (String part : csv.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isBlank()) result.add(trimmed);
        }
        return result;
    }

    // ── Server loop ───────────────────────────────────────────────────────────
    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(DEFAULT_PORT)) {
            //log(Message.serverInfo(String.format("Server listening on port %d.", DEFAULT_PORT)));
            while (!Thread.currentThread().isInterrupted()) {
                ClientThread ct = new ClientThread(serverSocket.accept(), ++clientCount);
                addClient(ct);
                ct.start();
            }
        } catch (Exception e) {
            if (!Thread.currentThread().isInterrupted())
                //log(Message.error(null, "Server socket failed: " + e.getMessage()));
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }


}