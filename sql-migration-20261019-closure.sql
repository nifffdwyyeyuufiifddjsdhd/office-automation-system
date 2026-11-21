-- 闭环补齐：付款记录落费用类别，统计优先按单据类别而不是关键词猜
ALTER TABLE payment_records
    ADD COLUMN category_id BIGINT NULL COMMENT '费用类别ID（付款时确认）' AFTER applicant_id,
    ADD COLUMN category_name VARCHAR(50) NULL COMMENT '费用类别名称（冗余，统计直接用）' AFTER category_id;

CREATE INDEX idx_payment_category ON payment_records (category_id);
