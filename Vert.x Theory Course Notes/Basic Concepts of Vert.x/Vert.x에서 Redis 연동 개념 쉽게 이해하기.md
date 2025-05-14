Vert.x에서 Redis 연동 개념 쉽게 이해하기
Redis란?
Redis는 "REmote DIctionary Server"의 약자로, 빠른 속도의 인메모리 데이터 저장소입니다.
쉬운 비유로, Redis를 초고속 디지털 메모장으로 생각해 보세요. 자주 필요한 정보를 빠르게 찾아볼 수 있도록 메모리에 저장해두는 곳입니다.
Redis의 주요 특징

속도: 모든 데이터를 메모리(RAM)에 저장하여 초고속 응답 시간
다양한 데이터 구조: 문자열, 리스트, 해시, 세트, 정렬된 세트 등 지원
영속성: 메모리의 데이터를 디스크에 저장하는 옵션 제공
원자적 연산: 복잡한 작업을 단일 명령으로 처리 가능
pub/sub 메시징: 발행-구독 패턴 기반의 메시지 교환 시스템

Vert.x에서 Redis를 연동하는 이유
1. 캐싱 (Caching)
쉬운 비유: 자주 사용하는 요리 재료를 조리대 위에 두는 것
java// 상품 정보 조회 요청 처리
void getProductInfo(RoutingContext ctx) {
  String productId = ctx.pathParam("id");
  
  // 1. 먼저 Redis에서 확인 (빠른 메모리)
  redisClient.get("product:" + productId, ar -> {
    if (ar.succeeded() && ar.result() != null) {
      // Redis에 캐시된 데이터가 있으면 바로 응답
      ctx.response()
         .putHeader("content-type", "application/json")
         .end(ar.result());
    } else {
      // 2. Redis에 없으면 데이터베이스에서 조회 (느린 디스크)
      databaseClient.query("SELECT * FROM products WHERE id = ?", productId, dbResult -> {
        // 데이터베이스에서 조회한 결과
        String productData = dbResult.result().toJson().encode();
        
        // 3. 다음 요청을 위해 Redis에 캐싱 (유효기간 30분)
        redisClient.setex("product:" + productId, 1800, productData, cacheResult -> {});
        
        // 응답 반환
        ctx.response()
           .putHeader("content-type", "application/json")
           .end(productData);
      });
    }
  });
}
2. 세션 관리 (Session Management)
쉬운 비유: 출입 카드로 건물 접근 권한 관리하기
java// 로그인 처리
void login(RoutingContext ctx) {
  // 사용자 인증 로직...
  String username = ctx.getBodyAsJson().getString("username");
  
  // 인증 성공 시 세션 생성
  String sessionId = UUID.randomUUID().toString();
  
  // Redis에 세션 정보 저장 (30분 유효)
  JsonObject sessionData = new JsonObject()
      .put("username", username)
      .put("loginTime", System.currentTimeMillis())
      .put("permissions", getUserPermissions(username));
  
  redisClient.setex("session:" + sessionId, 1800, sessionData.encode(), ar -> {
    if (ar.succeeded()) {
      // 세션 ID를 쿠키로 반환
      ctx.response()
         .putHeader("Set-Cookie", "sessionId=" + sessionId + "; Path=/; HttpOnly")
         .end("{\"status\":\"success\",\"message\":\"로그인 성공\"}");
    }
  });
}

// 인증 미들웨어
void authenticate(RoutingContext ctx) {
  // 쿠키에서 세션 ID 추출
  String sessionId = getCookieValue(ctx.request(), "sessionId");
  
  if (sessionId != null) {
    // Redis에서 세션 정보 조회
    redisClient.get("session:" + sessionId, ar -> {
      if (ar.succeeded() && ar.result() != null) {
        // 세션 유효 - 사용자 정보를 요청 컨텍스트에 추가
        JsonObject sessionData = new JsonObject(ar.result());
        ctx.put("user", sessionData);
        
        // 세션 만료 시간 갱신 (활동 시 세션 유지)
        redisClient.expire("session:" + sessionId, 1800, refreshResult -> {});
        
        // 다음 핸들러로 진행
        ctx.next();
      } else {
        // 세션 없음 - 인증 실패
        ctx.response().setStatusCode(401).end("{\"error\":\"인증이 필요합니다\"}");
      }
    });
  } else {
    // 세션 ID 없음 - 인증 실패
    ctx.response().setStatusCode(401).end("{\"error\":\"인증이 필요합니다\"}");
  }
}
3. 속도 제한 (Rate Limiting)
쉬운 비유: 놀이공원 입장 횟수 제한하기
java// API 요청 속도 제한 미들웨어
void rateLimit(RoutingContext ctx) {
  String clientIp = ctx.request().remoteAddress().host();
  String key = "ratelimit:" + clientIp;
  
  redisClient.incr(key, ar -> {
    if (ar.succeeded()) {
      long requestCount = ar.result();
      
      // 첫 요청인 경우 만료 시간 설정 (1분)
      if (requestCount == 1) {
        redisClient.expire(key, 60, expireResult -> {});
      }
      
      if (requestCount <= 60) {  // 분당 60개 요청 허용
        // 제한 내 - 다음 핸들러로 진행
        ctx.next();
      } else {
        // 제한 초과 - 429 Too Many Requests 오류
        ctx.response()
           .setStatusCode(429)
           .putHeader("Retry-After", "60")
           .end("{\"error\":\"너무 많은 요청이 발생했습니다. 잠시 후 다시 시도해주세요.\"}");
      }
    } else {
      // Redis 오류 시 기본적으로 허용
      ctx.next();
    }
  });
}
4. 실시간 데이터 공유 (Pub/Sub)
쉬운 비유: 회사 내 방송 시스템으로 전 직원에게 공지하기
java// 채팅 메시지 발행 (발신자)
void sendChatMessage(RoutingContext ctx) {
  String roomId = ctx.pathParam("roomId");
  String message = ctx.getBodyAsJson().getString("message");
  String sender = ctx.get("user").getString("username");
  
  JsonObject chatMessage = new JsonObject()
      .put("sender", sender)
      .put("message", message)
      .put("timestamp", System.currentTimeMillis());
  
  // Redis Pub/Sub으로 채팅방에 메시지 발행
  redisClient.publish("chat:" + roomId, chatMessage.encode(), ar -> {
    ctx.response().end("{\"status\":\"sent\"}");
  });
}

// 채팅 메시지 구독 (WebSocket으로 클라이언트에 전달)
void setupChatSubscription(String roomId, ServerWebSocket webSocket) {
  // Redis Pub/Sub 구독
  redisClient.subscribe("chat:" + roomId, ar -> {
    if (ar.succeeded()) {
      System.out.println("채팅방 " + roomId + " 구독 성공");
    }
  });
  
  // 메시지 수신 핸들러
  redisClient.handler(message -> {
    if (message.getChannel().equals("chat:" + roomId)) {
      // WebSocket으로 클라이언트에 메시지 전달
      webSocket.writeTextMessage(message.getValue());
    }
  });
}
5. 분산 락 (Distributed Lock)
쉬운 비유: 여러 사람이 사용하는 화장실의 '사용 중' 표시
java// 중복 결제 방지를 위한 분산 락
void processPayment(RoutingContext ctx) {
  String orderId = ctx.getBodyAsJson().getString("orderId");
  String lockKey = "lock:order:" + orderId;
  
  // 낙관적 락 구현 (NX: key가 없을 때만 설정, EX: 만료 시간 30초)
  redisClient.set(Arrays.asList(lockKey, "PROCESSING", "NX", "EX", "30"), ar -> {
    if (ar.succeeded() && "OK".equals(ar.result())) {
      try {
        // 락 획득 성공 - 결제 처리 진행
        processPaymentTransaction(orderId, result -> {
          // 처리 완료 후 락 해제
          redisClient.del(lockKey, delResult -> {});
          
          ctx.response().end("{\"status\":\"" + result + "\"}");
        });
      } catch (Exception e) {
        // 오류 발생 시 락 해제
        redisClient.del(lockKey, delResult -> {});
        ctx.fail(e);
      }
    } else {
      // 락 획득 실패 (이미 처리 중)
      ctx.response()
         .setStatusCode(409)  // Conflict
         .end("{\"error\":\"주문이 이미 처리 중입니다\"}");
    }
  });
}
6. 카운터 및 통계 (Counters & Statistics)
쉬운 비유: 방문자 수 카운터로 인기 전시회 확인하기
java// 페이지 조회수 증가 및 통계
void trackPageView(RoutingContext ctx) {
  String page = ctx.request().path();
  
  // 일일 페이지 조회수 증가
  String dailyKey = "stats:pageviews:" + getCurrentDate() + ":" + page;
  redisClient.incr(dailyKey, ar -> {});
  
  // 전체 페이지 조회수 증가
  redisClient.incr("stats:pageviews:total:" + page, ar -> {});
  
  // 실시간 활성 사용자 수 (Set에 사용자 추가)
  String activeUsersKey = "stats:active:users";
  String userId = ctx.get("user") != null ? 
      ctx.get("user").getString("id") : ctx.request().remoteAddress().host();
  
  redisClient.sadd(activeUsersKey, userId, ar -> {});
  redisClient.expire(activeUsersKey, 300, ar -> {});  // 5분 동안 활성으로 간주
  
  // 다음 핸들러로 진행
  ctx.next();
}

// 대시보드에 통계 데이터 제공
void getDashboardStats(RoutingContext ctx) {
  JsonObject stats = new JsonObject();
  
  // 활성 사용자 수 조회
  redisClient.scard("stats:active:users", activeUsers -> {
    if (activeUsers.succeeded()) {
      stats.put("activeUsers", activeUsers.result());
      
      // 오늘의 인기 페이지 조회 (정렬된 집합 사용)
      String today = getCurrentDate();
      redisClient.zrevrange("stats:toppages:" + today, 0, 9, topPages -> {
        if (topPages.succeeded()) {
          stats.put("topPages", new JsonArray(topPages.result()));
          
          // 응답 반환
          ctx.response()
             .putHeader("content-type", "application/json")
             .end(stats.encode());
        }
      });
    }
  });
}
Vert.x와 Redis 연동 방법
1. 의존성 추가 (build.gradle)
gradledependencies {
  // Vert.x 코어 및 웹
  implementation "io.vertx:vertx-core:${vertxVersion}"
  implementation "io.vertx:vertx-web:${vertxVersion}"
  
  // Vert.x Redis 클라이언트
  implementation "io.vertx:vertx-redis-client:${vertxVersion}"
}
2. Redis 클라이언트 설정
javapublic class MainVerticle extends AbstractVerticle {

  private Redis redis;
  
  @Override
  public void start(Promise<Void> startPromise) {
    // Redis 연결 옵션 설정
    RedisOptions options = new RedisOptions()
        .setConnectionString("redis://localhost:6379")  // Redis 서버 주소
        .setMaxPoolSize(8)  // 연결 풀 크기
        .setMaxPoolWaiting(24);  // 대기 큐 크기
    
    // Redis 클라이언트 생성
    Redis.createClient(vertx, options)
        .connect(ar -> {
          if (ar.succeeded()) {
            redis = ar.result();
            System.out.println("Redis 연결 성공");
            
            // 웹 서버 및 라우터 설정
            setupWebServer(startPromise);
          } else {
            System.err.println("Redis 연결 실패: " + ar.cause().getMessage());
            startPromise.fail(ar.cause());
          }
        });
  }
  
  private void setupWebServer(Promise<Void> startPromise) {
    // 라우터 설정
    Router router = Router.router(vertx);
    
    // Redis를 활용한 API 엔드포인트 설정
    router.get("/api/products/:id").handler(this::getProductWithCache);
    router.post("/api/login").handler(this::handleLogin);
    // 기타 라우트...
    
    // HTTP 서버 시작
    vertx.createHttpServer()
        .requestHandler(router)
        .listen(8888, http -> {
          if (http.succeeded()) {
            startPromise.complete();
            System.out.println("HTTP 서버가 8888 포트에서 시작되었습니다.");
          } else {
            startPromise.fail(http.cause());
          }
        });
  }
  
  // Redis 캐시를 활용한 상품 정보 조회
  private void getProductWithCache(RoutingContext ctx) {
    String productId = ctx.pathParam("id");
    String cacheKey = "product:" + productId;
    
    redis.get(cacheKey, ar -> {
      if (ar.succeeded() && ar.result() != null) {
        // 캐시 히트
        ctx.response()
           .putHeader("content-type", "application/json")
           .putHeader("X-Cache", "HIT")
           .end(ar.result().toString());
      } else {
        // 캐시 미스 - 데이터베이스에서 조회 (예시)
        fetchProductFromDatabase(productId, dbResult -> {
          String productData = dbResult.encode();
          
          // Redis에 캐싱 (TTL: 10분)
          redis.setex(cacheKey, 600, productData, cacheResult -> {});
          
          ctx.response()
             .putHeader("content-type", "application/json")
             .putHeader("X-Cache", "MISS")
             .end(productData);
        });
      }
    });
  }
}
Redis 사용 시 고려 사항
1. 메모리 관리
Redis는 메모리 기반이므로 메모리 사용량을 모니터링하고 적절한 키 만료 정책을 설정해야 합니다:
java// 캐싱 시 TTL(Time To Live) 설정
redis.setex("cache:key", 3600, "value", ar -> {});  // 1시간 후 자동 삭제

// 메모리 부족 시 삭제 정책 설정 (Redis 서버 설정)
// maxmemory 1gb
// maxmemory-policy allkeys-lru  # 가장 최근에 사용되지 않은 키부터 삭제
2. 장애 처리
Redis 서버 연결 실패 시 우아하게 대처하는 방법:
java// Redis 작업 실패 시 폴백(fallback) 처리
void getCachedData(String key, Handler<AsyncResult<String>> resultHandler) {
  redis.get(key, ar -> {
    if (ar.succeeded()) {
      resultHandler.handle(Future.succeededFuture(ar.result()));
    } else {
      System.err.println("Redis 오류: " + ar.cause().getMessage());
      // 캐시 없이 진행 (빈 결과로 처리)
      resultHandler.handle(Future.succeededFuture(null));
    }
  });
}
3. 클러스터 구성
고가용성을 위한 Redis 클러스터 설정:
java// Redis 센티널 또는 클러스터 모드 설정
RedisOptions options = new RedisOptions()
    .setType(RedisClientType.SENTINEL)
    .addConnectionString("redis-sentinel://sentinel1:26379")
    .addConnectionString("redis-sentinel://sentinel2:26379")
    .addConnectionString("redis-sentinel://sentinel3:26379")
    .setMasterName("mymaster")
    .setMaxPoolSize(16);
Redis 활용 사례 요약

캐싱: 데이터베이스 부하 감소, 응답 시간 단축
세션 관리: 분산 환경에서의 사용자 세션 저장
속도 제한: API 요청 제한으로 서비스 보호
실시간 메시징: Pub/Sub으로 클라이언트 간 실시간 통신
분산 락: 동시성 문제 해결을 위한 리소스 잠금
카운터/통계: 실시간 방문자 수, 사용 통계 집계
작업 큐: 백그라운드 작업 대기열 관리
지리공간 검색: 위치 기반 검색 서비스

Redis는 Vert.x와 같은 비동기 프레임워크와 결합하면, 높은 동시성과 낮은 지연 시간을 요구하는 현대적인 웹 애플리케이션에 이상적인 보완재가 됩니다.