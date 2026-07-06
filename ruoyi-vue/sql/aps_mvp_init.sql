SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `aps_insert_event`;
DROP TABLE IF EXISTS `aps_schedule_task`;
DROP TABLE IF EXISTS `aps_schedule_plan`;
DROP TABLE IF EXISTS `aps_order`;
DROP TABLE IF EXISTS `aps_equipment`;
DROP TABLE IF EXISTS `aps_equipment_group`;
DROP TABLE IF EXISTS `aps_route_operation`;
DROP TABLE IF EXISTS `aps_product`;

CREATE TABLE `aps_product` (
  `product_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '产品ID',
  `product_code` VARCHAR(64) NOT NULL COMMENT '产品编码',
  `product_name` VARCHAR(128) NOT NULL COMMENT '产品名称',
  `status` CHAR(1) NOT NULL DEFAULT '0' COMMENT '状态：0正常，1停用',
  `del_flag` CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志：0存在，2删除',
  `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`product_id`),
  UNIQUE KEY `uk_aps_product_code` (`product_code`),
  KEY `idx_aps_product_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='APS产品表';

CREATE TABLE `aps_route_operation` (
  `route_operation_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '产品工艺路线工序ID',
  `product_id` BIGINT NOT NULL COMMENT '产品ID',
  `product_code` VARCHAR(64) NOT NULL COMMENT '产品编码',
  `process_seq` INT NOT NULL COMMENT '工序顺序号',
  `process_code` VARCHAR(64) NOT NULL COMMENT '工序编码',
  `process_name` VARCHAR(128) NOT NULL COMMENT '工序名称',
  `equipment_group_id` BIGINT NOT NULL COMMENT '设备组ID',
  `standard_duration` INT NOT NULL COMMENT '标准加工时长，单位分钟',
  `status` CHAR(1) NOT NULL DEFAULT '0' COMMENT '状态：0正常，1停用',
  `del_flag` CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志：0存在，2删除',
  `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`route_operation_id`),
  UNIQUE KEY `uk_aps_route_operation_seq` (`product_id`, `process_seq`),
  KEY `idx_aps_route_operation_product` (`product_id`),
  KEY `idx_aps_route_operation_group` (`equipment_group_id`),
  KEY `idx_aps_route_operation_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='APS产品工艺路线工序表';

CREATE TABLE `aps_equipment_group` (
  `equipment_group_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '设备组ID',
  `equipment_group_code` VARCHAR(64) NOT NULL COMMENT '设备组编码',
  `equipment_group_name` VARCHAR(128) NOT NULL COMMENT '设备组名称',
  `status` CHAR(1) NOT NULL DEFAULT '0' COMMENT '状态：0正常，1停用',
  `del_flag` CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志：0存在，2删除',
  `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`equipment_group_id`),
  UNIQUE KEY `uk_aps_equipment_group_code` (`equipment_group_code`),
  KEY `idx_aps_equipment_group_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='APS设备组表';

CREATE TABLE `aps_equipment` (
  `equipment_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '设备ID',
  `equipment_code` VARCHAR(64) NOT NULL COMMENT '设备编码',
  `equipment_name` VARCHAR(128) NOT NULL COMMENT '设备名称',
  `equipment_group_id` BIGINT NOT NULL COMMENT '设备组ID',
  `equipment_group_code` VARCHAR(64) NOT NULL COMMENT '设备组编码',
  `status` CHAR(1) NOT NULL DEFAULT '0' COMMENT '状态：0正常，1停用',
  `del_flag` CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志：0存在，2删除',
  `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`equipment_id`),
  UNIQUE KEY `uk_aps_equipment_code` (`equipment_code`),
  KEY `idx_aps_equipment_group` (`equipment_group_id`),
  KEY `idx_aps_equipment_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='APS设备表';

CREATE TABLE `aps_order` (
  `order_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_code` VARCHAR(64) NOT NULL COMMENT '订单编码',
  `order_type` VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT '订单类型：NORMAL普通订单，INSERT插单订单',
  `product_id` BIGINT NOT NULL COMMENT '产品ID',
  `product_code` VARCHAR(64) NOT NULL COMMENT '产品编码',
  `quantity` DECIMAL(18,4) NOT NULL COMMENT '订单数量',
  `priority_level` INT NOT NULL DEFAULT 5 COMMENT '优先级，数值越小优先级越高',
  `release_time` DATETIME DEFAULT NULL COMMENT '可开工时间',
  `due_time` DATETIME NOT NULL COMMENT '交期',
  `order_status` VARCHAR(32) NOT NULL DEFAULT 'NEW' COMMENT '订单状态：NEW新建，SCHEDULED已排产，CLOSED关闭',
  `del_flag` CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志：0存在，2删除',
  `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`order_id`),
  UNIQUE KEY `uk_aps_order_code` (`order_code`),
  KEY `idx_aps_order_product` (`product_id`),
  KEY `idx_aps_order_type_status` (`order_type`, `order_status`),
  KEY `idx_aps_order_due_priority` (`due_time`, `priority_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='APS订单表';

CREATE TABLE `aps_schedule_plan` (
  `plan_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '调度方案ID',
  `plan_code` VARCHAR(64) NOT NULL COMMENT '调度方案编码',
  `plan_name` VARCHAR(128) NOT NULL COMMENT '调度方案名称',
  `plan_type` VARCHAR(32) NOT NULL COMMENT '方案类型：INITIAL初始方案，RESCHEDULE重调度方案',
  `plan_status` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '方案状态：PENDING待确认，ACTIVE当前有效，HISTORY历史，REJECTED拒绝，FAILED失败',
  `source_plan_id` BIGINT DEFAULT NULL COMMENT '来源方案ID，重调度方案对应的原方案',
  `event_id` BIGINT DEFAULT NULL COMMENT '插单事件ID',
  `algorithm_name` VARCHAR(128) DEFAULT NULL COMMENT '算法名称',
  `strategy_type` VARCHAR(64) DEFAULT NULL COMMENT '策略类型',
  `schedule_start_time` DATETIME DEFAULT NULL COMMENT '调度开始时间',
  `schedule_end_time` DATETIME DEFAULT NULL COMMENT '调度结束时间',
  `kpi_json` LONGTEXT COMMENT 'KPI结果JSON',
  `ai_explanation` LONGTEXT COMMENT 'AI解释文本',
  `active_flag` CHAR(1) NOT NULL DEFAULT 'N' COMMENT '是否当前有效：Y是，N否',
  `del_flag` CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志：0存在，2删除',
  `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`plan_id`),
  UNIQUE KEY `uk_aps_schedule_plan_code` (`plan_code`),
  KEY `idx_aps_schedule_plan_status` (`plan_status`, `active_flag`),
  KEY `idx_aps_schedule_plan_source` (`source_plan_id`),
  KEY `idx_aps_schedule_plan_event` (`event_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='APS调度方案主表';

CREATE TABLE `aps_schedule_task` (
  `task_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '调度任务ID',
  `plan_id` BIGINT NOT NULL COMMENT '调度方案ID',
  `order_id` BIGINT NOT NULL COMMENT '订单ID',
  `order_code` VARCHAR(64) NOT NULL COMMENT '订单编码',
  `product_id` BIGINT NOT NULL COMMENT '产品ID',
  `product_code` VARCHAR(64) NOT NULL COMMENT '产品编码',
  `process_seq` INT NOT NULL COMMENT '工序顺序号',
  `process_code` VARCHAR(64) NOT NULL COMMENT '工序编码',
  `process_name` VARCHAR(128) NOT NULL COMMENT '工序名称',
  `equipment_group_id` BIGINT NOT NULL COMMENT '设备组ID',
  `equipment_id` BIGINT NOT NULL COMMENT '设备ID',
  `equipment_code` VARCHAR(64) NOT NULL COMMENT '设备编码',
  `planned_start_time` DATETIME NOT NULL COMMENT '计划开始时间',
  `planned_end_time` DATETIME NOT NULL COMMENT '计划结束时间',
  `duration` INT NOT NULL COMMENT '加工时长，单位分钟',
  `task_status` VARCHAR(32) NOT NULL DEFAULT 'PLANNED' COMMENT '任务状态：PLANNED已计划，FROZEN冻结',
  `is_frozen` CHAR(1) NOT NULL DEFAULT 'N' COMMENT '是否冻结：Y是，N否',
  `is_inserted` CHAR(1) NOT NULL DEFAULT 'N' COMMENT '是否插单任务：Y是，N否',
  `is_changed` CHAR(1) NOT NULL DEFAULT 'N' COMMENT '相对来源方案是否变更：Y是，N否',
  `source_task_id` BIGINT DEFAULT NULL COMMENT '来源任务ID，用于重调度追溯',
  `original_start_time` DATETIME DEFAULT NULL COMMENT '原计划开始时间',
  `original_end_time` DATETIME DEFAULT NULL COMMENT '原计划结束时间',
  `original_equipment_id` BIGINT DEFAULT NULL COMMENT '原设备ID',
  `del_flag` CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志：0存在，2删除',
  `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`task_id`),
  KEY `idx_aps_schedule_task_plan` (`plan_id`),
  KEY `idx_aps_schedule_task_order` (`order_id`),
  KEY `idx_aps_schedule_task_product` (`product_id`),
  KEY `idx_aps_schedule_task_equipment_time` (`equipment_id`, `planned_start_time`, `planned_end_time`),
  KEY `idx_aps_schedule_task_flags` (`is_frozen`, `is_inserted`, `is_changed`),
  KEY `idx_aps_schedule_task_source` (`source_task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='APS调度任务明细表';

CREATE TABLE `aps_insert_event` (
  `event_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '插单事件ID',
  `event_code` VARCHAR(64) NOT NULL COMMENT '插单事件编码',
  `insert_order_id` BIGINT NOT NULL COMMENT '插单订单ID',
  `source_plan_id` BIGINT NOT NULL COMMENT '插单发生时的原有效方案ID',
  `new_plan_id` BIGINT DEFAULT NULL COMMENT '重调度生成的新方案ID',
  `event_time` DATETIME NOT NULL COMMENT '事件发生时间',
  `event_status` VARCHAR(32) NOT NULL DEFAULT 'NEW' COMMENT '事件状态：NEW新建，ANALYZED已分析，RESCHEDULED已重调度，CONFIRMED已采用，REJECTED已拒绝',
  `impact_json` LONGTEXT COMMENT '影响范围分析结果JSON',
  `strategy_type` VARCHAR(64) DEFAULT NULL COMMENT '重调度策略类型',
  `event_reason` VARCHAR(500) DEFAULT NULL COMMENT '插单原因',
  `del_flag` CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志：0存在，2删除',
  `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`event_id`),
  UNIQUE KEY `uk_aps_insert_event_code` (`event_code`),
  KEY `idx_aps_insert_event_order` (`insert_order_id`),
  KEY `idx_aps_insert_event_source_plan` (`source_plan_id`),
  KEY `idx_aps_insert_event_new_plan` (`new_plan_id`),
  KEY `idx_aps_insert_event_status` (`event_status`),
  KEY `idx_aps_insert_event_time` (`event_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='APS插单事件表';

SET FOREIGN_KEY_CHECKS = 1;
