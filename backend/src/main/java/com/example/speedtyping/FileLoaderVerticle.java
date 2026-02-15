package com.example.speedtyping;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.eventbus.Message;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class FileLoaderVerticle extends AbstractVerticle {

  private final List<String> commands = new ArrayList<>();

  @Override
  public void start(Promise<Void> startPromise) {
    // Assuming virtual thread deployment, so we can block here
    try {
      Path commandsDir = Paths.get("commands");
      if (Files.exists(commandsDir) && Files.isDirectory(commandsDir)) {
          try (DirectoryStream<Path> stream = Files.newDirectoryStream(commandsDir, "*.txt")) {
              for (Path file : stream) {
                  List<String> lines = Files.readAllLines(file);
                  for (String line : lines) {
                      if (!line.trim().isEmpty()) {
                          commands.add(line.trim());
                      }
                  }
              }
          }
      } else {
          System.out.println("Commands directory not found.");
      }

      vertx.eventBus().consumer("get.commands", this::handleGetCommands);

      startPromise.complete();
    } catch (Exception e) {
      startPromise.fail(e);
    }
  }

  private void handleGetCommands(Message<Integer> message) {
    int count = message.body();
    if (commands.isEmpty()) {
        message.reply(new JsonArray());
        return;
    }

    JsonArray result = new JsonArray();
    for (int i = 0; i < count; i++) {
        int index = ThreadLocalRandom.current().nextInt(commands.size());
        result.add(commands.get(index));
    }
    message.reply(result);
  }
}
