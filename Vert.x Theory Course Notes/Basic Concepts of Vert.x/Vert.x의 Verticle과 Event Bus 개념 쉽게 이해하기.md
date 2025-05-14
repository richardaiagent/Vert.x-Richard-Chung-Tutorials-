Vert.x의 Verticle과 Event Bus 개념 쉽게 이해하기
Verticle이란?
Verticle은 쉽게 말해 "일꾼" 또는 "작업자"라고 생각하면 됩니다.

독립적인 작업 단위: 각 Verticle은 독자적인 일을 하는 작은 모듈입니다.
자체 스레드: 각 Verticle은 자신만의 스레드에서 실행될 수 있습니다.
격리된 환경: 다른 Verticle과 메모리를 공유하지 않습니다.

쉬운 비유로, Verticle을 '공장 내 여러 작업자'로 생각해 보세요. 각 작업자는 자신의 자리에서 독립적으로 일합니다.
Event Bus란?
Event Bus는 쉽게 말해 "메시지 전달 시스템" 또는 "통신 채널"입니다.

메시지 전달: Verticle 간에 메시지를 주고받는 통로
비동기 통신: 메시지를 보내고 기다리지 않고 다른 일을 할 수 있음
주소 기반: 주소(문자열)를 사용해 특정 수신자에게 메시지 전달

쉬운 비유로, Event Bus를 '공장 내 컨베이어 벨트'로 생각해 보세요. 작업자들은 컨베이어 벨트를 통해 물건(메시지)을 다른 작업자에게 전달합니다.
간단한 작동 절차

Verticle 생성: 각 작업을 담당할 Verticle 클래스를 만듭니다.
Verticle 배포: Vert.x가 Verticle 인스턴스를 생성하고 실행합니다.
Event Bus 가져오기: 각 Verticle에서 Event Bus 인스턴스를 가져옵니다.
메시지 수신 등록: 특정 주소에서 메시지를 받을 준비를 합니다.
메시지 송신: 원하는 주소로 메시지를 보냅니다.
처리 및 응답: 메시지를 받은 Verticle이 처리하고 필요시 응답합니다.

실생활 비유로 이해하기
일상생활의 메신저 앱으로 예를 들어보겠습니다:

Verticle = 사람들: 각 사람(Verticle)은 독립적으로 메시지를 보내고 받습니다.
Event Bus = 메신저 플랫폼: 카카오톡이나 라인 같은 메신저 앱이 Event Bus 역할을 합니다.
주소 = 채팅방 ID: 특정 채팅방으로 메시지를 보내면 그 방에 있는 사람들만 메시지를 받습니다.
메시지 송신 유형:

Point-to-Point(send): 1:1 대화처럼 특정 한 수신자에게만 메시지 전송
Publish/Subscribe(publish): 단체 채팅방처럼 특정 주소를 구독한 모든 사람에게 메시지 전송
Request-Response(request): 질문을 하고 답변을 기다리는 것처럼 메시지를 보내고 응답을 기다림



간단한 코드 예시
java// 메시지 수신 Verticle
public class ReceiverVerticle extends AbstractVerticle {
  @Override
  public void start() {
    // Event Bus 가져오기
    EventBus eventBus = vertx.eventBus();
    
    // "news.sports" 주소에서 메시지 수신 등록
    eventBus.consumer("news.sports", message -> {
      System.out.println("스포츠 뉴스 받음: " + message.body());
      
      // 답장 보내기 (선택적)
      message.reply("뉴스 잘 받았습니다!");
    });
  }
}

// 메시지 발신 Verticle
public class SenderVerticle extends AbstractVerticle {
  @Override
  public void start() {
    // Event Bus 가져오기
    EventBus eventBus = vertx.eventBus();
    
    // 정기적으로 스포츠 뉴스 발행
    vertx.setPeriodic(5000, id -> {
      // 방법 1: 메시지 발행(publish) - 모든 구독자에게 전송
      eventBus.publish("news.sports", "한국 축구팀 승리!");
      
      // 방법 2: 메시지 전송(send) - 한 수신자에게만 전송
      eventBus.send("news.sports", "야구 경기 취소됨");
      
      // 방법 3: 요청-응답(request) - 응답을 기다림
      eventBus.request("news.sports", "농구 결과는?", ar -> {
        if (ar.succeeded()) {
          System.out.println("응답 받음: " + ar.result().body());
        }
      });
    });
  }
}
Verticle과 Event Bus의 장점

확장성: 필요에 따라 Verticle 인스턴스를 여러 개 생성할 수 있습니다.
유연한 통신: 다양한 통신 패턴(1:1, 1:N, 요청-응답 등)을 지원합니다.
모듈화: 작은 모듈로 나누어 개발하고 테스트하기 쉽습니다.
내결함성: 한 Verticle이 실패해도 전체 시스템에 영향이 적습니다.

간단한 사용 시나리오
예를 들어 쇼핑몰 애플리케이션을 만든다고 가정해 보겠습니다:

UserVerticle: 사용자 로그인, 프로필 관리 담당
ProductVerticle: 상품 정보 관리 담당
CartVerticle: 장바구니 관리 담당
OrderVerticle: 주문 처리 담당

이들은 Event Bus를 통해 아래와 같이 통신합니다:

사용자가 로그인 → UserVerticle은 인증 후 "user.loggedin" 주소로 메시지 발행
CartVerticle은 "user.loggedin" 메시지를 구독하여 사용자의 장바구니 정보 로드
사용자가 상품을 장바구니에 추가 → CartVerticle은 "product.info" 주소로 상품 정보 요청
ProductVerticle은 "product.info" 메시지를 수신하여 상품 정보 반환
사용자가 주문 → CartVerticle은 "order.new" 주소로 주문 정보 발송
OrderVerticle은 "order.new" 메시지를 수신하여 주문 처리

이처럼 각 Verticle은 자신의 역할만 집중하고, Event Bus를 통해 필요한 정보를 주고받습니다.