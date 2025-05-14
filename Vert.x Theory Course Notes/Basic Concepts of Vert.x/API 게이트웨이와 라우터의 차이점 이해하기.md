API 게이트웨이와 라우터의 차이점 이해하기
API 게이트웨이란?
API 게이트웨이는 마이크로서비스 아키텍처에서 클라이언트와 백엔드 서비스 사이에 위치하는 중앙 진입점입니다.
쉬운 비유로, API 게이트웨이를 호텔의 대표 전화번호로 생각해 보세요. 손님은 호텔 내 다양한 서비스(객실, 레스토랑, 스파 등)에 각각 전화하지 않고, 하나의 대표 번호로 전화하면 교환원이 적절한 부서로 연결해 줍니다.
API 게이트웨이의 주요 기능

라우팅: 클라이언트 요청을 적절한 백엔드 서비스로 전달
인증 및 인가: 모든 요청에 대한 중앙 집중식 인증/권한 관리
요청/응답 변환: 클라이언트와 백엔드 서비스 간의 데이터 형식 변환
로드 밸런싱: 여러 서비스 인스턴스 간의 부하 분산
캐싱: 자주 요청되는 데이터 임시 저장으로 성능 향상
모니터링/로깅: 모든 API 트래픽의 중앙 집중식 모니터링
속도 제한: 과도한 요청으로부터 서비스 보호
서킷 브레이커: 장애 발생 시 대체 응답 제공

라우터(Router)란?
라우터는 단일 애플리케이션 내에서 HTTP 요청 경로에 따라 적절한 핸들러로 요청을 전달하는 컴포넌트입니다.
쉬운 비유로, 라우터를 건물 내 안내판으로 생각해 보세요. 방문자가 특정 사무실을 찾을 때 안내판을 보고 찾아갑니다.
라우터의 주요 기능

경로 매칭: URL 패턴과 HTTP 메서드에 따라 요청 분류
핸들러 연결: 각 경로에 해당하는 처리 함수 매핑
파라미터 추출: URL에서 경로 변수나 쿼리 파라미터 추출
미들웨어 체인: 여러 처리 단계를 순차적으로 실행

API 게이트웨이 vs 라우터: 핵심 차이점
측면API 게이트웨이라우터범위여러 서비스/애플리케이션 간단일 애플리케이션 내부위치네트워크 경계(외부와 내부 사이)애플리케이션 내부기능 범위라우팅, 인증, 변환, 로드 밸런싱, 캐싱 등주로 경로 기반 요청 분배배포독립적인 서비스로 배포애플리케이션의 일부로 포함예시API 게이트웨이(Spring Cloud Gateway, Kong)Express Router, Vert.x Router
실제 예시로 이해하기
예시 상황: 쇼핑몰 시스템
API 게이트웨이 적용
클라이언트 → [API 게이트웨이] → 
                 ├── 사용자 서비스 (회원 관리)
                 ├── 상품 서비스 (상품 정보)
                 ├── 주문 서비스 (주문 처리)
                 └── 결제 서비스 (결제 처리)
고객이 /api/orders/123에 요청을 보내면:

API 게이트웨이가 요청을 받음
사용자 인증 확인
요청을 주문 서비스의 /orders/123 엔드포인트로 전달
주문 서비스의 응답을 클라이언트에 반환

라우터 적용 (주문 서비스 내부)
java// 주문 서비스 내의 라우터 설정
Router orderRouter = Router.router(vertx);

// 다양한 주문 관련 엔드포인트 정의
orderRouter.get("/orders").handler(this::getAllOrders);
orderRouter.get("/orders/:id").handler(this::getOrderById);
orderRouter.post("/orders").handler(this::createOrder);
orderRouter.put("/orders/:id").handler(this::updateOrder);
Vert.x에서의 API 게이트웨이 구현
Vert.x를 사용하여 간단한 API 게이트웨이를 구현할 수 있습니다:
javapublic class ApiGatewayVerticle extends AbstractVerticle {
  
  @Override
  public void start() {
    Router router = Router.router(vertx);
    
    // 모든 요청에 대한 공통 처리
    router.route().handler(this::logRequest);
    router.route().handler(this::authenticateRequest);
    
    // 각 서비스별 라우팅
    router.route("/api/users/*").handler(this::forwardToUserService);
    router.route("/api/products/*").handler(this::forwardToProductService);
    router.route("/api/orders/*").handler(this::forwardToOrderService);
    
    // 오류 처리
    router.route().failureHandler(this::handleFailure);
    
    // HTTP 서버 시작
    vertx.createHttpServer()
        .requestHandler(router)
        .listen(8080);
  }
  
  private void forwardToUserService(RoutingContext ctx) {
    // 사용자 서비스로 요청 전달
    String targetPath = ctx.request().uri().replace("/api/users", "");
    WebClient client = WebClient.create(vertx);
    
    client.get(8081, "user-service-host", targetPath)
        .send(ar -> {
          if (ar.succeeded()) {
            // 서비스 응답을 클라이언트에 전달
            HttpResponse<Buffer> response = ar.result();
            ctx.response()
                .setStatusCode(response.statusCode())
                .putHeaders(response.headers())
                .end(response.body());
          } else {
            ctx.fail(ar.cause());
          }
        });
  }
  
  // 다른 서비스로의 전달 메서드도 유사하게 구현...
}
왜 둘 다 필요한가?
API 게이트웨이와 라우터는 서로 다른 수준에서 작동하며 상호 보완적입니다:

API 게이트웨이는 여러 마이크로서비스 간의 통신을 관리하고, 외부 세계와 내부 시스템 사이의 경계를 제공합니다.
라우터는 각 마이크로서비스 내부에서 요청을 적절한 코드 경로로 안내합니다.

대규모 시스템에서는:

API 게이트웨이가 큰 도로와 주요 교차로를 관리합니다.
각 서비스 내의 라우터는 특정 구역 내 세부적인 길 안내를 담당합니다.

요약

API 게이트웨이: 여러 마이크로서비스 앞에 위치하여 단일 진입점을 제공하고, 인증, 라우팅, 부하 분산 등 다양한 기능을 제공하는 서비스 수준의 컴포넌트
라우터: 단일 애플리케이션/서비스 내에서 HTTP 요청 경로에 따라 적절한 핸들러로 분배하는 애플리케이션 내부 컴포넌트

둘 다 요청 라우팅을 처리하지만, 범위와 목적이 다릅니다. 마이크로서비스 아키텍처에서는 API 게이트웨이와 각 서비스 내의 라우터를 함께 사용하여 효율적인 요청 처리 시스템을 구축합니다.