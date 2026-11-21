-- 角色可见模块纠偏（依据 OA办公系统-角色功能与权限开发清单 TABLE 2/4/5/7）
-- 部门主管：用户管理(11)、全公司日志(19)、统计报表(39)、申诉审核(61) 都不属于他
DELETE FROM role_permissions WHERE role_id = 2 AND menu_id IN (11, 19, 39, 61);
-- 人事专员：组织机构管理(24) 归人事主管；统计报表(39) 只给管理员和总经理
DELETE FROM role_permissions WHERE role_id = 4 AND menu_id IN (24, 39);
-- 总经理：财务导出(42)、财务统计(44) 不在 TABLE 2 的模块清单里，看财务数据走统计报表
DELETE FROM role_permissions WHERE role_id = 6 AND menu_id IN (42, 44);

-- 新增「本部门」级别的查看码，供部门主管使用（数据范围仍由 DataScopeService 兜底）
INSERT INTO menus (id, parent_id, menu_name, menu_code, menu_type, is_nav, sort_order)
SELECT 64, 18, '日志查看（本部门）', 'worklog:view-dept', 3, 1, 6 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'worklog:view-dept');

INSERT INTO menus (id, parent_id, menu_name, menu_code, menu_type, is_nav, sort_order)
SELECT 65, 0, '出勤查看（本部门）', 'attendance:view-dept', 3, 0, 905 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'attendance:view-dept');

INSERT INTO role_permissions (role_id, menu_id)
SELECT 2, m.id FROM menus m
WHERE m.menu_code IN ('worklog:view-dept', 'attendance:view-dept')
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = 2 AND rp.menu_id = m.id AND rp.deleted_at IS NULL);

SELECT 'role2' AS k, GROUP_CONCAT(m.menu_code ORDER BY m.id) AS v FROM role_permissions rp JOIN menus m ON m.id=rp.menu_id WHERE rp.role_id=2 AND rp.deleted_at IS NULL;
SELECT 'role4' AS k, GROUP_CONCAT(m.menu_code ORDER BY m.id) AS v FROM role_permissions rp JOIN menus m ON m.id=rp.menu_id WHERE rp.role_id=4 AND rp.deleted_at IS NULL;
SELECT 'role6' AS k, GROUP_CONCAT(m.menu_code ORDER BY m.id) AS v FROM role_permissions rp JOIN menus m ON m.id=rp.menu_id WHERE rp.role_id=6 AND rp.deleted_at IS NULL;
