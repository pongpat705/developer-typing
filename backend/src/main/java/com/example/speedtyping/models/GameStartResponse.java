package com.example.speedtyping.models;

import java.util.List;

public record GameStartResponse(String sessionId, List<String> commands, long startTime, String signature) {}
