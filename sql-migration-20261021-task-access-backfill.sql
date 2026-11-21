-- 2026-10-21 给已有任务关系的人补开任务中心访问权（历史任务不能因为新规则就打不开）

INSERT INTO user_permissions (user_id, menu_id, granted_by, source, reason, created_at, updated_at)
SELECT DISTINCT x.user_id, m.id, NULL, 2, '按已有任务补开', NOW(), NOW()
FROM (
    SELECT assignee_id AS user_id FROM tasks WHERE assignee_id IS NOT NULL AND deleted_at IS NULL
    UNION
    SELECT user_id FROM task_members WHERE deleted_at IS NULL
    UNION
    SELECT creator_id AS user_id FROM tasks WHERE creator_id IS NOT NULL AND deleted_at IS NULL
) x
JOIN menus m ON m.menu_code = 'task:access' AND m.deleted_at IS NULL AND m.status = 1
WHERE x.user_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM user_permissions up WHERE up.user_id = x.user_id AND up.menu_id = m.id AND up.deleted_at IS NULL);

SELECT up.user_id, u.username, m.menu_code, up.reason FROM user_permissions up JOIN users u ON u.id=up.user_id JOIN menus m ON m.id=up.menu_id ORDER BY up.user_id;
