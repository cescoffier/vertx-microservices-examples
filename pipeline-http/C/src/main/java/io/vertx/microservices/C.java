package io.vertx.microservices;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.circuitbreaker.CircuitBreaker;
import io.vertx.ext.circuitbreaker.CircuitBreakerOptions;
import io.vertx.ext.discovery.DiscoveryService;
import io.vertx.ext.discovery.Record;
import io.vertx.ext.discovery.types.HttpEndpoint;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class C extends AbstractVerticle {

  private DiscoveryService discovery;
  private CircuitBreaker circuit;
  private HttpClient client;

  @Override
  public void start() throws Exception {
    Router router = Router.router(vertx);
    discovery = DiscoveryService.create(vertx);
    circuit = CircuitBreaker.create("C", vertx,
        new CircuitBreakerOptions()
            .setMaxFailures(1)
            .setTimeoutInMs(3000)
            .setResetTimeoutInMs(5000)
            .setFallbackOnFailure(true))
        .halfOpenHandler(v -> {
          if (client != null) {
            client.close();
            client = null;
          }
        });

    router.get("/").handler(this::hello);

    vertx.createHttpServer()
        .requestHandler(router::accept)
        .listen(config().getInteger("port"));
  }

  @Override
  public void stop() throws Exception {
    if (client != null) {
      client.close();
    }
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
      circuit.executeAsynchronousCodeWithFallback(
          future ->
              client.get("/?name=" + param, response -> {
                response.bodyHandler(buffer -> buildResponse(context, param, buffer));
                future.complete();
              })
                  .exceptionHandler(future::fail)
                  .end()
          ,
          v -> fallback(context, param)
      );
    }
  }

  private void getClient(Handler<HttpClient> resultHandler) {
    if (client != null) {
      resultHandler.handle(client);
    } else {
      HttpEndpoint.get(vertx, discovery, new JsonObject().put("name", "D"), ar -> {
        client = ar.result();
        resultHandler.handle(client);
      });
    }
  }

  private void buildResponse(RoutingContext context, String param, Buffer buffer) {
    context.response()
        .putHeader("content-type", "application/json")
        .end(buffer.toJsonObject().put("C", "Olá " + param).encodePrettily());
  }

  private void fallback(RoutingContext context, String param) {
    context.response()
        .putHeader("content-type", "application/json")
        .end(new JsonObject()
            .put("D", "No service available (fallback)")
            .put("C", "Olá " + param).encodePrettily());
  }
}
