-- APS 甘特图隐藏菜单补丁
-- 执行前请确保 aps_menu_init.sql 已执行，且调度任务菜单 menu_id = 2007。

DELETE FROM sys_menu WHERE menu_id = 20076;

INSERT INTO sys_menu(
    menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_by, create_time, update_by, update_time, remark
) VALUES (
    20076, '调度甘特图', 2007, 6, 'gantt', 'aps/scheduleTask/gantt', NULL, '',
    1, 0, 'C', '1', '0', 'aps:scheduleTask:list', 'chart',
    'admin', sysdate(), '', NULL, 'APS调度任务甘特图隐藏菜单'
);
