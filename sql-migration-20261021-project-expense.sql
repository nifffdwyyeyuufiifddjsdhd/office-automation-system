-- 项目费用报销：项目经理审批项目相关费用，walk 部门主管 -> 项目经理 -> 财务
INSERT INTO flow_definitions (id, flow_name, code, flow_type, description, form_config, status, audit_status, creator_id, publish_time, sort_order, need_payment)
VALUES (15, '项目费用报销', 'project_expense', 1, '项目内费用报销：部门主管确认，项目经理审核项目成本，财务审批后付款',
'[{"field":"project_name","label":"所属项目","type":"text","required":true,"isPrimary":true},{"field":"amount","label":"报销金额(元)","type":"number","required":true},{"field":"reason","label":"费用说明","type":"textarea","required":true},{"field":"expense_date","label":"发生日期","type":"date","required":false}]',
1, 1, 1, NOW(), 9, 1);

INSERT INTO flow_steps (id, definition_id, step_name, step_code, approver_type, approver_config, sort_order, sign_type)
VALUES (33, 15, '部门主管审批', 'dept_approve', 3, NULL, 1, 1),
       (34, 15, '项目经理审批', 'pm_approve', 2, '{"roleId":8}', 2, 1),
       (35, 15, '财务审批', 'finance_approve', 2, '{"roleId":5}', 3, 1);

INSERT INTO flow_step_transitions (id, step_id, target_step_id, condition_name, priority, is_default, transition_type)
VALUES (13, 33, 34, '部门主管通过转项目经理', 0, 1, 1),
       (14, 34, 35, '项目经理通过转财务', 0, 1, 1);

SELECT d.id, d.flow_name, d.code, d.status, s.id AS step_id, s.step_name, s.approver_type, s.approver_config, s.sort_order
FROM flow_definitions d JOIN flow_steps s ON s.definition_id=d.id
WHERE d.id IN (14,15) ORDER BY d.id, s.sort_order;
