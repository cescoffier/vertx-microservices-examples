package io.vertx.microservices;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.discovery.DiscoveryService;
import io.vertx.ext.discovery.Record;
import io.vertx.ext.discovery.types.HttpEndpoint;
import io.vertx.ext.web.Router;

public class C extends AbstractVerticle {

  @Override
  public void start() throws Exception {
    Router router = Router.router(vertx);

    router.get("/").handler(context -> {
      String param = context.request().getParam("name");
      context.response()
          .putHeader("content-type", "application/json")
          .end(new JsonObject().put("C", "Olá " + param).encodePrettily());
    });

    vertx.createHttpServer()
        .requestHandler(router::accept)
        .listen(config().getInteger("port"));
  }
}
