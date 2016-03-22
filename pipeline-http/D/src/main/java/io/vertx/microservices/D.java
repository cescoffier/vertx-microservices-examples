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

public class D extends AbstractVerticle {

  private final static Logger LOGGER = LoggerFactory.getLogger(D.class);
  private Record record;
  private DiscoveryService discovery;

  @Override
  public void start(Future future) throws Exception {
    Router router = Router.router(vertx);
    discovery = DiscoveryService.create(vertx);

    router.get("/").handler(context -> {
      String param = context.request().getParam("name");
      context.response()
          .putHeader("content-type", "application/json")
          .end(new JsonObject().put("D", "Aloha " + param).encodePrettily());
    });

    vertx.createHttpServer()
        .requestHandler(router::accept)
        .listen(config().getInteger("port"), v -> {
          publishService(future, "D");
        });
  }

  @Override
  public void stop(Future future) throws Exception {
    if (record != null) {
      discovery.unpublish(record.getRegistration(), ar -> {
        LOGGER.info("D has been un-published");
        future.complete();
      });
    }
  }

  private void publishService(Future future, String name) {
    if (config().getBoolean("publish-service", true)) {
      discovery.publish(HttpEndpoint.createRecord(name, "localhost", config().getInteger("port"), "/"),
          published -> {
            if (published.succeeded()) {
              this.record = published.result();
              LOGGER.info(name + " has been published");
              future.complete();
            } else {
              future.fail("Cannot publish " + name + ": " + published.cause());
            }
          });
    } else {
      future.complete();
    }
  }
}
