INSERT INTO menus (parent_id, menu_name, menu_code, menu_type, is_nav, sort_order, status, visible, created_at, updated_at)
SELECT 0, '出勤审核（申诉/补签终审）', 'attendance:audit', 3, 0, 903, 1, 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'attendance:audit');

INSERT INTO role_permissions (role_id, menu_id, created_at, updated_at)
SELECT r.id, m.id, NOW(), NOW() FROM roles r JOIN menus m ON m.menu_code = 'attendance:audit'
WHERE r.id IN (4, 7) AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.menu_id = m.id AND rp.deleted_at IS NULL);

SELECT r.role_code, m.menu_code FROM role_permissions rp JOIN roles r ON r.id = rp.role_id JOIN menus m ON m.id = rp.menu_id WHERE m.menu_code = 'attendance:audit' AND rp.deleted_at IS NULL;
