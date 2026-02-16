package com.example.speedtyping.models;

public record GameSubmitRequest(String sessionId, String username, String typedText, long finishTime, String signature) {}
