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

public class B extends AbstractVerticle {

  private final static Logger LOGGER = LoggerFactory.getLogger(B.class);
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
          .end(new JsonObject().put("B", "Hola " + param).encodePrettily());
    });

    vertx.createHttpServer()
        .requestHandler(router::accept)
        .listen(config().getInteger("port"), ar -> {
          if (ar.succeeded()) {
            publishService(future, "B");
          } else {
            future.fail(ar.cause());
          }
        });
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

  @Override
  public void stop(Future future) throws Exception {
    if (record != null) {
      discovery.unpublish(record.getRegistration(), ar -> {
        LOGGER.info("B has been un-published");
        future.complete();
      });
    }
  }
}
