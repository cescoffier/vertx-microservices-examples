package io.vertx.microservices;

import io.vertx.core.AbstractVerticle;
import io.vertx.ext.discovery.DiscoveryService;
import io.vertx.ext.discovery.kubernetes.KubernetesDiscoveryBridge;
import io.vertx.ext.discovery.rest.DiscoveryRestEndpoint;
import io.vertx.ext.web.Router;

public class OpenshiftVerticle extends AbstractVerticle {

  private DiscoveryService discovery;

  @Override
  public void start() throws Exception {
    Router router = Router.router(vertx);
    discovery = DiscoveryService.create(vertx).registerDiscoveryBridge(new KubernetesDiscoveryBridge(), config());
    DiscoveryRestEndpoint.create(router, discovery);
    vertx.createHttpServer()
        .requestHandler(router::accept)
        .listen(config().getInteger("port"));
  }

  @Override
  public void stop() throws Exception {
    discovery.close();
  }
}
