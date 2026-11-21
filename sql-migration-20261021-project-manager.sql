-- 项目经理（project_manager）模块：角色 + 项目成员表 + 菜单权限 + 项目采购申请流程
-- 依据：OA办公系统-角色功能与权限开发清单（六）项目经理 + TABLE 6
-- 1) 角色：project_manager，data_scope=5 表示「项目数据」（只看自己负责 / 参与的项目）
INSERT INTO roles (id, role_code, role_name, description, role_type, data_scope, sort_order, status, is_system)
SELECT 8, 'project_manager', '项目经理', '负责项目立项、成员管理、任务分派与验收，只能看自己项目的数据', 1, 5, 8, 1, 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE role_code = 'project_manager' AND deleted_at IS NULL);

-- 2) 项目成员表（projects 表本身没有成员关系）
CREATE TABLE IF NOT EXISTS project_members (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    project_id BIGINT NOT NULL COMMENT '项目ID',
    user_id BIGINT NOT NULL COMMENT '成员用户ID',
    role TINYINT NOT NULL DEFAULT 2 COMMENT '项目内角色：1项目经理 2成员 3关注人',
    join_time DATETIME NULL COMMENT '加入时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at DATETIME NULL COMMENT '软删除时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_project_user (project_id, user_id),
    KEY idx_project_members_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '项目成员';

-- 3) 菜单与权限码：项目管理导航 + 项目 / 任务 / 流程权限
INSERT INTO menus (id, parent_id, menu_name, menu_code, menu_type, is_nav, path, icon, sort_order, visible, open_type)
SELECT 66, 0, '项目管理', 'project', 2, 1, '/home/project', 'Folder', 3, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'project');

INSERT INTO menus (id, parent_id, menu_name, menu_code, menu_type, is_nav, path, sort_order, visible, open_type)
SELECT 67, 66, '项目列表', 'project:list', 3, 1, '/home/project', 1, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'project:list');
INSERT INTO menus (id, parent_id, menu_name, menu_code, menu_type, is_nav, sort_order, visible, open_type)
SELECT 68, 66, '新建项目', 'project:add', 3, 0, 2, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'project:add');
INSERT INTO menus (id, parent_id, menu_name, menu_code, menu_type, is_nav, sort_order, visible, open_type)
SELECT 69, 66, '编辑项目', 'project:edit', 3, 0, 3, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'project:edit');
INSERT INTO menus (id, parent_id, menu_name, menu_code, menu_type, is_nav, sort_order, visible, open_type)
SELECT 70, 66, '删除项目', 'project:delete', 3, 0, 4, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'project:delete');
INSERT INTO menus (id, parent_id, menu_name, menu_code, menu_type, is_nav, sort_order, visible, open_type)
SELECT 71, 66, '项目成员管理', 'project:member:manage', 3, 0, 5, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'project:member:manage');
INSERT INTO menus (id, parent_id, menu_name, menu_code, menu_type, is_nav, sort_order, visible, open_type)
SELECT 72, 66, '项目统计', 'project:stat', 3, 0, 6, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'project:stat');

-- 任务中心原本没有任何权限码，前端写死角色，这里按 TABLE 6/7/9 补齐
INSERT INTO menus (id, parent_id, menu_name, menu_code, menu_type, is_nav, sort_order, visible, open_type)
SELECT 73, 38, '创建任务', 'task:add', 3, 0, 1, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'task:add');
INSERT INTO menus (id, parent_id, menu_name, menu_code, menu_type, is_nav, sort_order, visible, open_type)
SELECT 74, 38, '分配与改派', 'task:assign', 3, 0, 2, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'task:assign');
INSERT INTO menus (id, parent_id, menu_name, menu_code, menu_type, is_nav, sort_order, visible, open_type)
SELECT 75, 38, '任务验收', 'task:accept-check', 3, 0, 3, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'task:accept-check');
INSERT INTO menus (id, parent_id, menu_name, menu_code, menu_type, is_nav, sort_order, visible, open_type)
SELECT 76, 38, '任务看板', 'task:board', 3, 0, 4, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'task:board');
INSERT INTO menus (id, parent_id, menu_name, menu_code, menu_type, is_nav, sort_order, visible, open_type)
SELECT 77, 38, '任务统计', 'task:stat', 3, 0, 5, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'task:stat');
INSERT INTO menus (id, parent_id, menu_name, menu_code, menu_type, is_nav, sort_order, visible, open_type)
SELECT 78, 38, '添加任务成员', 'task:member:add', 3, 0, 6, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'task:member:add');
INSERT INTO menus (id, parent_id, menu_name, menu_code, menu_type, is_nav, sort_order, visible, open_type)
SELECT 79, 38, '移除任务成员', 'task:member:remove', 3, 0, 7, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'task:member:remove');
INSERT INTO menus (id, parent_id, menu_name, menu_code, menu_type, is_nav, sort_order, visible, open_type)
SELECT 80, 38, '任务查看（本部门）', 'task:view-dept', 3, 0, 8, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'task:view-dept');
INSERT INTO menus (id, parent_id, menu_name, menu_code, menu_type, is_nav, sort_order, visible, open_type)
SELECT 81, 38, '接受任务', 'task:accept', 3, 0, 9, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'task:accept');
INSERT INTO menus (id, parent_id, menu_name, menu_code, menu_type, is_nav, sort_order, visible, open_type)
SELECT 82, 38, '更新任务', 'task:update', 3, 0, 10, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'task:update');
INSERT INTO menus (id, parent_id, menu_name, menu_code, menu_type, is_nav, sort_order, visible, open_type)
SELECT 83, 38, '提交任务', 'task:submit', 3, 0, 11, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'task:submit');
INSERT INTO menus (id, parent_id, menu_name, menu_code, menu_type, is_nav, sort_order, visible, open_type)
SELECT 84, 38, '任务评论', 'task:comment', 3, 0, 12, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'task:comment');
INSERT INTO menus (id, parent_id, menu_name, menu_code, menu_type, is_nav, sort_order, visible, open_type)
SELECT 85, 28, '项目类流程审批', 'flow:approve-project', 3, 0, 7, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'flow:approve-project');

-- 4) 授权：管理员全部；项目经理项目 + 任务管理 + 项目流程；部门主管任务本部门；员工任务执行
INSERT INTO role_permissions (role_id, menu_id)
SELECT 1, m.id FROM menus m
WHERE m.menu_code IN ('project','project:list','project:add','project:edit','project:delete','project:member:manage','project:stat',
    'task:add','task:assign','task:accept-check','task:board','task:stat','task:member:add','task:member:remove','task:view-dept',
    'task:accept','task:update','task:submit','task:comment','flow:approve-project')
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = 1 AND rp.menu_id = m.id AND rp.deleted_at IS NULL);

INSERT INTO role_permissions (role_id, menu_id)
SELECT 8, m.id FROM menus m
WHERE m.menu_code IN ('project','project:list','project:add','project:edit','project:delete','project:member:manage','project:stat',
    'task:add','task:assign','task:accept-check','task:board','task:stat','task:member:add','task:member:remove',
    'announcement:list','flow:start','flow:todo','flow:done','flow:mine','flow:approve-project')
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = 8 AND rp.menu_id = m.id AND rp.deleted_at IS NULL);

INSERT INTO role_permissions (role_id, menu_id)
SELECT 2, m.id FROM menus m
WHERE m.menu_code IN ('task:add','task:assign','task:stat','task:view-dept')
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = 2 AND rp.menu_id = m.id AND rp.deleted_at IS NULL);

INSERT INTO role_permissions (role_id, menu_id)
SELECT 3, m.id FROM menus m
WHERE m.menu_code IN ('task:accept','task:update','task:submit','task:comment')
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = 3 AND rp.menu_id = m.id AND rp.deleted_at IS NULL);

-- 5) 项目采购申请流程：部门主管审批 -> 项目经理审批 -> 财务审批（需要付款，进财务付款池）
INSERT INTO flow_definitions (id, flow_name, code, description, flow_type, status, need_payment, form_config, creator_id, sort_order, version, publish_time)
SELECT 14, '项目采购申请', 'project_purchase', '项目内采购：部门主管确认需求，项目经理审核项目预算，财务审批后付款', 1, 1, 1,
       '[{"field":"project_name","label":"所属项目","type":"text","required":true,"isPrimary":true},{"field":"item_name","label":"采购物品","type":"text","required":true},{"field":"amount","label":"采购金额(元)","type":"number","required":true},{"field":"reason","label":"采购理由","type":"textarea","required":true},{"field":"expect_date","label":"期望到货日期","type":"date","required":false}]',
       1, 8, 1, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM flow_definitions WHERE code = 'project_purchase');

INSERT INTO flow_steps (id, definition_id, step_name, step_code, prev_step_id, step_type, approver_type, approver_config, sort_order, sign_type, sign_ratio)
SELECT 30, 14, '部门主管审批', 'dept_approve', 0, 1, 3, NULL, 1, 1, 100 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM flow_steps WHERE definition_id = 14 AND step_code = 'dept_approve');
INSERT INTO flow_steps (id, definition_id, step_name, step_code, prev_step_id, step_type, approver_type, approver_config, sort_order, sign_type, sign_ratio)
SELECT 31, 14, '项目经理审批', 'pm_approve', 0, 1, 2, '{"roleId":8}', 2, 1, 100 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM flow_steps WHERE definition_id = 14 AND step_code = 'pm_approve');
INSERT INTO flow_steps (id, definition_id, step_name, step_code, prev_step_id, step_type, approver_type, approver_config, sort_order, sign_type, sign_ratio)
SELECT 32, 14, '财务审批', 'finance_approve', 0, 1, 2, '{"roleId":5}', 3, 1, 100 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM flow_steps WHERE definition_id = 14 AND step_code = 'finance_approve');

INSERT INTO flow_step_transitions (id, step_id, target_step_id, condition_expr, condition_name, is_default, priority, transition_type)
SELECT 11, 30, 31, NULL, '部门主管通过转项目经理', 1, 0, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM flow_step_transitions WHERE step_id = 30 AND target_step_id = 31);
INSERT INTO flow_step_transitions (id, step_id, target_step_id, condition_expr, condition_name, is_default, priority, transition_type)
SELECT 12, 31, 32, NULL, '项目经理通过转财务', 1, 0, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM flow_step_transitions WHERE step_id = 31 AND target_step_id = 32);

-- 6) 结果核对
SELECT 'role' AS k, id, role_code, role_name, data_scope FROM roles WHERE role_code = 'project_manager';
SELECT 'menus' AS k, COUNT(*) AS cnt FROM menus WHERE id BETWEEN 66 AND 85;
SELECT 'pm_perms' AS k, GROUP_CONCAT(m.menu_code ORDER BY m.id) AS v
FROM role_permissions rp JOIN menus m ON m.id = rp.menu_id WHERE rp.role_id = 8 AND rp.deleted_at IS NULL;
SELECT 'flow' AS k, id, flow_name, status, need_payment FROM flow_definitions WHERE code = 'project_purchase';
SELECT 'steps' AS k, id, step_name, approver_type, approver_config, sort_order FROM flow_steps WHERE definition_id = 14 ORDER BY sort_order;
SELECT 'trans' AS k, step_id, target_step_id, is_default FROM flow_step_transitions WHERE step_id IN (30, 31);
