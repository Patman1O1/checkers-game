package edu.uic.cs342.project3.backend;

import java.io.Serializable;
import java.util.List;
import java.util.logging.Logger;

public class Player implements Serializable {
    // ── Status ───────────────────────────────────────────────────────────────────────────────────────────────────────
    public static enum Status implements Serializable {
        // ── Enum Constants ───────────────────────────────────────────────────────────────────────────────────────────
        ONLINE,
        OFFLINE
    }

    // ── Fields ───────────────────────────────────────────────────────────────────────────────────────────────────────
    private static final long serialVersionUID = 42L;

    private static final Logger LOGGER = Logger.getLogger(Player.class.getName());

    private String username, password;

    private int wins, losses, draws;

    private Status status;

    private List<Player> friends;

    // ── Constructors ──────────────────────────────────────────────────────────────────────────────────────────────────────


    // ── Methods ──────────────────────────────────────────────────────────────────────────────────────────────────────

}
