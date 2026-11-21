-- 人事/行政主管（hr_manager）模块：角色、权限、招聘管理、登录日志、入职离职流程
-- 1) 角色
INSERT INTO roles (role_name, role_code, role_type, description, sort_order, status, is_system, data_scope, created_at, updated_at)
VALUES ('人事/行政主管','hr_manager',1,'人事模块审批权，全局出勤/人事数据查看，负责补签终审与申诉审核',7,1,1,3,NOW(),NOW())
ON DUPLICATE KEY UPDATE role_name=VALUES(role_name), description=VALUES(description), sort_order=7, status=1, data_scope=3;

-- 2) 菜单与权限码
INSERT INTO menus (id,parent_id,menu_name,menu_code,menu_type,path,icon,is_nav,sort_order,status,visible) VALUES
(50,0,'招聘管理','recruitment',2,'/home/recruitment','Suitcase',1,8,1,1),
(51,50,'招聘岗位','recruitment:position:list',3,'/home/recruitment/position',NULL,1,1,1,1),
(52,50,'发布岗位','recruitment:position:add',3,NULL,NULL,0,2,1,1),
(53,50,'编辑岗位','recruitment:position:edit',3,NULL,NULL,0,3,1,1),
(54,50,'删除岗位','recruitment:position:delete',3,NULL,NULL,0,4,1,1),
(55,50,'候选人管理','recruitment:candidate:list',3,'/home/recruitment/candidate',NULL,1,5,1,1),
(56,50,'新增候选人','recruitment:candidate:add',3,NULL,NULL,0,6,1,1),
(57,50,'编辑候选人','recruitment:candidate:edit',3,NULL,NULL,0,7,1,1),
(58,50,'候选人状态流转','recruitment:candidate:status',3,NULL,NULL,0,8,1,1),
(59,50,'删除候选人','recruitment:candidate:delete',3,NULL,NULL,0,9,1,1),
(60,0,'登录日志统计','login:stat',3,'/home/system/login-log',NULL,1,9,1,1)
ON DUPLICATE KEY UPDATE menu_name=VALUES(menu_name), parent_id=VALUES(parent_id), menu_type=VALUES(menu_type), path=VALUES(path), is_nav=VALUES(is_nav), sort_order=VALUES(sort_order), status=1;

-- 3) 授权：hr_manager 拿到人事全套（用户/组织/职位/职称/出勤/报表/在线/流程日常/招聘/登录日志）
INSERT INTO role_permissions (role_id,menu_id,permission_type)
SELECT 7, m.id, 1 FROM menus m
WHERE m.menu_code IN ('user:list','user:add','user:edit','user:reset','user:status','user:export','org:manage','position:manage','title:manage','attendance:view-all','attendance:manage','report:view','system:online','announcement:list','flow:start','flow:todo','flow:done','flow:mine','recruitment:position:list','recruitment:position:add','recruitment:position:edit','recruitment:position:delete','recruitment:candidate:list','recruitment:candidate:add','recruitment:candidate:edit','recruitment:candidate:status','recruitment:candidate:delete','login:stat')
AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id=7 AND rp.menu_id=m.id AND rp.deleted_at IS NULL);

-- 人事专员补上：新增/编辑用户、停用账号、组织架构、招聘岗位发布
INSERT INTO role_permissions (role_id,menu_id,permission_type)
SELECT 4, m.id, 1 FROM menus m
WHERE m.menu_code IN ('user:add','user:edit','user:status','org:manage','recruitment:position:list','recruitment:position:add')
AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id=4 AND rp.menu_id=m.id AND rp.deleted_at IS NULL);

-- 4) 测试账号：hrmanager01 / Admin@123（密码沿用 hr01）
INSERT INTO users (username,password,real_name,gender,phone,email,org_id,status,must_change_password,created_at,updated_at)
SELECT 'hrmanager01', password, '陈主管', 2, '13900000007', 'hrmanager01@example.com', org_id, 1, 0, NOW(), NOW()
FROM users WHERE username='hr01' AND NOT EXISTS (SELECT 1 FROM users x WHERE x.username='hrmanager01');
INSERT INTO user_roles (user_id, role_id, grant_type)
SELECT u.id, 7, 1 FROM users u
WHERE u.username='hrmanager01' AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id=u.id AND ur.role_id=7 AND ur.deleted_at IS NULL);

-- 5) 招聘岗位表
CREATE TABLE IF NOT EXISTS recruitment_positions (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '岗位ID',
  position_name VARCHAR(100) NOT NULL COMMENT '招聘岗位名称',
  dept_id BIGINT NULL COMMENT '用人部门',
  position_id BIGINT NULL COMMENT '关联职位表，用于定级',
  headcount INT NOT NULL DEFAULT 1 COMMENT '招聘人数',
  education VARCHAR(50) NULL COMMENT '学历要求',
  experience_years INT NULL COMMENT '经验要求（年）',
  salary_range VARCHAR(50) NULL COMMENT '薪资范围',
  work_place VARCHAR(100) NULL COMMENT '工作地点',
  duty TEXT NULL COMMENT '岗位职责',
  requirement TEXT NULL COMMENT '任职要求',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '0已停招 1在招 2已结束',
  publish_time DATETIME NULL COMMENT '发布时间',
  deadline DATE NULL COMMENT '截止日期',
  creator_id BIGINT NOT NULL COMMENT '创建人',
  remark VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  PRIMARY KEY (id),
  KEY idx_recruit_position_status (status),
  KEY idx_recruit_position_dept (dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='招聘岗位';

-- 6) 候选人表
CREATE TABLE IF NOT EXISTS recruitment_candidates (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '候选人ID',
  position_id BIGINT NOT NULL COMMENT '应聘岗位',
  candidate_name VARCHAR(50) NOT NULL COMMENT '姓名',
  gender TINYINT NOT NULL DEFAULT 0 COMMENT '0未知 1男 2女',
  phone VARCHAR(20) NULL COMMENT '手机号',
  email VARCHAR(100) NULL COMMENT '邮箱',
  education VARCHAR(50) NULL COMMENT '学历',
  experience_years INT NULL COMMENT '工作年限',
  source VARCHAR(50) NULL COMMENT '来源：简历投递/内推/校招/猎头',
  resume_path VARCHAR(255) NULL COMMENT '简历附件路径',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1待筛选 2待面试 3面试通过 4已入职 5已淘汰',
  interview_time DATETIME NULL COMMENT '面试时间',
  interviewer VARCHAR(50) NULL COMMENT '面试官',
  evaluation TEXT NULL COMMENT '面试评价',
  expected_salary DECIMAL(10,2) NULL COMMENT '期望薪资',
  creator_id BIGINT NOT NULL COMMENT '录入人',
  remark VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at DATETIME NULL,
  PRIMARY KEY (id),
  KEY idx_candidate_position (position_id),
  KEY idx_candidate_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='招聘候选人';

-- 7) 入职/离职流程模板与步骤
INSERT INTO flow_definitions (flow_name,code,flow_type,description,form_config,version,status,audit_status,creator_id,publish_time,sort_order,need_payment)
VALUES ('入职申请','onboard',1,'新员工入职审批：部门主管确认用人需求，人事主管审核入职材料与定岗定薪',
'[{"field":"employee_name","label":"姓名","type":"text","required":true,"isPrimary":true},{"field":"id_card","label":"身份证号","type":"text","required":true},{"field":"org_name","label":"入职部门","type":"text","required":true},{"field":"position_name","label":"入职岗位","type":"text","required":true},{"field":"entry_date","label":"入职日期","type":"date","required":true},{"field":"salary","label":"定薪（元）","type":"number","required":true},{"field":"reason","label":"备注","type":"textarea","required":false}]',
1,1,1,1,NOW(),1,0)
ON DUPLICATE KEY UPDATE flow_name=VALUES(flow_name), form_config=VALUES(form_config), status=1;

INSERT INTO flow_definitions (flow_name,code,flow_type,description,form_config,version,status,audit_status,creator_id,publish_time,sort_order,need_payment)
VALUES ('离职申请','resign',1,'员工离职审批：部门主管确认交接安排，人事主管审核离职手续，总经理终审',
'[{"field":"employee_name","label":"离职员工","type":"text","required":true,"isPrimary":true},{"field":"org_name","label":"所属部门","type":"text","required":true},{"field":"last_date","label":"最后工作日","type":"date","required":true},{"field":"reason","label":"离职原因","type":"textarea","required":true},{"field":"handover","label":"交接安排","type":"textarea","required":false}]',
1,1,1,1,NOW(),2,0)
ON DUPLICATE KEY UPDATE flow_name=VALUES(flow_name), form_config=VALUES(form_config), status=1;

SET @defOnboard := (SELECT id FROM flow_definitions WHERE code='onboard' LIMIT 1);
INSERT INTO flow_steps (definition_id,step_name,step_code,step_type,approver_type,approver_config,sort_order)
SELECT @defOnboard,'部门主管审批','step1',1,2,'{"roleId":2}',1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM flow_steps WHERE definition_id=@defOnboard AND step_code='step1');
INSERT INTO flow_steps (definition_id,step_name,step_code,step_type,approver_type,approver_config,sort_order)
SELECT @defOnboard,'人事主管审批','step2',1,2,'{"roleId":7}',2 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM flow_steps WHERE definition_id=@defOnboard AND step_code='step2');

SET @defResign := (SELECT id FROM flow_definitions WHERE code='resign' LIMIT 1);
INSERT INTO flow_steps (definition_id,step_name,step_code,step_type,approver_type,approver_config,sort_order)
SELECT @defResign,'部门主管审批','step1',1,2,'{"roleId":2}',1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM flow_steps WHERE definition_id=@defResign AND step_code='step1');
INSERT INTO flow_steps (definition_id,step_name,step_code,step_type,approver_type,approver_config,sort_order)
SELECT @defResign,'人事主管审批','step2',1,2,'{"roleId":7}',2 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM flow_steps WHERE definition_id=@defResign AND step_code='step2');
INSERT INTO flow_steps (definition_id,step_name,step_code,step_type,approver_type,approver_config,sort_order)
SELECT @defResign,'总经理审批','step3',1,2,'{"roleId":6}',3 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM flow_steps WHERE definition_id=@defResign AND step_code='step3');
