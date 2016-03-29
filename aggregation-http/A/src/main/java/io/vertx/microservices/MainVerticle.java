package io.vertx.microservices;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;


public class MainVerticle extends AbstractVerticle {

  @Override
  public void start() throws Exception {
    DeploymentOptions options = new DeploymentOptions().setConfig(config());
    vertx.deployVerticle(A.class.getName(), options);
    if (config().getBoolean("openshift", false)) {
      vertx.deployVerticle(OpenshiftVerticle.class.getName(), options);
    }
  }
}
