#.소스 폴더 구조: vertx-stage1 프로젝트의 전체 구조를 설명하고 각 파일의 역할을 정의했습니다.
#.데이터 흐름도: 클라이언트, HTTP 서버, Verticle, 이벤트 루프, EventBus 간의 데이터 흐름을 다이어그램으로 표현했습니다.
#.시퀀스 흐름도: HTTP 요청부터 응답까지의 전체 프로세스를 시퀀스 다이어그램으로 시각화했습니다.
#.실제 소스 코드:

Gradle 빌드 파일 (build.gradle, settings.gradle)
MainVerticle.java - 애플리케이션 진입점
EventLoopExample.java - 이벤트 루프 동작 이해를 위한 예제
AsyncExample.java - Future와 Promise를 활용한 비동기 프로그래밍 예제
HttpServerVerticle.java - HTTP 서버 및 라우팅 구현
index.html - 간단한 프론트엔드 페이지


#.README.md: 프로젝트 개요, 실행 방법, 핵심 개념 설명
#.학습 가이드: Vert.x의 핵심 개념(이벤트 루프, Verticle, 비동기 프로그래밍 모델, HTTP 서버 구현)에 대한 상세 설명



#. 실행 : gradlew run




# Vert.x 학습 1단계: 기초 개념 이해

이 프로젝트는 Vert.x 프레임워크의 기본 개념을 이해하기 위한 예제 코드를 제공합니다.

## 핵심 개념

- **이벤트 루프**: Vert.x의 비동기 작업 처리 메커니즘
- **Verticle**: Vert.x 애플리케이션의 기본 구성 요소
- **비동기 프로그래밍 모델**: Future와 Promise를 이용한 비동기 작업 처리

## 프로젝트 구성

- `MainVerticle.java`: 메인 애플리케이션 진입점
- `EventLoopExample.java`: 이벤트 루프 개념 예제
- `AsyncExample.java`: 비동기 프로그래밍 패턴 예제
- `HttpServerVerticle.java`: HTTP 서버와 라우팅 기능 예제

## 실행 방법

```bash
./gradlew run

테스트 방법
브라우저에서 다음 URL 접속:

http://localhost:8080 - 정적 HTML 페이지
http://localhost:8080/api/hello - API 엔드포인트

다음 단계
다음 단계에서는 Vert.x의 EventBus를 통한 통신 방법과 더 복잡한 웹 애플리케이션 구성에 대해 배웁니다.

## Vert.x 기본 개념 학습 가이드

### 1. 이벤트 루프 이해하기

Vert.x는 Node.js와 유사한 이벤트 루프 모델을 사용합니다. 이벤트 루프는 기본적으로 단일 스레드에서 실행되며, 이벤트(요청, 타이머, 콜백 등)를 처리합니다. `EventLoopExample.java`를 통해 이를 학습할 수 있습니다.

주요 특징:
- 비차단(Non-blocking) 작업만 수행
- 장기 실행 작업은 별도의 Worker Verticle에서 실행
- 단일 스레드에서 여러 이벤트 처리

### 2. Verticle 개념 이해하기

Verticle은 Vert.x 애플리케이션의 기본 빌딩 블록입니다. 각 Verticle은 독립적인 컴포넌트로, 자체 이벤트 루프 컨텍스트를 가집니다.

Verticle 종류:
- **Standard Verticle**: 이벤트 루프 스레드에서 실행
- **Worker Verticle**: 워커 풀의 스레드에서 실행 (블로킹 작업에 사용)
- **Multi-threaded Worker Verticle**: 여러 스레드에서 동시에 실행 가능

### 3. 비동기 프로그래밍 모델

Vert.x는 콜백, Future, Promise를 통한 비동기 프로그래밍 모델을 제공합니다. `AsyncExample.java`에서 이러한 패턴을 확인할 수 있습니다.

주요 개념:
- **Future**: 비동기 작업의 결과를 나타내는 객체
- **Promise**: Future 값을 완성하거나 실패시킬 수 있는 쓰기 가능한 객체
- **Handler**: 비동기 작업 완료 시 호출되는 콜백 함수

### 4. HTTP 서버 구현

`HttpServerVerticle.java`는 기본적인 HTTP 서버와 라우팅 구현을 보여줍니다. Vert.x Web 모듈을 사용하여 RESTful API와 정적 파일 제공 방법을 학습할 수 있습니다.

주요 기능:
- Router를 통한 엔드포인트 정의
- Handler를 통한 요청 처리
- 비동기 응답 처리
### ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
폴더구조
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── example/
│       │           └── vertx/
│       │               ├── MainVerticle.java        # 메인 Verticle 클래스
│       │               ├── EventLoopExample.java    # 이벤트 루프 예제
│       │               ├── AsyncExample.java        # 비동기 프로그래밍 예제
│       │               └── HttpServerVerticle.java  # HTTP 서버 예제
│       └── resources/
│           └── webroot/
│               └── index.html                       # 간단한 HTML 페이지
├── build.gradle                                     # Gradle 빌드 설정
├── settings.gradle                                  # Gradle 프로젝트 설정
└── README.md                                        # 프로젝트 설명


데이터 흐름도

┌─────────────┐      ┌─────────────┐      ┌─────────────┐
│   클라이언트   │──────▶│  HTTP 서버   │──────▶│  Verticle   │
│   (브라우저)   │◀─────│  (라우터)    │◀─────│ (비즈니스 로직) │
└─────────────┘      └─────────────┘      └─────────────┘
                           │                      │
                           ▼                      ▼
                     ┌─────────────┐      ┌─────────────┐
                     │  이벤트 루프   │◀────▶│   EventBus  │
                     └─────────────┘      └─────────────┘


시퀀스 흐름도

┌─────────┐          ┌────────────┐          ┌────────────┐
│클라이언트  │          │HTTP 서버    │          │MainVerticle│
└────┬────┘          └────┬───────┘          └─────┬──────┘
     │     HTTP 요청      │                         │
     │─────────────────▶ │                         │
     │                   │                         │
     │                   │     이벤트 발생           │
     │                   │─────────────────────────▶
     │                   │                         │
     │                   │                         │ 비동기 처리
     │                   │                         │◀───────────┐
     │                   │                         │            │
     │                   │     응답 반환            │            │
     │                   │◀────────────────────────┤            │
     │                   │                         │            │
     │    HTTP 응답       │                         │            │
     │◀─────────────────│                         │            │
     │                   │                         │            │
┌────┴────┐          ┌────┴───────┐          ┌─────┴──────┐    │
│클라이언트  │          │HTTP 서버    │          │MainVerticle│    │
└─────────┘          └────────────┘          └────────────┘    │
                                                               │
                                                         ┌─────┴──────┐
                                                         │  이벤트 루프 │
                                                         └────────────┘