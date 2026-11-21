-- 离职交接补两列：任务与项目也要跟着转，交接记录里要能看出转了多少
ALTER TABLE user_handovers ADD COLUMN task_count INT NOT NULL DEFAULT 0 AFTER running_count;
ALTER TABLE user_handovers ADD COLUMN project_count INT NOT NULL DEFAULT 0 AFTER task_count;
