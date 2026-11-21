-- 人事专员（hr_specialist，roles.id=4）模块：员工档案字段、入职开通账号与离职交接
-- 1) users 补员工档案字段：紧急联系人 / 紧急联系电话 / 银行卡号 / 开户行
SET @ddl = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE users ADD COLUMN emergency_contact VARCHAR(50) NULL AFTER wechat_id, ADD COLUMN emergency_phone VARCHAR(30) NULL AFTER emergency_contact, ADD COLUMN bank_account VARCHAR(50) NULL AFTER emergency_phone, ADD COLUMN bank_name VARCHAR(100) NULL AFTER bank_account',
  'SELECT 1')
  FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'emergency_contact');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) 入职开通账号、离职交接两个按钮权限（挂在「用户管理」菜单下）
INSERT INTO menus (parent_id, menu_name, menu_code, menu_type, is_nav, sort_order, status, visible, created_at, updated_at)
SELECT 10, '入职开通账号', 'user:onboard', 3, 1, 70, 1, 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'user:onboard');

INSERT INTO menus (parent_id, menu_name, menu_code, menu_type, is_nav, sort_order, status, visible, created_at, updated_at)
SELECT 10, '离职交接', 'user:transfer', 3, 1, 72, 1, 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE menu_code = 'user:transfer');

INSERT INTO role_permissions (role_id, menu_id, created_at, updated_at)
SELECT r.id, m.id, NOW(), NOW() FROM roles r JOIN menus m ON m.menu_code IN ('user:onboard', 'user:transfer')
WHERE r.id = 4 AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.menu_id = m.id AND rp.deleted_at IS NULL);

-- 3) 离职交接记录：谁把谁的哪些流程交给了谁、什么时候、什么原因
CREATE TABLE IF NOT EXISTS user_handovers (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL COMMENT '离职人',
  receiver_id BIGINT NOT NULL COMMENT '接收人',
  operator_id BIGINT NOT NULL COMMENT '办理人',
  reason VARCHAR(500) NULL COMMENT '交接原因',
  effective_date DATE NULL COMMENT '离职生效日期',
  todo_count INT NOT NULL DEFAULT 0 COMMENT '转交的待办流程数',
  running_count INT NOT NULL DEFAULT 0 COMMENT '转交的审批中发起流程数',
  done_count INT NOT NULL DEFAULT 0 COMMENT '历史已办流程数（只统计不转交）',
  item_summary TEXT NULL COMMENT '交接内容清单',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  PRIMARY KEY (id),
  KEY idx_handover_user (user_id),
  KEY idx_handover_receiver (receiver_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='离职交接记录';

SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME IN ('emergency_contact','emergency_phone','bank_account','bank_name');
SELECT r.role_code, m.menu_code FROM role_permissions rp JOIN roles r ON r.id = rp.role_id JOIN menus m ON m.id = rp.menu_id WHERE m.menu_code IN ('user:onboard','user:transfer') AND rp.deleted_at IS NULL;
