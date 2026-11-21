-- 付款管理模块迁移（20261019）
-- 1) 流程定义增加「需要财务付款」标记，审批通过后自动进入财务待付款池
ALTER TABLE flow_definitions
    ADD COLUMN need_payment TINYINT NOT NULL DEFAULT 0 COMMENT '是否需要财务付款：0否 1是';

-- 现有的报销/采购/资金类流程先标上，其余模板由管理员在流程定义里自己勾
UPDATE flow_definitions SET need_payment = 1 WHERE id IN (5, 6, 10);

-- 2) 付款记录表，一条审批通过的流程最多一笔有效付款
CREATE TABLE IF NOT EXISTS payment_records (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '付款记录ID',
    pay_no VARCHAR(64) NOT NULL COMMENT '付款单号（PAY-20261019-0001）',
    instance_id BIGINT NOT NULL COMMENT '流程实例ID',
    instance_no VARCHAR(64) DEFAULT NULL COMMENT '流程编号（冗余，列表直接展示）',
    definition_id BIGINT DEFAULT NULL COMMENT '流程定义ID',
    flow_name VARCHAR(100) DEFAULT NULL COMMENT '流程名称（冗余）',
    title VARCHAR(255) DEFAULT NULL COMMENT '流程标题',
    applicant_id BIGINT DEFAULT NULL COMMENT '申请人ID',
    amount DECIMAL(12,2) NOT NULL COMMENT '付款金额',
    payee_name VARCHAR(64) NOT NULL COMMENT '收款人',
    payee_account VARCHAR(128) DEFAULT NULL COMMENT '收款账号',
    pay_method TINYINT NOT NULL DEFAULT 1 COMMENT '付款方式：1银行转账 2现金 3微信 4支付宝 5支票',
    pay_time DATETIME NOT NULL COMMENT '付款时间',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1已付款 2已作废',
    operator_id BIGINT NOT NULL COMMENT '付款操作人ID',
    remark VARCHAR(500) DEFAULT NULL COMMENT '付款备注',
    void_reason VARCHAR(255) DEFAULT NULL COMMENT '作废原因',
    void_by BIGINT DEFAULT NULL COMMENT '作废操作人ID',
    void_time DATETIME DEFAULT NULL COMMENT '作废时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME DEFAULT NULL COMMENT '删除时间（软删除）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_pay_no (pay_no),
    UNIQUE KEY uk_payment_instance (instance_id),
    KEY idx_payment_status_time (status, pay_time),
    KEY idx_payment_payee (payee_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='付款记录表';

-- 3) 菜单与权限：财务管理目录 + 付款管理页面 + 两个按钮权限
INSERT INTO menus (id, parent_id, menu_name, menu_code, menu_type, path, component, icon, is_nav, page_url, nav_icon, open_type, sort_order, status, visible)
VALUES (40, 0, '财务管理', 'finance', 2, NULL, NULL, 'Money', 1, NULL, NULL, 1, 7, 1, 1),
       (41, 40, '付款管理', 'finance:pay', 3, '/home/finance/payment', 'finance/payment/index', 'Wallet', 1, NULL, NULL, 1, 1, 1, 1),
       (42, 40, '付款导出', 'finance:export', 3, NULL, NULL, NULL, 0, NULL, NULL, 1, 2, 1, 0),
       (43, 40, '作废付款', 'finance:pay:void', 3, NULL, NULL, NULL, 0, NULL, NULL, 1, 3, 1, 0)
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), menu_code = VALUES(menu_code), parent_id = VALUES(parent_id),
                        menu_type = VALUES(menu_type), path = VALUES(path), component = VALUES(component),
                        icon = VALUES(icon), is_nav = VALUES(is_nav), sort_order = VALUES(sort_order),
                        status = VALUES(status), visible = VALUES(visible), deleted_at = NULL;

-- 财务专员：付款执行 + 导出；管理员：额外可以作废付款
INSERT INTO role_permissions (role_id, menu_id, permission_type, created_at, updated_at)
SELECT 5, 41, 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 5 AND menu_id = 41 AND deleted_at IS NULL);
INSERT INTO role_permissions (role_id, menu_id, permission_type, created_at, updated_at)
SELECT 5, 42, 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 5 AND menu_id = 42 AND deleted_at IS NULL);
INSERT INTO role_permissions (role_id, menu_id, permission_type, created_at, updated_at)
SELECT 1, 41, 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 1 AND menu_id = 41 AND deleted_at IS NULL);
INSERT INTO role_permissions (role_id, menu_id, permission_type, created_at, updated_at)
SELECT 1, 42, 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 1 AND menu_id = 42 AND deleted_at IS NULL);
INSERT INTO role_permissions (role_id, menu_id, permission_type, created_at, updated_at)
SELECT 1, 43, 1, NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = 1 AND menu_id = 43 AND deleted_at IS NULL);
