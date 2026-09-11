// =====================================================================
// order-system 그래프 실습
// Neo4j Browser(http://localhost:7474) 에서 블록 단위로 실행하세요.
// 데이터는 POST /api/graph/sync 로 적재됩니다. (init.sql 기준)
//
// 그래프 모델
//   (:User {id, credits})  (:Menu {id, name, price})  (:Order {id, price, orderedAt})
//   (:User)-[:PLACED]->(:Order)-[:CONTAINS {count, menuPrice}]->(:Menu)
//   (:Menu)-[:CO_ORDERED {weight}]->(:Menu)   // 파생 관계
// =====================================================================


// ---------------------------------------------------------------------
// 0. 전체 그래프 눈으로 보기 (노드가 몇 개 안 되니 통째로)
// ---------------------------------------------------------------------
MATCH (n) RETURN n;

// 노드/관계 개수 확인
MATCH (n) RETURN labels(n)[0] AS label, count(*) AS cnt ORDER BY cnt DESC;
MATCH ()-[r]->() RETURN type(r) AS relType, count(*) AS cnt ORDER BY cnt DESC;


// ---------------------------------------------------------------------
// 1. 유저 1번의 주문 이력 (2-hop 탐색)
//    SQL: order_history JOIN order_details JOIN menu
// ---------------------------------------------------------------------
MATCH path = (u:User {id: 1})-[:PLACED]->(:Order)-[:CONTAINS]->(:Menu)
RETURN path;

// 집계로 압축 → 취향 프로필
MATCH (u:User {id: 1})-[:PLACED]->(o:Order)-[c:CONTAINS]->(m:Menu)
RETURN m.name AS 메뉴, count(DISTINCT o) AS 주문횟수, sum(c.count) AS 총수량
ORDER BY 총수량 DESC;


// ---------------------------------------------------------------------
// 2. 함께 주문된 메뉴 (장바구니 연관 분석)
//    메뉴 4종의 6개 조합이 모두 엣지로 생깁니다. weight 가 높은 쌍이 곧 세트 메뉴 후보.
//    기대값: 바닐라—딸기 3, 초코—망고 3, 딸기—망고 2, 나머지 1
// ---------------------------------------------------------------------
MATCH (m1:Menu)-[r:CO_ORDERED]-(m2:Menu)
RETURN m1.name AS 메뉴A, m2.name AS 메뉴B, r.weight AS 함께주문된횟수
ORDER BY 함께주문된횟수 DESC;

// 파생 관계 없이 원본 경로로 직접 계산해도 결과는 같다
MATCH (m1:Menu)<-[:CONTAINS]-(o:Order)-[:CONTAINS]->(m2:Menu)
WHERE m1.id < m2.id
RETURN m1.name, m2.name, count(DISTINCT o) AS weight;


// ---------------------------------------------------------------------
// 3. 취향이 겹치는 유저 찾기 (4-hop: User→Order→Menu←Order←User)
//    유저 5번(바닐라·딸기) 기준. 기대값: user2/user8 이 2개 겹침, user1/user6/user7 이 1개.
// ---------------------------------------------------------------------
MATCH (me:User {id: 5})-[:PLACED]->(:Order)-[:CONTAINS]->(m:Menu)
      <-[:CONTAINS]-(:Order)<-[:PLACED]-(other:User)
WHERE other.id <> me.id
RETURN other.id AS 이웃유저, collect(DISTINCT m.name) AS 겹치는메뉴
ORDER BY size(겹치는메뉴) DESC;


// ---------------------------------------------------------------------
// 4. 협업 필터링 추천 ★ 그래프DB 의 하이라이트
//    "나와 같은 메뉴를 먹은 사람들이 먹었지만, 나는 아직 안 먹은 메뉴"
//    SQL 이면 self-join 3번 + NOT EXISTS. 여기선 경로 하나로 끝납니다.
// ---------------------------------------------------------------------
MATCH (me:User {id: 5})-[:PLACED]->(:Order)-[:CONTAINS]->(mine:Menu)
WITH me, collect(DISTINCT mine) AS myMenus
MATCH (me)-[:PLACED]->(:Order)-[:CONTAINS]->(:Menu)
      <-[:CONTAINS]-(:Order)<-[:PLACED]-(other:User)
WHERE other <> me
MATCH (other)-[:PLACED]->(:Order)-[:CONTAINS]->(candidate:Menu)
WHERE NOT candidate IN myMenus
RETURN candidate.name AS 추천메뉴, candidate.price AS 가격,
       count(DISTINCT other) AS 점수, collect(DISTINCT other.id) AS 추천한이웃
ORDER BY 점수 DESC;

// 기대값: 망고 점수3(user 1/6/7), 초코 점수2(user 1/7)

// 콜드 스타트: 유저 3번은 주문이 없어 이웃이 잡히지 않는다 → 빈 결과.
// 그래프 기반 추천이 신규 유저에게 무력한 지점. (인기 메뉴 폴백이 필요한 이유)
MATCH (me:User {id: 3})-[:PLACED]->(:Order)-[:CONTAINS]->(mine:Menu)
RETURN count(mine) AS 내가먹은메뉴수;

MATCH (u:User {id: 3}) RETURN u;   // 고립 노드 눈으로 확인


// ---------------------------------------------------------------------
// 5. 두 유저 사이의 최단 경로 ★ 관계형 DB 로는 표현 자체가 어려운 질의
//    취향이 안 겹치는 유저 5번과 4번은 몇 다리 건너 연결될까?
//    유저 3번은 어느 유저와도 경로가 없다 (빈 결과).
// ---------------------------------------------------------------------
MATCH (a:User {id: 5}), (b:User {id: 4})
MATCH p = shortestPath((a)-[:PLACED|CONTAINS*..10]-(b))
RETURN p;

// 경로를 읽기 좋게
MATCH (a:User {id: 5}), (b:User {id: 4})
MATCH p = shortestPath((a)-[:PLACED|CONTAINS*..10]-(b))
RETURN length(p) AS 홉수,
       [n IN nodes(p) | head(labels(n)) + ':' + coalesce(n.name, toString(n.id))] AS 경로;

// 모든 유저 쌍의 거리 (연결이 없는 쌍은 아예 행이 안 나옴)
MATCH (a:User), (b:User) WHERE a.id < b.id
OPTIONAL MATCH p = shortestPath((a)-[:PLACED|CONTAINS*..10]-(b))
RETURN a.id AS from, b.id AS to, length(p) AS hops
ORDER BY hops, from, to;


// ---------------------------------------------------------------------
// 6. 그래프 차수(degree) 기반 인기 메뉴
//    MySQL menu_order_statistics / Redis best_menu 집계 결과와 비교해보기
// ---------------------------------------------------------------------
MATCH (u:User)-[:PLACED]->(o:Order)-[c:CONTAINS]->(m:Menu)
RETURN m.name AS 메뉴, count(DISTINCT u) AS 구매유저수,
       count(DISTINCT o) AS 주문수, sum(c.count) AS 총판매량
ORDER BY 구매유저수 DESC, 총판매량 DESC;

// 아무도 주문하지 않은 메뉴 = 고립 노드 찾기
MATCH (m:Menu) WHERE NOT (m)<-[:CONTAINS]-() RETURN m.name AS 안팔린메뉴;
MATCH (u:User) WHERE NOT (u)-[:PLACED]->() RETURN u.id AS 주문없는유저, u.credits AS 크레딧;


// ---------------------------------------------------------------------
// 7. 크레딧으로 살 수 있는 추천만 걸러내기 (그래프 탐색 + 속성 필터 조합)
//    유저 8번은 크레딧이 4500. 추천 1순위 망고(5000)는 못 사고 초코(4000)만 남는다.
// ---------------------------------------------------------------------
// 필터 없이: 망고 점수3, 초코 점수2
MATCH (me:User {id: 8})-[:PLACED]->(:Order)-[:CONTAINS]->(mine:Menu)
WITH me, collect(DISTINCT mine) AS myMenus
MATCH (me)-[:PLACED]->(:Order)-[:CONTAINS]->(:Menu)
      <-[:CONTAINS]-(:Order)<-[:PLACED]-(other:User)-[:PLACED]->(:Order)-[:CONTAINS]->(candidate:Menu)
WHERE other <> me AND NOT candidate IN myMenus
RETURN candidate.name AS 추천메뉴, candidate.price AS 가격, count(DISTINCT other) AS 점수
ORDER BY 점수 DESC;

// 크레딧 필터 적용: 초코만 남는다
MATCH (me:User {id: 8})-[:PLACED]->(:Order)-[:CONTAINS]->(mine:Menu)
WITH me, collect(DISTINCT mine) AS myMenus
MATCH (me)-[:PLACED]->(:Order)-[:CONTAINS]->(:Menu)
      <-[:CONTAINS]-(:Order)<-[:PLACED]-(other:User)-[:PLACED]->(:Order)-[:CONTAINS]->(candidate:Menu)
WHERE other <> me AND NOT candidate IN myMenus AND candidate.price <= me.credits
RETURN candidate.name AS 추천메뉴, candidate.price AS 가격, me.credits AS 내크레딧,
       count(DISTINCT other) AS 점수
ORDER BY 점수 DESC;


// ---------------------------------------------------------------------
// 정리: 그래프 비우기
// ---------------------------------------------------------------------
// MATCH (n) DETACH DELETE n;


// =====================================================================
// §7 확장 — 성분 · 알러지 · 팔로우 · 카테고리
// =====================================================================

// ---------------------------------------------------------------------
// 8. 성분 분류 트리 눈으로 보기
//    상위 = 알러지 그룹, 하위 = 구체 성분. 조상이 없는 성분은 알러지와 무관.
// ---------------------------------------------------------------------
MATCH p = (i:Ingredient)-[:PART_OF*]->(root:Ingredient)
WHERE NOT (root)-[:PART_OF]->()
RETURN p;

// ---------------------------------------------------------------------
// 9. 팔로우 관계망 — 화살표 방향과 맞팔이 눈에 보인다
//    맞팔 3쌍: 순진↔유근, 연희↔재휘, 휘찬↔주형
// ---------------------------------------------------------------------
MATCH p = (:User)-[:FOLLOWS]->(:User) RETURN p;

// 맞팔만
MATCH (a:User)-[:FOLLOWS]->(b:User)
WHERE (b)-[:FOLLOWS]->(a) AND a.id < b.id
RETURN a.name AS 유저, b.name AS 맞팔상대;

// ---------------------------------------------------------------------
// 10. 내 알러지가 어느 메뉴까지 뻗는지 (계층 탐색) ★
//     성분표에 '견과류' 라는 단어는 없다. 트리를 타야만 걸린다.
// ---------------------------------------------------------------------
--   ※ 반드시 유저에서 시작하는 '하나의 경로' 로 반환할 것.
--     RETURN p, u 처럼 경로와 노드를 따로 반환하면 ALLERGIC_TO 화살표가
--     반환 데이터에 없어서 유저가 혼자 떨어진 동그라미로 그려진다.
MATCH path = (u:User {name: '순진'})-[:ALLERGIC_TO]->(:Ingredient)
                                    <-[:PART_OF*0..]-(:Ingredient)
                                    <-[:MADE_WITH]-(:Menu)
RETURN path;

// 주형 케이스 ★★ — 티라미수 성분표엔 '우유' 라는 글자가 없다.
//   적혀 있는 건 '마스카포네크림' 뿐인데, 트리를 타고 우유에 닿는다.
//   평평한 조인으로 짰으면 놓쳤을 케이스.
MATCH path = (:User {name: '주형'})-[:ALLERGIC_TO]->(:Ingredient)
                                   <-[:PART_OF*0..]-(:Ingredient)
                                   <-[:MADE_WITH]-(:Menu {name: '티라미수'})
RETURN path;

// 표로 보기 — 원인 성분이 무엇인지
MATCH (u:User)-[:ALLERGIC_TO]->(mine:Ingredient)
MATCH (m:Menu)-[:MADE_WITH]->(cause:Ingredient)-[:PART_OF*0..]->(mine)
RETURN u.name AS 유저, mine.name AS 알러지, m.name AS 못먹는메뉴, cause.name AS 원인성분
ORDER BY 유저, 못먹는메뉴;

// ---------------------------------------------------------------------
// 11. ★★ 하이라이트 — 팔로우 + 알러지 + 이미먹음 + 크레딧을 한 질의에
//     "내가 팔로우하는 사람(의 팔로우)이 좋아하는 메뉴 중,
//      알러지 성분이 없고, 안 먹어봤고, 크레딧으로 살 수 있는 것"
//
//     가변 길이 탐색 2개 중첩: FOLLOWS*1..2 (방향), PART_OF*0.. (계층)
//     기대값(순진): 땅콩버터 2, 복숭아샤베트 2, 망고샤베트 1
//                   제외 → 피스타치오·마카롱(견과류), 팥빙수·티라미수(이미먹음)
// ---------------------------------------------------------------------
MATCH (me:User {name: '순진'})-[:FOLLOWS*1..2]->(other:User)
WHERE other <> me
MATCH (other)-[:LIKES]->(candidate:Menu)
WHERE NOT EXISTS { (me)-[:PLACED]->(:Order)-[:CONTAINS]->(candidate) }
  AND NOT EXISTS {
        MATCH (me)-[:ALLERGIC_TO]->(mine:Ingredient)
        MATCH (candidate)-[:MADE_WITH]->(:Ingredient)-[:PART_OF*0..]->(mine)
      }
  AND candidate.price <= me.credits
RETURN candidate.name AS 추천, candidate.price AS 가격,
       count(DISTINCT other) AS 점수, collect(DISTINCT other.name) AS 좋아한사람
ORDER BY 점수 DESC, 추천;

// 왜 걸러졌는지 같이 보기 (필터를 조건이 아니라 라벨로)
MATCH (me:User {name: '순진'})-[:FOLLOWS*1..2]->(other:User) WHERE other <> me
MATCH (other)-[:LIKES]->(c:Menu)
WITH me, c, count(DISTINCT other) AS score,
     EXISTS { MATCH (c)-[:MADE_WITH]->(:Ingredient)-[:PART_OF*0..]->(:Ingredient)<-[:ALLERGIC_TO]-(me) } AS allergyHit,
     EXISTS { (me)-[:PLACED]->(:Order)-[:CONTAINS]->(c) } AS eaten
RETURN c.name AS 메뉴, score AS 점수,
       CASE WHEN eaten THEN '이미먹음'
            WHEN allergyHit THEN '알러지'
            WHEN c.price > me.credits THEN '크레딧'
            ELSE '추천' END AS 판정
ORDER BY 판정, 점수 DESC;

// ---------------------------------------------------------------------
// 12. 알러지 성분이 든 메뉴를 주문한 이력 — 시점 조건 한 줄의 차이 ★
//     알러지는 나중에 등록되므로 "등록 이전 주문" 은 데이터 오류가 아니다.
// ---------------------------------------------------------------------
// (a) 시점을 무시하면 -> 주형의 티라미수가 잡힌다
MATCH (u:User)-[r:ALLERGIC_TO]->(mine:Ingredient)
MATCH (u)-[:PLACED]->(o:Order)-[:CONTAINS]->(m:Menu)
MATCH (m)-[:MADE_WITH]->(cause:Ingredient)-[:PART_OF*0..]->(mine)
RETURN u.name AS 유저, m.name AS 메뉴, cause.name AS 원인성분,
       o.orderedAt AS 주문시각, r.registeredAt AS 등록시각;

// (b) 등록 이후 주문만 = 실제로 경고해야 할 것 -> 결과 없음
MATCH (u:User)-[r:ALLERGIC_TO]->(mine:Ingredient)
MATCH (u)-[:PLACED]->(o:Order)-[:CONTAINS]->(m:Menu)
MATCH (m)-[:MADE_WITH]->(cause:Ingredient)-[:PART_OF*0..]->(mine)
WHERE o.orderedAt > r.registeredAt
RETURN u.name AS 유저, m.name AS 메뉴, cause.name AS 원인성분;

// ---------------------------------------------------------------------
// 13. 카테고리 트리 — 디저트 > 빙과 > 아이스크림
// ---------------------------------------------------------------------
MATCH p = (m:Menu)-[:IN_CATEGORY]->(:Category)-[:SUBCATEGORY_OF*0..]->(:Category)
RETURN p;
