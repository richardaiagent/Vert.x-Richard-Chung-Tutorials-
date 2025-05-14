package com.example.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;

public class EventLoopExample extends AbstractVerticle {

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    System.out.println("EventLoop Verticle 시작됨: " + Thread.currentThread().getName());
    
    // 이벤트 루프에서 실행되는 주기적인 작업
    vertx.setPeriodic(1000, id -> {
      System.out.println("주기적인 작업 실행 중: " + Thread.currentThread().getName());
    });
    
    // 컨텍스트 확인
    System.out.println("Verticle의 컨텍스트 이름: " + context.deploymentID());
    
    // 비차단 방식으로 작업 예약
    vertx.setTimer(2000, id -> {
      System.out.println("2초 후 실행됨: " + Thread.currentThread().getName());
    });
    
    startPromise.complete();
  }
}