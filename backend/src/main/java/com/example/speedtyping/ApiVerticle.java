package com.example.speedtyping;

import com.example.speedtyping.models.LeaderboardRequest;
import com.example.speedtyping.models.ScoreSubmission;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;

public class ApiVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> startPromise) {
    Router router = Router.router(vertx);

    router.route().handler(BodyHandler.create());
    router.route().handler(CorsHandler.create()
        .addOrigin("*")
        .allowedMethod(HttpMethod.GET)
        .allowedMethod(HttpMethod.POST)
        .allowedHeader("Content-Type"));

    router.get("/api/commands").handler(this::getCommands);
    router.post("/api/score").handler(this::submitScore);
    router.get("/api/leaderboard").handler(this::getLeaderboard);

    int port = 8080;
    try {
        String httpPort = System.getenv("HTTP_PORT");
        if (httpPort != null) {
            port = Integer.parseInt(httpPort);
        }
    } catch (NumberFormatException e) {
        System.out.println("Invalid HTTP_PORT environment variable, using default 8080");
    }

    vertx.createHttpServer()
      .requestHandler(router)
      .listen(port)
      .onSuccess(server -> {
        System.out.println("HTTP server started on port " + server.actualPort());
        startPromise.complete();
      })
      .onFailure(startPromise::fail);
  }

  private void getCommands(RoutingContext ctx) {
    vertx.eventBus().request("get.commands", 10)
      .onSuccess(msg -> ctx.json(msg.body()))
      .onFailure(err -> ctx.fail(500, err));
  }

  private void submitScore(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();
    if (body == null) {
      ctx.response().setStatusCode(400).end("Invalid JSON");
      return;
    }

    String username = body.getString("username", "Anonymous");
    int wpm = body.getInteger("wpm", 0);
    int maxCombo = body.getInteger("maxCombo", 0);
    long timestamp = System.currentTimeMillis();

    ScoreSubmission submission = new ScoreSubmission(username, wpm, maxCombo, timestamp);

    vertx.eventBus().request("save.score", submission)
      .onSuccess(msg -> ctx.response().setStatusCode(200).end("Score submitted"))
      .onFailure(err -> ctx.fail(500, err));
  }

  private void getLeaderboard(RoutingContext ctx) {
    int limit = 10;
    try {
        String l = ctx.request().getParam("limit");
        if (l != null) limit = Integer.parseInt(l);
    } catch (NumberFormatException e) {}

    vertx.eventBus().request("get.leaderboard", new LeaderboardRequest(limit))
      .onSuccess(msg -> ctx.json(msg.body()))
      .onFailure(err -> ctx.fail(500, err));
  }
}
