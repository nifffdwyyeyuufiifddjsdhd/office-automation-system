-- 权限管理 / 系统管理 / 系统通知 / 访客角色 补充脚本（可重复执行）
-- 1. 通知模板表
CREATE TABLE IF NOT EXISTS notification_templates (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  template_code VARCHAR(64) NOT NULL COMMENT '模板编码',
  template_name VARCHAR(100) NOT NULL COMMENT '模板名称',
  biz_type VARCHAR(32) DEFAULT NULL COMMENT '业务类型',
  title_template VARCHAR(200) NOT NULL COMMENT '标题模板，支持 {变量}',
  content_template TEXT COMMENT '内容模板',
  channels VARCHAR(100) DEFAULT 'system' COMMENT '推送渠道，逗号分隔',
  status TINYINT DEFAULT 1 COMMENT '0停用 1启用',
  remark VARCHAR(255) DEFAULT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME DEFAULT NULL,
  KEY idx_template_code (template_code),
  KEY idx_biz_type (biz_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知模板';

-- 2. 权限管理导航（menu_code 用作导航权限码 role:manage）
INSERT INTO menus (parent_id, menu_name, menu_code, menu_type, path, component, icon, is_nav, page_url, sort_order, status, visible, created_at, updated_at)
SELECT 0, '权限管理', 'role:manage', 2, '/home/system/role', 'system/role/index', 'Lock', 1, NULL, 95, 1, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'role:manage' AND deleted_at IS NULL);

-- 3. 系统管理导航（menu_code = system:param）
INSERT INTO menus (parent_id, menu_name, menu_code, menu_type, path, component, icon, is_nav, page_url, sort_order, status, visible, created_at, updated_at)
SELECT 0, '系统管理', 'system:param', 2, '/home/system/manage', 'system/manage/index', 'Setting', 1, NULL, 96, 1, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'system:param' AND deleted_at IS NULL);

-- 4. 按钮级权限码（挂在权限管理/系统管理下面，menu_type=3、is_nav=0）
INSERT INTO menus (parent_id, menu_name, menu_code, menu_type, path, component, icon, is_nav, page_url, sort_order, status, visible, created_at, updated_at)
SELECT (SELECT id FROM (SELECT id FROM menus WHERE menu_code = 'role:manage' AND deleted_at IS NULL LIMIT 1) t), '分配角色', 'role:assign', 3, NULL, NULL, NULL, 0, NULL, 951, 1, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'role:assign' AND deleted_at IS NULL);

INSERT INTO menus (parent_id, menu_name, menu_code, menu_type, path, component, icon, is_nav, page_url, sort_order, status, visible, created_at, updated_at)
SELECT (SELECT id FROM (SELECT id FROM menus WHERE menu_code = 'system:param' AND deleted_at IS NULL LIMIT 1) t), '菜单与权限码', 'system:menu', 3, NULL, NULL, NULL, 0, NULL, 961, 1, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'system:menu' AND deleted_at IS NULL);

INSERT INTO menus (parent_id, menu_name, menu_code, menu_type, path, component, icon, is_nav, page_url, sort_order, status, visible, created_at, updated_at)
SELECT (SELECT id FROM (SELECT id FROM menus WHERE menu_code = 'system:param' AND deleted_at IS NULL LIMIT 1) t), '操作日志查看', 'log:operation:view', 3, NULL, NULL, NULL, 0, NULL, 962, 1, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'log:operation:view' AND deleted_at IS NULL);

INSERT INTO menus (parent_id, menu_name, menu_code, menu_type, path, component, icon, is_nav, page_url, sort_order, status, visible, created_at, updated_at)
SELECT (SELECT id FROM (SELECT id FROM menus WHERE menu_code = 'system:param' AND deleted_at IS NULL LIMIT 1) t), '异常日志查看', 'log:exception:view', 3, NULL, NULL, NULL, 0, NULL, 963, 1, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'log:exception:view' AND deleted_at IS NULL);

INSERT INTO menus (parent_id, menu_name, menu_code, menu_type, path, component, icon, is_nav, page_url, sort_order, status, visible, created_at, updated_at)
SELECT (SELECT id FROM (SELECT id FROM menus WHERE menu_code = 'system:param' AND deleted_at IS NULL LIMIT 1) t), '通知模板维护', 'notify:template:manage', 3, NULL, NULL, NULL, 0, NULL, 964, 1, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'notify:template:manage' AND deleted_at IS NULL);

INSERT INTO menus (parent_id, menu_name, menu_code, menu_type, path, component, icon, is_nav, page_url, sort_order, status, visible, created_at, updated_at)
SELECT (SELECT id FROM (SELECT id FROM menus WHERE menu_code = 'system:param' AND deleted_at IS NULL LIMIT 1) t), '推送记录查看', 'notify:log:view', 3, NULL, NULL, NULL, 0, NULL, 965, 1, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'notify:log:view' AND deleted_at IS NULL);

-- 5. 访客权限码：只看公开公告（挂在公告目录下，父级不存在时挂在根）
INSERT INTO menus (parent_id, menu_name, menu_code, menu_type, path, component, icon, is_nav, page_url, sort_order, status, visible, created_at, updated_at)
SELECT 0, '公开公告查看', 'announcement:view-public', 3, NULL, NULL, NULL, 0, NULL, 971, 1, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'announcement:view-public' AND deleted_at IS NULL);

-- 6. 给系统管理员授权新菜单与权限码
INSERT INTO role_permissions (role_id, menu_id, permission_type, created_at, updated_at)
SELECT 1, m.id, CASE WHEN m.menu_type = 3 THEN 2 ELSE 1 END, NOW(), NOW()
FROM menus m
WHERE m.deleted_at IS NULL
  AND m.menu_code IN ('role:manage','role:assign','system:param','system:menu','log:operation:view','log:exception:view','notify:template:manage','notify:log:view','announcement:view-public')
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = 1 AND rp.menu_id = m.id AND rp.deleted_at IS NULL);

-- 7. 访客角色（默认停用，需要时才启用）
INSERT INTO roles (role_name, role_code, role_type, description, sort_order, status, is_system, data_scope, created_at, updated_at)
SELECT '访客', 'guest', 3, '可选角色，默认不启用；只能查看公开公告，不能进入内部业务模块', 99, 0, 0, 4, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE role_code = 'guest' AND deleted_at IS NULL);

INSERT INTO role_permissions (role_id, menu_id, permission_type, created_at, updated_at)
SELECT r.id, m.id, 2, NOW(), NOW()
FROM roles r JOIN menus m ON m.menu_code = 'announcement:view-public' AND m.deleted_at IS NULL
WHERE r.role_code = 'guest' AND r.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.menu_id = m.id AND rp.deleted_at IS NULL);

-- 8. 常用通知模板，后端推送时按 template_code 取文案
INSERT INTO notification_templates (template_code, template_name, biz_type, title_template, content_template, channels, status, remark, created_at, updated_at)
SELECT 'flow_arrive', '待办流程到达', 'flow_instance', '新的待办流程：{title}', '流程 {instanceNo} 已到达「{stepName}」，请及时处理。', 'system', 1, '流程流转到下一个审批节点时使用', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_templates WHERE template_code = 'flow_arrive' AND deleted_at IS NULL);

INSERT INTO notification_templates (template_code, template_name, biz_type, title_template, content_template, channels, status, remark, created_at, updated_at)
SELECT 'flow_result', '流程审批结果', 'flow_instance', '流程{result}：{title}', '您发起的流程 {instanceNo} 已被{approver}处理，处理结果：{result}。', 'system', 1, '审批通过/驳回后通知发起人', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_templates WHERE template_code = 'flow_result' AND deleted_at IS NULL);

INSERT INTO notification_templates (template_code, template_name, biz_type, title_template, content_template, channels, status, remark, created_at, updated_at)
SELECT 'task_assign', '任务分配', 'task', '您有新任务：{title}', '{assigner} 把任务「{title}」指派给您，计划完成时间 {planEndTime}。', 'system', 1, '任务指派时使用', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_templates WHERE template_code = 'task_assign' AND deleted_at IS NULL);

INSERT INTO notification_templates (template_code, template_name, biz_type, title_template, content_template, channels, status, remark, created_at, updated_at)
SELECT 'attendance_abnormal', '出勤异常提醒', 'attendance', '出勤异常提醒：{date}', '您 {date} 的考勤状态为「{status}」，如有异议请在出勤管理里提交申诉。', 'system', 1, '迟到/早退/缺勤自动提醒', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_templates WHERE template_code = 'attendance_abnormal' AND deleted_at IS NULL);

INSERT INTO notification_templates (template_code, template_name, biz_type, title_template, content_template, channels, status, remark, created_at, updated_at)
SELECT 'system_notice', '系统通知', 'system', '{title}', '{content}', 'system', 1, '系统级公告与提醒的通用模板', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_templates WHERE template_code = 'system_notice' AND deleted_at IS NULL);

-- 9. 查看结果
SELECT 'menus' AS item, COUNT(*) AS total FROM menus WHERE menu_code IN ('role:manage','role:assign','system:param','system:menu','log:operation:view','log:exception:view','notify:template:manage','notify:log:view','announcement:view-public') AND deleted_at IS NULL
UNION ALL SELECT 'templates', COUNT(*) FROM notification_templates WHERE deleted_at IS NULL
UNION ALL SELECT 'guest_role', COUNT(*) FROM roles WHERE role_code = 'guest' AND deleted_at IS NULL;
