package io.vertx.microservices;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

public class D extends AbstractVerticle {

  @Override
  public void start() throws Exception {
    Router router = Router.router(vertx);

    router.get("/").handler(context -> {
      String param = context.request().getParam("name");
      context.response()
          .putHeader("content-type", "application/json")
          .end(new JsonObject().put("D", "Aloha " + param).encodePrettily());
    });

    vertx.createHttpServer()
        .requestHandler(router::accept)
        .listen(config().getInteger("port"));
  }
}
