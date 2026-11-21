-- 薪资字段与查看权限：users 落库，只有 HR/财务/总经理/管理员能看
ALTER TABLE users ADD COLUMN salary DECIMAL(12,2) NULL COMMENT '月薪（仅HR/财务/总经理/管理员可见）' AFTER bank_name;

INSERT INTO menus (parent_id, menu_name, menu_code, menu_type, is_nav, sort_order, status, created_at, updated_at)
SELECT 11, '查看薪资', 'user:salary:view', 3, 0, 22, 1, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'user:salary:view');

INSERT INTO role_permissions (role_id, menu_id, created_at)
SELECT r.id, m.id, NOW() FROM roles r CROSS JOIN menus m
WHERE r.role_code IN ('hr','hr_manager','finance','gm') AND m.menu_code = 'user:salary:view'
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.menu_id = m.id);

-- 访客角色正式启用（默认停用改启用，白名单 /api/public/** 早前已放开）
UPDATE roles SET status = 1, updated_at = NOW() WHERE role_code = 'guest';
