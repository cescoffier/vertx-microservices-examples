package io.vertx.microservices;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.circuitbreaker.CircuitBreaker;
import io.vertx.ext.circuitbreaker.CircuitBreakerOptions;
import io.vertx.ext.discovery.DiscoveryService;
import io.vertx.ext.discovery.kubernetes.KubernetesDiscoveryBridge;
import io.vertx.ext.discovery.rest.DiscoveryRestEndpoint;
import io.vertx.ext.discovery.types.HttpEndpoint;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;

public class A extends AbstractVerticle {

  private DiscoveryService discovery;
  private CircuitBreaker circuit;
  private HttpClient client;

  @Override
  public void start() throws Exception {
    Router router = Router.router(vertx);
    discovery = DiscoveryService.create(vertx);

    circuit = CircuitBreaker.create("A", vertx,
        new CircuitBreakerOptions()
            .setMaxFailures(1)
            .setTimeout(3000)
            .setResetTimeout(5000)
            .setFallbackOnFailure(true))
        .halfOpenHandler(v -> {
          if (client != null) {
            client.close();
            client = null;
          }
        });

    router.route("/assets/*").handler(StaticHandler.create("assets"));
    router.get("/A").handler(this::hello);
    DiscoveryRestEndpoint.create(router, discovery);

    vertx.createHttpServer()
        .requestHandler(router::accept)
        .listen(config().getInteger("port"));
  }

  private void hello(RoutingContext context) {
    getClient(client -> invokeAndReply(client, context));
  }

  private void invokeAndReply(HttpClient client, RoutingContext context) {
    String param = context.request().getParam("name");
    if (client == null) {
      // No record - fallback
      fallback(context, param);
    } else {
      circuit.executeWithFallback(
          future ->
              client.get("/?name=" + param, response -> {
                response.bodyHandler(buffer -> buildResponse(context, param, buffer));
                future.complete();
              })
                  .exceptionHandler(future::fail)
                  .end()
          ,
          v -> {
            fallback(context, param);
            return null;
          }
      );
    }
  }

  private void getClient(Handler<HttpClient> resultHandler) {
    if (client != null) {
      resultHandler.handle(client);
    } else {
      HttpEndpoint.getClient(discovery, new JsonObject().put("name", "B"), ar -> {
        resultHandler.handle(ar.result());
      });
    }
  }

  private void buildResponse(RoutingContext context, String param, Buffer buffer) {
    context.response()
        .putHeader("content-type", "application/json")
        .end(buffer.toJsonObject().put("A", "Hello " + param).encodePrettily());
  }

  private void fallback(RoutingContext context, String param) {
    context.response()
        .putHeader("content-type", "application/json")
        .end(new JsonObject()
            .put("B", "No service available (fallback)")
            .put("A", "Hello " + param).encodePrettily());
  }
}
