-- 公告正文主题：正文样式改由前端主题统一定义，库里只存主题名，不再让正文自带内联样式
ALTER TABLE announcements ADD COLUMN theme VARCHAR(20) NOT NULL DEFAULT 'standard' AFTER cover_image;
