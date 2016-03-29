package io.vertx.microservices;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

public class B extends AbstractVerticle {

  @Override
  public void start() throws Exception {
    Router router = Router.router(vertx);

    router.get("/").handler(context -> {
      String param = context.request().getParam("name");
      context.response()
          .putHeader("content-type", "application/json")
          .end(new JsonObject().put("B", "Hola " + param).encodePrettily());
    });

    vertx.createHttpServer()
        .requestHandler(router::accept)
        .listen(config().getInteger("port"));
  }
}
