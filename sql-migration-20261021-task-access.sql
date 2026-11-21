-- 2026-10-21 任务中心访问权：员工默认没有，被派发任务时自动开通；也可由管理员/持 task:grant 的项目经理手动授予

INSERT INTO menus (menu_name, menu_code, parent_id, menu_type, is_nav, sort_order, status, created_at, updated_at)
SELECT '任务中心访问', 'task:access', 38, 3, 0, 0, 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'task:access');

INSERT INTO menus (menu_name, menu_code, parent_id, menu_type, is_nav, sort_order, status, created_at, updated_at)
SELECT '发放任务权限', 'task:grant', 38, 3, 0, 13, 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'task:grant');

-- 角色级默认：管理员/部门主管/总经理/项目经理能进任务中心；「发放权限」的权限只归管理员
INSERT INTO role_permissions (role_id, menu_id, permission_type, created_at, updated_at)
SELECT r.id, m.id, 2, NOW(), NOW()
FROM roles r
JOIN menus m ON m.menu_code = 'task:access'
WHERE r.role_code IN ('admin', 'manager', 'gm', 'project_manager')
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.menu_id = m.id AND rp.deleted_at IS NULL);

INSERT INTO role_permissions (role_id, menu_id, permission_type, created_at, updated_at)
SELECT r.id, m.id, 2, NOW(), NOW()
FROM roles r
JOIN menus m ON m.menu_code = 'task:grant'
WHERE r.role_code = 'admin'
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.menu_id = m.id AND rp.deleted_at IS NULL);

CREATE TABLE IF NOT EXISTS user_permissions (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  user_id BIGINT NOT NULL COMMENT '被授权的用户',
  menu_id BIGINT NOT NULL COMMENT '权限（menus.id）',
  granted_by BIGINT DEFAULT NULL COMMENT '授权人',
  source TINYINT NOT NULL DEFAULT 1 COMMENT '来源：1管理员授予 2派发任务自动开通',
  reason VARCHAR(200) DEFAULT NULL COMMENT '授予原因',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME DEFAULT NULL COMMENT '删除时间（软删除）',
  PRIMARY KEY (id),
  KEY idx_user_permissions_user (user_id),
  KEY idx_user_permissions_menu (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户级额外权限（角色之外单独授予）';
