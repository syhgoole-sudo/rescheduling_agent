SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

SET @semi_product_code := 'SEMI-PHOTO-01';

SET @lot_1 := 'LOT-SR-001';
SET @lot_2 := 'LOT-SR-002';
SET @lot_3 := 'LOT-SR-003';
SET @lot_4 := 'LOT-SR-004';
SET @lot_5 := 'LOT-SR-005';
SET @lot_6 := 'LOT-SR-006';
SET @hot_lot := 'LOT-HOT-REENTRANT-001';

DELETE FROM aps_insert_event
WHERE insert_order_id IN (
    SELECT order_id FROM aps_order
    WHERE order_code IN (@lot_1, @lot_2, @lot_3, @lot_4, @lot_5, @lot_6, @hot_lot)
);

DELETE FROM aps_schedule_task
WHERE order_id IN (
    SELECT order_id FROM aps_order
    WHERE order_code IN (@lot_1, @lot_2, @lot_3, @lot_4, @lot_5, @lot_6, @hot_lot)
);

DELETE FROM aps_order
WHERE order_code IN (@lot_1, @lot_2, @lot_3, @lot_4, @lot_5, @lot_6, @hot_lot);

DELETE FROM aps_route_operation
WHERE product_id IN (
    SELECT product_id FROM aps_product WHERE product_code = @semi_product_code
);

DELETE FROM aps_product
WHERE product_code = @semi_product_code;

SET @cut_group_id := (
    SELECT equipment_group_id FROM aps_equipment_group
    WHERE equipment_group_code = 'CUT'
    LIMIT 1
);
SET @cnc_group_id := (
    SELECT equipment_group_id FROM aps_equipment_group
    WHERE equipment_group_code = 'CNC'
    LIMIT 1
);
SET @heat_group_id := (
    SELECT equipment_group_id FROM aps_equipment_group
    WHERE equipment_group_code = 'HEAT'
    LIMIT 1
);
SET @inspect_group_id := (
    SELECT equipment_group_id FROM aps_equipment_group
    WHERE equipment_group_code = 'INSPECT'
    LIMIT 1
);

INSERT INTO aps_product
(product_code, product_name, status, del_flag, create_by, create_time, update_by, update_time, remark)
VALUES
(@semi_product_code, '半导体可重入光刻产品', '0', '0', 'admin', NOW(), '', NULL, 'semiconductor reentrant GA demo product');

SET @semi_product_id := LAST_INSERT_ID();

INSERT INTO aps_route_operation
(product_id, product_code, process_seq, process_code, process_name, equipment_group_id, standard_duration, status, del_flag, create_by, create_time, update_by, update_time, remark)
VALUES
(@semi_product_id, @semi_product_code, 1, 'CLEAN-L0', 'CLEAN / WET clean before lithography', @cut_group_id, 30, '0', '0', 'admin', NOW(), '', NULL, 'semiconductor reentrant GA demo route'),
(@semi_product_id, @semi_product_code, 2, 'PHOTO-L1', 'PHOTO layer 1 exposure', @cnc_group_id, 60, '0', '0', 'admin', NOW(), '', NULL, 'semiconductor reentrant GA demo route'),
(@semi_product_id, @semi_product_code, 3, 'ETCH-L1', 'ETCH layer 1', @heat_group_id, 80, '0', '0', 'admin', NOW(), '', NULL, 'semiconductor reentrant GA demo route'),
(@semi_product_id, @semi_product_code, 4, 'PHOTO-L2', 'PHOTO layer 2 exposure', @cnc_group_id, 60, '0', '0', 'admin', NOW(), '', NULL, 'semiconductor reentrant GA demo route'),
(@semi_product_id, @semi_product_code, 5, 'METROLOGY-L2', 'METROLOGY layer 2 inspection', @inspect_group_id, 30, '0', '0', 'admin', NOW(), '', NULL, 'semiconductor reentrant GA demo route'),
(@semi_product_id, @semi_product_code, 6, 'DIFF-L3', 'DIFF / ANNEAL layer 3', @heat_group_id, 90, '0', '0', 'admin', NOW(), '', NULL, 'semiconductor reentrant GA demo route'),
(@semi_product_id, @semi_product_code, 7, 'PHOTO-L3', 'PHOTO layer 3 exposure', @cnc_group_id, 60, '0', '0', 'admin', NOW(), '', NULL, 'semiconductor reentrant GA demo route'),
(@semi_product_id, @semi_product_code, 8, 'FINAL-INSPECT', 'FINAL INSPECTION', @inspect_group_id, 30, '0', '0', 'admin', NOW(), '', NULL, 'semiconductor reentrant GA demo route');

INSERT INTO aps_order
(order_code, order_type, product_id, product_code, quantity, priority_level, release_time, due_time, order_status, del_flag, create_by, create_time, update_by, update_time, remark)
VALUES
(@lot_1, 'NORMAL', @semi_product_id, @semi_product_code, 25.0000, 2, '2026-06-18 08:00:00', '2026-06-18 20:00:00', 'NEW', '0', 'admin', NOW(), '', NULL, 'semiconductor reentrant normal lot for GA test'),
(@lot_2, 'NORMAL', @semi_product_id, @semi_product_code, 25.0000, 3, '2026-06-18 08:20:00', '2026-06-18 21:00:00', 'NEW', '0', 'admin', NOW(), '', NULL, 'semiconductor reentrant normal lot for GA test'),
(@lot_3, 'NORMAL', @semi_product_id, @semi_product_code, 25.0000, 4, '2026-06-18 08:40:00', '2026-06-18 22:00:00', 'NEW', '0', 'admin', NOW(), '', NULL, 'semiconductor reentrant normal lot for GA test'),
(@lot_4, 'NORMAL', @semi_product_id, @semi_product_code, 25.0000, 2, '2026-06-18 09:00:00', '2026-06-18 23:00:00', 'NEW', '0', 'admin', NOW(), '', NULL, 'semiconductor reentrant normal lot for GA test'),
(@lot_5, 'NORMAL', @semi_product_id, @semi_product_code, 25.0000, 3, '2026-06-18 09:30:00', '2026-06-19 00:00:00', 'NEW', '0', 'admin', NOW(), '', NULL, 'semiconductor reentrant normal lot for GA test'),
(@lot_6, 'NORMAL', @semi_product_id, @semi_product_code, 25.0000, 4, '2026-06-18 10:00:00', '2026-06-19 01:00:00', 'NEW', '0', 'admin', NOW(), '', NULL, 'semiconductor reentrant normal lot for GA test'),
(@hot_lot, 'INSERT', @semi_product_id, @semi_product_code, 25.0000, 1, '2026-06-18 11:00:00', '2026-06-18 19:00:00', 'NEW', '0', 'admin', NOW(), '', NULL, 'semiconductor hot lot for GA local reschedule test');

SET FOREIGN_KEY_CHECKS = 1;

SELECT product_id, product_code, product_name
FROM aps_product
WHERE product_code = 'SEMI-PHOTO-01';

SELECT process_seq, process_code, process_name, equipment_group_id, standard_duration
FROM aps_route_operation
WHERE product_id = (SELECT product_id FROM aps_product WHERE product_code = 'SEMI-PHOTO-01')
ORDER BY process_seq;

SELECT order_id, order_code, order_type, order_status, priority_level, release_time, due_time
FROM aps_order
WHERE order_code LIKE 'LOT-SR-%'
OR order_code = 'LOT-HOT-REENTRANT-001'
ORDER BY release_time;
