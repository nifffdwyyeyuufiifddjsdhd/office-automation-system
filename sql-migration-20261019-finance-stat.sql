-- 财务管理补全：费用类别维护 + 财务统计
-- 2026-10-19 新增，可重复执行

-- 1) 费用类别字典表：财务用关键词从报销单里自动归类，维护页也改这里的词
CREATE TABLE IF NOT EXISTS expense_categories (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    category_name VARCHAR(50) NOT NULL COMMENT '类别名称',
    category_code VARCHAR(50) NOT NULL COMMENT '类别编码',
    keywords VARCHAR(255) DEFAULT NULL COMMENT '识别关键词，逗号分隔，用于从流程标题与表单里归类',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序号',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0停用 1启用',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_expense_category_code (category_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='费用类别字典';

-- 2) 默认六类，与需求文档的费用类别字典一致
INSERT INTO expense_categories (category_name, category_code, keywords, sort_order, status, remark) VALUES
('差旅费', 'TRAVEL',   '差旅,出差,交通,住宿,机票,火车,高铁,车费,行程', 1, 1, '出差相关的交通住宿等支出'),
('办公费', 'OFFICE',   '办公,办公用品,耗材,打印,复印,文具,纸张', 2, 1, '日常办公用品与耗材支出'),
('招待费', 'RECEPTION','招待,接待,宴请,餐饮,聚餐,客户用餐', 3, 1, '客户接待与餐饮支出'),
('培训费', 'TRAINING', '培训,课程,学习,讲师,进修,考试', 4, 1, '培训课程与讲师费用'),
('采购费', 'PURCHASE', '采购,购置,设备,物资,办公设备,器材', 5, 1, '物资设备采购支出'),
('其他',   'OTHER',    '', 6, 1, '无法归类时默认落在这里')
ON DUPLICATE KEY UPDATE category_name = VALUES(category_name), keywords = VALUES(keywords),
    sort_order = VALUES(sort_order), remark = VALUES(remark), deleted_at = NULL;

-- 3) 菜单与按钮权限：财务统计、费用类别维护
INSERT INTO menus (id, parent_id, menu_name, menu_code, menu_type, path, component, icon, is_nav, sort_order, status, visible, created_at, updated_at)
VALUES
(44, 40, '财务统计', 'finance:stat', 3, '/home/finance/stat', 'finance/stat/index', 'DataAnalysis', 1, 2, 1, 1, NOW(), NOW()),
(45, 40, '费用类别', 'finance:category:list', 3, '/home/finance/category', 'finance/category/index', 'Files', 1, 3, 1, 1, NOW(), NOW()),
(46, 40, '新增类别', 'finance:category:add', 3, NULL, NULL, NULL, 0, 4, 1, 0, NOW(), NOW()),
(47, 40, '编辑类别', 'finance:category:edit', 3, NULL, NULL, NULL, 0, 5, 1, 0, NOW(), NOW()),
(48, 40, '删除类别', 'finance:category:delete', 3, NULL, NULL, NULL, 0, 6, 1, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), path = VALUES(path), component = VALUES(component),
    icon = VALUES(icon), is_nav = VALUES(is_nav), sort_order = VALUES(sort_order), deleted_at = NULL;

-- 4) 授权：财务专员与系统管理员全部给；总经理给财务统计 + 导出（只读看数）
INSERT INTO role_permissions (role_id, menu_id, permission_type, created_at, updated_at)
SELECT r.role_id, m.menu_id, 1, NOW(), NOW() FROM
    (SELECT 1 AS role_id UNION ALL SELECT 5) r
    JOIN (SELECT 44 AS menu_id UNION ALL SELECT 45 UNION ALL SELECT 46 UNION ALL SELECT 47 UNION ALL SELECT 48) m
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.role_id AND rp.menu_id = m.menu_id AND rp.deleted_at IS NULL
);

INSERT INTO role_permissions (role_id, menu_id, permission_type, created_at, updated_at)
SELECT 6, m.menu_id, 1, NOW(), NOW() FROM
    (SELECT 44 AS menu_id UNION ALL SELECT 42) m
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions rp WHERE rp.role_id = 6 AND rp.menu_id = m.menu_id AND rp.deleted_at IS NULL
);

-- 5) 财务统计按 status + pay_time 查，该联合索引在 sql-migration-20261019-finance.sql 里已建好，这里不再重复添加
