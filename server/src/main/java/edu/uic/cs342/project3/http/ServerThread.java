package edu.uic.cs342.project3.http;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Modelled after ServerThread from the HW5 starter code.
 * Outer thread accepts connections; inner ClientThread handles each client.
 */
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
            setDaemon(true);
            setName("client-" + id);
        }

        // ── Methods ───────────────────────────────────────────────────────────

        private void send(HttpResponse response) {
            try {
                this.outputStream.writeObject(response);
                this.outputStream.flush();
            } catch (Exception e) {
                server.log("Failed to send response to client #" + id + ": " + e.getMessage());
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
                server.log("Could not open streams for client #" + id + ": " + e.getMessage());
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                return;
            }

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    HttpRequest  req = (HttpRequest) this.inputStream.readObject();
                    HttpResponse res = server.router.handle(req);
                    this.send(res);
                } catch (Exception e) {
                    LOGGER.log(Level.INFO, "Client #" + id + " disconnected: " + e.getMessage(), e);
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
        setDaemon(true);
        setName("ServerThread");
    }

    // ── Methods ───────────────────────────────────────────────────────────────

    public void stopServer() {
        interrupt();
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            log("Server listening on port " + PORT);
            while (!Thread.currentThread().isInterrupted()) {
                ClientThread clientThread =
                        new ClientThread(this, serverSocket.accept(), ++clientCount);
                clientThread.start();
            }
        } catch (Exception e) {
            if (!Thread.currentThread().isInterrupted())
                log("Server socket failed: " + e.getMessage());
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    private void log(String message) {
        LOGGER.info(message);
        if (logCallback != null) logCallback.accept(message);
    }
}
