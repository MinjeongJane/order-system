-- 인코딩 및 DB, 타임존 설정
SET NAMES utf8;
SET GLOBAL time_zone = '+09:00';
SET GLOBAL event_scheduler = ON;
USE `order_system`;

-- 테이블 삭제 (존재할 경우) — FK 자식을 먼저 지운다
DROP TABLE IF EXISTS `USER_RELATION`;
DROP TABLE IF EXISTS `USER_ALLERGY`;
DROP TABLE IF EXISTS `USER_MENU_LIKE`;
DROP TABLE IF EXISTS `INGREDIENT`;
DROP TABLE IF EXISTS `USER`;
DROP TABLE IF EXISTS `MENU_ORDER_STATISTICS`;
DROP TABLE IF EXISTS `ORDER_DETAILS`;
DROP TABLE IF EXISTS `ORDER_HISTORY`;
DROP TABLE IF EXISTS `MENU`;
DROP TABLE IF EXISTS `MENU_CATEGORY`;
DROP TABLE IF EXISTS `USER_CREDIT`;

CREATE USER IF NOT EXISTS 'exporter'@'%' IDENTIFIED BY 'strong_password';
GRANT PROCESS, REPLICATION CLIENT, SELECT ON *.* TO 'exporter'@'%';

-- USER_CREDIT
CREATE TABLE IF NOT EXISTS `USER_CREDIT` (
  `id` INT PRIMARY KEY,
  `credits` INT NOT NULL DEFAULT 0,
  `created_by` VARCHAR(50) DEFAULT 'system',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `modified_by` VARCHAR(50) DEFAULT 'system',
  `modified_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `deleted` TINYINT(1) NOT NULL
);

-- MENU_CATEGORY : 카테고리 트리 ★ (self-reference)
--   디저트 > 빙과 > 아이스크림 처럼 3단. 메뉴는 MENU.category_id 로 잎 노드를 가리킨다.
CREATE TABLE IF NOT EXISTS `MENU_CATEGORY` (
  `id` INT PRIMARY KEY,
  `name` VARCHAR(50) NOT NULL,
  `parent_id` INT NULL,
  FOREIGN KEY (`parent_id`) REFERENCES `MENU_CATEGORY`(`id`)
);

-- MENU
CREATE TABLE IF NOT EXISTS `MENU` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(100) NOT NULL,
  `price` INT NOT NULL,
  -- 카테고리는 조인 테이블 없이 메뉴가 직접 들고 있는다 (메뉴 1 : 카테고리 1)
  `category_id` INT NULL,
  -- 성분은 별도 테이블/조인 없이 상품 안에 리스트로 들고 있는다. 예) ["우유","생크림","설탕"]
  -- NULL 허용 필수: MenuEntity(JPA)와 통합 테스트가 이 컬럼 없이 INSERT 한다.
  -- 그래프 동기화 때 이 배열을 펼쳐 (:Ingredient) 노드로 쪼갠다 (비정규화 -> 정규화).
  `ingredients` JSON NULL,
  `created_by` VARCHAR(50) DEFAULT 'system',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `modified_by` VARCHAR(50) DEFAULT 'system',
  `modified_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `deleted` TINYINT(1) NOT NULL,
  FOREIGN KEY (`category_id`) REFERENCES `MENU_CATEGORY`(`id`)
);


-- ORDER_HISTORY
CREATE TABLE IF NOT EXISTS `ORDER_HISTORY` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `user_id` INT NOT NULL,
  `price` INT NOT NULL DEFAULT 0,
  `created_by` VARCHAR(50) DEFAULT 'system',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `modified_by` VARCHAR(50) DEFAULT 'system',
  `modified_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `deleted` TINYINT(1) NOT NULL,
  FOREIGN KEY (`user_id`) REFERENCES `USER_CREDIT`(`id`)
);

CREATE INDEX idx_order_history_user_id ON ORDER_HISTORY(user_id);

-- ORDER_DETAILS
CREATE TABLE IF NOT EXISTS `ORDER_DETAILS` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `order_id` INT NOT NULL,
  `menu_id` INT NOT NULL,
  `count` INT NOT NULL DEFAULT 0,
  `menu_price` INT NOT NULL DEFAULT 0,
  `created_by` VARCHAR(50) DEFAULT 'system',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `modified_by` VARCHAR(50) DEFAULT 'system',
  `modified_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `deleted` TINYINT(1) NOT NULL,
  FOREIGN KEY (`order_id`) REFERENCES `ORDER_HISTORY`(`id`) ON DELETE CASCADE
);

CREATE INDEX idx_order_details_order_id ON ORDER_DETAILS(order_id);

-- MENU_ORDER_STATISTICS
CREATE TABLE IF NOT EXISTS `MENU_ORDER_STATISTICS` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `date` DATE NOT NULL,
  `menu_id` INT NOT NULL,
  `count` INT NOT NULL,
  `created_by` VARCHAR(50) DEFAULT 'system',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `modified_by` VARCHAR(50) DEFAULT 'system',
  `modified_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `deleted` TINYINT(1) NOT NULL,
  UNIQUE KEY `uniq_date_menu` (`date`, `menu_id`)
);

CREATE EVENT delete_expired_order_statistics
ON SCHEDULE EVERY 1 YEAR
STARTS CURRENT_TIMESTAMP
DO
  DELETE FROM `MENU_ORDER_STATISTICS` WHERE `modified_at` < DATE_SUB(NOW(), INTERVAL 1 YEAR);

CREATE INDEX idx_menu_order_statistics_menu_id ON MENU_ORDER_STATISTICS(menu_id);
CREATE INDEX idx_menu_order_statistics_modified_at ON MENU_ORDER_STATISTICS(modified_at);

-- =====================================================================
-- 그래프 실습용 확장 테이블 (.claude/graph.md §7)
--
-- JPA 엔티티로 매핑하지 않는다. 그래프 동기화(JdbcTemplate)에서만 읽으므로
-- BaseEntity 의 created_by/modified_at/deleted 컬럼은 USER 를 제외하고 생략했다.
-- (`ddl-auto: none` 이라 Hibernate 가 이 테이블들을 검증하지 않는다)
--
-- ★ 표시는 "깊이가 가변이라 SQL 재귀 CTE가 필요한" 구조 = 그래프DB 가 이기는 지점
-- =====================================================================

-- USER : 유저 아이덴티티
--   기존 USER_CREDIT 은 '잔액' 테이블이라 id/credits 밖에 없다.
--   그래프의 (:User) 노드에 붙일 이름이 필요해 아이덴티티를 분리했다. id 를 공유한다.
--
--   ※ USER_CREDIT 에서 USER 로 향하는 FK 를 일부러 걸지 않았다.
--     OrderControllerIntegrationTest 가 USER_CREDIT 에 임의 id(99999)를 단독 INSERT 하는데,
--     FK 를 걸면 그 테스트가 깨진다.
CREATE TABLE IF NOT EXISTS `USER` (
  `id` INT PRIMARY KEY,
  `name` VARCHAR(50) NOT NULL,
  `created_by` VARCHAR(50) DEFAULT 'system',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `modified_by` VARCHAR(50) DEFAULT 'system',
  `modified_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `deleted` TINYINT(1) NOT NULL DEFAULT 0
);

-- INGREDIENT : 성분 분류 트리 ★ (self-reference)
--   알러지 그룹과 구체 성분을 한 트리에 담는다.
--     유제품 <- 우유 <- 마스카포네크림
--     견과류 <- 아몬드 <- 아몬드가루
--   MENU.ingredients 의 이름은 이 트리의 노드 이름이다.
--
--   ※ '알러지' 는 별도 테이블이 아니다. USER_ALLERGY 가 이 트리의 어느 노드를
--     가리키느냐가 곧 그 유저의 알러지다 — 주형은 '우유', 재휘는 '유제품' 처럼
--     사람마다 다른 높이를 가리킬 수 있다.
--
--   ※ 트레이드오프: parent_id 가 한 컬럼이라 다중 부모를 못 쓴다.
--     마스카포네크림은 실제로 우유+생크림이지만 우유 하나만 걸었다.
--     (조상이 {마스카포네크림, 우유, 유제품} 이라 우유·유제품 알러지 모두 걸린다)
--   ※ 실무에선 식약처 알러지 표기 22종은 '법정 목록', 성분은 '레시피 데이터' 라
--     라이프사이클이 달라서 보통 분리한다. 자세한 건 graph.md §7.
CREATE TABLE IF NOT EXISTS `INGREDIENT` (
  `id` INT PRIMARY KEY,
  `name` VARCHAR(50) NOT NULL,
  `parent_id` INT NULL,
  FOREIGN KEY (`parent_id`) REFERENCES `INGREDIENT`(`id`),
  UNIQUE KEY `uniq_ingredient_name` (`name`)
);

-- USER_ALLERGY : 유저 -> INGREDIENT 트리의 노드 (계층의 어느 레벨이든 지정 가능)
--   registered_at 이 핵심이다. 알러지는 "나중에" 등록되므로, 그 이전 주문에
--   알러지 성분이 들어있는 건 데이터 오류가 아니라 정상이다.
--   주문 이력은 과거의 사실이라 소급 수정하지 않는다. (자세한 건 graph.md §7)
CREATE TABLE IF NOT EXISTS `USER_ALLERGY` (
  `user_id` INT NOT NULL,
  `ingredient_id` INT NOT NULL,
  `registered_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`, `ingredient_id`),
  FOREIGN KEY (`user_id`) REFERENCES `USER`(`id`),
  FOREIGN KEY (`ingredient_id`) REFERENCES `INGREDIENT`(`id`)
);

-- USER_RELATION : 팔로우 ★ (방향 있음)
--   follower_id 가 followee_id 를 팔로우한다. 한 방향당 한 행.
--   맞팔이면 (A,B) 와 (B,A) 두 행이 생긴다 — 중복이 아니라 서로 다른 사실이다.
--
--   방향이 핵심이다. Cypher 는 화살표로 그대로 구분한다.
--     (me)-[:FOLLOWS]->(x)   내가 팔로우하는 사람
--     (me)<-[:FOLLOWS]-(x)   나를 팔로우하는 사람
--     (me)-[:FOLLOWS]->(x) AND (x)-[:FOLLOWS]->(me)   맞팔
CREATE TABLE IF NOT EXISTS `USER_RELATION` (
  `follower_id` INT NOT NULL,
  `followee_id` INT NOT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`follower_id`, `followee_id`),
  FOREIGN KEY (`follower_id`) REFERENCES `USER`(`id`),
  FOREIGN KEY (`followee_id`) REFERENCES `USER`(`id`),
  CHECK (`follower_id` <> `followee_id`)
);

-- USER_MENU_LIKE : 명시적 선호 (주문 = 암묵적 선호와 구분)
CREATE TABLE IF NOT EXISTS `USER_MENU_LIKE` (
  `user_id` INT NOT NULL,
  `menu_id` INT NOT NULL,
  `rating` TINYINT NOT NULL DEFAULT 5,
  PRIMARY KEY (`user_id`, `menu_id`),
  FOREIGN KEY (`user_id`) REFERENCES `USER`(`id`),
  FOREIGN KEY (`menu_id`) REFERENCES `MENU`(`id`)
);

-- USER 데이터 (USER_CREDIT 과 id 공유)
INSERT INTO `USER` (`id`, `name`, `created_by`, `created_at`, `modified_by`, `modified_at`, `deleted`)
VALUES (1, '순진', 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (2, '유근', 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (3, '연희', 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (4, '준영', 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (5, '재휘', 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (6, '휘찬', 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (7, '주형', 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (8, '민정', 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0);

-- USER_CREDIT 데이터
INSERT INTO USER_CREDIT (`id`, `credits`, `created_by`, `created_at`, `modified_by`, `modified_at`, `deleted`)
VALUES (1, 10000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (2, 8000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (3, 50000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (4, 500, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (5, 0, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (6, 6000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (7, 20000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (8, 4500, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0);

-- MENU_CATEGORY 데이터 : 3단 트리 (디저트 > 빙과 > 아이스크림)
INSERT INTO MENU_CATEGORY (`id`, `name`, `parent_id`) VALUES
  (1, '디저트',     NULL),
  (2, '빙과',       1),
  (3, '빙수',       1),
  (4, '구움과자',   1),
  (5, '케이크',     1),
  (6, '아이스크림', 2),
  (7, '젤라또',     2),
  (8, '샤베트',     2);

-- MENU 데이터
--   1~4 : 기존 아이스크림 (주문 이력이 달려 있으므로 순서/ID 를 바꾸면 안 된다)
--   5~11: 그래프 실습용 디저트 확장. 주문 이력 없음 -> CO_ORDERED 6쌍은 그대로 유지된다.
INSERT INTO MENU (`name`, `price`, `category_id`, `ingredients`, `created_by`, `created_at`, `modified_by`, `modified_at`, `deleted`)
VALUES ("바닐라", 3000, 6, '["우유","생크림","설탕","바닐라빈"]', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
       ("초코", 4000, 6, '["우유","생크림","설탕","코코아"]', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
       ("딸기", 3500, 6, '["우유","생크림","설탕","딸기과육"]', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
       ("망고", 5000, 6, '["우유","생크림","설탕","망고과육"]', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
       -- 이하 그래프 실습용 디저트
       ("팥빙수", 7000, 3, '["팥","우유","연유","설탕"]', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
       ("땅콩버터 아이스크림", 4500, 6, '["우유","생크림","땅콩버터","설탕"]', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
       ("피스타치오 젤라또", 6000, 7, '["우유","피스타치오","설탕"]', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
       ("마카롱", 2500, 4, '["아몬드가루","계란","설탕"]', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
       ("복숭아 샤베트", 4000, 8, '["복숭아과육","설탕","물"]', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
       ("티라미수", 6500, 5, '["마스카포네크림","계란","밀가루","커피","설탕"]', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
       -- 망고 샤베트: 알러지 유발 성분 없음 (대조군 / 안전 폴백)
       ("망고 샤베트", 4500, 8, '["망고과육","설탕","물"]', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0);

-- ORDER_HISTORY 데이터
INSERT INTO ORDER_HISTORY (`id`, `user_id`, `price`, `created_by`, `created_at`, `modified_by`, `modified_at`, `deleted`)
VALUES (1, 1, 3000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (2, 2, 3500, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (3, 4, 5000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (4, 4, 5000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (5, 1, 50000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (6, 6, 3000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (7, 5, 4500, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),  -- 재휘: 유제품 알러지라 망고 샤베트
       (8, 1, 5000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       -- 이하 디저트 복수 메뉴 주문 (그래프 연관관계 실습용)
       --   각 유저의 알러지를 지켜서 조합했다 (재휘=유제품이라 샤베트만, 주형=우유라 마카롱/샤베트만).
       (9, 1, 13500, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (10, 2, 9000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (11, 4, 8500, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (12, 5, 8500, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (13, 6, 13500, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (14, 7, 7000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (15, 8, 8000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (16, 6, 8500, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       -- 주문 17: 주형이 우유 알러지를 등록(3개월 전)하기 "이전"(6개월 전)에 먹은 티라미수.
       --   알러지 성분이 든 메뉴가 주문 이력에 남아 있는 케이스. 데이터 오류가 아니다.
       (17, 7, 6500, 'system', DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 6 MONTH), 'system', DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 6 MONTH), 0);
       
-- ORDER_DETAILS 데이터
INSERT INTO ORDER_DETAILS (`order_id`, `menu_id`, `count`, `menu_price`, `created_by`, `created_at`, `modified_by`, `modified_at`, `deleted`)
VALUES (1, 1, 1, 3000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (2, 3, 1, 3500, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (3, 4, 1, 5000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (4, 4, 1, 5000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (5, 1, 10, 30000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (5, 2, 5, 20000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (6, 1, 1, 3000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (7, 11, 1, 4500, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (8, 4, 1, 5000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       -- 주문 9: 순진 - 팥빙수 + 티라미수
       (9, 5, 1, 7000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (9, 10, 1, 6500, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       -- 주문 10: 유근 - 티라미수 + 마카롱
       (10, 10, 1, 6500, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (10, 8, 1, 2500, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       -- 주문 11: 준영 - 복숭아 샤베트 + 망고 샤베트
       (11, 9, 1, 4000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (11, 11, 1, 4500, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       -- 주문 12: 재휘 - 복숭아 샤베트 + 망고 샤베트 (유제품 알러지라 샤베트만 먹는다)
       (12, 9, 1, 4000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (12, 11, 1, 4500, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       -- 주문 13: 휘찬 - 팥빙수 + 티라미수
       (13, 5, 1, 7000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (13, 10, 1, 6500, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       -- 주문 14: 주형 - 마카롱 + 망고 샤베트 (우유 알러지라 유제품 디저트를 피한다)
       (14, 8, 1, 2500, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (14, 11, 1, 4500, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       -- 주문 15: 민정 - 딸기 + 망고 샤베트 (아이스크림 x 디저트 교차 조합)
       (15, 3, 1, 3500, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (15, 11, 1, 4500, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       -- 주문 16: 휘찬 - 피스타치오 젤라또 + 마카롱
       (16, 7, 1, 6000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (16, 8, 1, 2500, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       -- 주문 17: 주형 - 티라미수 (우유 알러지 등록 이전. 단일 메뉴라 CO_ORDERED 불변)
       (17, 10, 1, 6500, 'system', DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 6 MONTH), 'system', DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 6 MONTH), 0);

-- MENU_ORDER_STATISTICS 데이터
INSERT INTO MENU_ORDER_STATISTICS (`date`, `menu_id`, `count`, `created_by`, `created_at`, `modified_by`, `modified_at`, `deleted`)
VALUES
  (DATE_ADD(CURRENT_DATE, INTERVAL -2 DAY), 1, 109, 'system', DATE_ADD(CURRENT_TIMESTAMP, INTERVAL -2 DAY), 'system', DATE_ADD(CURRENT_TIMESTAMP, INTERVAL -2 DAY), 0),
  (DATE_ADD(CURRENT_DATE, INTERVAL -2 DAY), 2, 84, 'system', DATE_ADD(CURRENT_TIMESTAMP, INTERVAL -2 DAY), 'system', DATE_ADD(CURRENT_TIMESTAMP, INTERVAL -2 DAY), 0),
  (DATE_ADD(CURRENT_DATE, INTERVAL -2 DAY), 3, 47, 'system', DATE_ADD(CURRENT_TIMESTAMP, INTERVAL -2 DAY), 'system', DATE_ADD(CURRENT_TIMESTAMP, INTERVAL -2 DAY), 0),
  (DATE_ADD(CURRENT_DATE, INTERVAL -2 DAY), 4, 70, 'system', DATE_ADD(CURRENT_TIMESTAMP, INTERVAL -2 DAY), 'system', DATE_ADD(CURRENT_TIMESTAMP, INTERVAL -2 DAY), 0),
  (DATE_ADD(CURRENT_DATE, INTERVAL -1 DAY), 1, 89, 'system', DATE_ADD(CURRENT_TIMESTAMP, INTERVAL -1 DAY), 'system', DATE_ADD(CURRENT_TIMESTAMP, INTERVAL -1 DAY), 0),
  (DATE_ADD(CURRENT_DATE, INTERVAL -1 DAY), 2, 91, 'system', DATE_ADD(CURRENT_TIMESTAMP, INTERVAL -1 DAY), 'system', DATE_ADD(CURRENT_TIMESTAMP, INTERVAL -1 DAY), 0),
  (DATE_ADD(CURRENT_DATE, INTERVAL -1 DAY), 3, 47, 'system', DATE_ADD(CURRENT_TIMESTAMP, INTERVAL -1 DAY), 'system', DATE_ADD(CURRENT_TIMESTAMP, INTERVAL -1 DAY), 0),
  (DATE_ADD(CURRENT_DATE, INTERVAL -1 DAY), 4, 60, 'system', DATE_ADD(CURRENT_TIMESTAMP, INTERVAL -1 DAY), 'system', DATE_ADD(CURRENT_TIMESTAMP, INTERVAL -1 DAY), 0),
  (CURRENT_DATE, 1, 100, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
  (CURRENT_DATE, 2, 72, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
  (CURRENT_DATE, 3, 30, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
  (CURRENT_DATE, 4, 55, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0);

-- INGREDIENT : 성분 분류 트리
--   상위 = 알러지 그룹, 하위 = 구체 성분. 잎 이름이 MENU.ingredients 에 들어간다.
--
--   ※ 땅콩은 분류학적으로 콩과라 트리넛이 아니고, 식약처 표기도 별개 항목이다.
--     하지만 (1) 땅콩·견과류 알러지는 동반되는 경우가 흔하고 (2) 제조 설비를 공유해
--     교차오염 위험이 있어, 여기서는 '견과류·땅콩' 이라는 회피 그룹으로 묶었다.
--     그룹 이름을 '견과류' 가 아니라 '견과류·땅콩' 으로 둔 이유 —
--     분류(taxonomy)가 아니라 회피 정책(policy)임을 이름에서 드러내기 위함이다.
INSERT INTO INGREDIENT (`id`, `name`, `parent_id`) VALUES
  -- 알러지 그룹 (상위 개념)
  (1,  '유제품',         NULL),
  (2,  '견과류·땅콩',    NULL),
  -- 표기 수준
  (3,  '우유',           1),
  (4,  '생크림',         1),
  (5,  '연유',           1),
  (6,  '버터',           1),
  (7,  '아몬드',         2),
  (8,  '피스타치오',     2),
  (9,  '호두',           2),
  (10, '캐슈',           2),
  (11, '땅콩',           2),    -- 견과류·땅콩 밑으로 (분류상 콩과지만 알러지 회피는 같이 본다)
  (12, '난류',           NULL),
  (13, '밀',             NULL),
  (14, '복숭아',         NULL),
  (15, '대두',           NULL),
  -- 구체 성분 (잎). 이름만 봐서는 알러지가 안 보이는 것들이라 트리를 타야 한다 ★
  (16, '마스카포네크림', 3),    -- -> 우유 -> 유제품
  (17, '아몬드가루',     7),    -- -> 아몬드 -> 견과류
  (18, '땅콩버터',       11),   -- -> 땅콩
  (19, '계란',           12),   -- -> 난류
  (20, '밀가루',         13),   -- -> 밀
  (21, '복숭아과육',     14),   -- -> 복숭아
  -- 알러지와 무관한 성분 (조상이 없다)
  (22, '설탕',           NULL),
  (23, '물',             NULL),
  (24, '팥',             NULL),
  (25, '코코아',         NULL),
  (26, '바닐라빈',       NULL),
  (27, '커피',           NULL),
  (28, '딸기과육',       NULL),
  (29, '망고과육',       NULL);

-- USER_ALLERGY
--   user1 = 견과류·땅콩(상위 그룹) -> 하위 아몬드/피스타치오/땅콩까지 걸려야 한다 ★
--   user2 = 땅콩(하위 노드)       -> 아몬드·피스타치오는 안 걸린다 (위로만 탐색하므로)
--   user5 = 유제품(상위 그룹)
--   user7 = 우유(하위 노드 직접 지정) -> SUBTYPE_OF*0.. 의 0 케이스
--   user3 = 없음 (콜드스타트 유지), user4 = 없음
--   ※ 각 알러지가 실제로 후보를 걸러내도록 배치했다. 본인이 LIKES 한 메뉴와
--     모순되지 않는지도 확인 (예: user4 는 복숭아 샤베트을 좋아하므로 복숭아 알러지가 아니다)
--   주형(7)만 최근(3개월 전) 등록이다. 그 이전(6개월 전)에 티라미수를 주문한 이력이 있어
--   "알러지 성분이 든 메뉴를 먹은 이력" 이 존재한다 — 실무에서 흔한 정상 케이스다.
INSERT INTO USER_ALLERGY (`user_id`, `ingredient_id`, `registered_at`) VALUES
  (1, 2,  DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 YEAR)),   -- 견과류·땅콩 (상위 그룹) ★
  (2, 11, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 YEAR)),   -- 땅콩 (하위 노드만 지정)
  (5, 1,  DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 YEAR)),   -- 유제품 (상위 그룹) ★
  (6, 14, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 YEAR)),   -- 복숭아
  (7, 3,  DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 3 MONTH)),  -- 우유 (하위 노드) ← 최근 등록 ★
  (8, 12, DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 YEAR));   -- 난류

-- USER_RELATION : 팔로우 (방향 있음)
--   follower_id 가 followee_id 를 팔로우한다. 맞팔이면 두 행이 들어간다.
--   맞팔 3쌍(순진↔유근, 연희↔재휘, 휘찬↔주형)은 읽기 쉽게 붙여 뒀다.
--
--   팔로잉/팔로워가 비대칭인 게 포인트다. 예) 민정은 유근을 팔로우하지만 유근은 아니다
--   -> 추천에서 "내가 팔로우하는 쪽"만 따라가야 하므로 화살표 방향을 반드시 지켜야 한다.
--   민정은 나가는 팔로우가 (8,2) 하나뿐이라 2홉 도달 범위가 좁다 — 크레딧 필터 데모용.
INSERT INTO USER_RELATION (`follower_id`, `followee_id`) VALUES
  (1, 2),  -- 순진 -> 유근   (맞팔)
  (2, 1),  -- 유근 -> 순진   (맞팔)
  (3, 5),  -- 연희 -> 재휘   (맞팔)
  (5, 3),  -- 재휘 -> 연희   (맞팔)
  (6, 7),  -- 휘찬 -> 주형   (맞팔)
  (7, 6),  -- 주형 -> 휘찬   (맞팔)
  (1, 6),  -- 순진 -> 휘찬
  (2, 5),  -- 유근 -> 재휘
  (4, 7),  -- 준영 -> 주형
  (6, 4),  -- 휘찬 -> 준영
  (8, 2);  -- 민정 -> 유근

-- USER_MENU_LIKE : 명시적 선호 (주문 = 암묵적 선호와 별개)
INSERT INTO USER_MENU_LIKE (`user_id`, `menu_id`, `rating`) VALUES
  (1, 1,  5),  -- 바닐라
  (1, 5,  4),  -- 팥빙수
  (2, 5,  5),  -- 팥빙수
  (2, 7,  4),  -- 피스타치오 젤라또
  (4, 9,  5),  -- 복숭아 샤베트
  (4, 6,  3),  -- 땅콩버터 아이스크림
  (5, 8,  4),  -- 마카롱
  (5, 9,  5),  -- 복숭아 샤베트
  (6, 7,  5),  -- 피스타치오 젤라또
  (6, 10, 4),  -- 티라미수
  (6, 6,  3),  -- 땅콩버터 아이스크림 (user2 의 땅콩 필터를 발동시키는 경로)
  (7, 8,  4),  -- 마카롱 (주형은 우유 알러지 -> 유제품 디저트는 좋아할 수 없다)
  (7, 11, 5),  -- 망고 샤베트
  (8, 5,  5),  -- 팥빙수 (민정은 난류 알러지 -> 티라미수 불가. 좋아하지만 크레딧 4500 으론 못 산다)
  (8, 11, 4),  -- 망고 샤베트
  -- 연희: 재휘가 유일하게 팔로우하는 상대라, 연희의 취향이 곧 재휘의 후보가 된다.
  --       유제품(팥빙수·피스타치오)을 넣어 재휘의 유제품 계층 필터가 실제로 발동하게 한다.
  (3, 5,  5),  -- 팥빙수
  (3, 7,  4),  -- 피스타치오 젤라또
  (3, 11, 5);  -- 망고 샤베트
