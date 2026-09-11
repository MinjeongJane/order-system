# Neo4j 그래프DB 실습 (order-system)

MySQL에 있는 주문 데이터를 Neo4j 그래프로 옮겨서, **관계형 DB로는 표현하기 힘든 질의**를
Cypher로 풀어보는 실습. `org.neo4j.driver:neo4j-java-driver` 를 직접 사용한다
(Spring Data Neo4j 미사용 — 드라이버/Cypher를 그대로 다뤄보는 게 목적).

> **문서 위치**: `.claude/graph.md` (저장소 루트 아님)

---

## 0. 이 실습이 실제로 검증하려는 것

### 먼저, 그래프DB를 언제 쓰고 언제 안 쓰는가

실습을 다시 열었을 때 판단 기준이 흐려지지 않도록 결론부터 적어둔다.

**"남들이 산 상품 추천"(item-item 협업 필터링)은 실무에서 그래프DB로 안 푼다.**
쿼리가 고정돼 있고, 신선도가 하루 늦어도 되고, 조회는 10ms 안에 끝나야 하기 때문에
배치로 동시출현 행렬을 미리 굽고 Redis에 넣는 게 정석이다
(`item:123 → [456, 789, ...]`, 조회 O(1)). 이 저장소엔 이미 `best_menu` Redis Hash가 있으니
인프라도 사실상 갖춰져 있다.

성능 모양도 불리하다. 추천 질의는 **깊이는 얕고(2~4 hop) 팬아웃은 거대한** 형태다.
인기 상품 하나에 주문이 수백만 건 달려 있으면 "나와 겹치는 유저" 한 홉에서 노드가 터지고,
그래프DB의 강점인 포인터 추적이 사실상 풀스캔이 된다.

**그래프DB가 값을 하는 건 정반대 모양이다.**

| 조건 | 설명 |
|---|---|
| 깊이가 **가변** | 몇 홉인지 미리 모름 (친구의 친구, 성분의 성분, 카테고리 조상) |
| 팬아웃이 **작음** | 각 홉에서 강하게 걸러짐 (친구 수십 명 ↔ 구매자 수백만 명) |
| **미리 계산 불가** | 개인화 축이 많아 조합이 폭발 → 조회 시점 탐색이 유일한 선택 |
| 질의를 **미리 모름** | 애드혹 탐색 |

대표 사례: 이상거래 탐지(계정→기기→계정→카드→계정), 자금세탁 추적, 권한 상속 체인,
부품 리콜 영향 범위, 지식 그래프.

### 그래서 이 실습의 위치

- **1단계 (구현 완료)** — 주문 데이터만으로 그래프 사고방식 익히기.
  솔직히 6개 질의 중 **5번(최단 경로) 하나만** 그래프DB가 정당화된다.
  나머지는 "SQL로도 되지만 Cypher가 읽기 편하다" 수준이고, **그 차이를 직접 비교해보는 것**이 목적.
- **2단계 (설계만, §7)** — 성분·알러지·친구·카테고리를 붙여
  **미리 계산할 수 없는 개인화 + 계층 구조 탐색**으로 확장.
  여기서 그래프DB 정당화가 1개 → 4개로 늘어난다.

**핵심 통찰**: `CO_ORDERED` 를 "그래프판 머티리얼라이즈"로 만든 순간, 그건 이미 KV 조회다.
미리 구울 수 있으면 그래프DB가 필요 없다. **미리 굽는 게 무너질 때** 비로소 값을 한다.

---

## 1. 실행 순서

```bash
# 1) 인프라 기동 (Docker Desktop 먼저 실행)
#    redis 필수 — RedissonConfig 가 기동 시점에 접속하므로 없으면 bootRun 이 실패한다
docker compose -f /Users/minjeong.jane.lee/Project/study/order-system/docker-compose.yml \
  up -d db redis neo4j

# 2) init.sql 을 수정했다면 MySQL 볼륨을 비워야 재실행됨
#    (init.sql 은 최초 기동 시 1회만 실행되는 entrypoint 스크립트)
docker compose -f <repo>/docker-compose.yml down
rm -rf /Users/minjeong.jane.lee/Project/study/order-system/docker/db/mysql/data
docker compose -f <repo>/docker-compose.yml up -d db redis neo4j

# 3) 애플리케이션 기동
./gradlew bootRun

# 4) MySQL -> Neo4j 적재
curl -X POST localhost:8084/api/graph/sync
```

> ⚠️ **셸 cwd 함정** — `docker-compose.yml` 은 저장소 루트에만 있는데 작업 셸 cwd 는
> `order-system/docker` 서브디렉터리인 경우가 많다. 그 상태에서 상대경로로
> `rm -rf docker/db/mysql/data` 를 치면 존재하지 않는 `.../docker/docker/db/...` 를 지우려다
> `-f` 가 에러를 삼켜 **조용히 아무 일도 안 하고 끝난다.**
> 항상 절대경로를 쓰거나 `docker compose -f <루트>/docker-compose.yml` 로 파일을 명시할 것.

- Neo4j Browser: http://localhost:7474 (`neo4j` / `orderSystem123`)
- Bolt: `bolt://localhost:7687`
- 앱 포트는 현재 `8084` (`application.yml`)

접속 후 `src/main/resources/neo4j-practice.cypher` 를 블록 단위로 실행하면 그래프가 눈으로 보인다.

---

## 2. 그래프 모델 (구현 완료)

`init.sql` 의 4개 테이블을 이렇게 옮겼다.

```
(:User  {id, credits})
(:Menu  {id, name, price})
(:Order {id, price, orderedAt})

(:User)  -[:PLACED]->                      (:Order)
(:Order) -[:CONTAINS {count, menuPrice}]-> (:Menu)
(:Menu)  -[:CO_ORDERED {weight}]->         (:Menu)   <- 파생 관계
```

| 테이블 | 그래프 |
|---|---|
| `user_credit` | `(:User)` 노드 |
| `menu` | `(:Menu)` 노드 |
| `order_history` | `(:Order)` 노드 + `(:User)-[:PLACED]->(:Order)` |
| `order_details` | `(:Order)-[:CONTAINS]->(:Menu)` — **행이 관계가 된다** |
| `menu_order_statistics` | 노드로 만들지 않음 (아래 참고) |

**핵심 전환 포인트**

- **조인 테이블이 사라진다.** `order_details` 는 노드가 아니라 관계가 되고,
  `count`/`menu_price` 는 관계의 속성으로 붙는다. FK 조인 = 관계 탐색.
- **`menu_order_statistics` 는 옮기지 않았다.** 이미 집계된 스냅샷이기 때문.
  대신 그래프의 연결 차수(degree)로 다시 세서 MySQL/Redis 집계 결과와 비교해보는 게 실습 포인트.
- **`CO_ORDERED` 는 파생 관계.** 원본 경로(`Menu <- Order -> Menu`)로도 같은 답이 나오지만,
  자주 쓰는 2-hop 경로를 미리 1-hop 엣지로 요약해두는 패턴 (그래프판 비정규화/머티리얼라이즈).
  `m1.id < m2.id` 조건으로 (A,B)/(B,A) 중복을 막고, 조회할 때는 방향 없이 `-[:CO_ORDERED]-` 로 탐색한다.
  → **단, §0 에서 짚었듯 이걸 미리 굽는 순간 KV 조회와 다를 바 없어진다.**

---

## 3. 시드 데이터로 실제 나오는 관계

`init.sql` 의 주문 9~16 은 **디저트 복수 메뉴 주문**이다.
원래는 아이스크림 4종 조합이었는데, 실습의 초점이 §7(알러지·팔로우)로 옮겨가면서
**디저트 조합으로 갈아끼웠다.** 그래야 `CO_ORDERED` 도 디저트 기준으로 만들어지고,
§7 의 "이미 먹어본 메뉴 제외" 필터도 실제로 발동한다.

조합은 **각 유저의 알러지를 지켜서** 짰다 (재휘=유제품이라 샤베트만, 주형=우유라 마카롱/샤베트만).
주문 7(재휘)도 바닐라 → 망고 샤베트로 바꿨다 — 유제품 알러지인데 바닐라를 먹은 모순이었다.
정합성(금액 합계, 단가×수량, 주문·LIKES 가 본인 알러지와 충돌하지 않는지) 전부 검증했다. 정합성(FK, `order_history.price` = detail 합계,
`menu_price` = `count` × 단가) 검증 완료.

### 유저별 먹어본 메뉴

| 유저 | 크레딧 | 먹어본 메뉴 |
|---|---|---|
| 1 순진 | 10000 | 바닐라, 초코, 망고, 팥빙수, 티라미수 |
| 2 유근 | 8000 | 딸기, 마카롱, 티라미수 |
| **3 연희** | 50000 | **없음 — 주문 이력 0 (콜드스타트)** |
| 4 준영 | 500 | 망고, 복숭아 샤베트, 망고 샤베트 |
| 5 재휘 | 0 | 복숭아 샤베트, 망고 샤베트 — **유제품 프리로 일관** |
| 6 휘찬 | 6000 | 바닐라, 팥빙수, 피스타치오 젤라또, 마카롱, 티라미수 |
| 7 주형 | 20000 | 마카롱, 망고 샤베트 |
| **8 민정** | **4500** | 딸기, 망고 샤베트 — **크레딧 필터 데모용** |

### CO_ORDERED (함께 주문된 메뉴)

| 쌍 | weight | 근거 주문 |
|---|---|---|
| 팥빙수—티라미수 | 2 | 9, 13 |
| 복숭아 샤베트—망고 샤베트 | 2 | 11, 12 |
| 바닐라—초코 | 1 | 5 |
| 마카롱—티라미수 | 1 | 10 |
| 마카롱—망고 샤베트 | 1 | 14 |
| 딸기—망고 샤베트 | 1 | 15 |
| 피스타치오 젤라또—마카롱 | 1 | 16 |

weight 높은 쌍이 곧 세트 메뉴 후보. 주문 15(딸기+망고 샤베트)는 **아이스크림 × 디저트 교차 조합**이라
카테고리를 넘나드는 연관을 보여준다. 바닐라—초코는 원본 주문 5에서 남은 유일한 아이스크림 쌍이다.

### 협업 필터링 추천 (기대값)

**✅ 실측 검증 완료** — 아래는 시뮬레이션이 아니라 **실제 `GET /api/graph/users/{id}/recommend` 응답**이다.
같은 질의를 MySQL(self-join 3번 + `NOT IN` 서브쿼리)로도 계산해서 **8명 전원 완전 일치**를 확인했다.

| 유저 | 추천 결과 |
|---|---|
| 1 순진 | 마카롱 3 / 망고 샤베트 2 / 딸기·복숭아 샤베트·피스타치오 각 1 |
| 2 유근 | 망고 샤베트 2 · 바닐라 2 · 팥빙수 2 / 망고·초코·피스타치오 각 1 |
| **3 연희** | **없음 (주문 이력 0 → 이웃 없음)** |
| 4 준영 | 티라미수 2 / 딸기·마카롱·바닐라·초코·팥빙수 각 1 |
| 5 재휘 | 딸기·마카롱·망고·티라미수 각 1 |
| 6 휘찬 | 딸기·망고·망고 샤베트·초코 각 1 |
| 7 주형 | 딸기·망고·바닐라·복숭아 샤베트·팥빙수 각 2 / 초코·피스타치오 각 1 |
| 8 민정 | 마카롱 2 · 복숭아 샤베트 2 · 티라미수 2 / 망고 1 |

*점수 = 그 메뉴를 먹은 "나와 취향 겹치는 이웃 유저" 수*
*주형(7)의 티라미수는 주문 17로 본인이 먹었기 때문에 후보에서 빠진다.*

---

## 4. 볼 수 있는 관계 6가지

`neo4j-practice.cypher` 의 번호와 대응.
**"그래프DB?" 열은 이 질의가 실제로 그래프DB를 정당화하는지에 대한 솔직한 평가다.**

| # | 질의 | 그래프DB? |
|---|---|---|
| 1 | 유저 주문 이력 (2-hop) | △ SQL 조인 2번이면 됨 |
| 2 | 함께 주문된 메뉴 | △ `order_details` self-join |
| 3 | 취향 겹치는 유저 (4-hop) | ○ 읽기가 편해짐 |
| 4 | 협업 필터링 추천 | ○ 읽기가 편해짐 (성능은 오히려 불리) |
| 5 | **두 유저 사이 최단 경로** | **★ SQL 로는 표현 자체가 어려움** |
| 6 | 그래프 차수 기반 인기 메뉴 | △ `GROUP BY` 면 됨 |

### 1) 유저 주문 이력 (2-hop)
`(:User)-[:PLACED]->(:Order)-[:CONTAINS]->(:Menu)`.
SQL로는 `order_history JOIN order_details JOIN menu` — 그래프에선 경로 하나.

### 2) 함께 주문된 메뉴 (장바구니 연관 분석)
같은 `Order` 를 공유하는 `Menu` 쌍. SQL이면 `order_details` self-join.

### 3) 취향이 겹치는 유저 (4-hop)
`User -> Order -> Menu <- Order <- User`.
방향을 거슬러 올라가는 탐색이 자연스럽게 표현된다.

### 4) 협업 필터링 추천
> "나와 같은 메뉴를 먹은 사람들이 먹었지만, 나는 아직 안 먹은 메뉴"

SQL이면 self-join 3번 + `NOT EXISTS` 서브쿼리. Cypher에선 경로 서술 + `WHERE NOT ... IN`.
**가독성은 확실히 낫다. 다만 §0 대로 실무에선 배치+KV 로 푸는 영역이다.**

### 5) 두 유저 사이 최단 경로 ★ 관계형 DB로는 표현 자체가 어려움
`shortestPath((a)-[:PLACED|CONTAINS*..10]-(b))`.
가변 길이 경로 탐색은 SQL의 재귀 CTE로 비슷하게 만들 수는 있지만 쓰기도 읽기도 고통스럽다.
user3은 어느 유저와도 경로가 없다 (고립 노드).
**현 단계에서 그래프DB가 유일하게 확실히 이기는 질의.**

### 6) 그래프 차수 기반 인기 메뉴
`count(DISTINCT u)` / `sum(c.count)`.
기존 `menu_order_statistics`(MySQL) · `best_menu`(Redis Hash) 집계와 **결과를 비교해보면 좋다.**

### 보너스 7) 그래프 탐색 + 속성 필터 조합
user8(크레딧 4500)의 추천 1순위는 망고(5000)인데 크레딧이 부족하다.
`AND candidate.price <= me.credits` 한 줄 추가 → 망고가 걸러지고 초코(4000)만 남는다.
경로 탐색과 속성 조건을 같은 쿼리에서 섞는 패턴.

---

## 5. API

| Method | Path | 설명 |
|---|---|---|
| POST | `/api/graph/sync` | MySQL 주문 데이터를 Neo4j로 전체 재적재 |
| GET | `/api/graph/users/{userId}/taste` | 유저가 주문한 메뉴 취향 집계 |
| GET | `/api/graph/co-ordered?limit=10` | 함께 주문된 메뉴 쌍 |
| GET | `/api/graph/users/{userId}/similar?limit=5` | 취향이 겹치는 유저 |
| GET | `/api/graph/users/{userId}/recommend?limit=5` | 협업 필터링 추천 |
| GET | `/api/graph/connection?from=5&to=4` | 두 유저 사이 최단 경로 |
| GET | `/api/graph/popularity?limit=10` | 그래프 차수 기반 인기 메뉴 |

```bash
curl -X POST localhost:8084/api/graph/sync
curl localhost:8084/api/graph/users/5/recommend
curl localhost:8084/api/graph/users/5/similar
curl localhost:8084/api/graph/co-ordered
curl "localhost:8084/api/graph/connection?from=5&to=4"
curl localhost:8084/api/graph/popularity
```

---

## 6. 파일 구성

기존 프로젝트 규칙(도메인에 인터페이스, `infrastructure`에 구현체)을 따랐다.

| 파일 | 역할 |
|---|---|
| `docker-compose.yml` | `neo4j:5.26-community` (7474 Browser / 7687 Bolt) |
| `application.yml` | `neo4j.uri / username / password / database` |
| `common/config/Neo4jConfig.kt` | `Driver` 싱글턴 빈 (`destroyMethod = "close"`) |
| `domain/graph/GraphModel.kt` | 도메인 모델 (`UserMenuTaste`, `GraphRecommendation` 등) |
| `domain/graph/OrderGraphRepository.kt` | 그래프 조회 인터페이스 |
| `domain/graph/OrderGraphSyncRepository.kt` | 적재 인터페이스 |
| `infrastructure/graph/CypherQueries.kt` | **Cypher 전부 한 곳에** (실습용으로 모아 보기 좋게) |
| `infrastructure/graph/OrderGraphSyncRepositoryImpl.kt` | `JdbcTemplate` 로 MySQL 읽어 `UNWIND` 로 적재 |
| `infrastructure/graph/OrderGraphRepositoryImpl.kt` | 조회 6종 |
| `application/graph/OrderGraphService.kt` | 서비스 |
| `api/GraphController.kt` | `/api/graph/**` |
| `src/main/resources/neo4j-practice.cypher` | Browser용 실습 스크립트 |
| `src/main/resources/init.sql` | 복수 메뉴 주문 9~16, 유저 7·8 추가 |

### 드라이버 사용 시 유의점

- **`Driver` 는 애플리케이션당 하나.** 내부에 커넥션 풀을 들고 있어서 `HikariDataSource` 처럼
  싱글턴으로 두고 재사용해야 한다. `@Bean(destroyMethod = "close")`.
- **`Session` 은 짧게, `use {}` 로 닫는다.** 스레드 세이프하지 않다.
- **`executeRead` / `executeWrite`** 를 쓰면 재시도 가능한 트랜잭션 함수로 실행된다.
  (구버전의 `readTransaction`/`writeTransaction` 은 deprecated)
- **파라미터는 반드시 `$param`.** 문자열 조립 금지 — 인젝션 + 쿼리 플랜 캐시 미스.
- **결과 정수는 `Long`.** `record["cnt"].asInt()` 로 받으면 편하다.
- `spring-boot-starter-data-neo4j` 를 안 넣었으므로 Spring Boot 자동설정이 없다.
  그래서 설정 프리픽스도 `spring.neo4j.*` 가 아니라 자체 `neo4j.*` 를 썼다.

---

## 7. 확장 — 성분 · 알러지 · 팔로우 · 카테고리

> **상태**
> **✅ 2026-09-06 구현·검증 완료.** `init.sql` · 동기화 코드 · Cypher 스크립트 · API 전부.
> 소셜 추천 8명 전원 기대값과 일치, 콜드스타트 폴백까지 실측 확인했다.

### 왜 이 확장이 §2 모델보다 그래프DB에 어울리는가

§2 의 협업 필터링은 개인화 축이 하나(내 주문 이력)뿐이라 **미리 구울 수 있었다.**
여기에 축을 세 개 더 붙이면 — **내가 누굴 팔로우하는지 + 내 알러지가 뭔지 + 내가 뭘 먹어봤는지** —
미리 구울 대상이 `상품 수(10만)` 에서 `유저 수 × 상황` 으로 폭발하고,
팔로우한 사람이 뭔가 새로 좋아하는 순간 캐시가 무효화된다.
**미리 굽는 게 무너지면 조회 시점 탐색이 답이 되고, 거기서 그래프DB가 값을 한다.**

팬아웃 모양도 유리하다. 팔로잉은 사람당 수십 명이라
"이 상품 산 사람 수백만 명"과 전혀 다른 규모다. §0 표의 조건에 정확히 들어맞는다.

### 어떤 요소가 진짜 값을 하는지 (냉정하게)

| 추가 요소 | 그래프DB가 이기나 | 이유 |
|---|---|---|
| **팔로우 관계** | ★ 크게 이김 | 소셜 그래프는 정석. 방향 + 가변 홉(팔로우의 팔로우) |
| **알러지 계층** | ★ 크게 이김 | "견과류 알러지" → 아몬드·피스타치오·호두 전부 제외. 재귀 |
| **카테고리 트리** | ○ 적당히 | 재귀 CTE·클로저 테이블로도 됨. 읽기가 편해지는 정도 |
| 성분 필터 | △ 별로 | 배열 훑기. SQL 도 `JSON_TABLE` 로 됨 |

**핵심: "성분/알러지를 붙였다"가 아니라, 그것들이 계층 구조를 가질 때** SQL이 아파진다.

### MySQL 스키마 (init.sql, 작성 완료)

기존 테이블에 컬럼 2개, 신규 테이블 6개. **조인 테이블을 만들지 않는 쪽으로 정리했다.**

```
MENU.category_id        INT   -> MENU_CATEGORY     ← 신규 컬럼 (조인 테이블 대신)
MENU.ingredients        JSON  ["우유","생크림",…]   ← 신규 컬럼 ★
MENU_CATEGORY           (id, name, parent_id)      ← 트리 ★
USER                    (id, name)                 ← 신규. 아이덴티티
INGREDIENT              (id, name, parent_id)      ← 성분 분류 트리 ★
USER_ALLERGY            (user_id, ingredient_id, registered_at)  ← 등록 시점 ★
USER_RELATION           (follower_id, followee_id) ← 팔로우, 방향 있음 ★
USER_MENU_LIKE          (user_id, menu_id, rating)
```

**설계 판단 네 가지**

1. **성분은 테이블이 아니라 `MENU.ingredients` JSON 배열.**
   상품이 자기 성분표를 들고 있다. 성분 마스터도 `MENU_INGREDIENT` 조인 테이블도 없다.
   → **그래서 동기화가 하는 일이 분명해진다**: MySQL 에 문자열로 뭉쳐 있는 걸
   그래프로 옮기며 `(:Ingredient)` 노드로 쪼갠다. **비정규화 → 정규화**가 곧 적재 과정이다.
   성분 분류는 `INGREDIENT` 트리 하나로 접었다 (아래 5번).

2. **카테고리도 조인 테이블 없이 `MENU.category_id`.** 메뉴 1 : 카테고리 1 이라
   `MENU_CATEGORY_MAP` 같은 건 과했다. 트리는 `MENU_CATEGORY.parent_id` 가 담당한다.

3. **`USER` 를 따로 만들었다.** 기존 `USER_CREDIT` 은 잔액 테이블이라 `id`/`credits` 뿐이라
   `(:User)` 노드에 붙일 이름이 없었다. id 를 공유한다.
   **`USER_CREDIT` → `USER` FK 는 일부러 안 걸었다** — `OrderControllerIntegrationTest` 가
   `USER_CREDIT` 에 임의 id(99999)를 단독 INSERT 하는데 FK 를 걸면 그 테스트가 깨진다.

4. **`ALLERGY` 와 `INGREDIENT_ALLERGY` 를 `INGREDIENT` 트리 하나로 합쳤다.**
   원래는 알러지 계층 테이블과 "성분 이름 → 알러지" 사전을 따로 뒀는데,
   **11건 중 4건이 항등 매핑**(우유→우유, 생크림→생크림, 연유→연유, 피스타치오→피스타치오)이라
   같은 이름을 두 테이블에 나눠 적는 꼴이었다.

   ```
   유제품 <- 우유 <- 마스카포네크림
   견과류 <- 아몬드 <- 아몬드가루
   땅콩   <- 땅콩버터        난류 <- 계란        밀 <- 밀가루
   설탕 / 물 / 팥 / 코코아 …  (조상 없음 = 알러지와 무관)
   ```

   **알러지는 테이블이 아니라 관계가 된다** — `USER_ALLERGY` 가 트리의 어느 노드를
   가리키느냐가 곧 그 유저의 알러지다. 주형은 `우유`, 재휘는 `유제품` 처럼
   사람마다 다른 높이를 가리킨다. `is_allergen` 같은 플래그가 필요 없다.

   Cypher 도 2홉에서 1홉으로 줄어든다.
   ```
   전:  (m)-[:MADE_WITH]->(:Ingredient)-[:HAS_ALLERGY]->(a)-[:SUBTYPE_OF*0..]->(mine)
   후:  (m)-[:MADE_WITH]->(:Ingredient)-[:PART_OF*0..]->(mine)
   ```

   **트레이드오프 — 다중 부모를 못 쓴다.** `parent_id` 가 한 컬럼이라
   마스카포네크림(실제로는 우유+생크림)에 우유 하나만 걸었다.
   조상이 `{마스카포네크림, 우유, 유제품}` 이라 우유·유제품 알러지 모두 걸리므로
   지금 데이터에선 기대값 변화가 없다. 실무에선 복합 성분이 흔해 이게 진짜 제약이 된다.

   **의미상의 비용**: `PART_OF` 가 "is-a"(우유는 유제품의 일종)와
   "contains"(마스카포네크림은 우유를 함유)를 한 엣지에 섞는다.
   알러지 전파 방향은 같아서 기능상 문제는 없지만 정확한 분류는 아니다.

   > **실무에선 보통 분리한다.** 식약처 알러지 표기 22종은 **법으로 고정된 목록**이고
   > 성분은 레시피 데이터라 소유자도 변경 주기도 다르다. 합치면
   > "이 노드가 법정 표기 대상인가?" 를 구분할 수 없어 결국 플래그가 다시 생긴다.
   > 이 실습은 그래프 탐색이 목적이라 단순함을 택했다.

5. **`USER_RELATION` 은 방향 있는 팔로우** (소셜커머스식). `follower_id → followee_id`,
   한 방향당 한 행. 맞팔이면 두 행 — 중복이 아니라 서로 다른 사실이다.
   §2 의 `CO_ORDERED`(무방향, 한 쌍당 한 행)와 대비되는 패턴이라 같이 보면 좋다.

6. `MENU.ingredients` 의 이름은 `INGREDIENT.name` 을 가리킨다(문자열 조인).
   알러지와 무관한 성분(설탕·물·팥…)도 트리에 넣어뒀다 — 조상이 없을 뿐이다.

> `MENU.ingredients` / `MENU.category_id` 는 **NULL 허용 필수**다.
> `MenuEntity`(JPA)와 통합 테스트가 이 컬럼들 없이 INSERT 한다. 동기화도 NULL 을 견뎌야 한다.

### 목표 그래프 모델

```
(:User {id, name, credits})
(:User)-[:FOLLOWS]->(:User)                    <- 방향 있음, 가변 깊이 ★
(:User)-[:ALLERGIC_TO {registeredAt}]->(:Ingredient) <- 트리의 어느 노드든 가리킬 수 있다 ★
(:User)-[:LIKES {rating}]->(:Menu)
(:User)-[:PLACED]->(:Order)-[:CONTAINS]->(:Menu)

(:Menu)-[:MADE_WITH]->(:Ingredient)            <- JSON 배열을 펼쳐 만든다
(:Ingredient)-[:PART_OF]->(:Ingredient)        <- 성분 분류 트리 ★
(:Menu)-[:IN_CATEGORY]->(:Category)
(:Category)-[:SUBCATEGORY_OF]->(:Category)     <- 트리 ★
```

**방향이 핵심이다.** Cypher 는 화살표로 그대로 구분한다 — SQL 이면 컬럼 순서를 바꿔가며 조인해야 한다.

```cypher
(me)-[:FOLLOWS]->(x)                                  // 내가 팔로우하는 사람
(me)<-[:FOLLOWS]-(x)                                  // 나를 팔로우하는 사람
(me)-[:FOLLOWS]->(x) AND (x)-[:FOLLOWS]->(me)         // 맞팔
```

### 시드 데이터

**유저** — id 는 기존과 동일, 이름만 추가.

| id | 이름 | 크레딧 | 알러지 | 확인할 것 |
|---|---|---|---|---|
| 1 | 순진 | 10000 | **견과류** *(상위 그룹)* | 하위 아몬드·피스타치오까지 걸리는지 ★ |
| 2 | 유근 | 8000 | 땅콩 | 견과류와 별개로 동작하는지 |
| 3 | 연희 | 50000 | 없음 | 필터가 하나도 안 걸리는 대조군 (주문은 0이라 §3 에선 콜드스타트) |
| 4 | 준영 | 500 | 없음 | 크레딧이 극단적으로 낮은 케이스 |
| 5 | 재휘 | 0 | **유제품** *(상위 그룹)* | 우유·생크림·연유 전부 걸리는지 ★ |
| 6 | 휘찬 | 6000 | 복숭아 | 단일 알러지 |
| 7 | 주형 | 20000 | **우유** *(하위 노드 직접, 3개월 전 등록)* | `PART_OF*0..` 의 `0` 케이스 ★ / **등록 이전 주문 케이스** ★ |
| 8 | 민정 | 4500 | 난류 | 크레딧 필터와 겹치는 케이스 |

**팔로우** (11행) — 맞팔 3쌍: 순진↔유근, 연희↔재휘, 휘찬↔주형

| 유저 | 팔로잉 | 팔로워 | 2홉 도달 |
|---|---|---|---|
| 1 순진 | 유근, 휘찬 | 유근 | 유근·준영·재휘·휘찬·주형 |
| 2 유근 | 순진, 재휘 | 순진, 민정 | 순진·연희·재휘·휘찬 |
| 3 연희 | 재휘 | 재휘 | 재휘 |
| 4 준영 | 주형 | 휘찬 | 휘찬·주형 |
| 5 재휘 | 연희 | 유근, 연희 | 연희 |
| 6 휘찬 | 준영, 주형 | 순진, 주형 | 준영·주형 |
| 7 주형 | 휘찬 | 준영, 휘찬 | 준영·휘찬 |
| **8 민정** | **유근** | — | 순진·유근·재휘 |

민정은 나가는 팔로우가 하나뿐이라 도달 범위가 좁다 — 크레딧 필터 데모용.
**팔로우 0인 유저는 없다** (§7 에는 콜드스타트 케이스가 없다. §3 의 연희는 *주문* 이 0인 경우다).

**메뉴** — 1~4는 기존(주문 이력이 달려 있어 ID 고정), 5~11이 확장분.
주문 9~16 이 디저트 복수 메뉴 주문으로 교체돼서, `CO_ORDERED` 7쌍이 디저트 중심으로 만들어지고
"이미 먹어본 메뉴 제외" 필터도 5명(순진·유근·준영·재휘·휘찬)에게서 실제로 발동한다.

| id | 메뉴 | 가격 | 카테고리 | 알러지 |
|---|---|---|---|---|
| 5 | 팥빙수 | 7000 | 디저트 > 빙수 | 우유, 연유 |
| 6 | 땅콩버터 아이스크림 | 4500 | … > 아이스크림 | **땅콩**, 우유, 생크림 |
| 7 | 피스타치오 젤라또 | 6000 | … > 젤라또 | **피스타치오(견과류)**, 우유 |
| 8 | 마카롱 | 2500 | 디저트 > 구움과자 | **아몬드(견과류)**, 난류 |
| 9 | 복숭아 샤베트 | 4000 | … > 샤베트 | **복숭아** |
| 10 | 티라미수 | 6500 | 디저트 > 케이크 | 우유, 생크림, 난류, 밀 |
| 11 | 망고 샤베트 | 4500 | … > 샤베트 | **없음** (안전 폴백 대조군) |

카테고리 트리는 3단(`디저트 > 빙과 > 아이스크림/젤라또/샤베트`)이라 조상 탐색에 깊이가 필요하다.

**알러지 계층** (식약처 표기 기준. 땅콩은 콩과라 견과류와 별도)

```
유제품 <- 우유, 생크림, 연유, 버터
견과류 <- 아몬드, 피스타치오, 호두, 캐슈
땅콩 / 난류 / 밀 / 복숭아 / 대두   (독립)
```

`마스카포네크림` 처럼 한 성분이 여러 알러지(우유+생크림)를 갖는 경우도 넣어뒀다.

### 알러지와 주문 이력이 충돌할 때 — 실무에선 어떻게 하나

시드를 짜다 보면 **"알러지를 등록한 유저가 그 성분이 든 메뉴를 주문/찜한"** 상태가 나온다.
처음엔 데이터 오류로 보고 전부 지웠는데, **실무에서는 이게 정상 상태다.**

#### 왜 정상인가

| 상황 | 설명 |
|---|---|
| **시간 순서** | 알러지는 나중에 등록된다. 작년에 먹은 티라미수 주문은 그대로 남는다 |
| **주문자 ≠ 섭취자** | 선물, 가족 대신 주문, 회식 주문 |
| **강도가 다르다** | 아나필락시스 vs 유당불내증. 후자는 락타아제 먹고 그냥 먹기도 한다 |
| **성분이 바뀐다** | 레시피·공급처 변경. 어제 안전하던 메뉴가 오늘 유발 성분을 갖는다 |
| **찜의 의도** | 선물용, 나중에 참고, 남에게 추천하려고 담아둠 |

특히 **주문 이력은 과거의 사실(immutable fact)** 이다. 정산 데이터이기도 해서 소급 수정하지 않는다.
알러지를 등록했다고 예전 주문을 고치는 시스템은 없다.

#### 처리 원칙 — 막지 말고 다르게 취급한다

DB 제약조건(`CHECK`)으로 아예 못 넣게 막는 경우는 거의 없다.

1. **주문 시점 — 차단이 아니라 소프트 블록**
   "이 메뉴에는 회원님이 등록하신 **견과류**가 포함되어 있습니다" → 명시적 확인 후 진행.
   그리고 **그 확인 기록을 남긴다**(`allergy_acknowledged_at`). UX 와 법적 방어 양쪽에 필요하다.

2. **화면 종류에 따라 다르게** — 이게 제일 중요하다

   | 화면 | 처리 |
   |---|---|
   | **추천** (시스템이 들이미는 것) | 조용히 **제외**. 사용자가 요청한 게 아니라 빠져도 손해가 없다 |
   | **검색·카테고리** (사용자가 찾는 것) | 제외하지 않고 **배지 표시**. 빼버리면 "왜 안 나오지?" 혼란 |
   | **찜 목록** | 그대로 두고 경고 배지. 선물용일 수 있다 |

3. **성분 스냅샷 저장** — 주문 시점의 성분표를 주문 상세에 복사해둔다.
   `order_details.menu_price` 를 복사해둔 것과 **정확히 같은 이유**다.
   나중에 레시피가 바뀌어도 "그때 뭘 먹었는지"가 보존되고, 리콜 대응에도 필요하다.

4. **성분 변경 알림** — "찜하신 메뉴의 성분이 변경되어 등록하신 알러지 정보와 겹칩니다."

> ⚠️ **필터는 편의 기능이지 안전장치가 아니다.** 성분 데이터 누락 하나로 사람이 다친다.
> 실제 서비스는 필터를 걸어도 항상 원본 성분표를 노출하고 최종 확인을 사용자에게 남긴다.
> "우리가 걸러주니 안심하세요"라고 말하는 순간 책임이 넘어온다.

#### 이 실습에 넣은 케이스

`USER_ALLERGY.registered_at` 을 두고, **주형(7) 한 명**만 이 상황으로 만들었다.

```
주형의 우유 알러지 등록 :  3개월 전
주문 17 (티라미수, 우유) :  6개월 전   -> 등록 이전 = 정상
그 외 유저의 알러지 등록 :  1년 전     -> 모든 주문이 등록 이후 = 충돌 없음
```

주문 17은 **단일 메뉴 주문이라 `CO_ORDERED` 7쌍에는 영향이 없다.**

그래서 "알러지 성분이 든 메뉴를 먹은 이력" 을 찾는 질의는 **시점 조건을 넣느냐 빼느냐로 답이 달라진다.**

```cypher
// 등록 시점을 무시하면 -> 주형의 티라미수가 "충돌" 로 잡힌다
MATCH (u:User)-[:PLACED]->(o:Order)-[:CONTAINS]->(m:Menu)
MATCH (u)-[r:ALLERGIC_TO]->(mine:Ingredient)
MATCH (m)-[:MADE_WITH]->(i:Ingredient)-[:PART_OF*0..]->(mine)
RETURN u.name, m.name, i.name AS 원인성분, o.orderedAt, r.registeredAt

// 등록 이후 주문만 = 실제로 경고해야 할 것 -> 결과 없음
  AND o.orderedAt > r.registeredAt
```

한 줄 차이로 **"과거의 흔적"과 "지금 경고할 일"이 갈린다.**
시간 축이 들어가면 같은 그래프 패턴도 의미가 달라진다는 걸 보여주는 케이스다.

### Neo4j Browser 로 볼 때의 함정

**브라우저는 반환된 데이터에 포함된 관계만 그린다.** 노드 두 개를 각각 반환하면
둘 사이에 실제로 관계가 있어도 화살표가 안 그려지고 떨어진 동그라미로 보인다.

```cypher
-- ❌ 순진이 혼자 떨어져 나온다. ALLERGIC_TO 가 반환 데이터에 없기 때문
MATCH (u:User {name:'순진'})-[:ALLERGIC_TO]->(mine:Ingredient)
MATCH p = (m:Menu)-[:MADE_WITH]->(:Ingredient)-[:PART_OF*0..]->(mine)
RETURN p, u

-- ✅ 유저에서 시작하는 하나의 경로로 묶는다
MATCH path = (u:User {name:'순진'})-[:ALLERGIC_TO]->(:Ingredient)
                                   <-[:PART_OF*0..]-(:Ingredient)
                                   <-[:MADE_WITH]-(:Menu)
RETURN path
```

설정(톱니 → `Connect result nodes`)으로 자동 연결시킬 수도 있지만,
**경로 전체를 하나의 `path` 로 반환하는 게 확실하다.**

### 하이라이트 질의

> "내가 팔로우하는 사람(과 그들이 팔로우하는 사람)이 좋아하는 메뉴 중,
> 내 알러지 성분이 없고, 내가 아직 안 먹어본 것"

```cypher
MATCH (me:User {id: $userId})-[:FOLLOWS*1..2]->(other:User)
MATCH (other)-[:LIKES]->(candidate:Menu)
WHERE NOT (me)-[:PLACED]->(:Order)-[:CONTAINS]->(candidate)
  AND NOT EXISTS {
    MATCH (me)-[:ALLERGIC_TO]->(mine:Ingredient)
    MATCH (candidate)-[:MADE_WITH]->(:Ingredient)-[:PART_OF*0..]->(mine)
  }
  AND candidate.price <= me.credits
RETURN candidate.name, count(DISTINCT other) AS score
ORDER BY score DESC
```

- `FOLLOWS*1..2` — **방향 있는** 가변 길이 탐색. 화살표를 빼면 팔로워까지 딸려온다.
- `PART_OF*0..` 의 `0` 이 포인트 — 성분 자기 자신도 매칭돼서
  "우유 알러지"(주형)와 "유제품 알러지"(재휘)를 한 패턴으로 처리한다.

같은 걸 SQL로 쓰면 재귀 CTE 2개 + `JSON_TABLE` + `NOT EXISTS` + 조인 대여섯 개다.
**"직관적인 Cypher"라는 주장이 증명되는 지점이라, SQL 버전도 나란히 써서 비교할 것.**

### 기대값 (시뮬레이션으로 검증)

크레딧 필터까지 적용. **점수 = 그 메뉴를 좋아하는 2홉 이내 팔로잉 수.**

| 유저 | 추천 결과 | 걸러진 것 |
|---|---|---|
| **1 순진** | 땅콩버터 2 · 복숭아샤베트 2 / 망고샤베트 1 | **피스타치오(2)·마카롱(2) — 견과류 계층** ★ / 팥빙수·티라미수 — 이미 먹음 |
| 2 유근 | 팥빙수 2 · 피스타치오 2 / 망고샤베트·바닐라·복숭아샤베트 각 1 | 땅콩버터 — 땅콩 / 마카롱·티라미수 — 이미 먹음 |
| 3 연희 | 마카롱 1 · 복숭아샤베트 1 | 없음 — 알러지도 크레딧 제약도 없는 대조군 |
| 4 준영 | 없음 | 4종 크레딧(500) 부족 / 망고샤베트 — 이미 먹음 |
| 5 재휘 | 없음 | 팥빙수·피스타치오 — **유제품 계층** ★ / 망고샤베트 — 이미 먹음 |
| 6 휘찬 | 땅콩버터 1 · 망고샤베트 1 | 복숭아샤베트 — 복숭아 / 마카롱 — 이미 먹음 |
| **7 주형** | 복숭아샤베트 1 | **땅콩버터(2)·피스타치오 — 우유** ★ / 티라미수 — 이미 먹음(*알러지 등록 이전 주문*) |
| **8 민정** | 바닐라 1 · 복숭아샤베트 1 | 마카롱 — 난류 / 팥빙수(2)·피스타치오 — **크레딧 4500** ★ |

가장 볼 만한 케이스:
- **순진(1)** — 피스타치오 젤라또가 점수 2로 상위권인데 **"견과류" 한 단계 위 개념** 때문에 사라진다.
  성분에 "견과류"라는 단어는 없고 "피스타치오"만 있다. 계층을 타야만 걸린다.
- **주형(7)** — `우유` 알러지 하나로 후보 4종 중 3종이 날아간다. 유제품 기반 디저트의 현실.
- **민정(8)** — 알러지(마카롱)와 크레딧(팥빙수·피스타치오) 두 필터가 동시에 작동한다.

> API(`/recommend`)에는 크레딧 필터가 없으므로 위 표와 다를 수 있다.

### 구현 전 짚어둘 것

1. **홉 수 상한을 습관적으로 걸 것.** `FOLLOWS*1..2` 에서 `..2` 를 빼면 도달 가능한 전체를 훑는다.
   실습 데이터는 작아서 티가 안 나지만, 그래프 질의의 대표적인 사고 원인이다.
2. **화살표 방향을 빼먹지 말 것.** `-[:FOLLOWS*1..2]-` 로 쓰면 팔로워까지 섞여 결과가 달라진다.
3. **알러지 필터를 안전장치로 신뢰하면 안 된다.** 성분 데이터가 하나 누락되면 사람이 다치는
   도메인이라, 실무라면 "추천에서 제외"가 아니라 "표기 후 사용자 확인"이 맞다.
4. 확장 테이블은 **JPA 엔티티가 아니다.** `ddl-auto: none` 이라 Hibernate 검증에 걸리지 않고,
   그래프 동기화(JdbcTemplate)에서만 읽는다.
5. **`ALLERGIC_TO` 관계에 `registeredAt` 을, `(:Order)` 에 `orderedAt` 을 반드시 실어야 한다.**
   둘 중 하나라도 빠지면 "등록 이전 주문" 을 구분할 수 없다.
6. **시드를 바꿀 때마다 정합성 검증을 다시 돌릴 것** — 금액 합계, 단가×수량,
   그리고 *알러지 등록 이후* 주문·LIKES 가 본인 알러지와 충돌하지 않는지.
   이 검증으로 이번에 3건을 잡았다(재휘의 바닐라 주문, 주형의 팥빙수 LIKES, 민정의 티라미수 LIKES).

### 콜드스타트 폴백 (구현 완료)

`GET /api/graph/users/{id}/recommend/social` 은 소셜 추천이 비면 인기 메뉴로 폴백한다.

**빈 결과의 원인이 두 가지**인데 사용자에겐 똑같이 빈 화면이다.

| 경우 | 상황 | 현재 시드 |
|---|---|---|
| 진짜 콜드스타트 | 팔로우도 주문도 없는 신규 유저 | 없음 (전원 팔로우 있음) |
| 필터로 다 걸림 | 이웃은 있는데 알러지·크레딧·이미먹음으로 전부 제외 | 준영(크레딧 500), 재휘(크레딧 0) |

동작은 같지만 `note` 로 이유를 구분해 내려준다. UI 문구가 달라야 하기 때문이다.

**폴백에서 필터를 어떻게 다루는가 — 이게 핵심 판단이다.**

```
알러지 · 이미 먹은 것  ->  폴백에서도 반드시 제외
크레딧 부족           ->  제외하지 않고 affordable: false 플래그로만 표시
```

인기 있다고 견과류 알러지가 있는 사람에게 마카롱을 들이밀 수는 없다 — **안전 문제**다.
반면 잔액 부족은 결제 시점 문제지 "보여주면 안 되는" 이유가 아니다.
추천에서 아예 지우면 사용자는 "왜 안 보이지?" 가 된다.

**실측 결과**

| 유저 | 전략 | 결과 |
|---|---|---|
| 1 순진 / 2 유근 / 3 연희 / 6 휘찬 / 7 주형 / 8 민정 | `SOCIAL` | 소셜 추천 그대로 |
| 4 준영 (크레딧 500) | `POPULAR_FALLBACK` | 7종 전부 `affordable: false` |
| **5 재휘 (크레딧 0, 유제품)** | `POPULAR_FALLBACK` | **마카롱 하나뿐** ★ |

재휘가 이 구현의 증명이다. 인기 메뉴 상위가 전부 유제품인데
**폴백에서도 유제품 알러지로 걸러져** 마카롱(아몬드·난류)만 남았다.
이미 먹은 복숭아·망고 샤베트도 빠졌다.

응답 형태:
```json
{
  "userId": 5,
  "strategy": "POPULAR_FALLBACK",
  "note": "팔로우 기반 후보가 알러지·크레딧·이미 먹은 메뉴로 모두 걸러졌습니다.",
  "items": [{ "menuName": "마카롱", "price": 2500, "score": 3, "affordable": false }]
}
```

호출자가 `strategy` 를 보고 문구를 바꿀 수 있다 —
*"팔로우하는 분들이 좋아해요"* vs *"많이 찾는 메뉴예요"*.

### 남은 구현 순서

1. ~~`init.sql` — 테이블 + 시드~~ ✅ 완료
2. ~~**동기화 코드**~~ ✅ 완료. `MENU.ingredients` JSON 을 `JSON_TABLE` 로 펼쳐
   `(:Menu)-[:MADE_WITH]->(:Ingredient)` 로 적재. 관계 8종 전부 구현.
3. ~~**`neo4j-practice.cypher`**~~ ✅ §7 블록 6개(8~13) 추가.
   대조용 SQL 버전(재귀 CTE + `JSON_TABLE`)은 아직 안 넣었다.
4. ~~**API 추가**~~ ✅ `/recommend/social`(폴백 포함), `/allergy-conflicts`

## 8. 다음에 이어서 해볼 것

### 실측 결과 (2026-09-06)

- [x] **런타임 검증 완료.** Docker(db·redis·neo4j) → `bootRun` → `POST /api/graph/sync` → 6개 엔드포인트 전부 확인.
- **찾은 버그 1건** — `OrderGraphSyncRepositoryImpl` 가 `CREATE CONSTRAINT`(스키마 변경)를
  데이터 쓰기와 **같은 트랜잭션**에서 실행해 sync 가 500 으로 실패했다.
  > `Tried to execute Write query after executing Schema modification`

  Neo4j 는 한 트랜잭션 안에서 스키마 변경 후 데이터 쓰기를 금지한다.
  **제약조건을 각각 별도 트랜잭션으로 먼저 적용**하도록 고쳤다.
  컴파일·정적분석으로는 안 잡히고 실제로 돌려야만 나오는 종류다.
- **Kafka 미기동은 비치명적** — `KafkaAdmin` 이 타임아웃 ERROR 를 남기지만 기동은 된다(약 30초 지연).
  그래프 엔드포인트만 볼 거면 `db redis neo4j` 만 띄우면 된다.
- `sync` 응답: `userCount 8 / menuCount 11 / orderCount 17 / containsCount 26 / coOrderedCount 7` — 전부 기대값 일치.
- **교차 검증**: 협업 필터링을 MySQL(self-join 3번 + `NOT IN`)로도 계산해
  그래프 API 응답과 **8명 전원 완전 일치** 확인. 구현이 옳다는 걸 독립적으로 확인한 셈.
- 최단 경로 `?from=5&to=4` → 4홉 `User:5 → Order:12 → Menu:복숭아 샤베트 → Order:11 → User:4`.
  두 유저가 같은 메뉴를 주문해서 연결된다.
- MySQL 쪽 검증도 통과: 테이블 12개 생성, 한글 utf8mb4 정상,
  `CO_ORDERED` 7쌍이 SQL self-join 결과와 일치, 알러지 시점 판정(주형 티라미수 = 등록 이전) 정상.

**검증 체크리스트** (§3 기대값과 대조) — ✅ 전부 통과

| 확인 | 기대값 |
|---|---|
| `GET /api/graph/co-ordered` | 7쌍, weight 2/2/1/1/1/1/1 (§3 표) |
| `GET /api/graph/users/2/recommend` | 망고샤베트·바닐라·팥빙수 각 점수2 |
| `GET /api/graph/users/3/recommend` | 빈 배열 (콜드스타트) |
| `GET /api/graph/connection?from=5&to=4` | 최단 경로 |
| `GET /api/graph/popularity` | 차수 기반 인기 메뉴 |

그 다음 Neo4j Browser 에서 `neo4j-practice.cypher` 블록별 실행하며 눈으로 확인.

### 그 이후

- [x] ~~**§7 확장 구현**~~ ✅ 완료 (적재 8종 · 질의 2종 · API 2개 · 콜드스타트 폴백)
- [ ] **인기 메뉴 3중 비교** — MySQL `menu_order_statistics` / Redis `best_menu` /
      그래프 degree. 세 결과가 일치하는지, 다르면 왜 다른지.
- [x] ~~**콜드스타트 폴백**~~ ✅ 완료 (아래 §7 참고)
- [ ] **증분 동기화** — 지금은 `CLEAR_GRAPH` 후 전체 재적재. 이미 Kafka로 주문 이벤트를
      발행하고 있으니(`OrderRepositoryImpl`), 컨슈머에서 그래프에 증분 반영하는 게 자연스럽다.
- [ ] **기존 추천과 비교** — `application/recommend` 의 OpenAI 기반 추천 vs 그래프 협업 필터링.
      그래프 추천 결과를 LLM 프롬프트의 컨텍스트로 넣어주는 하이브리드도 가능.
- [ ] **APOC / GDS 플러그인** — PageRank, 커뮤니티 탐지(Louvain), 노드 유사도(Jaccard).
      `NEO4J_PLUGINS: '["apoc","graph-data-science"]'` 로 추가. 지금은 순수 Cypher만 쓴다.
- [ ] **인덱스/성능** — 지금은 `id` UNIQUE 제약(=인덱스)만. 데이터를 키우고
      `PROFILE` 로 실행 계획을 보면서 `db hits` 를 비교해보기.
      §7 확장 후 가변 길이 탐색의 팬아웃도 여기서 확인.
- [ ] **테스트** — Testcontainers `Neo4jContainer` 로 통합 테스트.

## 참고

- 컨테이너 볼륨: `docker/db/neo4j/{data,logs}` (`.gitignore` 처리됨)
- 그래프 초기화: `MATCH (n) DETACH DELETE n` 또는 `POST /api/graph/sync` (내부에서 비우고 재적재)
- Neo4j 5 환경변수 규칙: `server.memory.heap.max_size` → `NEO4J_server_memory_heap_max__size`
  (`.` → `_`, `_` → `__`)
  - 단 `pagecache_size` 는 원래 언더스코어가 1개라 `NEO4J_server_memory_pagecache_size` 가 맞다
    (`pagecache__size` 아님 — 실제로 겪은 버그)
