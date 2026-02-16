package com.example.speedtyping.models;

import java.util.List;

public class GameSession {
    private final String sessionId;
    private final String username;
    private final List<String> commands;
    private final long startTime;
    private long lastHeartbeat;
    private int keystrokes;

    public GameSession(String sessionId, String username, List<String> commands, long startTime) {
        this.sessionId = sessionId;
        this.username = username;
        this.commands = commands;
        this.startTime = startTime;
        this.lastHeartbeat = startTime;
        this.keystrokes = 0;
    }

    public String sessionId() { return sessionId; }
    public String username() { return username; }
    public List<String> commands() { return commands; }
    public long startTime() { return startTime; }
    public long lastHeartbeat() { return lastHeartbeat; }
    public int keystrokes() { return keystrokes; }

    public void updateHeartbeat(int keystrokes) {
        this.lastHeartbeat = System.currentTimeMillis();
        this.keystrokes = keystrokes;
    }
}
