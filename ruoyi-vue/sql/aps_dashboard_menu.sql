-- APS production scheduling dashboard menu.
-- The page aggregates existing APS query APIs and does not add business tables.
SET @aps_parent_id := 2000;

INSERT INTO sys_menu(
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark
)
SELECT 2010, '生产调度驾驶舱', @aps_parent_id, 0, 'dashboard',
       'aps/dashboard/index', NULL, 'ApsDashboard',
       1, 0, 'C', '0', '0', 'aps:dashboard:view', 'dashboard',
       'admin', sysdate(), '', NULL, 'APS生产调度状态统一展示入口'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu WHERE menu_id = 2010 OR (parent_id = @aps_parent_id AND path = 'dashboard')
);
