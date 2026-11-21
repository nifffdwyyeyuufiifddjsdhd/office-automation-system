-- 普通员工个人中心：企业微信 / 钉钉账号绑定
CREATE TABLE IF NOT EXISTS user_third_party_bindings (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id BIGINT NOT NULL COMMENT '本系统用户',
  platform VARCHAR(20) NOT NULL COMMENT 'wecom=企业微信 dingtalk=钉钉',
  external_id VARCHAR(64) NOT NULL COMMENT '第三方账号标识（企业微信 UserID / 钉钉 userId）',
  external_name VARCHAR(50) DEFAULT NULL COMMENT '第三方显示名，仅用于本人核对',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1已绑定 0已解绑',
  bind_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_binding_user (user_id, platform),
  KEY idx_binding_external (platform, external_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='第三方账号绑定（企业微信/钉钉）';
