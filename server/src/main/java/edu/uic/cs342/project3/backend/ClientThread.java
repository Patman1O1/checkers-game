package edu.uic.cs342.project3.backend;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientThread extends Thread {
    private static final Logger LOGGER = Logger.getLogger(ClientThread.class.getName());

    private final Socket socket;

    private ObjectOutputStream outputStream;

    private ObjectInputStream inputStream;

    private final int id;

    private String username;

    public ClientThread(Socket socket, int id) throws NullPointerException {
        if (socket == null) {
            throw new NullPointerException("socket is null");
        }
        this.socket = socket;

        this.id = id;

        super.setDaemon(true);
    }

    private void send(Message msg) {
        try {
            outputStream.writeObject(msg);
            outputStream.flush();
        } catch (Exception e) {
            //log(Message.error(username, "Failed to send to client #" + id));
        }
    }

    private void cleanup() {

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
            this.outputStream = new ObjectOutputStream(this.socket.getOutputStream());
            this.inputStream = new ObjectInputStream(this.socket.getInputStream());
            this.socket.setTcpNoDelay(true);
        } catch (IOException exception) {
            //log(Message.error(null, String.format("Could not open streams for client #%d", id)));
            ClientThread.LOGGER.log(Level.SEVERE, exception.getMessage(), exception);
            return;
        }

        try {
            // Username handshake
            while (username == null) {

            }

            // Message loop
            while (!Thread.currentThread().isInterrupted()) {
                try {

                } catch (Exception exception) {
                    ClientThread.LOGGER.log(Level.SEVERE, exception.getMessage(), exception);
                    break;
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.INFO, e.getMessage(), e);
        }

        // Close the connection
        try {
            this.socket.close();
        } catch (IOException ignore) {
            // Ignored
        }
    }
}
