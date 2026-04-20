package edu.uic.cs342.project3.http;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerThread extends Thread {
    // ── ClientThread ─────────────────────────────────────────────────────────────────────────────────────────────────
    private static class ClientThread extends Thread {
        // ── Fields ───────────────────────────────────────────────────────────────────────────────────────────────────
        private final ServerThread server;

        private final Socket socket;

        private final int id;

        private ObjectOutputStream outputStream;

        private ObjectInputStream inputStream;

        // ── Constructors ─────────────────────────────────────────────────────────────────────────────────────────────
        private ClientThread(ServerThread server, Socket socket, int id) {
            this.server = server;
            this.socket = socket;
            this.id = id;
            this.setDaemon(true);
            this.setName(String.format("client-%d", this.id));
        }

        // ── Methods ──────────────────────────────────────────────────────────────────────────────────────────────────
        private void send(HttpResponse response) {
            try {
                this.outputStream.writeObject(response);
                this.outputStream.flush();
            } catch (Exception exception) {
                this.server.log(String.format("Failed to send response to client #%d: %s", this.id, exception.getMessage()));
            }
        }

        @Override
        public void run() {
            try {
                this.outputStream = new ObjectOutputStream(this.socket.getOutputStream());
                this.outputStream.flush();
                this.inputStream = new ObjectInputStream(this.socket.getInputStream());
                this.socket.setTcpNoDelay(true);
            } catch (IOException exception) {
                this.server.log(String.format("Could not open streams for client #%d: %s", this.id, exception.getMessage()));
                ServerThread.LOGGER.log(Level.SEVERE, exception.getMessage(), exception);
                return;
            }

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    this.send(this.server.router.handle((HttpRequest) this.inputStream.readObject()));
                } catch (Exception exception) {
                    ServerThread.LOGGER.log(Level.INFO, String.format("Client #%d disconnected: %s", this.id, exception.getMessage()));
                    break;
                }
            }

            try {
                this.socket.close();
            } catch (IOException ignored) {
                // Ignore
            }
        }
    }

    // ── Fields ───────────────────────────────────────────────────────────────────────────────────────────────────────
    private static final Logger LOGGER = Logger.getLogger(ServerThread.class.getName());

    public static final int DEFAULT_PORT = 8080;

    private final Router router;

    private final Consumer<String> callback;

    private int clientCount = 0;

    // ── Constructors ─────────────────────────────────────────────────────────────────────────────────────────────────
    public ServerThread(Consumer<String> callback) {
        this.callback = callback;
        this.router = new Router(callback);
        this.setDaemon(true);
        this.setName("server-thread");
    }

    // ── Methods ──────────────────────────────────────────────────────────────────────────────────────────────────────
    public void stopServer() { this.interrupt(); }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(ServerThread.DEFAULT_PORT)) {
            this.log("Server listening on port " + ServerThread.DEFAULT_PORT);
            while (!Thread.currentThread().isInterrupted()) {
                ClientThread clientThread = new ClientThread(this, serverSocket.accept(), ++this.clientCount);
                clientThread.start();
            }
        } catch (Exception exception) {
            if (!Thread.currentThread().isInterrupted()) {
                this.log(String.format("Server socket failed: %s", exception.getMessage()));
            }
            ServerThread.LOGGER.log(Level.SEVERE, exception.getMessage(), exception);
        }
    }

    private void log(String message) {
        ServerThread.LOGGER.info(message);
        if (this.callback != null) {
            this.callback.accept(message);
        }
    }
}
