Redis 초기 데이터와 데이터 구조 파악하기
Redis 환경에서 기존 데이터 구조나 초기 데이터를 파악하는 것은 새로운 프로젝트에 참여하거나 문서화가 부족한 시스템을 인계받을 때 중요한 과정입니다. 설계서 없이도 Redis의 데이터를 탐색하고 이해하는 방법을 알려드리겠습니다.
1. Redis CLI를 활용한 데이터 탐색
Redis CLI는 Redis 서버와 상호작용할 수 있는 명령줄 도구입니다.
연결 및 기본 탐색
bash# Redis 서버에 연결
$ redis-cli -h [호스트] -p [포트] -a [비밀번호]

# 모든 키 목록 조회 (주의: 키가 많은 프로덕션 환경에서는 사용 자제)
127.0.0.1:6379> KEYS *

# 패턴으로 키 검색
127.0.0.1:6379> KEYS user:*
127.0.0.1:6379> KEYS session:*
127.0.0.1:6379> KEYS cache:product:*
키 유형 및 정보 파악
bash# 키의 타입 확인
127.0.0.1:6379> TYPE user:1001

# 키의 TTL(Time-To-Live) 확인
127.0.0.1:6379> TTL session:abc123

# 문자열 값 조회
127.0.0.1:6379> GET user:1001

# 해시 데이터 조회
127.0.0.1:6379> HGETALL user:profile:1001

# 리스트 데이터 조회
127.0.0.1:6379> LRANGE user:feed:1001 0 -1

# 집합 데이터 조회
127.0.0.1:6379> SMEMBERS user:friends:1001

# 정렬된 집합 조회
127.0.0.1:6379> ZRANGE leaderboard 0 -1 WITHSCORES
통계 정보 확인
bash# 데이터베이스 크기 확인
127.0.0.1:6379> DBSIZE

# 메모리 사용량 확인
127.0.0.1:6379> INFO memory

# 서버 정보 확인
127.0.0.1:6379> INFO server
2. 데이터 패턴 분석 및 체계화
키 패턴을 분석하여 데이터 구조를 유추할 수 있습니다.
키 네이밍 패턴 분석

콜론(:)으로 구분된 키: 일반적으로 네임스페이스를 의미

user:1001 - 사용자 ID가 1001인 데이터
session:abc123 - 세션 ID가 abc123인 세션 정보
product:category:electronics - 전자제품 카테고리 정보


접두사로 데이터 유형 구분:

cache: - 캐싱된 데이터
lock: - 분산 락 관련 데이터
counter: - 카운터 값
queue: - 작업 대기열



스크립트로 체계적인 분석
아래와 같은 Lua 스크립트를 Redis에서 실행하여 데이터 구조를 체계적으로 분석할 수 있습니다:
lua-- key-patterns.lua: 키 패턴별 개수 분석
local patterns = {}
local cursor = "0"
local count = 0

repeat
    local result = redis.call("SCAN", cursor, "COUNT", 1000)
    cursor = result[1]
    local keys = result[2]
    
    for i, key in ipairs(keys) do
        -- 첫 번째 콜론까지의 패턴 추출
        local pattern = string.match(key, "^([^:]*)")
        if pattern ~= "" then
            patterns[pattern] = (patterns[pattern] or 0) + 1
        end
        count = count + 1
    end
until cursor == "0"

-- 결과 반환
local result = {}
table.insert(result, "총 키 개수: " .. count)
for pattern, patternCount in pairs(patterns) do
    table.insert(result, pattern .. ": " .. patternCount)
end
return result
실행 방법:
bash$ redis-cli --eval key-patterns.lua
3. Vert.x에서 프로그래밍 방식으로 데이터 구조 파악
Vert.x 애플리케이션에서 Redis의 데이터 구조를 파악하기 위한 코드를 작성할 수 있습니다.
javapublic void analyzeRedisStructure(Promise<JsonObject> resultPromise) {
  JsonObject result = new JsonObject();
  
  // 첫 번째 단계: 키 패턴 분석
  analyzeKeyPatterns().compose(keyPatterns -> {
    result.put("keyPatterns", keyPatterns);
    
    // 두 번째 단계: 주요 키 샘플 분석
    return analyzeSampleKeys(keyPatterns);
  }).compose(sampleData -> {
    result.put("sampleData", sampleData);
    
    // 세 번째 단계: 메모리 사용량 분석
    return analyzeMemoryUsage();
  }).onComplete(ar -> {
    if (ar.succeeded()) {
      result.put("memoryUsage", ar.result());
      resultPromise.complete(result);
    } else {
      resultPromise.fail(ar.cause());
    }
  });
}

// 키 패턴 분석
private Future<JsonObject> analyzeKeyPatterns() {
  Promise<JsonObject> promise = Promise.promise();
  JsonObject patterns = new JsonObject();
  
  // SCAN을 사용하여 키 반복 처리
  String cursor = "0";
  scanKeys(cursor, patterns, 0, promise);
  
  return promise.future();
}

// 재귀적으로 SCAN 수행
private void scanKeys(String cursor, JsonObject patterns, int count, Promise<JsonObject> promise) {
  redis.scan(Arrays.asList(cursor, "COUNT", "1000"), scanResult -> {
    if (scanResult.succeeded()) {
      JsonArray result = scanResult.result();
      String nextCursor = result.getString(0);
      JsonArray keys = result.getJsonArray(1);
      
      // 키 패턴 분류
      for (int i = 0; i < keys.size(); i++) {
        String key = keys.getString(i);
        String pattern = extractPattern(key);
        
        if (!patterns.containsKey(pattern)) {
          patterns.put(pattern, new JsonArray());
        }
        
        // 각 패턴별로 10개까지만 샘플 키 저장
        JsonArray patternKeys = patterns.getJsonArray(pattern);
        if (patternKeys.size() < 10) {
          patternKeys.add(key);
        }
      }
      
      // 다음 커서가 0이면 종료, 아니면 재귀 호출
      if ("0".equals(nextCursor)) {
        // 분석 결과 구성
        JsonObject result = new JsonObject();
        result.put("patternCount", new JsonObject());
        
        for (String pattern : patterns.fieldNames()) {
          result.getJsonObject("patternCount").put(pattern, patterns.getJsonArray(pattern).size());
        }
        
        result.put("sampleKeys", patterns);
        result.put("totalScanned", count + keys.size());
        
        promise.complete(result);
      } else {
        scanKeys(nextCursor, patterns, count + keys.size(), promise);
      }
    } else {
      promise.fail(scanResult.cause());
    }
  });
}

// 키에서 패턴 추출 (첫 번째 콜론까지의 부분)
private String extractPattern(String key) {
  int colonIndex = key.indexOf(':');
  return colonIndex > 0 ? key.substring(0, colonIndex) : key;
}

// 샘플 키의 데이터 분석
private Future<JsonObject> analyzeSampleKeys(JsonObject keyPatterns) {
  Promise<JsonObject> promise = Promise.promise();
  JsonObject sampleData = new JsonObject();
  
  // 각 패턴별로 최대 3개 키의 샘플 데이터 분석
  List<Future> futures = new ArrayList<>();
  
  for (String pattern : keyPatterns.getJsonObject("sampleKeys").fieldNames()) {
    JsonArray keys = keyPatterns.getJsonObject("sampleKeys").getJsonArray(pattern);
    
    // 패턴별로 최대 3개 키만 분석
    int samplesToAnalyze = Math.min(keys.size(), 3);
    
    for (int i = 0; i < samplesToAnalyze; i++) {
      String key = keys.getString(i);
      futures.add(analyzeSingleKey(key).compose(keyData -> {
        if (!sampleData.containsKey(pattern)) {
          sampleData.put(pattern, new JsonArray());
        }
        sampleData.getJsonArray(pattern).add(keyData);
        return Future.succeededFuture();
      }));
    }
  }
  
  // 모든 샘플 키 분석이 완료되면 결과 반환
  CompositeFuture.all(futures).onComplete(ar -> {
    if (ar.succeeded()) {
      promise.complete(sampleData);
    } else {
      promise.fail(ar.cause());
    }
  });
  
  return promise.future();
}

// 단일 키 분석
private Future<JsonObject> analyzeSingleKey(String key) {
  Promise<JsonObject> promise = Promise.promise();
  
  // 키 타입 확인
  redis.type(key, typeResult -> {
    if (typeResult.succeeded()) {
      String type = typeResult.result();
      
      JsonObject keyData = new JsonObject()
          .put("key", key)
          .put("type", type);
      
      // TTL 확인
      redis.ttl(key, ttlResult -> {
        if (ttlResult.succeeded()) {
          keyData.put("ttl", ttlResult.result());
          
          // 타입별 데이터 샘플 확인
          switch (type.toLowerCase()) {
            case "string":
              redis.get(key, valueResult -> {
                if (valueResult.succeeded()) {
                  String value = valueResult.result();
                  keyData.put("length", value != null ? value.length() : 0);
                  
                  // JSON 형식인지 확인
                  try {
                    new JsonObject(value);
                    keyData.put("format", "json");
                  } catch (Exception e) {
                    keyData.put("format", "string");
                  }
                  
                  // 값 미리보기 (최대 100자)
                  if (value != null && value.length() > 100) {
                    keyData.put("preview", value.substring(0, 100) + "...");
                  } else {
                    keyData.put("preview", value);
                  }
                }
                promise.complete(keyData);
              });
              break;
              
            case "hash":
              redis.hgetall(key, hashResult -> {
                if (hashResult.succeeded()) {
                  JsonObject hash = hashResult.result();
                  keyData.put("fields", hash.fieldNames());
                  keyData.put("size", hash.size());
                  
                  // 일부 필드 미리보기
                  JsonObject preview = new JsonObject();
                  int count = 0;
                  for (String field : hash.fieldNames()) {
                    if (count < 5) {
                      preview.put(field, hash.getValue(field));
                      count++;
                    } else {
                      break;
                    }
                  }
                  keyData.put("preview", preview);
                }
                promise.complete(keyData);
              });
              break;
              
            case "list":
              // 리스트 크기 확인
              redis.llen(key, sizeResult -> {
                if (sizeResult.succeeded()) {
                  keyData.put("size", sizeResult.result());
                  
                  // 처음 5개 요소 미리보기
                  redis.lrange(Arrays.asList(key, "0", "4"), rangeResult -> {
                    if (rangeResult.succeeded()) {
                      keyData.put("preview", rangeResult.result());
                    }
                    promise.complete(keyData);
                  });
                } else {
                  promise.complete(keyData);
                }
              });
              break;
              
            case "set":
              // 집합 크기 확인
              redis.scard(key, sizeResult -> {
                if (sizeResult.succeeded()) {
                  keyData.put("size", sizeResult.result());
                  
                  // 최대 5개 요소 미리보기
                  redis.srandmember(Arrays.asList(key, "5"), membersResult -> {
                    if (membersResult.succeeded()) {
                      keyData.put("preview", membersResult.result());
                    }
                    promise.complete(keyData);
                  });
                } else {
                  promise.complete(keyData);
                }
              });
              break;
              
            case "zset":
              // 정렬된 집합 크기 확인
              redis.zcard(key, sizeResult -> {
                if (sizeResult.succeeded()) {
                  keyData.put("size", sizeResult.result());
                  
                  // 상위 5개 요소 미리보기
                  redis.zrange(Arrays.asList(key, "0", "4", "WITHSCORES"), rangeResult -> {
                    if (rangeResult.succeeded()) {
                      keyData.put("preview", rangeResult.result());
                    }
                    promise.complete(keyData);
                  });
                } else {
                  promise.complete(keyData);
                }
              });
              break;
              
            default:
              promise.complete(keyData);
          }
        } else {
          promise.complete(keyData);
        }
      });
    } else {
      promise.fail(typeResult.cause());
    }
  });
  
  return promise.future();
}

// 메모리 사용량 분석
private Future<JsonObject> analyzeMemoryUsage() {
  Promise<JsonObject> promise = Promise.promise();
  
  redis.info(Arrays.asList("memory"), infoResult -> {
    if (infoResult.succeeded()) {
      String info = infoResult.result();
      JsonObject memoryInfo = new JsonObject();
      
      // 메모리 정보 파싱
      Pattern pattern = Pattern.compile("used_memory_human:([^\\r\\n]+)");
      Matcher matcher = pattern.matcher(info);
      if (matcher.find()) {
        memoryInfo.put("used_memory_human", matcher.group(1));
      }
      
      pattern = Pattern.compile("used_memory_peak_human:([^\\r\\n]+)");
      matcher = pattern.matcher(info);
      if (matcher.find()) {
        memoryInfo.put("used_memory_peak_human", matcher.group(1));
      }
      
      pattern = Pattern.compile("maxmemory_human:([^\\r\\n]+)");
      matcher = pattern.matcher(info);
      if (matcher.find()) {
        memoryInfo.put("maxmemory_human", matcher.group(1));
      }
      
      promise.complete(memoryInfo);
    } else {
      promise.fail(infoResult.cause());
    }
  });
  
  return promise.future();
}
4. 데이터 분석 결과 시각화
Redis 데이터 구조 분석 결과를 웹 인터페이스로 시각화할 수 있는 간단한 Vert.x 애플리케이션을 만들 수 있습니다.
javapublic class RedisAnalyzerVerticle extends AbstractVerticle {

  private Redis redis;
  
  @Override
  public void start(Promise<Void> startPromise) {
    // Redis 연결 설정
    RedisOptions options = new RedisOptions()
        .setConnectionString("redis://localhost:6379");
    
    Redis.createClient(vertx, options)
        .connect(ar -> {
          if (ar.succeeded()) {
            redis = ar.result();
            System.out.println("Redis 연결 성공");
            
            // 웹 서버 설정
            setupWebServer(startPromise);
          } else {
            System.err.println("Redis 연결 실패: " + ar.cause().getMessage());
            startPromise.fail(ar.cause());
          }
        });
  }
  
  private void setupWebServer(Promise<Void> startPromise) {
    Router router = Router.router(vertx);
    
    // 정적 파일 제공
    router.route("/static/*").handler(StaticHandler.create("webroot"));
    
    // 메인 페이지
    router.get("/").handler(ctx -> {
      ctx.response()
         .putHeader("content-type", "text/html")
         .sendFile("webroot/index.html");
    });
    
    // Redis 데이터 구조 분석 API
    router.get("/api/redis/analyze").handler(ctx -> {
      analyzeRedisStructure(Promise.promise()).onComplete(ar -> {
        if (ar.succeeded()) {
          ctx.response()
             .putHeader("content-type", "application/json")
             .end(ar.result().encode());
        } else {
          ctx.response()
             .setStatusCode(500)
             .putHeader("content-type", "application/json")
             .end(new JsonObject()
                 .put("error", ar.cause().getMessage())
                 .encode());
        }
      });
    });
    
    // HTTP 서버 시작
    vertx.createHttpServer()
        .requestHandler(router)
        .listen(8080, http -> {
          if (http.succeeded()) {
            System.out.println("서버가 http://localhost:8080/ 에서 실행 중입니다.");
            startPromise.complete();
          } else {
            startPromise.fail(http.cause());
          }
        });
  }
  
  // (앞서 정의한 analyzeRedisStructure 메서드 및 관련 메서드들 포함)
}
5. 초기 데이터 스냅샷 생성 및 복원
분석 후에는 현재 상태를 스냅샷으로 저장하여 필요할 때 복원할 수 있습니다.
스냅샷 생성 (RDB 파일)
bash# Redis CLI에서 RDB 파일 생성
127.0.0.1:6379> SAVE

# 또는 비동기 저장 (백그라운드 저장)
127.0.0.1:6379> BGSAVE
특정 패턴의 키만 덤프
javapublic void dumpKeysWithPattern(String pattern, String outputFile) {
  // 패턴과 일치하는 키 스캔
  scanKeysWithPattern(pattern, "0", new JsonArray(), scanResult -> {
    if (scanResult.succeeded()) {
      JsonArray keys = scanResult.result();
      JsonObject dump = new JsonObject();
      
      List<Future> futures = new ArrayList<>();
      for (int i = 0; i < keys.size(); i++) {
        String key = keys.getString(i);
        Promise<Void> keyPromise = Promise.promise();
        futures.add(keyPromise.future());
        
        // 키 유형 및 값 덤프
        dumpKey(key, ar -> {
          if (ar.succeeded()) {
            dump.put(key, ar.result());
          }
          keyPromise.complete();
        });
      }
      
      // 모든 키 덤프 완료 후 파일로 저장
      CompositeFuture.all(futures).onComplete(ar -> {
        if (ar.succeeded()) {
          // JSON 파일로 저장
          vertx.fileSystem().writeFile(outputFile, Buffer.buffer(dump.encodePrettily()), writeResult -> {
            if (writeResult.succeeded()) {
              System.out.println("키 덤프 파일 저장 완료: " + outputFile);
            } else {
              System.err.println("키 덤프 파일 저장 실패: " + writeResult.cause().getMessage());
            }
          });
        }
      });
    }
  });
}

// 패턴과 일치하는 키 스캔
private void scanKeysWithPattern(String pattern, String cursor, JsonArray keys, Handler<AsyncResult<JsonArray>> resultHandler) {
  redis.scan(Arrays.asList(cursor, "MATCH", pattern, "COUNT", "1000"), scanResult -> {
    if (scanResult.succeeded()) {
      JsonArray result = scanResult.result();
      String nextCursor = result.getString(0);
      JsonArray batch = result.getJsonArray(1);
      
      // 결과에 키 추가
      for (int i = 0; i < batch.size(); i++) {
        keys.add(batch.getString(i));
      }
      
      // 모든 키를 스캔할 때까지 재귀 호출
      if (!"0".equals(nextCursor)) {
        scanKeysWithPattern(pattern, nextCursor, keys, resultHandler);
      } else {
        resultHandler.handle(Future.succeededFuture(keys));
      }
    } else {
      resultHandler.handle(Future.failedFuture(scanResult.cause()));
    }
  });
}

// 단일 키 덤프
private void dumpKey(String key, Handler<AsyncResult<JsonObject>> resultHandler) {
  // 키 타입 확인
  redis.type(key, typeResult -> {
    if (typeResult.succeeded()) {
      String type = typeResult.result().toLowerCase();
      JsonObject keyData = new JsonObject().put("type", type);
      
      // TTL 확인
      redis.ttl(key, ttlResult -> {
        if (ttlResult.succeeded() && ttlResult.result() > 0) {
          keyData.put("ttl", ttlResult.result());
        }
        
        // 타입별 값 덤프
        switch (type) {
          case "string":
            redis.get(key, valueResult -> {
              if (valueResult.succeeded()) {
                keyData.put("value", valueResult.result());
              }
              resultHandler.handle(Future.succeededFuture(keyData));
            });
            break;
            
          case "hash":
            redis.hgetall(key, hashResult -> {
              if (hashResult.succeeded()) {
                keyData.put("value", hashResult.result());
              }
              resultHandler.handle(Future.succeededFuture(keyData));
            });
            break;
            
          case "list":
            redis.lrange(Arrays.asList(key, "0", "-1"), listResult -> {
              if (listResult.succeeded()) {
                keyData.put("value", listResult.result());
              }
              resultHandler.handle(Future.succeededFuture(keyData));
            });
            break;
            
          case "set":
            redis.smembers(key, setResult -> {
              if (setResult.succeeded()) {
                keyData.put("value", setResult.result());
              }
              resultHandler.handle(Future.succeededFuture(keyData));
            });
            break;
            
          case "zset":
            redis.zrange(Arrays.asList(key, "0", "-1", "WITHSCORES"), zsetResult -> {
              if (zsetResult.succeeded()) {
                JsonArray elements = zsetResult.result();
                JsonObject zset = new JsonObject();
                
                // WITHSCORES 결과를 키-값 쌍으로 변환
                for (int i = 0; i < elements.size(); i += 2) {
                  String member = elements.getString(i);
                  double score = Double.parseDouble(elements.getString(i + 1));
                  zset.put(member, score);
                }
                
                keyData.put("value", zset);
              }
              resultHandler.handle(Future.succeededFuture(keyData));
            });
            break;
            
          default:
            resultHandler.handle(Future.succeededFuture(keyData));
        }
      });
    } else {
      resultHandler.handle(Future.failedFuture(typeResult.cause()));
    }
  });
}
6. 초기 데이터 복제 및 테스트 환경 구성
분석한 데이터 구조를 기반으로 테스트 환경을 구성할 수 있습니다.
javapublic void setupTestData(JsonObject dumpData) {
  // 먼저 존재하는 키 삭제 (주의: 테스트 환경에서만 사용)
  redis.flushdb(flushResult -> {
    if (flushResult.succeeded()) {
      System.out.println("테스트 데이터베이스 초기화 완료");
      
      // 덤프 데이터에서 키 복원
      List<Future> futures = new ArrayList<>();
      for (String key : dumpData.fieldNames()) {
        Promise<Void> keyPromise = Promise.promise();
        futures.add(keyPromise.future());
        
        JsonObject keyData = dumpData.getJsonObject(key);
        restoreKey(key, keyData, restoreResult -> {
          if (restoreResult.succeeded()) {
            keyPromise.complete();
          } else {
            System.err.println("키 복원 실패: " + key + " - " + restoreResult.cause().getMessage());
            keyPromise.complete(); // 오류가 있어도 계속 진행
          }
        });
      }
      
      // 모든 키 복원 완료 후 처리
      CompositeFuture.all(futures).onComplete(ar -> {
        if (ar.succeeded()) {
          System.out.println("테스트 데이터 설정 완료");
        } else {
          System.err.println("테스트 데이터 설정 중 오류 발생");
        }
      });
    }
  });
}

// 단일 키 복원
private void restoreKey(String key, JsonObject keyData, Handler<AsyncResult<Void>> resultHandler) {
  String type = keyData.getString("type");
  Object value = keyData.getValue("value");
  
  switch (type) {
    case "string":
      redis.set(Arrays.asList(key, value.toString()), setResult -> {
        if (setResult.succeeded()) {
          // TTL 설정 (있는 경우)
          if (keyData.containsKey("ttl")) {
            redis.expire(Arrays.asList(key, String.valueOf(keyData.getInteger("ttl"))), expireResult -> {
              resultHandler.handle(Future.succeededFuture());
            });
          } else {
            resultHandler.handle(Future.succeededFuture());
          }
        } else {
          resultHandler.handle(Future.failedFuture(setResult.cause()));
        }
      });
      break;
      
    case "hash":
      JsonObject hash = (JsonObject) value;
      List<String> hmsetArgs = new ArrayList<>();
      hmsetArgs.add(key);
      
      for (String field : hash.fieldNames()) {
        hmsetArgs.add(field);
        hmsetArgs.add(hash.getValue(field).toString());
      }
      
      redis.hmset(hmsetArgs, hmsetResult -> {
        if (hmsetResult.succeeded()) {
          // TTL 설정 (있는 경우)
          if (keyData.containsKey("ttl")) {
            redis.expire(Arrays.asList(key, String.valueOf(keyData.getInteger("ttl"))), expireResult -> {
              resultHandler.handle(Future.succeededFuture());
            });
          } else {
            resultHandler.handle(Future.succeededFuture());
          }
        } else {
          resultHandler.handle(Future.failedFuture(hmsetResult.cause()));
        }
      });
      break;
      
    // 다른 타입도 유사하게 처리...
    
    default:
      resultHandler.handle(Future.failedFuture("지원되지 않는 타입: " + type));
  }
}
7. Redis 패턴 문서화 자동화 (이어서)
javapublic void generateRedisDocumentation(JsonObject analysisData, String outputFile) {
  StringBuilder doc = new StringBuilder();
  
  doc.append("# Redis 데이터 구조 문서\n\n");
  doc.append("*자동 생성된 문서: " + LocalDateTime.now() + "*\n\n");
  
  // 요약 정보
  JsonObject keyPatterns = analysisData.getJsonObject("keyPatterns");
  JsonObject patternCount = keyPatterns.getJsonObject("patternCount");
  doc.append("## 요약\n\n");
  doc.append("총 키 개수: " + keyPatterns.getInteger("totalScanned") + "\n\n");
  doc.append("| 패턴 | 키 개수 |\n");
  doc.append("|------|-------|\n");
  
  for (String pattern : patternCount.fieldNames()) {
    doc.append("| " + pattern + " | " + patternCount.getInteger(pattern) + " |\n");
  }
  doc.append("\n");
  
  // 메모리 사용량
  JsonObject memoryUsage = analysisData.getJsonObject("memoryUsage");
  doc.append("## 메모리 사용량\n\n");
  doc.append("- 현재 사용 메모리: " + memoryUsage.getString("used_memory_human", "N/A") + "\n");
  doc.append("- 최대 사용 메모리: " + memoryUsage.getString("used_memory_peak_human", "N/A") + "\n");
  doc.append("- 메모리 한도: " + memoryUsage.getString("maxmemory_human", "제한 없음") + "\n\n");
  
  // 각 패턴별 상세 정보
  JsonObject sampleData = analysisData.getJsonObject("sampleData");
  doc.append("## 데이터 패턴 상세\n\n");
  
  for (String pattern : sampleData.fieldNames()) {
    doc.append("### " + pattern + "\n\n");
    JsonArray samples = sampleData.getJsonArray(pattern);
    
    // 샘플 데이터가 있는 경우
    if (samples.size() > 0) {
      JsonObject firstSample = samples.getJsonObject(0);
      String type = firstSample.getString("type");
      
      doc.append("**타입:** " + type + "\n\n");
      
      // TTL 정보
      if (firstSample.containsKey("ttl")) {
        int ttl = firstSample.getInteger("ttl");
        if (ttl > 0) {
          doc.append("**TTL:** " + formatTTL(ttl) + "\n\n");
        } else if (ttl == -1) {
          doc.append("**TTL:** 영구 저장 (만료 없음)\n\n");
        }
      }
      
      // 데이터 구조 설명
      doc.append("**데이터 구조:**\n\n");
      
      switch (type.toLowerCase()) {
        case "string":
          if (firstSample.getString("format", "string").equals("json")) {
            doc.append("JSON 문자열을 저장합니다. 일반적으로 캐싱된 객체나 설정 정보를 포함합니다.\n\n");
          } else {
            doc.append("일반 문자열 값을 저장합니다.\n\n");
          }
          break;
          
        case "hash":
          JsonObject preview = firstSample.getJsonObject("preview", new JsonObject());
          doc.append("해시 맵 데이터를 저장합니다. 일반적으로 객체나 엔티티 속성을 표현합니다.\n\n");
          doc.append("**필드 목록:** ");
          doc.append(String.join(", ", firstSample.getJsonArray("fields", new JsonArray()).getList()));
          doc.append("\n\n");
          break;
          
        case "list":
          doc.append("리스트 데이터를 저장합니다. 일반적으로 시간 순서가 중요한 데이터나 대기열에 사용됩니다.\n\n");
          doc.append("**크기:** " + firstSample.getInteger("size", 0) + " 항목\n\n");
          break;
          
        case "set":
          doc.append("집합 데이터를 저장합니다. 일반적으로 중복되지 않는 멤버 컬렉션에 사용됩니다.\n\n");
          doc.append("**크기:** " + firstSample.getInteger("size", 0) + " 항목\n\n");
          break;
          
        case "zset":
          doc.append("정렬된 집합 데이터를 저장합니다. 일반적으로 순위나 우선순위가 있는 데이터에 사용됩니다.\n\n");
          doc.append("**크기:** " + firstSample.getInteger("size", 0) + " 항목\n\n");
          break;
      }
      
      // 샘플 키 목록
      doc.append("**샘플 키:**\n\n");
      for (int i = 0; i < samples.size(); i++) {
        JsonObject sample = samples.getJsonObject(i);
        doc.append("- `" + sample.getString("key") + "`\n");
      }
      doc.append("\n");
      
      // 샘플 데이터 미리보기
      doc.append("**샘플 데이터 미리보기:**\n\n");
      doc.append("```\n");
      doc.append(formatPreview(firstSample.getValue("preview", "")));
      doc.append("\n```\n\n");
      
      // 용도 및 설명 (패턴 이름 기반 추론)
      doc.append("**추정 용도:**\n\n");
      if (pattern.contains("session")) {
        doc.append("세션 데이터를 저장하는 것으로 보입니다. 사용자 인증 및 상태 관리에 사용됩니다.\n\n");
      } else if (pattern.contains("cache")) {
        doc.append("캐싱 데이터를 저장하는 것으로 보입니다. 데이터베이스 쿼리 결과나 계산 비용이 큰 연산 결과를 임시 저장하는 데 사용됩니다.\n\n");
      } else if (pattern.contains("lock")) {
        doc.append("분산 락을 구현하는 데 사용되는 것으로 보입니다. 동시성 제어에 활용됩니다.\n\n");
      } else if (pattern.contains("counter")) {
        doc.append("카운터 값을 저장하는 것으로 보입니다. 페이지 조회수, 사용자 통계 등에 사용됩니다.\n\n");
      } else if (pattern.contains("queue")) {
        doc.append("작업 대기열을 구현하는 데 사용되는 것으로 보입니다. 비동기 작업 처리에 활용됩니다.\n\n");
      } else {
        doc.append("명확한 용도를 추론할 수 없습니다. 애플리케이션별 비즈니스 로직과 관련된 데이터로 추정됩니다.\n\n");
      }
    } else {
      doc.append("샘플 데이터가 없습니다.\n\n");
    }
  }
  
  // 문서를 파일로 저장
  vertx.fileSystem().writeFile(outputFile, Buffer.buffer(doc.toString()), ar -> {
    if (ar.succeeded()) {
      System.out.println("Redis 문서가 생성되었습니다: " + outputFile);
    } else {
      System.err.println("Redis 문서 생성 실패: " + ar.cause().getMessage());
    }
  });
}

// TTL 형식화
private String formatTTL(int seconds) {
  if (seconds < 60) {
    return seconds + "초";
  } else if (seconds < 3600) {
    return (seconds / 60) + "분 " + (seconds % 60) + "초";
  } else if (seconds < 86400) {
    return (seconds / 3600) + "시간 " + ((seconds % 3600) / 60) + "분";
  } else {
    return (seconds / 86400) + "일 " + ((seconds % 86400) / 3600) + "시간";
  }
}

// 미리보기 형식화
private String formatPreview(Object preview) {
  if (preview instanceof JsonObject) {
    return ((JsonObject) preview).encodePrettily();
  } else if (preview instanceof JsonArray) {
    return ((JsonArray) preview).encodePrettily();
  } else {
    return String.valueOf(preview);
  }
}

8. 초기 데이터를 위한 데이터 모델 시각화
Redis 데이터 구조를 시각화하여 이해를 돕는 웹 인터페이스를 만들 수 있습니다.
HTML 템플릿 예시 (webroot/index.html)
html<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Redis 데이터 구조 분석기</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
  <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
  <style>
    .key-pattern {
      cursor: pointer;
      padding: 8px;
      border-radius: 4px;
      margin-bottom: 4px;
    }
    .key-pattern:hover {
      background-color: #f0f0f0;
    }
    pre {
      background-color: #f8f9fa;
      padding: 10px;
      border-radius: 4px;
    }
  </style>
</head>
<body>
  <div class="container mt-4">
    <h1>Redis 데이터 구조 분석기</h1>
    
    <div class="row mt-4">
      <div class="col-12">
        <div class="card">
          <div class="card-header">
            데이터 분석
          </div>
          <div class="card-body">
            <button id="analyzeBtn" class="btn btn-primary">Redis 데이터 구조 분석하기</button>
            <div id="loadingIndicator" class="mt-3 d-none">
              <div class="spinner-border text-primary" role="status">
                <span class="visually-hidden">분석 중...</span>
              </div>
              <span class="ms-2">데이터를 분석하는 중입니다. 잠시 기다려주세요...</span>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <div id="resultsContainer" class="d-none mt-4">
      <div class="row">
        <!-- 요약 정보 -->
        <div class="col-md-6">
          <div class="card">
            <div class="card-header">요약 정보</div>
            <div class="card-body">
              <div id="summaryInfo"></div>
              <canvas id="patternChart" width="400" height="300"></canvas>
            </div>
          </div>
        </div>
        
        <!-- 메모리 사용량 -->
        <div class="col-md-6">
          <div class="card">
            <div class="card-header">메모리 사용량</div>
            <div class="card-body">
              <div id="memoryInfo"></div>
              <canvas id="memoryChart" width="400" height="300"></canvas>
            </div>
          </div>
        </div>
      </div>
      
      <div class="row mt-4">
        <!-- 패턴 목록 -->
        <div class="col-md-4">
          <div class="card">
            <div class="card-header">데이터 패턴</div>
            <div class="card-body">
              <div id="patternList"></div>
            </div>
          </div>
        </div>
        
        <!-- 패턴 상세 정보 -->
        <div class="col-md-8">
          <div class="card">
            <div class="card-header">패턴 상세 정보</div>
            <div class="card-body">
              <div id="patternDetails">
                <p class="text-muted">왼쪽에서 패턴을 선택하세요.</p>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <div class="row mt-4">
        <div class="col-12">
          <div class="card">
            <div class="card-header">문서 생성</div>
            <div class="card-body">
              <button id="generateDocBtn" class="btn btn-success">Markdown 문서 생성</button>
              <a id="downloadLink" class="btn btn-outline-primary ms-2 d-none" download="redis-data-structure.md">문서 다운로드</a>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
  
  <script>
    // 데이터 분석 결과를 저장할 변수
    let analysisData = null;
    
    // 분석 버튼 클릭 이벤트
    document.getElementById('analyzeBtn').addEventListener('click', async function() {
      document.getElementById('loadingIndicator').classList.remove('d-none');
      
      try {
        // API 호출
        const response = await fetch('/api/redis/analyze');
        analysisData = await response.json();
        
        // 결과 표시
        showResults(analysisData);
      } catch (error) {
        alert('데이터 분석 중 오류가 발생했습니다: ' + error);
      } finally {
        document.getElementById('loadingIndicator').classList.add('d-none');
      }
    });
    
    // 분석 결과 표시
    function showResults(data) {
      document.getElementById('resultsContainer').classList.remove('d-none');
      
      // 요약 정보 표시
      const keyPatterns = data.keyPatterns;
      const totalKeys = keyPatterns.totalScanned;
      const patternCount = keyPatterns.patternCount;
      
      let summaryHtml = `<p>총 키 개수: <strong>${totalKeys}</strong></p>`;
      summaryHtml += '<ul>';
      for (const pattern in patternCount) {
        summaryHtml += `<li>${pattern}: ${patternCount[pattern]}</li>`;
      }
      summaryHtml += '</ul>';
      document.getElementById('summaryInfo').innerHTML = summaryHtml;
      
      // 패턴 차트 표시
      const patternLabels = Object.keys(patternCount);
      const patternValues = Object.values(patternCount);
      
      const patternChartCtx = document.getElementById('patternChart').getContext('2d');
      new Chart(patternChartCtx, {
        type: 'doughnut',
        data: {
          labels: patternLabels,
          datasets: [{
            data: patternValues,
            backgroundColor: generateColors(patternLabels.length)
          }]
        },
        options: {
          responsive: true,
          plugins: {
            legend: {
              position: 'right'
            },
            title: {
              display: true,
              text: '키 패턴 분포'
            }
          }
        }
      });
      
      // 메모리 정보 표시
      const memoryUsage = data.memoryUsage;
      let memoryHtml = `<p>현재 사용 메모리: <strong>${memoryUsage.used_memory_human || 'N/A'}</strong></p>`;
      memoryHtml += `<p>최대 사용 메모리: <strong>${memoryUsage.used_memory_peak_human || 'N/A'}</strong></p>`;
      memoryHtml += `<p>메모리 한도: <strong>${memoryUsage.maxmemory_human || '제한 없음'}</strong></p>`;
      document.getElementById('memoryInfo').innerHTML = memoryHtml;
      
      // 패턴 목록 표시
      let patternListHtml = '';
      for (const pattern in patternCount) {
        patternListHtml += `<div class="key-pattern" data-pattern="${pattern}">${pattern} <span class="badge bg-secondary">${patternCount[pattern]}</span></div>`;
      }
      document.getElementById('patternList').innerHTML = patternListHtml;
      
      // 패턴 클릭 이벤트 등록
      document.querySelectorAll('.key-pattern').forEach(element => {
        element.addEventListener('click', function() {
          const pattern = this.getAttribute('data-pattern');
          showPatternDetails(pattern, data.sampleData[pattern]);
          
          // 선택된 패턴 하이라이트
          document.querySelectorAll('.key-pattern').forEach(el => {
            el.classList.remove('bg-light');
          });
          this.classList.add('bg-light');
        });
      });
      
      // 문서 생성 버튼 이벤트
      document.getElementById('generateDocBtn').addEventListener('click', function() {
        generateMarkdownDoc(data);
      });
    }
    
    // 패턴 상세 정보 표시
    function showPatternDetails(pattern, samples) {
      if (!samples || samples.length === 0) {
        document.getElementById('patternDetails').innerHTML = '<p>이 패턴에 대한 샘플 데이터가 없습니다.</p>';
        return;
      }
      
      const firstSample = samples[0];
      const type = firstSample.type;
      
      let detailsHtml = `<h4>${pattern}</h4>`;
      detailsHtml += `<p><strong>타입:</strong> ${type}</p>`;
      
      // TTL 정보
      if (firstSample.ttl) {
        if (firstSample.ttl > 0) {
          detailsHtml += `<p><strong>TTL:</strong> ${formatTTL(firstSample.ttl)}</p>`;
        } else if (firstSample.ttl === -1) {
          detailsHtml += `<p><strong>TTL:</strong> 영구 저장 (만료 없음)</p>`;
        }
      }
      
      // 타입별 상세 정보
      detailsHtml += '<h5>데이터 구조:</h5>';
      
      switch (type.toLowerCase()) {
        case 'string':
          if (firstSample.format === 'json') {
            detailsHtml += '<p>JSON 문자열을 저장합니다. 일반적으로 캐싱된 객체나 설정 정보를 포함합니다.</p>';
          } else {
            detailsHtml += '<p>일반 문자열 값을 저장합니다.</p>';
          }
          break;
          
        case 'hash':
          detailsHtml += '<p>해시 맵 데이터를 저장합니다. 일반적으로 객체나 엔티티 속성을 표현합니다.</p>';
          if (firstSample.fields) {
            detailsHtml += `<p><strong>필드 목록:</strong> ${firstSample.fields.join(', ')}</p>`;
          }
          break;
          
        case 'list':
          detailsHtml += '<p>리스트 데이터를 저장합니다. 일반적으로 시간 순서가 중요한 데이터나 대기열에 사용됩니다.</p>';
          if (firstSample.size) {
            detailsHtml += `<p><strong>크기:</strong> ${firstSample.size} 항목</p>`;
          }
          break;
          
        case 'set':
          detailsHtml += '<p>집합 데이터를 저장합니다. 일반적으로 중복되지 않는 멤버 컬렉션에 사용됩니다.</p>';
          if (firstSample.size) {
            detailsHtml += `<p><strong>크기:</strong> ${firstSample.size} 항목</p>`;
          }
          break;
          
        case 'zset':
          detailsHtml += '<p>정렬된 집합 데이터를 저장합니다. 일반적으로 순위나 우선순위가 있는 데이터에 사용됩니다.</p>';
          if (firstSample.size) {
            detailsHtml += `<p><strong>크기:</strong> ${firstSample.size} 항목</p>`;
          }
          break;
      }
      
      // 샘플 키 목록
      detailsHtml += '<h5>샘플 키:</h5><ul>';
      for (const sample of samples) {
        detailsHtml += `<li><code>${sample.key}</code></li>`;
      }
      detailsHtml += '</ul>';
      
      // 샘플 데이터 미리보기
      detailsHtml += '<h5>샘플 데이터 미리보기:</h5>';
      detailsHtml += `<pre>${formatPreview(firstSample.preview)}</pre>`;
      
      // 추정 용도
      detailsHtml += '<h5>추정 용도:</h5>';
      if (pattern.includes('session')) {
        detailsHtml += '<p>세션 데이터를 저장하는 것으로 보입니다. 사용자 인증 및 상태 관리에 사용됩니다.</p>';
      } else if (pattern.includes('cache')) {
        detailsHtml += '<p>캐싱 데이터를 저장하는 것으로 보입니다. 데이터베이스 쿼리 결과나 계산 비용이 큰 연산 결과를 임시 저장하는 데 사용됩니다.</p>';
      } else if (pattern.includes('lock')) {
        detailsHtml += '<p>분산 락을 구현하는 데 사용되는 것으로 보입니다. 동시성 제어에 활용됩니다.</p>';
      } else if (pattern.includes('counter')) {
        detailsHtml += '<p>카운터 값을 저장하는 것으로 보입니다. 페이지 조회수, 사용자 통계 등에 사용됩니다.</p>';
      } else if (pattern.includes('queue')) {
        detailsHtml += '<p>작업 대기열을 구현하는 데 사용되는 것으로 보입니다. 비동기 작업 처리에 활용됩니다.</p>';
      } else {
        detailsHtml += '<p>명확한 용도를 추론할 수 없습니다. 애플리케이션별 비즈니스 로직과 관련된 데이터로 추정됩니다.</p>';
      }
      
      document.getElementById('patternDetails').innerHTML = detailsHtml;
    }
    
    // Markdown 문서 생성
    function generateMarkdownDoc(data) {
      const doc = generateMarkdownContent(data);
      
      // 다운로드 링크 생성
      const blob = new Blob([doc], { type: 'text/markdown' });
      const url = URL.createObjectURL(blob);
      
      const downloadLink = document.getElementById('downloadLink');
      downloadLink.href = url;
      downloadLink.classList.remove('d-none');
    }
    
    // 유틸리티 함수들
    function formatTTL(seconds) {
      if (seconds < 60) {
        return seconds + '초';
      } else if (seconds < 3600) {
        return Math.floor(seconds / 60) + '분 ' + (seconds % 60) + '초';
      } else if (seconds < 86400) {
        return Math.floor(seconds / 3600) + '시간 ' + Math.floor((seconds % 3600) / 60) + '분';
      } else {
        return Math.floor(seconds / 86400) + '일 ' + Math.floor((seconds % 86400) / 3600) + '시간';
      }
    }
    
    function formatPreview(preview) {
      if (!preview) return '';
      if (typeof preview === 'object') {
        return JSON.stringify(preview, null, 2);
      }
      return preview;
    }
    
    function generateColors(count) {
      const colors = [];
      for (let i = 0; i < count; i++) {
        const hue = (i * 137) % 360;
        colors.push(`hsl(${hue}, 70%, 60%)`);
      }
      return colors;
    }
    
    // Markdown 문서 내용 생성 (서버 측 코드와 유사)
    function generateMarkdownContent(data) {
      // Markdown 문서 생성 로직
      // (서버 측 코드와 유사한 내용)
      // ...
    }
  </script>
</body>
</html>
요약: Redis 데이터 구조 파악을 위한 체크리스트
설계서 없이 Redis 데이터 구조를 파악하기 위한 체계적인 접근법은 다음과 같습니다:

기본 탐색

Redis CLI로 연결하여 KEYS * 또는 패턴으로 키 조회
키 유형 파악 (TYPE 명령어)
샘플 데이터 확인 (유형별 조회 명령어)


패턴 분석

키 네이밍 규칙 분석 (콜론 구분자, 접두사 등)
데이터 타입별 분류 및 통계
TTL(만료 시간) 패턴 확인


자동화 도구 개발

패턴 분석 스크립트 작성
샘플 데이터 추출 프로그램
데이터 시각화 도구


문서화

발견된 패턴 문서화
샘플 데이터 포함
추정 용도 기록


테스트 환경 구성

프로덕션 데이터 복제
테스트 케이스 작성
데이터 모델 검증