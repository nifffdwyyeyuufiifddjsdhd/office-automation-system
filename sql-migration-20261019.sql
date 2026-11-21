-- ============================================================
-- OA 办公系统：后端审查后需要的数据库补丁
-- 生成时间：2026-10-19
-- 执行方式：mysql -u root -p office_automation < sql-migration-20261019.sql
-- 说明：这些是代码修不了的“表结构缺口”，分必做和可选两部分，可重复执行前先看注释
-- 执行记录（2026-10-19 09:47）：本机 oa_system 已执行第一节、第二节、第七节；
--   第三节 flow_approvals.idx_flow_approvals_instance 库里本来就有，跳过；
--   第四节 online_users.user_id 已是 UNIQUE，无需再加普通索引；
--   第五节 user_notification_preferences.uk_user 库里本来就有，跳过。
--   换环境部署时按 1→2→7 顺序执行，其余节先查 information_schema.statistics 再决定。
-- ============================================================

-- ------------------------------------------------------------
-- 一、必做：公告已读记录唯一索引
-- 原因：announcement_reads 没有唯一约束，同一用户重复打开公告会插重复行，
--       导致“已读人数”统计偏大、已读列表重复。
-- 先清理历史重复行（保留 id 最小的一条），再加唯一索引。
-- ------------------------------------------------------------
DELETE a FROM `announcement_reads` a
JOIN `announcement_reads` b
  ON a.`announcement_id` = b.`announcement_id`
 AND a.`user_id` = b.`user_id`
 AND a.`id` > b.`id`;

ALTER TABLE `announcement_reads`
  ADD UNIQUE KEY `uk_announcement_reads_user` (`announcement_id`, `user_id`);

-- ------------------------------------------------------------
-- 二、必做：通知清理任务的支撑索引
-- 原因：NotificationCleanupTask 改成物理删除 “已读且 30 天前” 的通知，
--       没有索引时会全表扫，通知表越大越慢。
-- ------------------------------------------------------------
ALTER TABLE `notification_records`
  ADD INDEX `idx_notification_cleanup` (`is_read`, `created_at`);

-- ------------------------------------------------------------
-- 三、建议：审批记录按实例查询的索引
-- 原因：详情页、导出、统计都按 instance_id 查审批轨迹，目前只有主键索引。
-- ------------------------------------------------------------
ALTER TABLE `flow_approvals`
  ADD INDEX `idx_flow_approvals_instance` (`instance_id`);

-- ------------------------------------------------------------
-- 四、建议：在线用户按部门/状态筛选的索引（可选）
-- ------------------------------------------------------------
ALTER TABLE `online_users`
  ADD INDEX `idx_online_users_user` (`user_id`);

-- ------------------------------------------------------------
-- 五、可选：通知偏好表唯一约束
-- 说明：user_notification_preferences 一人一行（六个开关列），
--       代码层已改成“先查再更新 + 取 id 最大的一条”，但唯一索引才能彻底防并发重复行。
--       若建表脚本里没有这张表，跳过本节。
-- ------------------------------------------------------------
-- DELETE a FROM `user_notification_preferences` a
-- JOIN `user_notification_preferences` b
--   ON a.`user_id` = b.`user_id` AND a.`id` > b.`id`;
-- ALTER TABLE `user_notification_preferences`
--   ADD UNIQUE KEY `uk_user_notification_preferences` (`user_id`);

-- ------------------------------------------------------------
-- 六、不需要动手的（代码已解决）
-- 1) users.username / phone / email 的 UNIQUE + 软删除冲突：
--    代码改为在软删除时把 username 改写为 “原名#del<id>”、phone/email 置 NULL，
--    释放标识符给新账号复用，所以不用改表结构。
-- 2) online_users.user_id 已有 UNIQUE，代码改成撞键后转 update，无需改表。
-- 3) flow_instances.instance_no 已有 UNIQUE，代码改成序号递增 + 撞键重试。
-- ------------------------------------------------------------

-- ------------------------------------------------------------
-- 七、可选：把“在线用户明细”变成可授权的权限点
-- 说明：后端 /api/online/list 现在要求 system:online 权限，admin 角色天然放行，
--       所以“只给管理员看”不用执行这里。要让部门经理等角色也能看，先插这条菜单，
--       再在前端“角色权限”页给对应角色勾上；不要直接在库里插 role_permissions，
--       容易和界面保存的权限集冲突。
-- ------------------------------------------------------------
INSERT IGNORE INTO `menus` (`parent_id`, `menu_name`, `menu_code`, `menu_type`, `path`, `component`, `is_nav`, `sort_order`, `status`)
VALUES (0, '在线用户', 'system:online', 3, '/home/online', 'views/system/online.vue', 0, 90, 1);
