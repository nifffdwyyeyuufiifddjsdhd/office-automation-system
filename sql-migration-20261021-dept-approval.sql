-- 部门主管审批改为按发起人所在部门动态解析（approver_type=3），并补上兜底所需的部门负责人
UPDATE organizations SET leader_id = 7 WHERE id = 1 AND leader_id IS NULL;

UPDATE flow_steps
SET approver_type = 3, approver_config = NULL
WHERE deleted_at IS NULL AND id IN (1, 3, 4, 22, 9, 10, 15, 17);

-- 部门经理（role 2）应能审核本部门申诉
INSERT INTO role_permissions (role_id, menu_id, permission_type)
SELECT 2, 61, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 2 AND menu_id = 61 AND deleted_at IS NULL);
