-- Test data for SoftDeleteFilterAspectTest
INSERT INTO MENU (name, price, created_by, created_at, modified_by, modified_at, deleted)
VALUES ('Test Active Menu 1', 1000, 'test', CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 0);

INSERT INTO MENU (name, price, created_by, created_at, modified_by, modified_at, deleted)
VALUES ('Test Active Menu 2', 2000, 'test', CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 0);

INSERT INTO MENU (name, price, created_by, created_at, modified_by, modified_at, deleted)
VALUES ('Test Deleted Menu', 3000, 'test', CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 1);
