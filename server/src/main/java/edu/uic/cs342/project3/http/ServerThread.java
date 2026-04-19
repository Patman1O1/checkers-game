package edu.uic.cs342.project3.http;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerThread extends Thread {
    // ── Client Thread ────────────────────────────────────────────────────────────────────────────────────────────────
    private static class ClientThread extends Thread {
        // ── Fields ───────────────────────────────────────────────────────────────────────────────────────────────────
        private final ServerThread serverThread;

        private final Socket socket;

        private final int id;

        private ObjectOutputStream outputStream;

        private ObjectInputStream inputStream;

        // ── Constructors ─────────────────────────────────────────────────────────────────────────────────────────────
        private ClientThread(ServerThread serverThread, Socket socket, int id) {
            this.serverThread = serverThread;
            this.socket = socket;
            this.id = id;

            // Make this thread terminate if all other threads have terminated
            super.setDaemon(true);

            // Set the name of the thread
            super.setName(String.format("client-#%d", id));
        }

        // ── Methods ──────────────────────────────────────────────────────────────────────────────────────────────────
        private void send(HttpResponse response) {
            try {
                this.outputStream.writeObject(response);
                this.outputStream.flush();
            } catch (IOException exception) {
                String message = String.format("Failed to send response to client #%d: %s", this.id, exception.getMessage());
                ServerThread.LOGGER.log(Level.SEVERE, message, exception);
            }
        }

        @Override
        public void run() {
            try {
                // Set up the output stream to the client
                this.outputStream = new ObjectOutputStream(this.socket.getOutputStream());
                this.outputStream.flush();

                // Set up the input stream from the client
                this.inputStream  = new ObjectInputStream(this.socket.getInputStream());

                this.socket.setTcpNoDelay(true);
            } catch (IOException exception) {
                // Let the calling thread know that I/O streams could not be opened for the client
                this.serverThread.callback.accept(String.format(
                        "Failed to open streams for client #%d because %s was thrown with description \"%s\"",
                        this.id, exception.getClass().getName(), exception.getMessage()
                ));

                // Print the callstack and the exception's type and description to the terminal
                ServerThread.LOGGER.log(Level.SEVERE, exception.getMessage(), exception);
                return;
            }

            // While the client thread does not receive an interrupt signal...
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // Read the request send by the client
                    HttpRequest request = (HttpRequest) this.inputStream.readObject();

                    // Send the request to the HTTP router to be processed
                    HttpResponse response = this.serverThread.router.handleRequest(request);

                    // Send the router's response to the client
                    this.send(response);
                } catch (EOFException exception) {
                    // Let the server know the client has disconnected
                    ServerThread.LOGGER.log(Level.INFO, String.format("Client #%d disconnected", this.id));
                    break;
                } catch (Exception exception) {
                    // Print the callstack and the exception's type and description to the terminal
                    ServerThread.LOGGER.log(Level.SEVERE, exception.getMessage(), exception);
                    break;
                }
            }

            try {
                // Close the connection to the client's socket
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
    public ServerThread(Consumer<String> callback) throws NullPointerException {
        if (callback == null) {
            throw new NullPointerException("callback is null");
        }

        this.callback = callback;
        this.router = new Router(callback);

        // Make this thread terminate when all other threads have terminated
        super.setDaemon(true);

        // Set the name of the thread
        super.setName("server");
    }

    // ── Methods ──────────────────────────────────────────────────────────────────────────────────────────────────────
    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(ServerThread.DEFAULT_PORT)) {
            // Let the calling thread know the server has been started and is listening on the default port number
            this.callback.accept(String.format("Server listening on port %d", ServerThread.DEFAULT_PORT));

            // Send a message to stdout indicating the server has been started and is listening on the default port number
            ServerThread.LOGGER.log(Level.INFO, String.format("Server listening on port %d", ServerThread.DEFAULT_PORT));

            // While the server thread does receive an interrupt signal...
            while (!Thread.currentThread().isInterrupted()) {
                // Accept the client's connection request and spin up a new thread to handle their request
                ClientThread clientThread = new ClientThread(this, serverSocket.accept(), ++this.clientCount);
                clientThread.start();
            }
        } catch (IOException exception) {
            // If the server thread has not received an interrupt signal...
            if (!Thread.currentThread().isInterrupted()) {
                // Let the calling thread know that the server thread failed
                this.callback.accept(String.format("Server socket failed because %s was thrown with description \"%s\"", exception.getClass().getName(), exception.getMessage()));
            }

            // Print the callstack and the exception's type and description to the terminal
            ServerThread.LOGGER.log(Level.SEVERE, exception.getMessage(), exception);
        }
    }
}
