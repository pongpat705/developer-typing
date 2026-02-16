package com.example.speedtyping.codecs;

import com.example.speedtyping.models.GameSubmitRequest;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;

public class GameSubmitRequestCodec implements MessageCodec<GameSubmitRequest, GameSubmitRequest> {

  @Override
  public void encodeToWire(Buffer buffer, GameSubmitRequest s) {
    // SessionId
    byte[] sessionBytes = s.sessionId().getBytes();
    buffer.appendInt(sessionBytes.length);
    buffer.appendBytes(sessionBytes);

    // Username
    byte[] userBytes = s.username().getBytes();
    buffer.appendInt(userBytes.length);
    buffer.appendBytes(userBytes);

    // TypedText
    byte[] textBytes = s.typedText().getBytes();
    buffer.appendInt(textBytes.length);
    buffer.appendBytes(textBytes);

    // FinishTime
    buffer.appendLong(s.finishTime());

    // Signature
    byte[] sigBytes = s.signature().getBytes();
    buffer.appendInt(sigBytes.length);
    buffer.appendBytes(sigBytes);
  }

  @Override
  public GameSubmitRequest decodeFromWire(int pos, Buffer buffer) {
    int _pos = pos;

    // SessionId
    int sessionLen = buffer.getInt(_pos);
    _pos += 4;
    String sessionId = new String(buffer.getBytes(_pos, _pos + sessionLen));
    _pos += sessionLen;

    // Username
    int userLen = buffer.getInt(_pos);
    _pos += 4;
    String username = new String(buffer.getBytes(_pos, _pos + userLen));
    _pos += userLen;

    // TypedText
    int textLen = buffer.getInt(_pos);
    _pos += 4;
    String typedText = new String(buffer.getBytes(_pos, _pos + textLen));
    _pos += textLen;

    // FinishTime
    long finishTime = buffer.getLong(_pos);
    _pos += 8;

    // Signature
    int sigLen = buffer.getInt(_pos);
    _pos += 4;
    String signature = new String(buffer.getBytes(_pos, _pos + sigLen));
    _pos += sigLen;

    return new GameSubmitRequest(sessionId, username, typedText, finishTime, signature);
  }

  @Override
  public GameSubmitRequest transform(GameSubmitRequest s) {
    return s;
  }

  @Override
  public String name() {
    return "GameSubmitRequestCodec";
  }

  @Override
  public byte systemCodecID() {
    return -1;
  }
}
