package com.example.speedtyping.codecs;

import com.example.speedtyping.models.HeartbeatRequest;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;

public class HeartbeatRequestCodec implements MessageCodec<HeartbeatRequest, HeartbeatRequest> {

  @Override
  public void encodeToWire(Buffer buffer, HeartbeatRequest s) {
    byte[] sessionBytes = s.sessionId().getBytes();
    buffer.appendInt(sessionBytes.length);
    buffer.appendBytes(sessionBytes);
    buffer.appendInt(s.progress());
  }

  @Override
  public HeartbeatRequest decodeFromWire(int pos, Buffer buffer) {
    int _pos = pos;
    int sessionLen = buffer.getInt(_pos);
    _pos += 4;
    String sessionId = new String(buffer.getBytes(_pos, _pos + sessionLen));
    _pos += sessionLen;
    int progress = buffer.getInt(_pos);
    return new HeartbeatRequest(sessionId, progress);
  }

  @Override
  public HeartbeatRequest transform(HeartbeatRequest s) {
    return s;
  }

  @Override
  public String name() {
    return "HeartbeatRequestCodec";
  }

  @Override
  public byte systemCodecID() {
    return -1;
  }
}
