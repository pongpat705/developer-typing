package com.example.speedtyping.codecs;

import com.example.speedtyping.models.LeaderboardRequest;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;

public class LeaderboardRequestCodec implements MessageCodec<LeaderboardRequest, LeaderboardRequest> {

  @Override
  public void encodeToWire(Buffer buffer, LeaderboardRequest s) {
    buffer.appendInt(s.limit());
  }

  @Override
  public LeaderboardRequest decodeFromWire(int pos, Buffer buffer) {
    int _pos = pos;
    int limit = buffer.getInt(_pos);
    return new LeaderboardRequest(limit);
  }

  @Override
  public LeaderboardRequest transform(LeaderboardRequest s) {
    return s;
  }

  @Override
  public String name() {
    return "LeaderboardRequestCodec";
  }

  @Override
  public byte systemCodecID() {
    return -1;
  }
}
