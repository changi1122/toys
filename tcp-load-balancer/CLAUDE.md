# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.
Treat all other directories under the parent folder as unrelated. Focus only on the directory where Claude Code is currently being executed.

## Project

A toy TCP load balancer implemented in Java. Currently in early scaffolding stage.

- Group: `net.studio1122`
- Build tool: Gradle 9 (Groovy DSL)
- Test framework: JUnit 5 (Jupiter)
- Main entry point: `src/main/java/net/studio1122/Main.java`

## Architecture

목표 파일 구조:
```
src/main/java/net/studio1122/
├── Config.java              # 상수 설정 (포트, 백엔드 목록, 타임아웃 등)
├── Backend.java             # 백엔드 서버 상태 객체
├── Algorithm/
│   ├── BalanceStrategy.java # 선택 전략 인터페이스
│   ├── RoundRobin.java      # AtomicInteger 기반 순환
│   └── LeastConnection.java # activeConnections 최솟값 선택
├── BackendPool.java         # 백엔드 목록 관리, 전략 위임
├── ProxyHandler.java        # 클라이언트↔백엔드 바이트 중계 (Runnable)
├── HealthChecker.java       # 주기적 TCP 연결 시도로 생존 확인 (Runnable)
└── Main.java                # ServerSocket accept 루프 + 진입점
```

### 동시성 설계
| 필드 | 클래스 | 타입 | 이유 |
|------|--------|------|------|
| `healthy` | Backend | `volatile boolean` | 단일 writer(HealthChecker), 다수 reader |
| `activeConnections` | Backend | `AtomicInteger` | 다수 ProxyHandler 스레드 동시 증감 |
| `backends` | BackendPool | `CopyOnWriteArrayList` | 다수 스레드 읽기 |
| `counter` | RoundRobin | `AtomicInteger` | 다수 스레드 getAndIncrement |
| `running` | Main/HealthChecker | `volatile boolean` | shutdown hook 스레드에서 쓰기 |

연결당 스레드: ProxyHandler 1 + relay(client→backend) 1 + relay(backend→client) 1 = 3개

### 단계별 구현 순서
1. **기본 TCP 프록시**: Config, Backend, ProxyHandler, Main (단일 백엔드 하드코딩)
2. **Round Robin**: BalanceStrategy 인터페이스, RoundRobin, BackendPool 추가
3. **Least Connection**: LeastConnection 추가, Config에서 전략 전환
4. **Health Check**: HealthChecker 추가, Main에 daemon thread로 연동
5. **(선택) 스레드 모델 개선**: ThreadPool → NIO Selector, `ab`/`wrk`로 성능 측정

### E2E 테스트 (nc 사용)
```bash
nc -lk 9001   # 백엔드 1
nc -lk 9002   # 백엔드 2
./gradlew run # 로드밸런서 (포트 8080)
nc localhost 8080  # 클라이언트
```
HealthCheck 검증: 백엔드 하나를 Ctrl+C로 종료 → 5초 후 "DOWN" 로그 확인 → 재시작 → "UP" 로그 확인

---

## Commands

```bash
# Build
./gradlew build

# Run
./gradlew run

# Run tests
./gradlew test

# Run a single test class
./gradlew test --tests "net.studio1122.SomeTestClass"

# Run a single test method
./gradlew test --tests "net.studio1122.SomeTestClass.methodName"
```