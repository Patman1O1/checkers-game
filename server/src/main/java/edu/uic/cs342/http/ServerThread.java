package edu.uic.cs342.http;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerThread extends Thread {

    // ── Subclasses ────────────────────────────────────────────────────────────

    private static class ClientThread extends Thread {

        // ── Fields ────────────────────────────────────────────────────────────

        private final ServerThread server;
        private final Socket       socket;
        private final int          id;

        private ObjectOutputStream outputStream;
        private ObjectInputStream  inputStream;

        // ── Constructors ──────────────────────────────────────────────────────

        private ClientThread(ServerThread server, Socket socket, int id) {
            this.server = server;
            this.socket = socket;
            this.id     = id;
            this.setDaemon(true);
            this.setName("client-" + this.id);
        }

        // ── Methods ───────────────────────────────────────────────────────────

        private void send(HttpResponse response) {
            try {
                this.outputStream.writeObject(response);
                this.outputStream.flush();
            } catch (Exception e) {
                this.server.log("Failed to send response to client #" + this.id + ": " + e.getMessage());
            }
        }

        @Override
        public void run() {
            try {
                this.outputStream = new ObjectOutputStream(this.socket.getOutputStream());
                this.outputStream.flush();
                this.inputStream  = new ObjectInputStream(this.socket.getInputStream());
                this.socket.setTcpNoDelay(true);
            } catch (IOException e) {
                this.server.log("Could not open streams for client #" + this.id + ": " + e.getMessage());
                ServerThread.LOGGER.log(Level.SEVERE, e.getMessage(), e);
                return;
            }

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    HttpRequest  req = (HttpRequest) this.inputStream.readObject();
                    HttpResponse res = this.server.router.handle(req);
                    this.send(res);
                } catch (Exception e) {
                    ServerThread.LOGGER.log(Level.INFO, "Client #" + this.id + " disconnected: " + e.getMessage());
                    break;
                }
            }

            try { this.socket.close(); } catch (IOException ignored) {}
        }
    }

    // ── Fields ────────────────────────────────────────────────────────────────

    private static final Logger LOGGER = Logger.getLogger(ServerThread.class.getName());
    public  static final int    PORT   = 8080;

    private final Router           router;
    private final Consumer<String> logCallback;
    private int                    clientCount = 0;

    // ── Constructors ──────────────────────────────────────────────────────────

    public ServerThread(Consumer<String> logCallback) {
        this.logCallback = logCallback;
        this.router      = new Router(logCallback);
        this.setDaemon(true);
        this.setName("ServerThread");
    }

    // ── Methods ───────────────────────────────────────────────────────────────

    public void stopServer() {
        this.interrupt();
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(ServerThread.PORT)) {
            this.log("Server listening on port " + ServerThread.PORT);
            while (!Thread.currentThread().isInterrupted()) {
                ClientThread clientThread =
                        new ClientThread(this, serverSocket.accept(), ++this.clientCount);
                clientThread.start();
            }
        } catch (Exception e) {
            if (!Thread.currentThread().isInterrupted())
                this.log("Server socket failed: " + e.getMessage());
            ServerThread.LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    private void log(String message) {
        ServerThread.LOGGER.info(message);
        if (this.logCallback != null) this.logCallback.accept(message);
    }
}
