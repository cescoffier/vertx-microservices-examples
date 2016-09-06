package io.vertx.microservices;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.types.HttpEndpoint;

public class MainVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class.getName());

  private Record record;
  private ServiceDiscovery discovery;

  @Override
  public void start(Future future) throws Exception {
    DeploymentOptions options = new DeploymentOptions().setConfig(config());
    vertx.deployVerticle(C.class.getName(), options);
    if (!config().getBoolean("openshift", false)) {
      discovery = ServiceDiscovery.create(vertx);
      publishService(future, discovery, "C");
    } else {
      future.complete();
    }
  }

  @Override
  public void stop(Future future) throws Exception {
    if (discovery != null && record != null) {
      discovery.unpublish(record.getRegistration(), ar -> {
        LOGGER.info("C has been un-published");
        future.complete();
      });
    } else {
      future.complete();
    }
  }

  private void publishService(Future future, ServiceDiscovery discovery, String name) {
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
