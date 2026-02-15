package com.example.speedtyping.codecs;

import com.example.speedtyping.models.ScoreSubmission;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;

public class ScoreSubmissionCodec implements MessageCodec<ScoreSubmission, ScoreSubmission> {

  @Override
  public void encodeToWire(Buffer buffer, ScoreSubmission s) {
    String username = s.username();
    // Handle null username gracefully, though not expected
    if (username == null) {
      buffer.appendInt(0);
    } else {
      byte[] userBytes = username.getBytes();
      buffer.appendInt(userBytes.length);
      buffer.appendBytes(userBytes);
    }
    buffer.appendInt(s.wpm());
    buffer.appendInt(s.maxCombo());
    buffer.appendLong(s.timestamp());
  }

  @Override
  public ScoreSubmission decodeFromWire(int pos, Buffer buffer) {
    int _pos = pos;
    int length = buffer.getInt(_pos);
    _pos += 4;
    String username = "";
    if (length > 0) {
      byte[] userBytes = buffer.getBytes(_pos, _pos + length);
      username = new String(userBytes);
      _pos += length;
    }
    int wpm = buffer.getInt(_pos);
    _pos += 4;
    int maxCombo = buffer.getInt(_pos);
    _pos += 4;
    long timestamp = buffer.getLong(_pos);
    return new ScoreSubmission(username, wpm, maxCombo, timestamp);
  }

  @Override
  public ScoreSubmission transform(ScoreSubmission s) {
    return s;
  }

  @Override
  public String name() {
    return "ScoreSubmissionCodec";
  }

  @Override
  public byte systemCodecID() {
    return -1;
  }
}
