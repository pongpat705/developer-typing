package com.example.speedtyping.codecs;

import com.example.speedtyping.models.GameStartResponse;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import java.util.ArrayList;
import java.util.List;

public class GameStartResponseCodec implements MessageCodec<GameStartResponse, GameStartResponse> {

  @Override
  public void encodeToWire(Buffer buffer, GameStartResponse s) {
    // Session ID
    byte[] sessionBytes = s.sessionId().getBytes();
    buffer.appendInt(sessionBytes.length);
    buffer.appendBytes(sessionBytes);

    // Commands
    List<String> commands = s.commands();
    buffer.appendInt(commands.size());
    for (String cmd : commands) {
      byte[] cmdBytes = cmd.getBytes();
      buffer.appendInt(cmdBytes.length);
      buffer.appendBytes(cmdBytes);
    }

    // Start Time
    buffer.appendLong(s.startTime());

    // Signature
    byte[] sigBytes = s.signature().getBytes();
    buffer.appendInt(sigBytes.length);
    buffer.appendBytes(sigBytes);
  }

  @Override
  public GameStartResponse decodeFromWire(int pos, Buffer buffer) {
    int _pos = pos;

    // Session ID
    int sessionLen = buffer.getInt(_pos);
    _pos += 4;
    String sessionId = new String(buffer.getBytes(_pos, _pos + sessionLen));
    _pos += sessionLen;

    // Commands
    int commandsSize = buffer.getInt(_pos);
    _pos += 4;
    List<String> commands = new ArrayList<>(commandsSize);
    for (int i = 0; i < commandsSize; i++) {
      int cmdLen = buffer.getInt(_pos);
      _pos += 4;
      commands.add(new String(buffer.getBytes(_pos, _pos + cmdLen)));
      _pos += cmdLen;
    }

    // Start Time
    long startTime = buffer.getLong(_pos);
    _pos += 8;

    // Signature
    int sigLen = buffer.getInt(_pos);
    _pos += 4;
    String signature = new String(buffer.getBytes(_pos, _pos + sigLen));
    _pos += sigLen;

    return new GameStartResponse(sessionId, commands, startTime, signature);
  }

  @Override
  public GameStartResponse transform(GameStartResponse s) {
    return s;
  }

  @Override
  public String name() {
    return "GameStartResponseCodec";
  }

  @Override
  public byte systemCodecID() {
    return -1;
  }
}
