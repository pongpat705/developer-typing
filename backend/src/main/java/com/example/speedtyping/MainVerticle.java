package com.example.speedtyping;

import com.example.speedtyping.codecs.*;
import com.example.speedtyping.models.*;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.core.ThreadingModel;

public class MainVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> startPromise) {
    // Register codecs
    vertx.eventBus().registerDefaultCodec(ScoreSubmission.class, new ScoreSubmissionCodec());
    vertx.eventBus().registerDefaultCodec(LeaderboardRequest.class, new LeaderboardRequestCodec());
    vertx.eventBus().registerDefaultCodec(GameStartRequest.class, new GameStartRequestCodec());
    vertx.eventBus().registerDefaultCodec(GameStartResponse.class, new GameStartResponseCodec());
    vertx.eventBus().registerDefaultCodec(HeartbeatRequest.class, new HeartbeatRequestCodec());
    vertx.eventBus().registerDefaultCodec(GameSubmitRequest.class, new GameSubmitRequestCodec());

    // Deploy verticles
    // Database and FileLoader use Virtual Threads
    DeploymentOptions virtualThreadOptions = new DeploymentOptions().setThreadingModel(ThreadingModel.VIRTUAL_THREAD);

    vertx.deployVerticle(new DatabaseVerticle(), virtualThreadOptions)
      .compose(id -> vertx.deployVerticle(new FileLoaderVerticle(), virtualThreadOptions))
      .compose(id -> vertx.deployVerticle(new GameVerticle()))
      .compose(id -> vertx.deployVerticle(new ApiVerticle()))
      .onSuccess(id -> {
        System.out.println("All verticles deployed successfully");
        startPromise.complete();
      })
      .onFailure(startPromise::fail);
  }
}
