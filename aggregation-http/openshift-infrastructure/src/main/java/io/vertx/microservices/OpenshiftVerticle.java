package io.vertx.microservices;

import io.vertx.core.AbstractVerticle;
import io.vertx.ext.discovery.DiscoveryService;
import io.vertx.ext.discovery.bridge.kubernates.KubernatesBridge;

public class OpenshiftVerticle extends AbstractVerticle {

  private DiscoveryService discovery;

  @Override
  public void start() throws Exception {
    discovery = DiscoveryService.create(vertx).registerDiscoveryBridge(new KubernatesBridge(), config());
  }

  @Override
  public void stop() throws Exception {
    discovery.close();
  }
}
