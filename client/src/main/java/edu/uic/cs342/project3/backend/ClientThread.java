package edu.uic.cs342.project3;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.function.Consumer;

class ClientThread extends Thread {
    // ── Fields ───────────────────────────────────────────────────────────────────────────────────────────────────────
    private final InetSocketAddress address;

    private Socket socket;

    private Consumer<Serializable> callback;

    private ObjectInputStream inputStream;

    private ObjectOutputStream outputStream;

    // ── Constructors ─────────────────────────────────────────────────────────────────────────────────────────────────
    public ClientThread(Consumer<Serializable> callback) throws NullPointerException {
        if (callback == null) {
            throw new NullPointerException("callback is null");
        }

        this.address = new InetSocketAddress("127.0.0.1", 5555);

        this.setCallback(callback);
    }

    // ── Setters ──────────────────────────────────────────────────────────────────────────────────────────────────────
    public void setCallback(Consumer<Serializable> callback) throws NullPointerException {
        if (callback == null) {
            throw new NullPointerException("callback is null");
        }
        this.callback = callback;
    }

    // ── Methods ──────────────────────────────────────────────────────────────────────────────────────────────────────
    @Override
    public void run() {

    }
}
