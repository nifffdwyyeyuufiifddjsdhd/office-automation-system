-- 数据范围码统一为：1=全公司 2=本部门 3=本部门及下级 4=仅本人
UPDATE roles SET data_scope = 1 WHERE role_code IN ('admin', 'gm');
UPDATE roles SET data_scope = 2 WHERE role_code IN ('manager', 'hr', 'finance');
UPDATE roles SET data_scope = 3 WHERE role_code = 'hr_manager';
UPDATE roles SET data_scope = 4 WHERE role_code = 'employee';

-- 请假申请按文档默认模板改成 部门主管 -> 人事主管，原来只有一步且批到人事专员
UPDATE flow_steps SET step_name = '部门主管审批', step_code = 'dept_approve', sort_order = 1,
       approver_type = 2, approver_config = '{"roleId":2}' WHERE id = 1;
INSERT INTO flow_steps (id, definition_id, step_name, step_code, prev_step_id, step_type, approver_type,
                        approver_config, step_form, timeout_hours, timeout_action, sort_order, sign_type, sign_ratio)
VALUES (21, 1, '人事主管备案', 'hr_confirm', 0, 1, 2, '{"roleId":7}', NULL, 0, 0, 2, 1, NULL);

-- 重复的请假定义 def3 停用，审批角色一并改回部门主管，历史实例仍可继续审批
UPDATE flow_steps SET approver_config = '{"roleId":2}' WHERE id = 3;
UPDATE flow_definitions SET flow_name = '请假申请（旧版）', status = 0 WHERE id = 3;

-- 资金申请第一步应该是部门主管，之前错配成财务
UPDATE flow_steps SET approver_config = '{"roleId":2}' WHERE id = 4;

-- 大额资金报销补齐第一步部门主管，形成 部门主管 -> 财务主管 -> 经理
INSERT INTO flow_steps (id, definition_id, step_name, step_code, prev_step_id, step_type, approver_type,
                        approver_config, step_form, timeout_hours, timeout_action, sort_order, sign_type, sign_ratio)
VALUES (22, 6, '部门主管审批', 'dept_approve', 0, 1, 2, '{"roleId":2}', NULL, 0, 0, 1, 1, NULL);
UPDATE flow_steps SET sort_order = 2 WHERE id = 5;
UPDATE flow_steps SET sort_order = 3 WHERE id = 6;

-- 测试残留的流程定义停用，发起流程列表里不再出现
UPDATE flow_definitions SET status = 0 WHERE id IN (2, 7);
