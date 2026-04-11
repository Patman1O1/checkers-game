package edu.uic.cs342.project3.backend;

import java.io.Serializable;

public class Message<T extends Serializable> implements Serializable {
    // ── Type ─────────────────────────────────────────────────────────────────────────────────────────────────────────
    public static enum Type implements Serializable {
        // ── Enum Constants ───────────────────────────────────────────────────────────────────────────────────────────
        SIGNUP,
        LOGIN,
        LOGOUT,
        GET_ONLINE_USERS,
        GET_PLAYER_INFO,
        POST_PLAYER_INFO,
        POST_FRIEND_REQUEST,
        POST_GAME_REQUEST,
        POST_MOVE,
        POST_MESSAGE,
        END_GAME
    }

    // ── Fields ───────────────────────────────────────────────────────────────────────────────────────────────────────
    private final Type type;

    private final String senderName, recipientName;

    private final T content;

    // ── Constructors ─────────────────────────────────────────────────────────────────────────────────────────────────
    public Message(Type type, String senderName, String recipientName, T content) throws NullPointerException {
        if (type == null) {
            throw new NullPointerException("type is null");
        }

        if (senderName == null) {
            throw new NullPointerException("senderName is null");
        }

        if (recipientName == null) {
            throw new NullPointerException("recipientName is null");
        }

        this.type = type;
        this.senderName = senderName;
        this.recipientName = recipientName;
        this.content = content;
    }

    // ── Getters ──────────────────────────────────────────────────────────────────────────────────────────────────────
    public Type getType() { return this.type; }

    public String getSenderName() { return this.senderName; }

    public String getRecipientName() { return this.recipientName; }

    public T getContent() { return this.content; }
}
