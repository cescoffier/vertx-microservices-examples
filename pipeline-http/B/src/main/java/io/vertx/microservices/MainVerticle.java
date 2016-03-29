package io.vertx.microservices;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.discovery.DiscoveryService;
import io.vertx.ext.discovery.Record;
import io.vertx.ext.discovery.types.HttpEndpoint;


public class MainVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class.getName());

  private Record record;
  private DiscoveryService discovery;

  @Override
  public void start(Future future) throws Exception {
    DeploymentOptions options = new DeploymentOptions().setConfig(config());
    vertx.deployVerticle(B.class.getName(), options);

    if (!config().getBoolean("openshift", false)) {
      discovery = DiscoveryService.create(vertx);
      publishService(future, discovery, "B");
    } else {
      vertx.deployVerticle(OpenshiftVerticle.class.getName(), options);
      future.complete();
    }
  }

  @Override
  public void stop(Future future) throws Exception {
    if (discovery != null && record != null) {
      discovery.unpublish(record.getRegistration(), ar -> {
        LOGGER.info("B has been un-published");
        future.complete();
      });
    } else {
      future.complete();
    }
  }

  private void publishService(Future future, DiscoveryService discovery, String name) {
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
  }
}
