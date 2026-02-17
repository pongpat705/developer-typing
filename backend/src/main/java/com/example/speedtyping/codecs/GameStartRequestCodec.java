package com.example.speedtyping.codecs;

import com.example.speedtyping.models.GameStartRequest;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;

public class GameStartRequestCodec implements MessageCodec<GameStartRequest, GameStartRequest> {

  @Override
  public void encodeToWire(Buffer buffer, GameStartRequest s) {
    String username = s.username();
    if (username == null) {
      buffer.appendInt(0);
    } else {
      byte[] bytes = username.getBytes();
      buffer.appendInt(bytes.length);
      buffer.appendBytes(bytes);
    }
  }

  @Override
  public GameStartRequest decodeFromWire(int pos, Buffer buffer) {
    int _pos = pos;
    int length = buffer.getInt(_pos);
    _pos += 4;
    String username = null;
    if (length > 0) {
      byte[] bytes = buffer.getBytes(_pos, _pos + length);
      username = new String(bytes);
      _pos += length;
    }
    return new GameStartRequest(username);
  }

  @Override
  public GameStartRequest transform(GameStartRequest s) {
    return s;
  }

  @Override
  public String name() {
    return "GameStartRequestCodec";
  }

  @Override
  public byte systemCodecID() {
    return -1;
  }
}
