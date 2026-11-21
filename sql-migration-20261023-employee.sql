-- 2026-10-23 普通员工（employee）模块：补齐文档 TABLE 9 要求的按钮级权限码
-- 用途：工作日志撰写/修改/删除/评论/补写、出勤签到签退补签申诉外出、流程撤回催办、个人中心改密与通知、在线用户基础列表
-- 授权策略：这些都是「日常办公基线」，文档里各角色都写了「日常办公同 employee」，所以统一授予 role 1~8

INSERT INTO menus (parent_id, menu_name, menu_code, menu_type, is_nav, sort_order, status, visible, created_at, updated_at)
SELECT 18, '撰写工作日志', 'worklog:add', 3, 0, 1, 1, 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'worklog:add' AND deleted_at IS NULL);

INSERT INTO menus (parent_id, menu_name, menu_code, menu_type, is_nav, sort_order, status, visible, created_at, updated_at)
SELECT 18, '修改当天日志', 'worklog:edit', 3, 0, 2, 1, 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'worklog:edit' AND deleted_at IS NULL);

INSERT INTO menus (parent_id, menu_name, menu_code, menu_type, is_nav, sort_order, status, visible, created_at, updated_at)
SELECT 18, '删除日志', 'worklog:delete', 3, 0, 3, 1, 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'worklog:delete' AND deleted_at IS NULL);

INSERT INTO menus (parent_id, menu_name, menu_code, menu_type, is_nav, sort_order, status, visible, created_at, updated_at)
SELECT 18, '日志评论', 'worklog:comment', 3, 0, 4, 1, 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'worklog:comment' AND deleted_at IS NULL);

INSERT INTO menus (parent_id, menu_name, menu_code, menu_type, is_nav, sort_order, status, visible, created_at, updated_at)
SELECT 18, '日志补写申请', 'worklog:makeup', 3, 0, 5, 1, 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'worklog:makeup' AND deleted_at IS NULL);

INSERT INTO menus (parent_id, menu_name, menu_code, menu_type, is_nav, sort_order, status, visible, created_at, updated_at)
SELECT 18, '查看被授权日志', 'worklog:view-auth', 3, 0, 6, 1, 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'worklog:view-auth' AND deleted_at IS NULL);

INSERT INTO menus (parent_id, menu_name, menu_code, menu_type, is_nav, sort_order, status, visible, created_at, updated_at)
SELECT 28, '撤回流程', 'flow:withdraw', 3, 0, 10, 1, 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'flow:withdraw' AND deleted_at IS NULL);

INSERT INTO menus (parent_id, menu_name, menu_code, menu_type, is_nav, sort_order, status, visible, created_at, updated_at)
SELECT 28, '催办流程', 'flow:urge', 3, 0, 11, 1, 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'flow:urge' AND deleted_at IS NULL);

INSERT INTO menus (parent_id, menu_name, menu_code, menu_type, is_nav, sort_order, status, visible, created_at, updated_at)
SELECT 0, '上班签到', 'attendance:check-in', 3, 0, 910, 1, 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'attendance:check-in' AND deleted_at IS NULL);

INSERT INTO menus (parent_id, menu_name, menu_code, menu_type, is_nav, sort_order, status, visible, created_at, updated_at)
SELECT 0, '下班签退', 'attendance:check-out', 3, 0, 911, 1, 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'attendance:check-out' AND deleted_at IS NULL);

INSERT INTO menus (parent_id, menu_name, menu_code, menu_type, is_nav, sort_order, status, visible, created_at, updated_at)
SELECT 0, '查看本人出勤', 'attendance:view-own', 3, 0, 912, 1, 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'attendance:view-own' AND deleted_at IS NULL);

INSERT INTO menus (parent_id, menu_name, menu_code, menu_type, is_nav, sort_order, status, visible, created_at, updated_at)
SELECT 0, '补签申请', 'attendance:makeup:apply', 3, 0, 913, 1, 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'attendance:makeup:apply' AND deleted_at IS NULL);

INSERT INTO menus (parent_id, menu_name, menu_code, menu_type, is_nav, sort_order, status, visible, created_at, updated_at)
SELECT 0, '出勤异常申诉', 'attendance:appeal:apply', 3, 0, 914, 1, 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'attendance:appeal:apply' AND deleted_at IS NULL);

INSERT INTO menus (parent_id, menu_name, menu_code, menu_type, is_nav, sort_order, status, visible, created_at, updated_at)
SELECT 0, '外出打卡', 'attendance:out', 3, 0, 915, 1, 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'attendance:out' AND deleted_at IS NULL);

INSERT INTO menus (parent_id, menu_name, menu_code, menu_type, is_nav, sort_order, status, visible, created_at, updated_at)
SELECT 0, '维护个人资料', 'user:profile:edit', 3, 0, 920, 1, 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'user:profile:edit' AND deleted_at IS NULL);

INSERT INTO menus (parent_id, menu_name, menu_code, menu_type, is_nav, sort_order, status, visible, created_at, updated_at)
SELECT 0, '修改密码', 'user:change-pwd', 3, 0, 921, 1, 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'user:change-pwd' AND deleted_at IS NULL);

INSERT INTO menus (parent_id, menu_name, menu_code, menu_type, is_nav, sort_order, status, visible, created_at, updated_at)
SELECT 0, '查看系统通知', 'notify:view', 3, 0, 922, 1, 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'notify:view' AND deleted_at IS NULL);

INSERT INTO menus (parent_id, menu_name, menu_code, menu_type, is_nav, sort_order, status, visible, created_at, updated_at)
SELECT 0, '我的待办汇总', 'todo:view', 3, 0, 923, 1, 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'todo:view' AND deleted_at IS NULL);

INSERT INTO menus (parent_id, menu_name, menu_code, menu_type, is_nav, sort_order, status, visible, created_at, updated_at)
SELECT 0, '在线用户基础列表', 'online:view', 3, 0, 924, 1, 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'online:view' AND deleted_at IS NULL);

-- 统一授予 role 1~8（日常办公基线权限）
INSERT INTO role_permissions (role_id, menu_id, permission_type, created_at, updated_at)
SELECT r.id, m.id, 1, NOW(), NOW()
FROM roles r
JOIN menus m ON m.menu_code IN (
  'worklog:add','worklog:edit','worklog:delete','worklog:comment','worklog:makeup','worklog:view-auth',
  'flow:withdraw','flow:urge',
  'attendance:check-in','attendance:check-out','attendance:view-own','attendance:makeup:apply','attendance:appeal:apply','attendance:out',
  'user:profile:edit','user:change-pwd','notify:view','todo:view','online:view'
)
LEFT JOIN role_permissions rp ON rp.role_id = r.id AND rp.menu_id = m.id AND rp.deleted_at IS NULL
WHERE r.id BETWEEN 1 AND 8 AND r.deleted_at IS NULL AND m.deleted_at IS NULL AND rp.id IS NULL;

-- 公告「查看已读情况」按文档属于员工能力，一并放开（只读统计，不改数据）
INSERT INTO role_permissions (role_id, menu_id, permission_type, created_at, updated_at)
SELECT r.id, m.id, 1, NOW(), NOW()
FROM roles r
JOIN menus m ON m.menu_code = 'announcement:reads'
LEFT JOIN role_permissions rp ON rp.role_id = r.id AND rp.menu_id = m.id AND rp.deleted_at IS NULL
WHERE r.id BETWEEN 1 AND 8 AND r.deleted_at IS NULL AND m.deleted_at IS NULL AND rp.id IS NULL;
