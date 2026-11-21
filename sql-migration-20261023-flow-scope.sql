-- 2026-10-23 流程使用范围（flow:perm:grant）+ 任务删除权限码
ALTER TABLE flow_definitions
    ADD COLUMN use_scope TINYINT NOT NULL DEFAULT 0 COMMENT '使用范围 0全员 1指定部门 2指定角色' AFTER need_payment,
    ADD COLUMN use_scope_ids VARCHAR(500) NULL COMMENT '使用范围对象 id，JSON 数组' AFTER use_scope;

INSERT INTO menus (parent_id, menu_name, menu_code, menu_type, is_nav, sort_order, status, created_at, updated_at)
SELECT 38, '删除任务', 'task:delete', 3, 0, 60, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code='task:delete');

INSERT INTO menus (parent_id, menu_name, menu_code, menu_type, is_nav, sort_order, status, created_at, updated_at)
SELECT 28, '设置流程使用权限', 'flow:perm:grant', 3, 0, 70, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code='flow:perm:grant');

INSERT INTO role_permissions (role_id, menu_id, permission_type, created_at, updated_at)
SELECT 1, m.id, 1, NOW(), NOW() FROM menus m
WHERE m.menu_code IN ('task:delete','flow:perm:grant')
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id=1 AND rp.menu_id=m.id);
