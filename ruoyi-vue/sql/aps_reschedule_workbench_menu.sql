-- APS 插单重调度工作台菜单
-- 仅新增统一业务入口，不替换现有 CRUD 菜单。
SET @aps_parent_id := 2000;

INSERT INTO sys_menu(
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark
)
SELECT 2009, '插单重调度工作台', @aps_parent_id, 9, 'rescheduleWorkbench',
       'aps/rescheduleWorkbench/index', NULL, 'RescheduleWorkbench',
       1, 0, 'C', '0', '0', 'aps:rescheduleWorkbench:view', 'dashboard',
       'admin', sysdate(), '', NULL, '半导体可重入插单重调度统一演示入口'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2009);
