-- 部门主管数据权限按文档调整为「本部门及下级」，才能做日志查看的层级继承
UPDATE roles SET data_scope = 3 WHERE role_code = 'manager' AND deleted_at IS NULL;
SELECT id, role_code, data_scope FROM roles WHERE deleted_at IS NULL ORDER BY id;
