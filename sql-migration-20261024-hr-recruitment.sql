-- 人事专员要能录入候选人、推进面试，否则招聘只能发岗位没人可用
INSERT INTO role_permissions (role_id, menu_id, created_at, updated_at)
SELECT 4, m.id, NOW(), NOW() FROM menus m
WHERE m.menu_code IN ('recruitment:candidate:list','recruitment:candidate:add','recruitment:candidate:edit','recruitment:candidate:status')
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id=4 AND rp.menu_id=m.id);
