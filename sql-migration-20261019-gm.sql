-- GM（总经理/高管）功能补齐：权限标识、流程定义、金额阈值跳转
-- 2026-10-19

-- 1) 补齐文档里要求但库里缺失的权限标识（按钮型，不进导航栏）
INSERT INTO menus (parent_id, menu_name, menu_code, menu_type, path, component, icon, is_nav, page_url, nav_icon, open_type, sort_order, status, visible)
VALUES
  (0, '出勤全局查看', 'attendance:view-all', 3, NULL, NULL, NULL, 0, NULL, NULL, 1, 901, 1, 0),
  (0, '出勤配置管理', 'attendance:manage', 3, NULL, NULL, NULL, 0, NULL, NULL, 1, 902, 1, 0),
  (0, '任务全局查看', 'task:view-all', 3, NULL, NULL, NULL, 0, NULL, NULL, 1, 903, 1, 0),
  (0, '报表查看', 'report:view', 3, NULL, NULL, NULL, 0, NULL, NULL, 1, 904, 1, 0)
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), menu_type = 3, is_nav = 0, visible = 0, status = 1, deleted_at = NULL;

-- 2) 总经理/高管拿到出勤、任务、报表的全局查看标识
INSERT INTO role_permissions (role_id, menu_id, permission_type)
SELECT 6, m.id, 2
FROM menus m
WHERE m.menu_code IN ('attendance:view-all', 'task:view-all', 'report:view')
  AND m.deleted_at IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = 6 AND rp.menu_id = m.id AND rp.deleted_at IS NULL
  );

-- 3) 流程的四个标识补发给部门主管、普通员工、人事、财务（此前只有 gm/admin 有，接口鉴权后会误伤）
INSERT INTO role_permissions (role_id, menu_id, permission_type)
SELECT r.id, m.id, IF(m.menu_type = 3, 2, 1)
FROM roles r
JOIN menus m ON m.menu_code IN ('flow:start', 'flow:todo', 'flow:done', 'flow:mine') AND m.deleted_at IS NULL
WHERE r.id IN (2, 3, 4, 5) AND r.deleted_at IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = r.id AND rp.menu_id = m.id AND rp.deleted_at IS NULL
  );

-- 4) 大额资金报销：表单补上金额字段，阈值跳转改成真正的条件分支
UPDATE flow_definitions
SET form_config = '[{"field":"amount","label":"报销金额(元)","type":"number","required":true,"isPrimary":true},{"field":"reason","label":"报销事由","type":"textarea","required":true}]',
    description = '报销金额满 1500 元转总经理审批，未达到由财务审批后直接结束',
    sort_order = 3
WHERE id = 6 AND deleted_at IS NULL;

-- 原来 5→6 是默认转移，改成条件：财务审批后只有金额达标才流转到总经理
UPDATE flow_step_transitions
SET condition_expr = 'amount >= 1500',
    condition_name = '金额满1500转总经理',
    is_default = 0,
    priority = 1
WHERE id = 2 AND deleted_at IS NULL;

-- 5) 采购申请：部门主管 → 财务 → 金额满 5000 再转总经理
INSERT INTO flow_definitions (flow_name, code, flow_type, description, dept_id, icon, form_config, version, status, audit_status, creator_id, publish_time, sort_order)
VALUES ('采购申请', 'purchase', 1, '部门主管 → 财务审批；金额满 5000 元在财务之后追加总经理审批', NULL, 'ShoppingCart',
        '[{"field":"item_name","label":"采购物品","type":"text","required":true,"isPrimary":true},{"field":"amount","label":"采购金额(元)","type":"number","required":true},{"field":"reason","label":"采购事由","type":"textarea","required":true},{"field":"expect_date","label":"期望到货日期","type":"date","required":false}]',
        1, 1, 1, 1, NOW(), 4)
ON DUPLICATE KEY UPDATE flow_name = VALUES(flow_name), form_config = VALUES(form_config), description = VALUES(description), status = 1, deleted_at = NULL;

SET @def := (SELECT id FROM flow_definitions WHERE code = 'purchase' AND deleted_at IS NULL);

INSERT INTO flow_steps (definition_id, step_name, step_code, prev_step_id, step_type, approver_type, approver_config, step_form, timeout_hours, timeout_action, sort_order, sign_type)
SELECT @def, '部门主管审批', 'step1', 0, 1, 2, '{"roleId":2}', NULL, 0, 0, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM flow_steps WHERE definition_id = @def AND step_code = 'step1' AND deleted_at IS NULL);

INSERT INTO flow_steps (definition_id, step_name, step_code, prev_step_id, step_type, approver_type, approver_config, step_form, timeout_hours, timeout_action, sort_order, sign_type)
SELECT @def, '财务审批', 'step2', 0, 1, 2, '{"roleId":5}', NULL, 0, 0, 2, 1
WHERE NOT EXISTS (SELECT 1 FROM flow_steps WHERE definition_id = @def AND step_code = 'step2' AND deleted_at IS NULL);

INSERT INTO flow_steps (definition_id, step_name, step_code, prev_step_id, step_type, approver_type, approver_config, step_form, timeout_hours, timeout_action, sort_order, sign_type)
SELECT @def, '总经理审批', 'step3', 0, 1, 2, '{"roleId":6}', NULL, 0, 0, 3, 1
WHERE NOT EXISTS (SELECT 1 FROM flow_steps WHERE definition_id = @def AND step_code = 'step3' AND deleted_at IS NULL);

SET @s1 := (SELECT id FROM flow_steps WHERE definition_id = @def AND step_code = 'step1' AND deleted_at IS NULL LIMIT 1);
SET @s2 := (SELECT id FROM flow_steps WHERE definition_id = @def AND step_code = 'step2' AND deleted_at IS NULL LIMIT 1);
SET @s3 := (SELECT id FROM flow_steps WHERE definition_id = @def AND step_code = 'step3' AND deleted_at IS NULL LIMIT 1);

INSERT INTO flow_step_transitions (step_id, target_step_id, condition_expr, condition_name, priority, is_default, transition_type)
SELECT @s1, @s2, NULL, '部门主管通过', 0, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM flow_step_transitions WHERE step_id = @s1 AND target_step_id = @s2 AND deleted_at IS NULL);

INSERT INTO flow_step_transitions (step_id, target_step_id, condition_expr, condition_name, priority, is_default, transition_type)
SELECT @s2, @s3, 'amount >= 5000', '金额满5000转总经理', 1, 0, 1
WHERE NOT EXISTS (SELECT 1 FROM flow_step_transitions WHERE step_id = @s2 AND target_step_id = @s3 AND deleted_at IS NULL);

-- 6) 人事变动：人事专员 → 总经理，覆盖入职、离职、调岗三类
INSERT INTO flow_definitions (flow_name, code, flow_type, description, dept_id, icon, form_config, version, status, audit_status, creator_id, publish_time, sort_order)
VALUES ('人事变动', 'hr_change', 1, '入职/离职/调岗走人事专员审核，再报总经理审批', NULL, 'UserFilled',
        '[{"field":"change_type","label":"变动类型（入职/离职/调岗）","type":"text","required":true,"isPrimary":true},{"field":"employee_name","label":"涉及员工","type":"text","required":true},{"field":"org_name","label":"所属部门","type":"text","required":false},{"field":"effective_date","label":"生效日期","type":"date","required":true},{"field":"reason","label":"变动说明","type":"textarea","required":true}]',
        1, 1, 1, 1, NOW(), 6)
ON DUPLICATE KEY UPDATE flow_name = VALUES(flow_name), form_config = VALUES(form_config), description = VALUES(description), status = 1, deleted_at = NULL;

SET @hdef := (SELECT id FROM flow_definitions WHERE code = 'hr_change' AND deleted_at IS NULL);

INSERT INTO flow_steps (definition_id, step_name, step_code, prev_step_id, step_type, approver_type, approver_config, step_form, timeout_hours, timeout_action, sort_order, sign_type)
SELECT @hdef, '人事专员审核', 'step1', 0, 1, 2, '{"roleId":4}', NULL, 0, 0, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM flow_steps WHERE definition_id = @hdef AND step_code = 'step1' AND deleted_at IS NULL);

INSERT INTO flow_steps (definition_id, step_name, step_code, prev_step_id, step_type, approver_type, approver_config, step_form, timeout_hours, timeout_action, sort_order, sign_type)
SELECT @hdef, '总经理审批', 'step2', 0, 1, 2, '{"roleId":6}', NULL, 0, 0, 2, 1
WHERE NOT EXISTS (SELECT 1 FROM flow_steps WHERE definition_id = @hdef AND step_code = 'step2' AND deleted_at IS NULL);

SET @h1 := (SELECT id FROM flow_steps WHERE definition_id = @hdef AND step_code = 'step1' AND deleted_at IS NULL LIMIT 1);
SET @h2 := (SELECT id FROM flow_steps WHERE definition_id = @hdef AND step_code = 'step2' AND deleted_at IS NULL LIMIT 1);

INSERT INTO flow_step_transitions (step_id, target_step_id, condition_expr, condition_name, priority, is_default, transition_type)
SELECT @h1, @h2, NULL, '人事审核通过转总经理', 0, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM flow_step_transitions WHERE step_id = @h1 AND target_step_id = @h2 AND deleted_at IS NULL);
