package com.example.speedtyping;

import com.example.speedtyping.models.*;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameVerticle extends AbstractVerticle {

  private final Map<String, GameSession> sessions = new ConcurrentHashMap<>();
  private final String HMAC_SECRET = System.getenv().getOrDefault("HMAC_SECRET", "dev-secret");
  private static final long SESSION_TIMEOUT = 5 * 60 * 1000; // 5 minutes

  @Override
  public void start(Promise<Void> startPromise) {
    vertx.eventBus().consumer("game.start", this::handleGameStart);
    vertx.eventBus().consumer("game.heartbeat", this::handleHeartbeat);
    vertx.eventBus().consumer("game.submit", this::handleGameSubmit);

    // Cleanup every minute
    vertx.setPeriodic(60000, id -> cleanupSessions());

    startPromise.complete();
  }

  private void handleGameStart(Message<GameStartRequest> message) {
    // Request commands from FileLoader
    vertx.eventBus().request("get.commands", 10).onSuccess(reply -> {
      JsonArray commandsJson = (JsonArray) reply.body();
      List<String> commands = new ArrayList<>();
      for (Object o : commandsJson) {
        commands.add((String) o);
      }

      String sessionId = UUID.randomUUID().toString();
      long startTime = System.currentTimeMillis();
      String username = message.body().username();
      if (username == null) username = "Anonymous";

      GameSession session = new GameSession(sessionId, username, commands, startTime);
      sessions.put(sessionId, session);

      // Sign the session ID
      String signature = sign(sessionId);

      GameStartResponse response = new GameStartResponse(sessionId, commands, startTime, signature);
      message.reply(response);
    }).onFailure(err -> message.fail(500, err.getMessage()));
  }

  private void handleHeartbeat(Message<HeartbeatRequest> message) {
    HeartbeatRequest req = message.body();
    GameSession session = sessions.get(req.sessionId());
    if (session != null) {
      session.updateHeartbeat(req.progress());
      message.reply("OK");
    } else {
      message.fail(404, "Session not found");
    }
  }

  private void handleGameSubmit(Message<GameSubmitRequest> message) {
    GameSubmitRequest req = message.body();
    GameSession session = sessions.get(req.sessionId());

    if (session == null) {
      message.fail(404, "Session not found or expired");
      return;
    }

    // 1. Verify Signature
    // Signature over sessionId + typedText
    String payloadToVerify = req.sessionId() + req.typedText();
    if (!verify(payloadToVerify, req.signature())) {
        message.fail(403, "Invalid signature");
        return;
    }

    // 2. Validate Heartbeat
    long now = System.currentTimeMillis();
    // Use a slightly larger grace period for network latency
    if (now - session.lastHeartbeat() > 15000) {
        message.fail(400, "No heartbeat received recently");
        return;
    }

    // 3. Validate Content (Accuracy)
    // Reconstruct expected text
    StringBuilder expected = new StringBuilder();
    for (String c : session.commands()) expected.append(c);
    String expectedStr = expected.toString();

    // We allow the user to submit whatever they typed, but we calculate accuracy/combo based on it.
    // However, if the text is completely different, we might flag it.
    // For now, let's just use the length and time to calculate WPM.

    // 4. Calculate WPM
    // We use server-side time difference
    long durationMs = req.finishTime() - session.startTime();
    if (durationMs <= 0) durationMs = 1; // Prevent division by zero

    double minutes = durationMs / 60000.0;
    int charsTyped = req.typedText().length();
    int wpm = (int) ((charsTyped / 5.0) / minutes);

    // 5. Sanity Checks
    if (wpm > 400) {
        message.fail(400, "Cheating detected: WPM too high");
        return;
    }

    if (durationMs < 2000) { // 2 seconds
        message.fail(400, "Cheating detected: Impossible time");
        return;
    }

    // Calculate Max Combo
    int maxCombo = calculateMaxCombo(req.typedText(), expectedStr);

    ScoreSubmission submission = new ScoreSubmission(req.username(), wpm, maxCombo, req.finishTime());

    vertx.eventBus().request("save.score", submission).onSuccess(r -> {
        sessions.remove(req.sessionId());
        message.reply(new JsonObject().put("wpm", wpm).put("maxCombo", maxCombo));
    }).onFailure(err -> message.fail(500, err.getMessage()));
  }

  private int calculateMaxCombo(String typed, String expected) {
      int combo = 0;
      int maxCombo = 0;
      int len = Math.min(typed.length(), expected.length());

      for (int i = 0; i < len; i++) {
          if (typed.charAt(i) == expected.charAt(i)) {
              combo++;
              if (combo > maxCombo) maxCombo = combo;
          } else {
              combo = 0;
          }
      }
      return maxCombo;
  }

  private void cleanupSessions() {
    long now = System.currentTimeMillis();
    sessions.entrySet().removeIf(entry -> now - entry.getValue().lastHeartbeat() > SESSION_TIMEOUT);
  }

  private String sign(String data) {
    try {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(HMAC_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        return Base64.getEncoder().encodeToString(sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
        throw new RuntimeException("Failed to sign", e);
    }
  }

  private boolean verify(String data, String signature) {
      return sign(data).equals(signature);
  }
}
