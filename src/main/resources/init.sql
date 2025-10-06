-- 인코딩 및 DB, 타임존 설정
SET NAMES utf8;
SET GLOBAL time_zone = '+09:00';
SET GLOBAL event_scheduler = ON;
USE `order_system`;

-- 테이블 삭제 (존재할 경우)
DROP TABLE IF EXISTS `MENU_ORDER_STATISTICS`;
DROP TABLE IF EXISTS `ORDER_HISTORY`;
DROP TABLE IF EXISTS `ORDER_DETAILS`;
DROP TABLE IF EXISTS `MENU`;
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

-- MENU
CREATE TABLE IF NOT EXISTS `MENU` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(100) NOT NULL,
  `price` INT NOT NULL,
  `created_by` VARCHAR(50) DEFAULT 'system',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `modified_by` VARCHAR(50) DEFAULT 'system',
  `modified_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `deleted` TINYINT(1) NOT NULL
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

-- USER_CREDIT 데이터
INSERT INTO USER_CREDIT (`id`, `credits`, `created_by`, `created_at`, `modified_by`, `modified_at`, `deleted`)
VALUES (1, 10000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (2, 8000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (3, 50000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (4, 500, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (5, 0, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (6, 6000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0);

-- MENU 데이터
INSERT INTO MENU (`name`, `price`, `created_by`, `created_at`, `modified_by`, `modified_at`, `deleted`)
VALUES ("바닐라", 3000, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
       ("초코", 4000, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
       ("딸기", 3500, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
       ("망고", 5000, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0);

-- ORDER_HISTORY 데이터
INSERT INTO ORDER_HISTORY (`id`, `user_id`, `price`, `created_by`, `created_at`, `modified_by`, `modified_at`, `deleted`)
VALUES (1, 1, 3000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (2, 2, 3500, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (3, 4, 5000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (4, 4, 5000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (5, 1, 50000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (6, 6, 3000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (7, 5, 3000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (8, 1, 5000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0);
       
-- ORDER_DETAILS 데이터
INSERT INTO ORDER_DETAILS (`order_id`, `menu_id`, `count`, `menu_price`, `created_by`, `created_at`, `modified_by`, `modified_at`, `deleted`)
VALUES (1, 1, 1, 3000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (2, 3, 1, 3500, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (3, 4, 1, 5000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (4, 4, 1, 5000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (5, 1, 10, 30000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (5, 2, 5, 20000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (6, 1, 1, 3000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (7, 1, 1, 3000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0),
       (8, 4, 1, 5000, 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 0);

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