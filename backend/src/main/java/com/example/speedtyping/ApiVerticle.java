package com.example.speedtyping;

import com.example.speedtyping.models.*;
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
    router.post("/api/score").handler(this::submitScore); // Keeping for backward compatibility or removing? Plan said remove or deprecate.
    router.get("/api/leaderboard").handler(this::getLeaderboard);

    // New Endpoints
    router.post("/api/game/start").handler(this::startGame);
    router.post("/api/game/heartbeat").handler(this::heartbeat);
    router.post("/api/game/submit").handler(this::submitGame);

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
     // Deprecated - or just return error saying "Please upgrade client"
     ctx.response().setStatusCode(410).end("Endpoint deprecated. Use /api/game/submit");
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

  private void startGame(RoutingContext ctx) {
      JsonObject body = ctx.body().asJsonObject();
      String username = (body != null) ? body.getString("username", "Anonymous") : "Anonymous";

      vertx.eventBus().request("game.start", new GameStartRequest(username))
        .onSuccess(msg -> {
             GameStartResponse resp = (GameStartResponse) msg.body();
             JsonObject json = new JsonObject()
                 .put("sessionId", resp.sessionId())
                 .put("commands", resp.commands())
                 .put("startTime", resp.startTime())
                 .put("signature", resp.signature());
             ctx.json(json);
        })
        .onFailure(err -> ctx.fail(500, err));
  }

  private void heartbeat(RoutingContext ctx) {
      JsonObject body = ctx.body().asJsonObject();
      if (body == null) {
          ctx.response().setStatusCode(400).end("Invalid JSON");
          return;
      }
      String sessionId = body.getString("sessionId");
      int progress = body.getInteger("progress", 0);

      vertx.eventBus().request("game.heartbeat", new HeartbeatRequest(sessionId, progress))
          .onSuccess(msg -> ctx.response().end("OK"))
          .onFailure(err -> {
              // Convert EventBus error to HTTP error
              ctx.response().setStatusCode(400).end(err.getMessage());
          });
  }

  private void submitGame(RoutingContext ctx) {
      JsonObject body = ctx.body().asJsonObject();
      if (body == null) {
          ctx.response().setStatusCode(400).end("Invalid JSON");
          return;
      }

      String sessionId = body.getString("sessionId");
      String username = body.getString("username", "Anonymous");
      String typedText = body.getString("typedText", "");
      // Using server time as finish time
      long finishTime = System.currentTimeMillis();
      String signature = body.getString("signature", "");

      GameSubmitRequest req = new GameSubmitRequest(sessionId, username, typedText, finishTime, signature);

      vertx.eventBus().request("game.submit", req)
          .onSuccess(msg -> ctx.json((JsonObject) msg.body()))
          .onFailure(err -> {
              ctx.response().setStatusCode(400).end(err.getMessage());
          });
  }
}
