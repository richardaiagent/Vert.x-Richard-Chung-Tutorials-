Vert.x의 요청 라우팅 개념 쉽게 이해하기
라우팅이란?
라우팅은 쉽게 말해 "길 안내 시스템" 또는 "교통 정리"라고 생각하면 됩니다.

요청 분류: 클라이언트로부터 들어오는 HTTP 요청을 경로(URL)에 따라 분류합니다.
처리 담당자 연결: 각 경로마다 적절한 핸들러(처리 담당자)에게 요청을 전달합니다.
응답 관리: 요청 처리 후 클라이언트에 적절한 응답을 돌려줍니다.

쉬운 비유로, 라우팅을 '우체국의 분류 시스템'으로 생각해 보세요. 주소지에 따라 우편물(요청)을 적절한 배달부(핸들러)에게 배정합니다.
Vert.x의 Router
Vert.x에서는 Router 객체가 이러한 라우팅 작업을 담당합니다:

Router 생성: HTTP 서버에 들어오는 요청을 처리할 라우터를 생성합니다.
경로 정의: 다양한 URL 패턴과 HTTP 메서드(GET, POST 등)에 대한 경로를 정의합니다.
핸들러 연결: 각 경로에 해당하는 처리 로직(핸들러)을 연결합니다.
서버에 연결: 라우터를 HTTP 서버의 요청 핸들러로 설정합니다.

간단한 작동 절차

라우터 생성: Router.router(vertx) 로 라우터 인스턴스를 생성합니다.
경로 및 핸들러 정의:

GET, POST 등의 HTTP 메서드 매핑
URL 패턴 매핑 (정확한 경로, 패턴 매칭, 정규식 등)
각 경로에 처리할 핸들러 함수 연결


HTTP 서버에 연결: server.requestHandler(router) 로 라우터를 HTTP 서버에 연결합니다.
요청 처리:

클라이언트가 HTTP 요청을 보냄
라우터가 요청의 메서드와 경로를 확인
일치하는 핸들러로 요청을 전달
핸들러가 요청을 처리하고 응답을 생성



실생활 비유로 이해하기
라우팅을 호텔 프런트 데스크로 비유해 보겠습니다:

Router = 프런트 데스크 직원: 호텔에 오는 모든 요청(손님)을 처음 마주하는 사람입니다.
경로(Path) = 요청 종류: "객실 예약", "레스토랑 예약", "수하물 보관" 등 다양한 요청 유형
핸들러(Handler) = 담당 부서: 객실은 객실팀, 레스토랑은 식음료팀 등 각 요청을 처리할 담당자
HTTP 메서드 = 요청 방식: "예약하기(POST)", "조회하기(GET)", "변경하기(PUT)", "취소하기(DELETE)" 등

프런트 데스크 직원(Router)은 손님의 요청 유형과 방식을 확인한 후, 적절한 담당 부서(Handler)로 안내합니다.
간단한 코드 예시
javapublic class MainVerticle extends AbstractVerticle {
  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    // 라우터 생성
    Router router = Router.router(vertx);
    
    // 1. 기본 경로 처리
    router.get("/").handler(ctx -> {
      ctx.response()
        .putHeader("content-type", "text/plain")
        .end("안녕하세요, Vert.x 웹에 오신 것을 환영합니다!");
    });
    
    // 2. API 경로 처리
    router.get("/api/users").handler(this::getAllUsers);     // 모든 사용자 조회
    router.get("/api/users/:id").handler(this::getUserById); // 특정 사용자 조회
    router.post("/api/users").handler(this::createUser);     // 사용자 생성
    
    // 3. 정적 파일 제공
    router.route("/static/*").handler(StaticHandler.create("webroot"));
    
    // 4. 오류 핸들러
    router.route().failureHandler(ctx -> {
      int statusCode = ctx.statusCode();
      ctx.response()
        .setStatusCode(statusCode)
        .putHeader("content-type", "application/json")
        .end("{\"error\": \"오류가 발생했습니다.\", \"code\": " + statusCode + "}");
    });
    
    // HTTP 서버 생성 및 라우터 연결
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
  
  // 핸들러 메서드들...
  private void getAllUsers(RoutingContext ctx) {
    // 모든 사용자 정보를 가져와 JSON 형태로 응답
    ctx.response()
      .putHeader("content-type", "application/json")
      .end("[{\"id\":1,\"name\":\"홍길동\"},{\"id\":2,\"name\":\"김철수\"}]");
  }
  
  private void getUserById(RoutingContext ctx) {
    // URL에서 id 파라미터 추출
    String id = ctx.pathParam("id");
    // id에 해당하는 사용자 정보 응답
    ctx.response()
      .putHeader("content-type", "application/json")
      .end("{\"id\":" + id + ",\"name\":\"사용자" + id + "\"}");
  }
  
  private void createUser(RoutingContext ctx) {
    // 요청 본문에서 사용자 정보 추출
    ctx.request().bodyHandler(buffer -> {
      // 새 사용자 생성 로직...
      // 응답
      ctx.response()
        .setStatusCode(201) // Created
        .putHeader("content-type", "application/json")
        .end("{\"message\":\"사용자가 생성되었습니다.\",\"id\":3}");
    });
  }
}
라우팅의 특별한 기능들
1. 경로 파라미터 (Path Parameters)
경로에 변수를 포함시켜 동적인 라우팅을 할 수 있습니다:
java// /api/products/123 같은 URL에서 123을 추출
router.get("/api/products/:productId").handler(ctx -> {
  String productId = ctx.pathParam("productId");
  // productId를 사용하여 특정 상품 정보 조회
});
2. 쿼리 파라미터 (Query Parameters)
URL의 ? 뒤에 오는 파라미터를 처리할 수 있습니다:
java// /api/search?keyword=노트북&page=2 같은 URL의 파라미터 처리
router.get("/api/search").handler(ctx -> {
  String keyword = ctx.request().getParam("keyword");
  String page = ctx.request().getParam("page", "1"); // 기본값 1
  // 검색 로직...
});
3. 라우트 순서 (Route Order)
라우터는 정의된 순서대로 경로를 확인합니다. 구체적인 경로를 먼저 정의하고 일반적인 경로를 나중에 정의하는 것이 중요합니다:
java// 구체적인 경로 먼저
router.get("/api/special-case").handler(ctx -> { /*...*/ });

// 그 다음 패턴 매칭 경로
router.get("/api/:type").handler(ctx -> { /*...*/ });

// 가장 마지막에 catch-all 경로
router.route("/api/*").handler(ctx -> { /*...*/ });
4. 서브 라우터 (Sub-routers)
큰 애플리케이션에서는 기능별로 라우터를 분리할 수 있습니다:
java// 메인 라우터
Router mainRouter = Router.router(vertx);

// 사용자 관련 서브 라우터
Router userRouter = Router.router(vertx);
userRouter.get("/").handler(this::getAllUsers);
userRouter.get("/:id").handler(this::getUserById);

// 상품 관련 서브 라우터
Router productRouter = Router.router(vertx);
productRouter.get("/").handler(this::getAllProducts);
productRouter.get("/:id").handler(this::getProductById);

// 서브 라우터 마운트
mainRouter.mountSubRouter("/api/users", userRouter);
mainRouter.mountSubRouter("/api/products", productRouter);
미들웨어 개념 (Handler Chain)
Vert.x 라우팅에서는 여러 핸들러를 체인으로 연결할 수 있습니다. 이는 웹 개발에서 미들웨어 패턴이라고 알려져 있습니다:
javarouter.route("/api/*")
  .handler(this::logRequest)        // 1. 요청 로깅
  .handler(this::authenticateUser)  // 2. 사용자 인증
  .handler(this::checkPermissions)  // 3. 권한 확인
  .failureHandler(this::handleError); // 오류 처리

// 각 핸들러에서는 ctx.next()를 호출하여 체인의 다음 핸들러로 진행
private void logRequest(RoutingContext ctx) {
  System.out.println("요청: " + ctx.request().method() + " " + ctx.request().uri());
  ctx.next(); // 중요! 다음 핸들러로 진행
}

private void authenticateUser(RoutingContext ctx) {
  String token = ctx.request().getHeader("Authorization");
  if (token == null || !isValidToken(token)) {
    // 인증 실패 시 에러 응답 후 체인 중단
    ctx.response().setStatusCode(401).end("인증이 필요합니다");
  } else {
    // 사용자 정보 컨텍스트에 추가
    ctx.put("user", getUserFromToken(token));
    ctx.next(); // 인증 성공 시 다음 핸들러로 진행
  }
}
라우팅의 장점

모듈화: 기능별로 코드를 분리하여 유지보수가 쉬워집니다.
가독성: 각 경로와 처리 로직이 명확하게 매핑되어 코드 이해가 쉬워집니다.
유연성: 다양한 HTTP 메서드, URL 패턴, 미들웨어 등을 조합하여 복잡한 API를 구현할 수 있습니다.
확장성: 새로운 기능을 추가할 때 기존 코드를 수정하지 않고 새 라우트만 추가하면 됩니다.

실제 애플리케이션 구조 예시
간단한 블로그 API를 만든다고 가정해 보겠습니다:
/api
  /posts           # 게시글 관련 API
    GET /          # 모든 게시글 목록
    POST /         # 새 게시글 작성
    GET /:id       # 특정 게시글 조회
    PUT /:id       # 게시글 수정
    DELETE /:id    # 게시글 삭제
    
  /comments        # 댓글 관련 API
    GET /:postId   # 특정 게시글의 모든 댓글
    POST /:postId  # 댓글 작성
    
  /users           # 사용자 관련 API
    POST /login    # 로그인
    POST /register # 회원가입
이런 구조는 Vert.x 라우터를 사용하여 깔끔하게 구현할 수 있습니다. 각 기능별로 서브 라우터를 만들고, 적절한 핸들러를 연결하면 됩니다.