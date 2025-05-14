package com.example.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;

public class AsyncExample extends AbstractVerticle {

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    System.out.println("AsyncExample Verticle 시작됨");
    
    // Future 예제
    Future<String> future = getDataAsync();
    
    future.onComplete(ar -> {
      if (ar.succeeded()) {
        System.out.println("데이터 받음: " + ar.result());
      } else {
        System.out.println("오류 발생: " + ar.cause().getMessage());
      }
    });
    
    // Promise 체이닝 예제
    Promise<String> promise = Promise.promise();
    
    promise.future()
      .map(s -> s + " 처리됨")
      .onSuccess(result -> System.out.println("최종 결과: " + result))
      .onFailure(err -> System.out.println("오류: " + err.getMessage()));
    
    // 비동기 작업 시뮬레이션
    vertx.setTimer(1000, id -> {
      promise.complete("비동기 데이터");
    });
    
    startPromise.complete();
  }
  
  private Future<String> getDataAsync() {
    Promise<String> promise = Promise.promise();
    
    // 비동기 작업 시뮬레이션
    vertx.setTimer(1500, id -> {
      double random = Math.random();
      if (random > 0.3) {
        promise.complete("비동기 데이터 성공");
      } else {
        promise.fail("비동기 작업 실패");
      }
    });
    
    return promise.future();
  }
}