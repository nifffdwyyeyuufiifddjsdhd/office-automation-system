-- 补齐流程流转线：审批通过后靠 flow_step_transitions 找下一步，缺线流程会直接结束
INSERT INTO flow_step_transitions (step_id, target_step_id, condition_expr, condition_name, priority, is_default, transition_type)
SELECT 1, 21, NULL, '部门主管通过转人事备案', 0, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM flow_step_transitions WHERE step_id = 1 AND target_step_id = 21 AND deleted_at IS NULL);

INSERT INTO flow_step_transitions (step_id, target_step_id, condition_expr, condition_name, priority, is_default, transition_type)
SELECT 22, 5, NULL, '部门主管通过转财务主管', 0, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM flow_step_transitions WHERE step_id = 22 AND target_step_id = 5 AND deleted_at IS NULL);

INSERT INTO flow_step_transitions (step_id, target_step_id, condition_expr, condition_name, priority, is_default, transition_type)
SELECT 15, 16, NULL, '部门主管通过转人事主管', 0, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM flow_step_transitions WHERE step_id = 15 AND target_step_id = 16 AND deleted_at IS NULL);

INSERT INTO flow_step_transitions (step_id, target_step_id, condition_expr, condition_name, priority, is_default, transition_type)
SELECT 17, 18, NULL, '部门主管通过转人事主管', 0, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM flow_step_transitions WHERE step_id = 17 AND target_step_id = 18 AND deleted_at IS NULL);

INSERT INTO flow_step_transitions (step_id, target_step_id, condition_expr, condition_name, priority, is_default, transition_type)
SELECT 18, 19, NULL, '人事主管通过转总经理', 0, 1, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM flow_step_transitions WHERE step_id = 18 AND target_step_id = 19 AND deleted_at IS NULL);

-- 采购申请财务通过后一律转总经理，不再受金额门槛限制（文档口径：采购必须总经理审批）
UPDATE flow_step_transitions
SET condition_expr = NULL, condition_name = '财务通过转总经理', priority = 0, is_default = 1, updated_at = NOW()
WHERE id = 4;

SELECT id, step_id, target_step_id, condition_expr, condition_name, is_default FROM flow_step_transitions ORDER BY step_id, priority;
