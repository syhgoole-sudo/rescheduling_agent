SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM aps_order WHERE order_id BETWEEN 1001 AND 1009;
DELETE FROM aps_route_operation WHERE route_operation_id BETWEEN 1001 AND 1014;
DELETE FROM aps_equipment WHERE equipment_id BETWEEN 1001 AND 1008;
DELETE FROM aps_equipment_group WHERE equipment_group_id BETWEEN 1001 AND 1004;
DELETE FROM aps_product WHERE product_id BETWEEN 1001 AND 1003;

INSERT INTO aps_product
(product_id, product_code, product_name, status, del_flag, create_by, create_time, update_by, update_time, remark)
VALUES
(1001, 'P-A', '产品A', '0', '0', 'admin', NOW(), '', NULL, 'APS MVP demo product'),
(1002, 'P-B', '产品B', '0', '0', 'admin', NOW(), '', NULL, 'APS MVP demo product'),
(1003, 'P-C', '产品C', '0', '0', 'admin', NOW(), '', NULL, 'APS MVP demo product');

INSERT INTO aps_equipment_group
(equipment_group_id, equipment_group_code, equipment_group_name, status, del_flag, create_by, create_time, update_by, update_time, remark)
VALUES
(1001, 'CUT', '切割设备组', '0', '0', 'admin', NOW(), '', NULL, 'APS MVP demo equipment group'),
(1002, 'CNC', '加工设备组', '0', '0', 'admin', NOW(), '', NULL, 'APS MVP demo equipment group'),
(1003, 'HEAT', '热处理设备组', '0', '0', 'admin', NOW(), '', NULL, 'APS MVP demo equipment group'),
(1004, 'INSPECT', '检测设备组', '0', '0', 'admin', NOW(), '', NULL, 'APS MVP demo equipment group');

INSERT INTO aps_equipment
(equipment_id, equipment_code, equipment_name, equipment_group_id, equipment_group_code, status, del_flag, create_by, create_time, update_by, update_time, remark)
VALUES
(1001, 'CUT-01', '切割设备01', 1001, 'CUT', '0', '0', 'admin', NOW(), '', NULL, 'APS MVP demo equipment'),
(1002, 'CUT-02', '切割设备02', 1001, 'CUT', '0', '0', 'admin', NOW(), '', NULL, 'APS MVP demo equipment'),
(1003, 'CNC-01', '加工设备01', 1002, 'CNC', '0', '0', 'admin', NOW(), '', NULL, 'APS MVP demo equipment'),
(1004, 'CNC-02', '加工设备02', 1002, 'CNC', '0', '0', 'admin', NOW(), '', NULL, 'APS MVP demo equipment'),
(1005, 'HEAT-01', '热处理设备01', 1003, 'HEAT', '0', '0', 'admin', NOW(), '', NULL, 'APS MVP demo equipment'),
(1006, 'HEAT-02', '热处理设备02', 1003, 'HEAT', '0', '0', 'admin', NOW(), '', NULL, 'APS MVP demo equipment'),
(1007, 'INSPECT-01', '检测设备01', 1004, 'INSPECT', '0', '0', 'admin', NOW(), '', NULL, 'APS MVP demo equipment'),
(1008, 'INSPECT-02', '检测设备02', 1004, 'INSPECT', '0', '0', 'admin', NOW(), '', NULL, 'APS MVP demo equipment');

INSERT INTO aps_route_operation
(route_operation_id, product_id, product_code, process_seq, process_code, process_name, equipment_group_id, standard_duration, status, del_flag, create_by, create_time, update_by, update_time, remark)
VALUES
(1001, 1001, 'P-A', 1, 'A-CUT', '产品A切割', 1001, 30, '0', '0', 'admin', NOW(), '', NULL, 'APS MVP demo route'),
(1002, 1001, 'P-A', 2, 'A-CNC-1', '产品A粗加工', 1002, 60, '0', '0', 'admin', NOW(), '', NULL, 'APS MVP demo route'),
(1003, 1001, 'P-A', 3, 'A-HEAT', '产品A热处理', 1003, 90, '0', '0', 'admin', NOW(), '', NULL, 'APS MVP demo route'),
(1004, 1001, 'P-A', 4, 'A-INSPECT', '产品A检测', 1004, 30, '0', '0', 'admin', NOW(), '', NULL, 'APS MVP demo route'),

(1005, 1002, 'P-B', 1, 'B-CUT', '产品B切割', 1001, 45, '0', '0', 'admin', NOW(), '', NULL, 'APS MVP demo route'),
(1006, 1002, 'P-B', 2, 'B-CNC-1', '产品B粗加工', 1002, 60, '0', '0', 'admin', NOW(), '', NULL, 'APS MVP demo route'),
(1007, 1002, 'P-B', 3, 'B-HEAT', '产品B热处理', 1003, 90, '0', '0', 'admin', NOW(), '', NULL, 'APS MVP demo route'),
(1008, 1002, 'P-B', 4, 'B-CNC-2', '产品B精加工', 1002, 60, '0', '0', 'admin', NOW(), '', NULL, 'APS MVP demo reentrant route'),
(1009, 1002, 'P-B', 5, 'B-INSPECT', '产品B检测', 1004, 30, '0', '0', 'admin', NOW(), '', NULL, 'APS MVP demo route'),

(1010, 1003, 'P-C', 1, 'C-CUT', '产品C切割', 1001, 30, '0', '0', 'admin', NOW(), '', NULL, 'APS MVP demo route'),
(1011, 1003, 'P-C', 2, 'C-CNC', '产品C加工', 1002, 45, '0', '0', 'admin', NOW(), '', NULL, 'APS MVP demo route'),
(1012, 1003, 'P-C', 3, 'C-HEAT', '产品C热处理', 1003, 60, '0', '0', 'admin', NOW(), '', NULL, 'APS MVP demo route'),
(1013, 1003, 'P-C', 4, 'C-INSPECT-1', '产品C初检', 1004, 30, '0', '0', 'admin', NOW(), '', NULL, 'APS MVP demo route'),
(1014, 1003, 'P-C', 5, 'C-INSPECT-2', '产品C终检', 1004, 30, '0', '0', 'admin', NOW(), '', NULL, 'APS MVP demo route');

INSERT INTO aps_order
(order_id, order_code, order_type, product_id, product_code, quantity, priority_level, release_time, due_time, order_status, del_flag, create_by, create_time, update_by, update_time, remark)
VALUES
(1001, 'O-NORMAL-001', 'NORMAL', 1001, 'P-A', 20.0000, 3, '2026-06-18 08:00:00', '2026-06-18 18:00:00', 'NEW', '0', 'admin', NOW(), '', NULL, 'APS MVP demo normal order'),
(1002, 'O-NORMAL-002', 'NORMAL', 1002, 'P-B', 15.0000, 4, '2026-06-18 08:30:00', '2026-06-18 20:00:00', 'NEW', '0', 'admin', NOW(), '', NULL, 'APS MVP demo normal order'),
(1003, 'O-NORMAL-003', 'NORMAL', 1003, 'P-C', 30.0000, 5, '2026-06-18 09:00:00', '2026-06-18 22:00:00', 'NEW', '0', 'admin', NOW(), '', NULL, 'APS MVP demo normal order'),
(1004, 'O-NORMAL-004', 'NORMAL', 1001, 'P-A', 25.0000, 2, '2026-06-18 09:30:00', '2026-06-19 00:00:00', 'NEW', '0', 'admin', NOW(), '', NULL, 'APS MVP demo normal order'),
(1005, 'O-NORMAL-005', 'NORMAL', 1002, 'P-B', 18.0000, 3, '2026-06-18 10:00:00', '2026-06-19 04:00:00', 'NEW', '0', 'admin', NOW(), '', NULL, 'APS MVP demo normal order'),
(1006, 'O-NORMAL-006', 'NORMAL', 1003, 'P-C', 22.0000, 4, '2026-06-18 10:30:00', '2026-06-19 08:00:00', 'NEW', '0', 'admin', NOW(), '', NULL, 'APS MVP demo normal order'),
(1007, 'O-NORMAL-007', 'NORMAL', 1001, 'P-A', 16.0000, 5, '2026-06-18 11:00:00', '2026-06-19 10:00:00', 'NEW', '0', 'admin', NOW(), '', NULL, 'APS MVP demo normal order'),
(1008, 'O-NORMAL-008', 'NORMAL', 1002, 'P-B', 12.0000, 2, '2026-06-18 11:00:00', '2026-06-19 12:00:00', 'NEW', '0', 'admin', NOW(), '', NULL, 'APS MVP demo normal order'),
(1009, 'O-INSERT-001', 'INSERT', 1002, 'P-B', 10.0000, 1, '2026-06-18 10:00:00', '2026-06-18 17:00:00', 'NEW', '0', 'admin', NOW(), '', NULL, 'APS MVP demo insert order');

SET FOREIGN_KEY_CHECKS = 1;
