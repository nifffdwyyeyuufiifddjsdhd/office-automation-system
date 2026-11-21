-- 审核人模块补全：委托代理、离职/停用三种处理方式、流程定义版本管理

-- 1) 委托代理：委托人 A 在时间段内把待办委托给代理人 B，B 能看到并审批 A 的待办
CREATE TABLE IF NOT EXISTS flow_delegates (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    delegator_id BIGINT NOT NULL COMMENT '委托人（原审核人）',
    agent_id BIGINT NOT NULL COMMENT '代理人',
    start_time DATETIME NOT NULL COMMENT '生效时间',
    end_time DATETIME NOT NULL COMMENT '失效时间',
    reason VARCHAR(200) NULL COMMENT '委托原因',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1生效 0已取消',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME NULL,
    KEY idx_flow_delegates_agent (agent_id, status, end_time),
    KEY idx_flow_delegates_delegator (delegator_id, status)
) COMMENT '流程审批委托代理';

-- 2) 离职/停用处理方式：保留模式下没有接收人，receiver_id 要允许为空
ALTER TABLE user_handovers MODIFY COLUMN receiver_id BIGINT NULL COMMENT '接收人，保留模式下为空';
ALTER TABLE user_handovers ADD COLUMN handle_mode VARCHAR(20) NOT NULL DEFAULT 'transfer' COMMENT 'transfer 批量转交 / keep 保留待办 / auto 自动转直属上级';

-- 3) 流程定义版本管理：同一个 code 允许多个版本，已发起单据继续绑定原版本
ALTER TABLE flow_definitions DROP INDEX code;
ALTER TABLE flow_definitions ADD INDEX idx_flow_definitions_code (code, version);
ALTER TABLE flow_definitions ADD COLUMN superseded_by BIGINT NULL COMMENT '被哪个新版本替代';
