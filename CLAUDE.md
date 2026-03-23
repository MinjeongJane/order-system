# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

### Build & Run
```bash
./gradlew build           # 전체 빌드 (테스트 포함)
./gradlew bootRun         # 애플리케이션 실행
./gradlew composeUp       # Docker 인프라 실행 (MySQL, Redis, Kafka, Prometheus, Grafana)
```

### Test
```bash
./gradlew test                                              # 전체 테스트 (자동으로 composeUp 실행)
./gradlew test --tests "order.application.order.OrderServiceTest"   # 단일 테스트 클래스 실행
./gradlew test --tests "order.application.order.OrderServiceTest.주문_성공"  # 단일 테스트 메서드 실행
```

> `tasks.named("test") { dependsOn("composeUp") }` 설정으로 테스트 실행 시 Docker 컨테이너가 자동 시작됨.

### QueryDSL Q클래스 생성
```bash
./gradlew kaptKotlin       # Q클래스 생성 (build/generated/kapt/main에 생성됨)
```

## Architecture

### 레이어 구조

```
api/            → Controller, Request/Response DTO
application/    → Service (비즈니스 로직 오케스트레이션)
domain/         → 도메인 모델, Repository 인터페이스, 도메인 로직
infrastructure/ → JPA Entity, Repository 구현체, 외부 API 클라이언트
common/         → AOP, Config, 공통 유틸
```

- **도메인 모델과 JPA 엔티티는 분리**되어 있음. `infrastructure/` 아래 `*Entity`가 DB 매핑을 담당하고, `domain/` 아래 순수 도메인 객체로 변환해서 사용.
- Repository 인터페이스는 `domain/`에 정의, 구현체는 `infrastructure/`에 위치 (의존성 역전).

### 핵심 패턴

**1. 분산 락 (`@DistributedLock`)**
- `common/config/DistributedLock.kt` — 어노테이션 정의
- `common/config/DistributedLockAspect.kt` — Redisson 기반 AOP 구현, `@Order(Ordered.HIGHEST_PRECEDENCE)`로 트랜잭션보다 먼저 실행
- SpEL로 동적 키 생성: `key = "order:processing"`, `keyExpression = "#request.userId"` → 최종 키: `order:processing:{userId}`

**2. 소프트 삭제**
- `BaseEntity`에 `@FilterDef(name = "deletedFilter")` + `@Filter(condition = "deleted = false")` 정의
- `SoftDeleteFilterAspect` — `@Transactional` 메서드 진입 시 자동으로 Hibernate 필터 활성화
- 엔티티 삭제 시 `@SQLDelete`로 `UPDATE ... SET deleted = true` 실행

**3. 다단계 캐시 (`common/config/cache/`)**
- `menu` 캐시: Caffeine (TTL 1분, 단건 조회)
- `menus` 캐시: Caffeine + `CacheLoader` (TTL 10분, refreshAfterWrite 5분, 목록 조회)
- Redis 캐시: `RedisCacheManager` (TTL 1시간, JSON 직렬화)
- `CacheInvalidationPublisher/Subscriber`: Redis Pub/Sub으로 분산 캐시 무효화
- `CacheResolver`로 캐시 이름에 따라 적절한 CacheManager 라우팅

**4. 인기 메뉴 집계 (`BestRepositoryImpl`)**
- Redis Hash(`best_menu` 키)에 메뉴별 주문 수 실시간 카운트 (TTL 7일)
- Redis 미스 시 DB(`menu_order_statistics` 테이블) fallback → 결과를 Redis에 재캐싱
- `@RateLimiter(name = "dbFallbackLimiter")`로 DB 과부하 방지

**5. 주문 이벤트 (Kafka)**
- 주문 저장 후 `OrderRepositoryImpl`에서 Kafka 비동기 발행
- `whenComplete`로 비블로킹 처리

### 주요 외부 의존성 포트

| 서비스 | 포트 |
|--------|------|
| Spring Boot App | 8080 |
| MySQL | 3307 |
| Redis | 6379 |
| Kafka | 9093 |
| Grafana | 3000 (admin/admin) |
| Prometheus | 9090 |

### 테스트 구조

- **단위 테스트**: MockK 사용, Kotest BDD 스타일 (`describe/it` 또는 `given/when/then`)
- **통합 테스트**: `@SpringBootTest` + 실제 Docker 컨테이너
- `TestJpaConfig`: 테스트용 JPA 설정 오버라이드
- `SecurityConfig`가 모든 경로 permitAll로 열려있어 테스트 시 별도 인증 불필요
