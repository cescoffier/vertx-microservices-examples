package io.vertx.microservices;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.types.HttpEndpoint;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class B extends AbstractVerticle {

  private final static Logger LOGGER = LoggerFactory.getLogger(B.class);
  private ServiceDiscovery discovery;
  private CircuitBreaker circuit;
  private HttpClient client;

  @Override
  public void start() throws Exception {
    Router router = Router.router(vertx);
    discovery = ServiceDiscovery.create(vertx);
    circuit = CircuitBreaker.create("B", vertx,
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
      HttpEndpoint.getClient(discovery, new JsonObject().put("name", "C"), ar -> {
        if (ar.failed()) {
          LOGGER.info("Service lookup failure", ar.cause());
        }
        client = ar.result();
        resultHandler.handle(client);
      });
    }
  }

  private void buildResponse(RoutingContext context, String param, Buffer buffer) {
    context.response()
        .putHeader("content-type", "application/json")
        .end(buffer.toJsonObject().put("B", "Hola " + param).encodePrettily());
  }

  private void fallback(RoutingContext context, String param) {
    context.response()
        .putHeader("content-type", "application/json")
        .end(new JsonObject()
            .put("C", "No service available (fallback)")
            .put("B", "Hola " + param).encodePrettily());
  }
}
