package io.vertx.microservices;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonObject;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.rest.ServiceDiscoveryRestEndpoint;
import io.vertx.servicediscovery.types.HttpEndpoint;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;

public class A extends AbstractVerticle {

  private ServiceDiscovery discovery;
  private CircuitBreaker circuitB, circuitC, circuitD;
  private HttpClient clientB, clientC, clientD;

  @Override
  public void start() throws Exception {
    Router router = Router.router(vertx);
    discovery = ServiceDiscovery.create(vertx);
    ServiceDiscoveryRestEndpoint.create(router, discovery);

    CircuitBreakerOptions options = new CircuitBreakerOptions()
        .setMaxFailures(2)
        .setTimeout(2000)
        .setResetTimeout(5000)
        .setFallbackOnFailure(true);

    circuitB = CircuitBreaker.create("B", vertx, options).openHandler(v -> {
      if (clientB != null) {
        clientB.close();
        clientB = null;
      }
    });
    circuitC = CircuitBreaker.create("C", vertx, options).openHandler(v -> {
      if (clientC != null) {
        clientC.close();
        clientC = null;
      }
    });
    ;
    circuitD = CircuitBreaker.create("D", vertx, options).openHandler(v -> {
      if (clientD != null) {
        clientD.close();
        clientD = null;
      }
    });
    ;

    router.route("/assets/*").handler(StaticHandler.create("assets"));
    router.get("/A").handler(this::hello);
    setupSockJsBridge(router);

    vertx.createHttpServer()
        .requestHandler(router::accept)
        .listen(config().getInteger("port"));
  }

  private void hello(RoutingContext context) {
    String param = context.request().getParam("name");

    Future<String> b = Future.future();
    Future<String> c = Future.future();
    Future<String> d = Future.future();

    getB(client -> {
      invoke("B", client, circuitB, param, b);
    });

    getC(client -> {
      invoke("C", client, circuitC, param, c);
    });

    getD(client -> {
      invoke("D", client, circuitD, param, d);
    });

    CompositeFuture.all(b, c, d).setHandler(ar -> {
      JsonObject result = new JsonObject();
      result
          .put("A", "Hello " + param)
          .put("B", b.result())
          .put("C", c.result())
          .put("D", d.result());

      context.response().putHeader("content-type", "application/json").end(result.encodePrettily());
    });
  }

  private void getB(Handler<HttpClient> next) {
    if (clientB != null) {
      next.handle(clientB);
    } else {
      HttpEndpoint.getClient(discovery, new JsonObject().put("name", "B"), ar -> {
        clientB = ar.result();
        next.handle(clientB);
      });
    }
  }

  private void getD(Handler<HttpClient> next) {
    if (clientD != null) {
      next.handle(clientD);
    } else {
      HttpEndpoint.getClient(discovery, new JsonObject().put("name", "D"), ar -> {
        clientD = ar.result();
        next.handle(clientD);
      });
    }
  }

  private void getC(Handler<HttpClient> next) {
    if (clientC != null) {
      next.handle(clientC);
    } else {
      HttpEndpoint.getClient(discovery, new JsonObject().put("name", "C"), ar -> {
        clientC = ar.result();
        next.handle(clientC);
      });
    }
  }

  private void invoke(String name, HttpClient client, CircuitBreaker circuit, String param, Future<String> future) {
    circuit.executeWithFallback(
        circuitFuture -> {
          if (client == null) {
            circuitFuture.fail("No service available");
          } else {
            client.get("/?name=" + param, response -> {
              response.bodyHandler(buffer -> {
                future.complete(buffer.toJsonObject().getString(name));
                circuitFuture.complete();
              });
            }).exceptionHandler(circuitFuture::fail)
              .end();
          }
        },
        v -> {
          // the future has already been completed, a failure or timeout.
          if (!future.isComplete()) future.complete("No service available (fallback)");
          return null;
        }
    );
  }

  private void setupSockJsBridge(Router router) {
    SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
    BridgeOptions options = new BridgeOptions()
        .addOutboundPermitted(
            new PermittedOptions().setAddress("vertx.circuit-breaker"))
        .addInboundPermitted(
            new PermittedOptions().setAddress("vertx.circuit-breaker"));
    sockJSHandler.bridge(options);
    router.route("/eventbus/*").handler(sockJSHandler);
  }
}
