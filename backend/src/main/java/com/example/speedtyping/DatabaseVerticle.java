package com.example.speedtyping;

import com.example.speedtyping.models.LeaderboardRequest;
import com.example.speedtyping.models.ScoreSubmission;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.rocksdb.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class DatabaseVerticle extends AbstractVerticle {

  private RocksDB db;
  private BackupEngine backupEngine;
  private final String DB_PATH = "rocksdb_data";
  private final String BACKUP_PATH = "rocksdb_backup";
  private final ObjectMapper mapper = new ObjectMapper();

  // In-memory buffer for leaderboard (Sequenced Collection)
  // Sorted by Total Score descending
  private final NavigableMap<ScoreSubmission, Long> leaderboardBuffer = new TreeMap<>(
      Comparator.comparingInt(this::calculateTotalScore).reversed()
          .thenComparingLong(ScoreSubmission::timestamp)
  );

  private int calculateTotalScore(ScoreSubmission s) {
    return (s.wpm() * 10) + (s.maxCombo() * 5);
  }

  @Override
  public void start(Promise<Void> startPromise) {
    try {
      RocksDB.loadLibrary();
      Options options = new Options().setCreateIfMissing(true);

      restoreIfCorrupted();

      db = RocksDB.open(options, DB_PATH);

      BackupEngineOptions backupOptions = new BackupEngineOptions(BACKUP_PATH);
      backupEngine = BackupEngine.open(Env.getDefault(), backupOptions);

      vertx.eventBus().consumer("save.score", this::handleSaveScore);
      vertx.eventBus().consumer("get.leaderboard", this::handleGetLeaderboard);

      // Periodic flush every 500ms
      vertx.setPeriodic(500, id -> flushBuffer());

      // Daily backup (24 hours)
      vertx.setPeriodic(24L * 60 * 60 * 1000, id -> backup());

      startPromise.complete();
    } catch (Exception e) {
      startPromise.fail(e);
    }
  }

  @Override
  public void stop() {
    if (db != null) db.close();
    if (backupEngine != null) backupEngine.close();
  }

  private void restoreIfCorrupted() {
    File dbDir = new File(DB_PATH);
    boolean corrupted = false;
    if (!dbDir.exists() || !dbDir.isDirectory()) {
        corrupted = true;
    }

    if (corrupted) {
        System.out.println("DB missing or corrupted. Attempting restore from " + BACKUP_PATH);
        try (BackupEngine be = BackupEngine.open(Env.getDefault(), new BackupEngineOptions(BACKUP_PATH))) {
            be.restoreDbFromLatestBackup(DB_PATH, DB_PATH, new RestoreOptions(false));
            System.out.println("Restored from backup.");
        } catch (RocksDBException e) {
            System.out.println("Restore failed or no backup found (this is expected on first run): " + e.getMessage());
        }
    }
  }

  private void backup() {
    try {
      backupEngine.createNewBackup(db);
      System.out.println("Backup created at " + new Date());
    } catch (RocksDBException e) {
      e.printStackTrace();
    }
  }

  private void handleSaveScore(Message<ScoreSubmission> message) {
    ScoreSubmission score = message.body();
    leaderboardBuffer.put(score, System.currentTimeMillis());
    message.reply("Buffered");
  }

  private void flushBuffer() {
    if (leaderboardBuffer.isEmpty()) return;

    try (WriteBatch batch = new WriteBatch()) {
        for (ScoreSubmission s : leaderboardBuffer.keySet()) {
            int totalScore = calculateTotalScore(s);
            // Key format: score:{total_score_padded}:{timestamp}:{username}
            // Padding with 0 to 10 digits handles scores up to 9,999,999,999
            String key = String.format("score:%010d:%d:%s", totalScore, s.timestamp(), s.username());

            try {
                byte[] value = mapper.writeValueAsBytes(s);
                batch.put(key.getBytes(StandardCharsets.UTF_8), value);
            } catch (JsonProcessingException | RocksDBException e) {
                e.printStackTrace();
            }
        }
        db.write(new WriteOptions(), batch);
        leaderboardBuffer.clear();
    } catch (RocksDBException e) {
        e.printStackTrace();
    }
  }

  private void handleGetLeaderboard(Message<LeaderboardRequest> message) {
    int limit = message.body().limit();
    JsonArray results = new JsonArray();

    // Read from DB
    // We assume flush happens frequently enough.
    try (RocksIterator iterator = db.newIterator()) {
        iterator.seekToLast(); // Highest keys (highest scores due to totalScore prefix)

        while (iterator.isValid() && results.size() < limit) {
            String key = new String(iterator.key(), StandardCharsets.UTF_8);
            if (key.startsWith("score:")) {
                byte[] value = iterator.value();
                try {
                    ScoreSubmission s = mapper.readValue(value, ScoreSubmission.class);
                    JsonObject json = new JsonObject()
                        .put("username", s.username())
                        .put("wpm", s.wpm())
                        .put("maxCombo", s.maxCombo())
                        .put("timestamp", s.timestamp())
                        .put("totalScore", calculateTotalScore(s));
                    results.add(json);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            iterator.prev();
        }
    }

    message.reply(results);
  }
}
