package com.example.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;

public class HttpServerVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    // 라우터 생성
    Router router = Router.router(vertx);
    
    // API 엔드포인트 설정
    router.get("/api/hello").handler(ctx -> {
      ctx.response()
        .putHeader("content-type", "application/json")
        .end("{\"message\":\"Hello from Vert.x API!\"}");
    });
    
    // 정적 파일 제공
    router.route().handler(StaticHandler.create("webroot"));
    
    // HTTP 서버 생성 및 라우터 연결
    HttpServer server = vertx.createHttpServer();
    server.requestHandler(router)
      .listen(8080, http -> {
        if (http.succeeded()) {
          startPromise.complete();
          System.out.println("HTTP 서버가 포트 8080에서 시작되었습니다");
        } else {
          startPromise.fail(http.cause());
        }
      });
  }
}