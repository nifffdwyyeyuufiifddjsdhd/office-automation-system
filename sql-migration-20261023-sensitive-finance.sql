-- 2026-10-23 敏感字段脱敏与总经理财务只读
-- 1) 新增「查看完整联系方式」权限码，发给人事专员与人事主管（管理员天然豁免）
INSERT INTO menus (parent_id, menu_name, menu_code, menu_type, is_nav, sort_order, status, created_at, updated_at)
SELECT 11, '查看完整联系方式', 'user:sensitive:view', 3, 0, 21, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'user:sensitive:view' AND deleted_at IS NULL);

INSERT INTO role_permissions (role_id, menu_id, created_at, updated_at)
SELECT r.id, m.id, NOW(), NOW()
FROM roles r JOIN menus m ON m.menu_code = 'user:sensitive:view' AND m.deleted_at IS NULL
WHERE r.role_code IN ('hr', 'hr_manager')
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.menu_id = m.id);

-- 2) 总经理恢复财务模块只读：给「财务管理」目录与「财务统计」，不给付款、导出、费用类别维护
INSERT INTO role_permissions (role_id, menu_id, created_at, updated_at)
SELECT r.id, m.id, NOW(), NOW()
FROM roles r JOIN menus m ON m.id IN (40, 44) AND m.deleted_at IS NULL
WHERE r.role_code = 'gm'
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.menu_id = m.id);
