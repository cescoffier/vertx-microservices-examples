package io.vertx.microservices;

import io.vertx.core.AbstractVerticle;
import io.vertx.ext.discovery.DiscoveryService;
import io.vertx.ext.discovery.kubernetes.KubernetesDiscoveryBridge;

/**
 * This verticle configure the Vert.x discovery to import services from Kubernetes.
 */
public class OpenshiftVerticle extends AbstractVerticle {

  private DiscoveryService discovery;

  @Override
  public void start() throws Exception {
    discovery = DiscoveryService
        .create(vertx)
        .registerDiscoveryBridge(new KubernetesDiscoveryBridge(), config());
  }

  @Override
  public void stop() throws Exception {
    discovery.close();
  }
}
