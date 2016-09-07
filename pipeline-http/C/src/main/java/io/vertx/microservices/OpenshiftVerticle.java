package io.vertx.microservices;

import io.vertx.core.AbstractVerticle;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.kubernetes.KubernetesServiceImporter;

/**
 * This verticle configure the Vert.x discovery to import services from Kubernetes.
 */
public class OpenshiftVerticle extends AbstractVerticle {

  private ServiceDiscovery discovery;

  @Override
  public void start() throws Exception {
    discovery = ServiceDiscovery
        .create(vertx)
        .registerServiceImporter(new KubernetesServiceImporter(), config());
  }

  @Override
  public void stop() throws Exception {
    discovery.close();
  }
}
