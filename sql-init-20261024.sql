-- MySQL dump 10.13  Distrib 8.0.32, for Win64 (x86_64)
--
-- Host: localhost    Database: oa_system
-- ------------------------------------------------------
-- Server version	8.0.32

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `announcement_categories`
--

DROP TABLE IF EXISTS `announcement_categories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `announcement_categories` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '类别ID',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父类别ID',
  `category_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '类别名称',
  `category_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '类别编码',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0停用 1启用',
  `view_permission_type` tinyint NOT NULL DEFAULT '1' COMMENT '查看权限：1全部可见 2指定角色可见',
  `icon` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '图标',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间（软删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `category_code` (`category_code`),
  KEY `idx_announcement_categories_parent` (`parent_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公告类别表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `announcement_categories`
--

LOCK TABLES `announcement_categories` WRITE;
/*!40000 ALTER TABLE `announcement_categories` DISABLE KEYS */;
INSERT INTO `announcement_categories` VALUES (1,0,'系统公告','SYS',1,1,1,NULL,'2026-09-26 18:06:24','2026-09-26 18:06:24',NULL);
/*!40000 ALTER TABLE `announcement_categories` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `announcement_category_roles`
--

DROP TABLE IF EXISTS `announcement_category_roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `announcement_category_roles` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `category_id` bigint NOT NULL COMMENT '公告类别ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间（软删除）',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公告类别-角色权限关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `announcement_category_roles`
--

LOCK TABLES `announcement_category_roles` WRITE;
/*!40000 ALTER TABLE `announcement_category_roles` DISABLE KEYS */;
/*!40000 ALTER TABLE `announcement_category_roles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `announcement_reads`
--

DROP TABLE IF EXISTS `announcement_reads`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `announcement_reads` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `announcement_id` bigint NOT NULL COMMENT '公告ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `read_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '阅读时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间（软删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_announcement_reads_user` (`announcement_id`,`user_id`),
  KEY `idx_announcement_reads_announcement` (`announcement_id`),
  KEY `idx_announcement_reads_user` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公告已读记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `announcement_reads`
--

LOCK TABLES `announcement_reads` WRITE;
/*!40000 ALTER TABLE `announcement_reads` DISABLE KEYS */;
INSERT INTO `announcement_reads` VALUES (1,3,1,'2026-10-04 15:26:30','2026-10-04 15:26:30','2026-10-04 15:26:30',NULL),(2,2,3,'2026-10-05 16:58:22','2026-10-05 16:58:22','2026-10-05 16:58:22',NULL),(3,2,1,'2026-10-19 09:52:24','2026-10-19 09:52:24','2026-10-19 09:52:24',NULL);
/*!40000 ALTER TABLE `announcement_reads` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `announcements`
--

DROP TABLE IF EXISTS `announcements`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `announcements` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '公告ID',
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '公告标题',
  `category_id` bigint NOT NULL COMMENT '所属类别ID',
  `content` longtext COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '公告内容（富文本）',
  `summary` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '内容摘要',
  `cover_image` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '封面图片URL',
  `author_id` bigint NOT NULL COMMENT '发布人ID',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0草稿 1已发布 2已下线',
  `is_top` tinyint NOT NULL DEFAULT '0' COMMENT '是否置顶：0否 1是',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `offline_time` datetime DEFAULT NULL COMMENT '下线时间',
  `view_count` int NOT NULL DEFAULT '0' COMMENT '查看次数',
  `like_count` int NOT NULL DEFAULT '0' COMMENT '点赞数',
  `allow_comment` tinyint NOT NULL DEFAULT '1' COMMENT '是否允许评论：0否 1是',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间（软删除）',
  PRIMARY KEY (`id`),
  KEY `idx_announcements_category_status_top` (`category_id`,`status`,`is_top`),
  KEY `idx_announcements_publish_time` (`publish_time`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公告表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `announcements`
--

LOCK TABLES `announcements` WRITE;
/*!40000 ALTER TABLE `announcements` DISABLE KEYS */;
INSERT INTO `announcements` VALUES (2,'1',1,'<pre><code>&lt;h2 style=\"color:#409EFF;\"&gt;OA 办公系统 v2.0 升级公告&lt;/h2&gt;\r\n\r\n&lt;p&gt;各位同事：&lt;/p&gt;\r\n\r\n&lt;p&gt;为提升办公效率，系统已完成&lt;strong&gt;用户管理&lt;/strong&gt;与&lt;strong&gt;公告通知&lt;/strong&gt;模块升级，具体内容如下：&lt;/p&gt;\r\n\r\n&lt;h3&gt;一、新增功能&lt;/h3&gt;\r\n&lt;ul&gt;\r\n  &lt;li&gt;用户管理：支持&lt;strong&gt;批量导入&lt;/strong&gt;（Excel）、部门树筛选、账号锁定解锁&lt;/li&gt;\r\n  &lt;li&gt;公告通知：正文支持富文本编辑，可插入图片、链接、表格&lt;/li&gt;\r\n  &lt;li&gt;安全增强：公告内容入库前自动&lt;strong&gt;HTML 净化&lt;/strong&gt;，防 XSS 注入&lt;/li&gt;\r\n&lt;/ul&gt;\r\n\r\n&lt;h3&gt;二、操作说明&lt;/h3&gt;\r\n&lt;ol&gt;\r\n  &lt;li&gt;进入「用户管理」→ 点击「批量导入」→ 选择 Excel 模板&lt;/li&gt;\r\n  &lt;li&gt;进入「公告通知」→ 新增公告 → 选择分类「系统公告」&lt;/li&gt;\r\n  &lt;li&gt;正文编辑完成后点击保存，草稿可在列表中继续编辑或发布&lt;/li&gt;\r\n&lt;/ol&gt;\r\n\r\n&lt;blockquote style=\"border-left:4px solid #409EFF; padding:8px 16px; background:#f0f7ff;\"&gt;\r\n  提示：首次登录或密码被重置后，系统会强制要求修改密码。\r\n&lt;/blockquote&gt;\r\n\r\n&lt;p&gt;详细说明请查看 &lt;a href=\"https://www.example.com\" target=\"_blank\"&gt;操作手册&lt;/a&gt;，如有问题请联系管理员。&lt;/p&gt;\r\n\r\n&lt;p&gt;&lt;img src=\"https://via.placeholder.com/800x300/409EFF/FFFFFF?text=OA+System\" alt=\"OA系统示意图\" /&gt;&lt;/p&gt;\r\n</code></pre>\n<p><br></p>','SSSS',NULL,1,1,0,'2026-10-05 16:58:13',NULL,3,0,1,'2026-09-26 18:15:47','2026-10-23 19:11:06',NULL),(3,'走查测试公告',1,'<p>这是一条走查测试公告，验证公告发布闭环。</p>','',NULL,1,1,0,NULL,NULL,1,0,1,'2026-10-04 15:25:50','2026-10-04 15:27:51','2026-10-04 15:27:51');
/*!40000 ALTER TABLE `announcements` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `attachments`
--

DROP TABLE IF EXISTS `attachments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `attachments` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '附件ID',
  `file_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '原始文件名',
  `file_path` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文件存储路径',
  `file_size` bigint NOT NULL COMMENT '文件大小（字节）',
  `file_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '文件MIME类型',
  `file_ext` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '文件扩展名',
  `target_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `target_id` bigint DEFAULT NULL,
  `uploader_id` bigint NOT NULL COMMENT '上传人ID',
  `storage_type` tinyint NOT NULL DEFAULT '1' COMMENT '存储方式：1本地存储 2云存储OSS 3MinIO',
  `md5` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '文件MD5（用于秒传和去重）',
  `download_count` int NOT NULL DEFAULT '0' COMMENT '下载次数',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间（软删除）',
  PRIMARY KEY (`id`),
  KEY `idx_attachments_target` (`target_type`,`target_id`)
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通用附件表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `attachments`
--

LOCK TABLES `attachments` WRITE;
/*!40000 ALTER TABLE `attachments` DISABLE KEYS */;
INSERT INTO `attachments` VALUES (1,'image.png','D:\\VSC\\Employment Project\\office-automation-system\\uploads\\flow\\2026\\09\\29\\a1d665245735483d9da1abff61ee2615.png',103403,'image/png','.png',NULL,NULL,4,1,NULL,0,'2026-09-29 17:09:04','2026-09-29 17:09:04',NULL),(2,'独属于我的蒙娜丽莎.txt','D:\\VSC\\Employment Project\\office-automation-system\\uploads\\flow\\2026\\09\\29\\85cb31a505fb44b0820f1e9510f1f904.txt',2681,'text/plain','.txt',NULL,NULL,4,1,NULL,0,'2026-09-29 17:09:24','2026-09-29 17:09:24',NULL),(3,'image.png','D:\\VSC\\Employment Project\\office-automation-system\\uploads\\flow\\2026\\09\\29\\4b8ca90b1ec340bcb88b6cc4cb3b31b0.png',103403,'image/png','.png',NULL,NULL,4,1,NULL,0,'2026-09-29 17:20:01','2026-09-29 17:20:01',NULL),(4,'image.png','D:\\VSC\\Employment Project\\office-automation-system\\uploads\\flow\\2026\\09\\29\\d94aeba63a3140709a1d4848c705d0b7.png',103403,'image/png','.png','flow_instance',4,1,1,NULL,0,'2026-09-29 17:54:38','2026-09-29 17:54:39',NULL),(5,'image.png','D:\\VSC\\Employment Project\\office-automation-system\\uploads\\flow\\2026\\09\\29\\b672d745897049778c92f3244aadc8cf.png',104822,'image/png','.png','flow_instance',7,4,1,NULL,0,'2026-09-29 18:57:48','2026-09-30 16:55:11',NULL),(9,'【哲风壁纸】个性涂鸦-二次元角色.png','D:\\VSC\\Employment Project\\office-automation-system\\uploads\\flow\\2026\\10\\05\\e2a68c52e0794b709e24d10d8c868b71.png',3038307,'image/png','.png','attendance_appeal',10,3,1,NULL,0,'2026-10-05 16:32:33','2026-10-05 16:32:43',NULL),(10,'测试附件.txt','D:\\VSC\\Employment Project\\office-automation-system\\uploads\\worklog\\2026\\10\\05\\a4482eb761974777aa8d83963377192b.txt',41,'text/plain','.txt','worklog',4,1,1,NULL,1,'2026-10-05 17:21:45','2026-10-05 17:21:45',NULL),(11,'OA办公系统-用户管理功能代码.docx','D:\\VSC\\Employment Project\\office-automation-system\\uploads\\worklog\\2026\\10\\09\\1d1b5a412efd4e7e8c32ba39da10e906.docx',72699,'application/vnd.openxmlformats-officedocument.wordprocessingml.document','.docx','worklog',7,1,1,NULL,1,'2026-10-09 10:27:04','2026-10-09 10:43:42',NULL),(12,'OA办公系统-角色功能与权限开发清单.docx','D:\\VSC\\Employment Project\\office-automation-system\\uploads\\worklog\\2026\\10\\09\\ff068bb4f533493b8e5d6bec50dc212e.docx',49072,'application/vnd.openxmlformats-officedocument.wordprocessingml.document','.docx','worklog',8,4,1,NULL,0,'2026-10-09 15:04:52','2026-10-09 15:04:54',NULL),(21,'黄恒富-前端开发简历（终版）.docx','D:\\VSC\\Employment Project\\office-automation-system\\uploads\\flow\\2026\\10\\19\\164bc09905e64f5097c75d955f9b2d1d.docx',44593,'application/vnd.openxmlformats-officedocument.wordprocessingml.document','.docx','flow_instance',17,9,1,NULL,0,'2026-10-19 11:23:52','2026-10-19 11:23:54',NULL);
/*!40000 ALTER TABLE `attachments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `attendance_appeals`
--

DROP TABLE IF EXISTS `attendance_appeals`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `attendance_appeals` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `record_id` bigint NOT NULL COMMENT '出勤记录ID',
  `user_id` bigint NOT NULL,
  `reason` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` int DEFAULT '0' COMMENT '0待处理 1通过 2驳回',
  `approver_id` bigint DEFAULT NULL,
  `approve_time` datetime DEFAULT NULL,
  `approve_remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `attendance_appeals`
--

LOCK TABLES `attendance_appeals` WRITE;
/*!40000 ALTER TABLE `attendance_appeals` DISABLE KEYS */;
INSERT INTO `attendance_appeals` VALUES (1,3,3,'SSDDSADFSFSDFDSFSDFDSFD',1,1,'2026-10-03 16:24:01','SADASDASDSAD','2026-10-03 16:23:43','2026-10-03 16:24:01',NULL),(8,40,4,'今天临时外出办事忘记打卡，实际全天在岗，申请补全出勤记录',0,NULL,NULL,NULL,'2026-10-05 16:23:44','2026-10-05 16:23:44',NULL),(9,11,4,'昨天早高峰路上严重堵车导致迟到，申请撤销迟到标记',0,NULL,NULL,NULL,'2026-10-05 16:23:44','2026-10-05 16:23:44',NULL),(10,42,3,'不可抗力',1,1,'2026-10-05 16:33:35','dfdf','2026-10-05 16:32:43','2026-10-05 16:33:35',NULL);
/*!40000 ALTER TABLE `attendance_appeals` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `attendance_makeup`
--

DROP TABLE IF EXISTS `attendance_makeup`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `attendance_makeup` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '申请人',
  `attendance_date` date NOT NULL COMMENT '补签日期',
  `type` tinyint NOT NULL DEFAULT '1' COMMENT '1补签到 2补签退 3两者都补',
  `check_in_time` datetime DEFAULT NULL COMMENT '申请的签到时间',
  `check_out_time` datetime DEFAULT NULL COMMENT '申请的签退时间',
  `reason` varchar(500) NOT NULL COMMENT '补签原因',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0待审批 1已通过 2已驳回',
  `current_stage` tinyint NOT NULL DEFAULT '1' COMMENT '当前审批阶段 1部门主管初审 2人事终审',
  `approver_id` bigint DEFAULT NULL COMMENT '审批人',
  `first_approver_id` bigint DEFAULT NULL COMMENT '部门主管初审人',
  `first_approve_time` datetime DEFAULT NULL COMMENT '初审时间',
  `first_approve_remark` varchar(500) DEFAULT NULL COMMENT '初审意见',
  `approve_time` datetime DEFAULT NULL COMMENT '审批时间',
  `approve_remark` varchar(500) DEFAULT NULL COMMENT '审批意见',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user_date` (`user_id`,`attendance_date`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='补签申请';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `attendance_makeup`
--

LOCK TABLES `attendance_makeup` WRITE;
/*!40000 ALTER TABLE `attendance_makeup` DISABLE KEYS */;
INSERT INTO `attendance_makeup` VALUES (5,3,'2026-09-30',3,'2026-10-03 00:00:00','2026-10-03 00:00:00','不可抗力',2,1,1,NULL,NULL,NULL,'2026-10-03 15:29:33','不可抗力','2026-10-03 15:26:56','2026-10-03 15:29:33',NULL),(6,3,'2026-10-03',2,NULL,'2026-10-02 00:00:00','不可抗力',1,1,1,NULL,NULL,NULL,'2026-10-03 15:29:23','不可控力','2026-10-03 15:27:50','2026-10-03 15:29:23',NULL),(7,3,'2026-10-03',3,'2026-10-03 00:00:00','2026-10-03 00:00:00','不可抗力',2,1,1,NULL,NULL,NULL,'2026-10-03 15:28:59','不可抗力','2026-10-03 15:28:14','2026-10-03 15:28:59',NULL),(8,3,'2026-10-03',3,'2026-10-03 00:00:00','2026-10-03 00:00:00','不可抗力',0,1,1,NULL,NULL,NULL,NULL,NULL,'2026-10-03 15:33:28','2026-10-03 16:12:18','2026-10-03 16:12:18'),(9,3,'2026-10-03',3,'2026-10-03 00:00:00','2026-10-03 00:00:00','不可抗力',0,1,1,NULL,NULL,NULL,NULL,NULL,'2026-10-03 15:45:12','2026-10-03 16:12:21','2026-10-03 16:12:21'),(11,3,'2026-10-03',3,'2026-10-03 00:00:00','2026-10-03 00:00:00','不可抗力',0,1,1,NULL,NULL,NULL,NULL,NULL,'2026-10-03 16:12:24','2026-10-03 16:12:36','2026-10-03 16:12:36'),(12,3,'2026-10-03',3,'2026-10-03 00:00:00','2026-10-03 00:00:00','不可抗力',0,1,1,NULL,NULL,NULL,NULL,NULL,'2026-10-03 16:12:28','2026-10-03 16:12:39','2026-10-03 16:12:39'),(17,4,'2026-10-14',3,'2026-10-14 09:00:00','2026-10-14 18:00:00','14日外出忘记打卡，补签当天上下班',1,2,5,3,'2026-10-16 10:50:29','情况属实，同意补签，送人事终审','2026-10-16 10:53:49','同意，补签生效','2026-10-16 10:46:06','2026-10-16 10:53:49',NULL);
/*!40000 ALTER TABLE `attendance_makeup` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `attendance_outing`
--

DROP TABLE IF EXISTS `attendance_outing`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `attendance_outing` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '外出记录ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
  `outing_date` date NOT NULL COMMENT '外出日期',
  `start_time` datetime NOT NULL COMMENT '外出开始时间',
  `reason` varchar(300) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '外出事由',
  `expected_return` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '预计返回时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间（软删除）',
  PRIMARY KEY (`id`),
  KEY `idx_outing_user_date` (`user_id`,`outing_date`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='外出打卡记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `attendance_outing`
--

LOCK TABLES `attendance_outing` WRITE;
/*!40000 ALTER TABLE `attendance_outing` DISABLE KEYS */;
INSERT INTO `attendance_outing` VALUES (2,1,1,'2026-10-08','2026-10-08 11:37:54','拜访客户','3点','2026-10-08 11:37:54',NULL);
/*!40000 ALTER TABLE `attendance_outing` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `attendance_records`
--

DROP TABLE IF EXISTS `attendance_records`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `attendance_records` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '出勤记录ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `dept_id` bigint NOT NULL COMMENT '部门ID（冗余字段，便于部门统计）',
  `attendance_date` date NOT NULL COMMENT '出勤日期',
  `check_in_time` datetime DEFAULT NULL COMMENT '上班签到时间',
  `check_out_time` datetime DEFAULT NULL COMMENT '下班签退时间',
  `check_in_ip` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '上班签到IP',
  `check_out_ip` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '下班签退IP',
  `check_in_mac` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '上班签到设备MAC地址',
  `check_out_mac` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '下班签退设备MAC地址',
  `check_in_location` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '上班签到地理位置',
  `check_out_location` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '下班签退地理位置',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '出勤状态：0正常 1迟到 2早退 3缺勤 4外出 5出差 6请假',
  `work_hours` decimal(4,2) DEFAULT NULL COMMENT '工作时长（小时）',
  `overtime_hours` decimal(5,2) DEFAULT NULL,
  `is_makeup` tinyint NOT NULL DEFAULT '0' COMMENT '是否补签：0正常 1补签',
  `makeup_apply_id` bigint DEFAULT NULL COMMENT '补签申请单ID（关联流程实例）',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间（软删除）',
  `check_in_count` int DEFAULT '0',
  `check_out_count` int DEFAULT '0',
  `latitude` decimal(10,6) DEFAULT NULL,
  `longitude` decimal(10,6) DEFAULT NULL,
  `check_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'normal',
  `shift_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'day',
  `geo_abnormal` tinyint NOT NULL DEFAULT '0' COMMENT '0正常 1异地/无法定位强制待审核 2主管确认 3驳回改缺勤',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_attendance_user_date` (`user_id`,`attendance_date`),
  KEY `idx_attendance_user_checkin` (`user_id`,`check_in_time`),
  KEY `idx_attendance_dept_date` (`dept_id`,`attendance_date`)
) ENGINE=InnoDB AUTO_INCREMENT=106 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='出勤记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `attendance_records`
--

LOCK TABLES `attendance_records` WRITE;
/*!40000 ALTER TABLE `attendance_records` DISABLE KEYS */;
INSERT INTO `attendance_records` VALUES (1,4,6,'2026-09-30','2026-09-30 19:05:41',NULL,'0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,NULL,8,NULL,NULL,0,NULL,'未签退，系统自动标记[数据修正]签退时间早于签到时间，已清空签退待补签','2026-09-30 19:05:41','2026-10-20 10:02:10',NULL,0,0,NULL,NULL,'normal','day',0),(2,1,1,'2026-09-30','2026-09-30 19:55:53','2026-09-30 19:56:10','0:0:0:0:0:0:0:1','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,7,0.00,NULL,0,NULL,'严重迟到','2026-09-30 19:55:53','2026-09-30 19:56:10',NULL,0,0,NULL,NULL,'normal','day',0),(3,3,4,'2026-10-03','2026-10-03 16:23:30',NULL,'0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,NULL,0,NULL,NULL,1,6,'申诉通过','2026-10-03 15:29:23','2026-10-20 10:01:57',NULL,0,0,NULL,NULL,'normal','day',0),(5,1,1,'2026-10-02',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-04 14:50:36','2026-10-14 16:25:45','2026-10-14 16:25:45',0,0,NULL,NULL,'normal','day',0),(6,2,0,'2026-10-02',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-04 14:50:36','2026-10-14 16:25:45','2026-10-14 16:25:45',0,0,NULL,NULL,'normal','day',0),(7,3,4,'2026-10-02',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-04 14:50:36','2026-10-14 16:25:45','2026-10-14 16:25:45',0,0,NULL,NULL,'normal','day',0),(8,4,6,'2026-10-02',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-04 14:50:36','2026-10-14 16:25:45','2026-10-14 16:25:45',0,0,NULL,NULL,'normal','day',0),(9,5,2,'2026-10-02',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-04 14:50:36','2026-10-14 16:25:45','2026-10-14 16:25:45',0,0,NULL,NULL,'normal','day',0),(10,6,3,'2026-10-02',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-04 14:50:36','2026-10-14 16:25:45','2026-10-14 16:25:45',0,0,NULL,NULL,'normal','day',0),(11,4,6,'2026-10-04','2026-10-04 14:51:13','2026-10-04 18:00:00',NULL,NULL,NULL,NULL,NULL,NULL,8,3.13,NULL,0,NULL,'未签退，系统自动标记','2026-10-04 14:51:13','2026-10-05 08:04:11',NULL,0,0,22.540000,114.060000,'normal','day',2),(12,5,2,'2026-10-04','2026-10-04 14:51:13','2026-10-04 18:00:00',NULL,NULL,NULL,NULL,NULL,NULL,8,3.13,NULL,0,NULL,'未签退，系统自动标记','2026-10-04 14:51:13','2026-10-05 08:04:11',NULL,0,0,22.540000,114.060000,'normal','day',3),(13,4,6,'2026-09-21','2026-09-21 09:00:00','2026-09-21 18:00:00',NULL,NULL,NULL,NULL,NULL,NULL,0,9.00,NULL,1,14,NULL,'2026-10-05 14:46:56','2026-10-05 14:46:56',NULL,0,0,NULL,NULL,'normal','day',0),(15,1,1,'2026-09-22',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-05 14:48:04','2026-10-05 14:48:04',NULL,0,0,NULL,NULL,'normal','day',0),(16,2,0,'2026-09-22',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-05 14:48:04','2026-10-05 14:48:04',NULL,0,0,NULL,NULL,'normal','day',0),(17,3,4,'2026-09-22',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-05 14:48:04','2026-10-05 14:48:04',NULL,0,0,NULL,NULL,'normal','day',0),(18,5,2,'2026-09-22',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-05 14:48:04','2026-10-05 14:48:04',NULL,0,0,NULL,NULL,'normal','day',0),(19,6,3,'2026-09-22',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-05 14:48:04','2026-10-05 14:48:04',NULL,0,0,NULL,NULL,'normal','day',0),(39,1,1,'2026-10-05','2026-10-05 16:17:44','2026-10-05 16:18:28','0:0:0:0:0:0:0:1','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,0,0.00,NULL,0,NULL,NULL,'2026-10-05 16:17:44','2026-10-05 16:18:28',NULL,1,1,22.766752,113.232742,'normal','day',0),(40,4,6,'2026-10-05','2026-10-05 16:22:39','2026-10-05 16:23:44','0:0:0:0:0:0:0:1','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,0,0.02,NULL,0,NULL,NULL,'2026-10-05 16:22:39','2026-10-05 16:23:44',NULL,1,1,23.130000,113.320000,'normal','day',0),(42,3,4,'2026-10-05','2026-10-05 09:35:00',NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,NULL,NULL,0,NULL,'申诉通过','2026-10-05 16:30:53','2026-10-05 16:33:35',NULL,1,0,NULL,NULL,'normal','day',0),(43,1,1,'2026-10-06',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-07 14:23:06','2026-10-14 16:25:45','2026-10-14 16:25:45',0,0,NULL,NULL,'normal','day',0),(44,2,0,'2026-10-06',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-07 14:23:06','2026-10-14 16:25:45','2026-10-14 16:25:45',0,0,NULL,NULL,'normal','day',0),(45,3,4,'2026-10-06',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-07 14:23:06','2026-10-14 16:25:45','2026-10-14 16:25:45',0,0,NULL,NULL,'normal','day',0),(46,4,6,'2026-10-06',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-07 14:23:06','2026-10-14 16:25:45','2026-10-14 16:25:45',0,0,NULL,NULL,'normal','day',0),(47,5,2,'2026-10-06',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-07 14:23:06','2026-10-14 16:25:45','2026-10-14 16:25:45',0,0,NULL,NULL,'normal','day',0),(48,6,3,'2026-10-06',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-07 14:23:06','2026-10-14 16:25:45','2026-10-14 16:25:45',0,0,NULL,NULL,'normal','day',0),(49,1,1,'2026-10-08','2026-10-08 11:35:18','2026-10-08 11:38:08','0:0:0:0:0:0:0:1','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,0,0.03,NULL,0,NULL,NULL,'2026-10-08 11:35:18','2026-10-08 11:38:08',NULL,1,3,23.150860,113.029337,'normal','day',0),(50,1,1,'2026-10-13',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-14 08:19:48','2026-10-14 08:19:48',NULL,0,0,NULL,NULL,'normal','day',0),(51,2,0,'2026-10-13',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-14 08:19:48','2026-10-14 08:19:48',NULL,0,0,NULL,NULL,'normal','day',0),(52,3,4,'2026-10-13',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-14 08:19:48','2026-10-14 08:19:48',NULL,0,0,NULL,NULL,'normal','day',0),(53,4,6,'2026-10-13',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-14 08:19:48','2026-10-14 08:19:48',NULL,0,0,NULL,NULL,'normal','day',0),(54,5,2,'2026-10-13',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-14 08:19:48','2026-10-14 08:19:48',NULL,0,0,NULL,NULL,'normal','day',0),(55,6,3,'2026-10-13',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-14 08:19:48','2026-10-14 08:19:48',NULL,0,0,NULL,NULL,'normal','day',0),(56,4,6,'2026-10-14','2026-10-14 09:00:00','2026-10-14 18:00:00',NULL,NULL,NULL,NULL,NULL,NULL,0,9.00,NULL,1,17,NULL,'2026-10-16 10:53:49','2026-10-16 10:53:49',NULL,0,0,NULL,NULL,'normal','day',0),(57,1,1,'2026-10-19','2026-10-19 10:15:06','2026-10-19 18:00:00','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,NULL,8,7.73,NULL,0,NULL,'未签退，系统自动标记','2026-10-19 10:15:06','2026-10-20 00:05:00',NULL,1,0,23.149060,113.027532,'normal','day',0),(58,3,4,'2026-10-19','2026-10-19 10:25:42','2026-10-19 18:00:00','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,NULL,8,7.57,NULL,0,NULL,'未签退，系统自动标记','2026-10-19 10:25:42','2026-10-20 00:05:00',NULL,1,0,23.150473,113.028856,'normal','day',0),(59,2,0,'2026-10-19',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-20 08:41:43','2026-10-20 08:41:43',NULL,0,0,NULL,NULL,'normal','day',0),(60,4,6,'2026-10-19',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-20 08:41:43','2026-10-20 08:41:43',NULL,0,0,NULL,NULL,'normal','day',0),(61,5,2,'2026-10-19',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-20 08:41:43','2026-10-20 08:41:43',NULL,0,0,NULL,NULL,'normal','day',0),(62,6,3,'2026-10-19',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-20 08:41:43','2026-10-20 08:41:43',NULL,0,0,NULL,NULL,'normal','day',0),(63,7,1,'2026-10-19',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-20 08:41:43','2026-10-20 08:41:43',NULL,0,0,NULL,NULL,'normal','day',0),(64,8,1,'2026-10-19','2026-10-19 09:00:00',NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,1,18,'补签通过','2026-10-20 08:41:43','2026-10-20 19:46:30',NULL,0,0,NULL,NULL,'normal','day',0),(65,9,1,'2026-10-19',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-20 08:41:43','2026-10-20 08:41:43',NULL,0,0,NULL,NULL,'normal','day',0),(66,1,1,'2026-10-20',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-21 08:37:31','2026-10-21 08:37:31',NULL,0,0,NULL,NULL,'normal','day',0),(67,2,0,'2026-10-20',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-21 08:37:31','2026-10-21 08:37:31',NULL,0,0,NULL,NULL,'normal','day',0),(68,3,4,'2026-10-20',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-21 08:37:31','2026-10-21 08:37:31',NULL,0,0,NULL,NULL,'normal','day',0),(69,4,6,'2026-10-20',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-21 08:37:31','2026-10-21 08:37:31',NULL,0,0,NULL,NULL,'normal','day',0),(70,5,2,'2026-10-20',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-21 08:37:31','2026-10-21 08:37:31',NULL,0,0,NULL,NULL,'normal','day',0),(71,6,3,'2026-10-20',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-21 08:37:31','2026-10-21 08:37:31',NULL,0,0,NULL,NULL,'normal','day',0),(72,7,1,'2026-10-20',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-21 08:37:31','2026-10-21 08:37:31',NULL,0,0,NULL,NULL,'normal','day',0),(73,8,2,'2026-10-20',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-21 08:37:31','2026-10-21 08:37:31',NULL,0,0,NULL,NULL,'normal','day',0),(74,9,1,'2026-10-20',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-21 08:37:31','2026-10-21 08:37:31',NULL,0,0,NULL,NULL,'normal','day',0),(75,10,2,'2026-10-20',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-21 08:37:31','2026-10-21 08:37:31',NULL,0,0,NULL,NULL,'normal','day',0),(77,1,1,'2026-10-22',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-23 01:59:52','2026-10-23 01:59:52',NULL,0,0,NULL,NULL,'normal','day',0),(78,2,0,'2026-10-22',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-23 01:59:52','2026-10-23 01:59:52',NULL,0,0,NULL,NULL,'normal','day',0),(79,3,4,'2026-10-22',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-23 01:59:52','2026-10-23 01:59:52',NULL,0,0,NULL,NULL,'normal','day',0),(80,4,6,'2026-10-22','2026-10-22 09:00:00',NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,1,21,'补签通过','2026-10-23 01:59:52','2026-10-23 18:37:03',NULL,0,0,NULL,NULL,'normal','day',0),(81,5,2,'2026-10-22',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-23 01:59:52','2026-10-23 01:59:52',NULL,0,0,NULL,NULL,'normal','day',0),(82,6,3,'2026-10-22',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-23 01:59:52','2026-10-23 01:59:52',NULL,0,0,NULL,NULL,'normal','day',0),(83,7,1,'2026-10-22',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-23 01:59:52','2026-10-23 01:59:52',NULL,0,0,NULL,NULL,'normal','day',0),(84,8,2,'2026-10-22',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-23 01:59:52','2026-10-23 01:59:52',NULL,0,0,NULL,NULL,'normal','day',0),(85,9,1,'2026-10-22',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-23 01:59:52','2026-10-23 01:59:52',NULL,0,0,NULL,NULL,'normal','day',0),(86,10,2,'2026-10-22',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-23 01:59:52','2026-10-23 01:59:52',NULL,0,0,NULL,NULL,'normal','day',0),(87,12,2,'2026-10-22',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-23 01:59:52','2026-10-23 01:59:52',NULL,0,0,NULL,NULL,'normal','day',0),(88,13,6,'2026-10-22',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-23 01:59:52','2026-10-23 01:59:52',NULL,0,0,NULL,NULL,'normal','day',0),(94,1,1,'2026-10-23',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-24 02:00:00','2026-10-24 02:00:00',NULL,0,0,NULL,NULL,'normal','day',0),(95,2,0,'2026-10-23',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-24 02:00:00','2026-10-24 02:00:00',NULL,0,0,NULL,NULL,'normal','day',0),(96,3,4,'2026-10-23',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-24 02:00:00','2026-10-24 02:00:00',NULL,0,0,NULL,NULL,'normal','day',0),(97,4,6,'2026-10-23',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-24 02:00:00','2026-10-24 02:00:00',NULL,0,0,NULL,NULL,'normal','day',0),(98,5,2,'2026-10-23',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-24 02:00:00','2026-10-24 02:00:00',NULL,0,0,NULL,NULL,'normal','day',0),(99,6,3,'2026-10-23',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-24 02:00:00','2026-10-24 02:00:00',NULL,0,0,NULL,NULL,'normal','day',0),(100,7,1,'2026-10-23',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-24 02:00:00','2026-10-24 02:00:00',NULL,0,0,NULL,NULL,'normal','day',0),(101,8,2,'2026-10-23',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-24 02:00:00','2026-10-24 02:00:00',NULL,0,0,NULL,NULL,'normal','day',0),(102,9,1,'2026-10-23',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-24 02:00:00','2026-10-24 02:00:00',NULL,0,0,NULL,NULL,'normal','day',0),(103,10,2,'2026-10-23',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-24 02:00:00','2026-10-24 02:00:00',NULL,0,0,NULL,NULL,'normal','day',0),(104,12,2,'2026-10-23',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-24 02:00:00','2026-10-24 02:00:00',NULL,0,0,NULL,NULL,'normal','day',0),(105,13,6,'2026-10-23',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3,NULL,NULL,0,NULL,'自动结算缺勤','2026-10-24 02:00:00','2026-10-24 02:00:00',NULL,0,0,NULL,NULL,'normal','day',0);
/*!40000 ALTER TABLE `attendance_records` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `exception_logs`
--

DROP TABLE IF EXISTS `exception_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `exception_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '异常ID',
  `user_id` bigint DEFAULT NULL COMMENT '操作用户ID',
  `username` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作用户名',
  `url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '请求URL',
  `method` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '请求方法',
  `ip_address` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '请求IP',
  `browser` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '浏览器信息',
  `os` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作系统',
  `screen_resolution` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '屏幕分辨率',
  `exception_type` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '异常类型（如 NullPointerException, SQLException）',
  `error_msg` text COLLATE utf8mb4_unicode_ci COMMENT '异常信息',
  `stack_trace` longtext COLLATE utf8mb4_unicode_ci COMMENT '异常堆栈',
  `request_params` text COLLATE utf8mb4_unicode_ci COMMENT '请求参数（JSON格式，敏感字段脱敏）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '处理状态：0未解决 1已解决',
  `resolved` tinyint NOT NULL DEFAULT '0' COMMENT '是否已解决：0否 1是',
  `resolved_by` bigint DEFAULT NULL COMMENT '处理人ID',
  `resolved_time` datetime DEFAULT NULL COMMENT '处理时间',
  `resolve_note` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '处理说明',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间（软删除）',
  PRIMARY KEY (`id`),
  KEY `idx_exception_logs_status_created` (`status`,`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='异常日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `exception_logs`
--

LOCK TABLES `exception_logs` WRITE;
/*!40000 ALTER TABLE `exception_logs` DISABLE KEYS */;
/*!40000 ALTER TABLE `exception_logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `expense_categories`
--

DROP TABLE IF EXISTS `expense_categories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `expense_categories` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `category_name` varchar(50) NOT NULL COMMENT '类别名称',
  `category_code` varchar(50) NOT NULL COMMENT '类别编码',
  `keywords` varchar(255) DEFAULT NULL COMMENT '识别关键词，逗号分隔，用于从流程标题与表单里归类',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0停用 1启用',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_expense_category_code` (`category_code`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='费用类别字典';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `expense_categories`
--

LOCK TABLES `expense_categories` WRITE;
/*!40000 ALTER TABLE `expense_categories` DISABLE KEYS */;
INSERT INTO `expense_categories` VALUES (1,'差旅费','TRAVEL','差旅,出差,交通,住宿,机票,火车,高铁,车费,行程',1,1,'出差相关的交通住宿等支出','2026-10-19 14:19:50','2026-10-19 14:19:50',NULL),(2,'办公费','OFFICE','办公,办公用品,耗材,打印,复印,文具,纸张',2,1,'日常办公用品与耗材支出','2026-10-19 14:19:50','2026-10-19 14:19:50',NULL),(3,'招待费','RECEPTION','招待,接待,宴请,餐饮,聚餐,客户用餐',3,1,'客户接待与餐饮支出','2026-10-19 14:19:50','2026-10-19 14:19:50',NULL),(4,'培训费','TRAINING','培训,课程,学习,讲师,进修,考试',4,1,'培训课程与讲师费用','2026-10-19 14:19:50','2026-10-19 14:19:50',NULL),(5,'采购费','PURCHASE','采购,购置,设备,物资,办公设备,器材',5,1,'物资设备采购支出','2026-10-19 14:19:50','2026-10-19 14:19:50',NULL),(6,'其他','OTHER','团建',6,1,'无法归类时默认落在这里','2026-10-19 14:19:50','2026-10-19 15:53:00',NULL);
/*!40000 ALTER TABLE `expense_categories` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `flow_approvals`
--

DROP TABLE IF EXISTS `flow_approvals`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flow_approvals` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '审批记录ID',
  `instance_id` bigint NOT NULL COMMENT '流程实例ID',
  `step_id` bigint NOT NULL COMMENT '审批步骤ID',
  `approver_id` bigint NOT NULL COMMENT '审批人ID',
  `action` tinyint NOT NULL COMMENT '操作：1通过 2驳回 3转交 4撤回 5催办 6加签',
  `comment` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审批意见',
  `approve_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '审批时间',
  `target_user_id` bigint DEFAULT NULL COMMENT '目标用户ID（转交/加签时的目标用户）',
  `sign_status` tinyint DEFAULT NULL COMMENT '会签状态：1待审批 2已通过 3已驳回（会签场景使用）',
  `read_time` datetime DEFAULT NULL COMMENT '查看时间（用于统计审批响应时长）',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间（软删除）',
  PRIMARY KEY (`id`),
  KEY `idx_flow_approvals_instance` (`instance_id`)
) ENGINE=InnoDB AUTO_INCREMENT=197 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `flow_approvals`
--

LOCK TABLES `flow_approvals` WRITE;
/*!40000 ALTER TABLE `flow_approvals` DISABLE KEYS */;
INSERT INTO `flow_approvals` VALUES (4,2,1,4,1,'提交申请','2026-09-29 16:10:37',NULL,1,NULL,'2026-09-29 16:10:37','2026-09-29 16:10:37',NULL),(5,2,1,5,1,'同意（批量审批）','2026-09-29 17:22:38',NULL,2,NULL,'2026-09-29 17:22:38','2026-09-29 17:22:38',NULL),(8,4,3,1,1,'提交申请','2026-09-29 17:54:39',NULL,1,NULL,'2026-09-29 17:54:39','2026-09-29 17:54:39',NULL),(9,4,3,1,5,'催办','2026-09-29 18:16:09',5,NULL,NULL,'2026-09-29 18:16:09','2026-09-29 18:16:09',NULL),(12,5,4,4,1,'提交申请','2026-09-29 18:57:50',NULL,1,NULL,'2026-09-29 18:57:50','2026-09-29 18:57:50',NULL),(13,5,4,6,2,'你自己出我们是黑公司','2026-09-29 19:10:13',NULL,3,NULL,'2026-09-29 19:10:13','2026-09-29 19:10:13',NULL),(14,6,4,4,1,'提交申请','2026-09-30 15:40:12',NULL,1,NULL,'2026-09-30 15:40:12','2026-09-30 15:40:12',NULL),(15,6,4,4,5,'催办','2026-09-30 15:40:16',6,NULL,NULL,'2026-09-30 15:40:16','2026-09-30 15:40:16',NULL),(16,6,4,4,5,'催办','2026-09-30 15:40:32',6,NULL,NULL,'2026-09-30 15:40:32','2026-09-30 15:40:32',NULL),(17,6,4,6,1,'同意（批量审批）','2026-09-30 16:07:23',NULL,2,NULL,'2026-09-30 16:07:23','2026-09-30 16:07:23',NULL),(18,7,4,4,1,'提交申请','2026-09-30 16:55:11',NULL,1,NULL,'2026-09-30 16:55:11','2026-09-30 16:55:11',NULL),(19,7,4,4,5,'催办','2026-09-30 16:55:16',6,NULL,NULL,'2026-09-30 16:55:16','2026-09-30 16:55:16',NULL),(20,7,4,4,4,'发起人撤回','2026-09-30 16:55:23',NULL,NULL,NULL,'2026-09-30 16:55:23','2026-09-30 16:55:23',NULL),(23,9,5,1,1,'提交申请','2026-09-30 18:05:41',NULL,1,NULL,'2026-09-30 18:05:41','2026-09-30 18:05:41',NULL),(24,9,5,6,1,'同意','2026-09-30 18:06:36',NULL,2,NULL,'2026-09-30 18:06:36','2026-09-30 18:06:36',NULL),(35,13,5,4,1,'提交申请','2026-10-15 16:15:20',NULL,1,NULL,'2026-10-15 16:15:20','2026-10-15 16:15:20',NULL),(36,13,5,6,1,'财务审核通过，票据合规','2026-10-15 16:15:20',NULL,2,NULL,'2026-10-15 16:15:20','2026-10-15 16:15:20',NULL),(46,17,10,9,1,'提交申请','2026-10-19 11:23:54',NULL,1,NULL,'2026-10-19 11:23:54','2026-10-19 11:23:54',NULL),(47,17,10,9,5,'催办','2026-10-19 11:24:15',NULL,NULL,NULL,'2026-10-19 11:24:15','2026-10-19 11:24:15',NULL),(48,17,10,3,2,'不行，拆公司是违法的行为。','2026-10-19 11:25:51',NULL,3,NULL,'2026-10-19 11:25:51','2026-10-19 11:25:51',NULL),(163,4,3,1,5,'系统自动催办：当前步骤已停留 566 小时，超过 48 小时','2026-10-23 09:00:00',5,NULL,NULL,'2026-10-23 09:00:00','2026-10-23 09:00:00',NULL);
/*!40000 ALTER TABLE `flow_approvals` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `flow_definitions`
--

DROP TABLE IF EXISTS `flow_definitions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flow_definitions` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '流程定义ID',
  `flow_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '流程名称',
  `code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '流程编码（如 leave, expense, overtime）',
  `flow_type` tinyint NOT NULL DEFAULT '1' COMMENT '流程类型：1审批流程 2业务流程',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '流程描述',
  `dept_id` bigint DEFAULT NULL COMMENT '所属部门，NULL=全员可用',
  `icon` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '流程图标',
  `form_config` text COLLATE utf8mb4_unicode_ci COMMENT '表单配置（JSON格式，定义申请时的表单字段）',
  `version` int NOT NULL DEFAULT '1' COMMENT '版本号',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0草稿 1已发布 2已停用',
  `audit_status` tinyint NOT NULL DEFAULT '1' COMMENT '审核状态：0待审核 1通过 2拒绝',
  `creator_id` bigint NOT NULL COMMENT '创建人ID',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间（软删除）',
  `need_payment` tinyint NOT NULL DEFAULT '0' COMMENT '是否需要财务付款：0否 1是',
  `use_scope` tinyint NOT NULL DEFAULT '0' COMMENT '使用范围 0全员 1指定部门 2指定角色',
  `use_scope_ids` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '使用范围对象 id，JSON 数组',
  `superseded_by` bigint DEFAULT NULL COMMENT '被哪个新版本替代',
  PRIMARY KEY (`id`),
  KEY `idx_flow_definitions_status` (`status`),
  KEY `idx_flow_definitions_code` (`code`,`version`)
) ENGINE=InnoDB AUTO_INCREMENT=24 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程定义表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `flow_definitions`
--

LOCK TABLES `flow_definitions` WRITE;
/*!40000 ALTER TABLE `flow_definitions` DISABLE KEYS */;
INSERT INTO `flow_definitions` VALUES (1,'请假申请','leave',1,'出现显示怪兽了，他们需要我',NULL,NULL,'[{\"field\":\"startDate\",\"label\":\"开始日期\",\"type\":\"date\",\"required\":true},{\"field\":\"endDate\",\"label\":\"结束日期\",\"type\":\"date\",\"required\":true},{\"field\":\"days\",\"label\":\"请假天数\",\"type\":\"number\",\"required\":true,\"readonly\":true,\"isPrimary\":true},{\"field\":\"reason\",\"label\":\"请假事由\",\"type\":\"textarea\",\"required\":true}]',1,1,1,1,'2026-10-20 17:43:20',1,'2026-09-28 19:09:16','2026-10-23 20:04:52',NULL,0,0,NULL,NULL),(2,'AAA','AAA',1,'AAAAAAAAA',NULL,NULL,'[{\"field\":\"reason\",\"label\":\"AA\",\"type\":\"text\",\"required\":false}]',1,0,1,1,NULL,1,'2026-09-29 14:59:17','2026-10-20 17:43:43',NULL,0,0,NULL,NULL),(3,'请假申请（旧版）','levean',1,'以便每个成员请假',NULL,NULL,'[{\"field\":\"reason\",\"label\":\"请假事由\",\"type\":\"text\",\"required\":true}]',1,0,1,1,NULL,0,'2026-09-29 17:52:38','2026-10-20 17:43:43',NULL,0,0,NULL,NULL),(5,'资金申请','资金',1,'用于资金申请',NULL,NULL,'[{\"field\":\"field_mumk22px\",\"label\":\"申请事由\",\"type\":\"textarea\",\"required\":true}]',1,1,1,1,'2026-09-29 18:56:37',0,'2026-09-29 18:55:03','2026-10-19 11:32:40',NULL,1,0,NULL,NULL),(6,'大额资金报销','大额资金',1,'报销金额满 1500 元转总经理审批，未达到由财务审批后直接结束',NULL,NULL,'[{\"field\":\"amount\",\"label\":\"报销金额(元)\",\"type\":\"number\",\"required\":true,\"isPrimary\":true},{\"field\":\"reason\",\"label\":\"报销事由\",\"type\":\"textarea\",\"required\":true}]',1,1,1,1,'2026-09-30 18:03:48',3,'2026-09-30 18:03:10','2026-10-19 11:32:40',NULL,1,0,NULL,NULL),(7,'两步审批演示','two_step_demo',1,'员工发起 -> 主管审批 -> 经理审批 -> 结束',NULL,NULL,NULL,1,0,1,1,NULL,0,'2026-09-30 18:34:59','2026-10-20 17:43:43',NULL,0,0,NULL,NULL),(9,'出差申请','business_trip',1,'出差申请审批，通过后出差期间自动标记出差状态，无需打卡',NULL,NULL,'[{\"field\":\"start_date\",\"label\":\"出差开始日期\",\"type\":\"date\",\"required\":true},{\"field\":\"end_date\",\"label\":\"出差结束日期\",\"type\":\"date\",\"required\":true},{\"field\":\"destination\",\"label\":\"出差地点\",\"type\":\"text\",\"required\":true},{\"field\":\"reason\",\"label\":\"出差事由\",\"type\":\"textarea\",\"required\":true}]',1,1,1,1,NULL,5,'2026-10-03 14:51:21','2026-10-03 14:55:14',NULL,0,0,NULL,NULL),(10,'采购申请','purchase',1,'部门主管 → 财务审批；金额满 5000 元在财务之后追加总经理审批',NULL,'ShoppingCart','[{\"field\":\"item_name\",\"label\":\"采购物品\",\"type\":\"text\",\"required\":true,\"isPrimary\":true},{\"field\":\"amount\",\"label\":\"采购金额(元)\",\"type\":\"number\",\"required\":true},{\"field\":\"reason\",\"label\":\"采购事由\",\"type\":\"textarea\",\"required\":true},{\"field\":\"expect_date\",\"label\":\"期望到货日期\",\"type\":\"date\",\"required\":false}]',1,1,1,1,'2026-10-19 10:47:11',4,'2026-10-19 10:47:11','2026-10-19 11:32:40',NULL,1,0,NULL,NULL),(11,'人事变动','hr_change',1,'入职/离职/调岗走人事专员审核，再报总经理审批',NULL,'UserFilled','[{\"field\":\"change_type\",\"label\":\"变动类型（入职/离职/调岗）\",\"type\":\"text\",\"required\":true,\"isPrimary\":true},{\"field\":\"employee_name\",\"label\":\"涉及员工\",\"type\":\"text\",\"required\":true},{\"field\":\"org_name\",\"label\":\"所属部门\",\"type\":\"text\",\"required\":false},{\"field\":\"effective_date\",\"label\":\"生效日期\",\"type\":\"date\",\"required\":true},{\"field\":\"reason\",\"label\":\"变动说明\",\"type\":\"textarea\",\"required\":true}]',1,1,1,1,'2026-10-19 10:47:11',6,'2026-10-19 10:47:11','2026-10-19 10:47:11',NULL,0,0,NULL,NULL),(12,'入职申请','onboard',1,'新员工入职审批：部门主管确认用人需求，人事主管审核入职材料与定岗定薪',NULL,NULL,'[{\"field\":\"employee_name\",\"label\":\"姓名\",\"type\":\"text\",\"required\":true,\"isPrimary\":true},{\"field\":\"id_card\",\"label\":\"身份证号\",\"type\":\"text\",\"required\":true},{\"field\":\"org_name\",\"label\":\"入职部门\",\"type\":\"text\",\"required\":true},{\"field\":\"position_name\",\"label\":\"入职岗位\",\"type\":\"text\",\"required\":true},{\"field\":\"entry_date\",\"label\":\"入职日期\",\"type\":\"date\",\"required\":true},{\"field\":\"salary\",\"label\":\"定薪（元）\",\"type\":\"number\",\"required\":true},{\"field\":\"reason\",\"label\":\"备注\",\"type\":\"textarea\",\"required\":false}]',1,1,1,1,'2026-10-20 14:42:08',1,'2026-10-20 14:42:08','2026-10-20 14:42:08',NULL,0,0,NULL,NULL),(13,'离职申请','resign',1,'员工离职审批：部门主管确认交接安排，人事主管审核离职手续，总经理终审',NULL,NULL,'[{\"field\":\"employee_name\",\"label\":\"离职员工\",\"type\":\"text\",\"required\":true,\"isPrimary\":true},{\"field\":\"org_name\",\"label\":\"所属部门\",\"type\":\"text\",\"required\":true},{\"field\":\"last_date\",\"label\":\"最后工作日\",\"type\":\"date\",\"required\":true},{\"field\":\"reason\",\"label\":\"离职原因\",\"type\":\"textarea\",\"required\":true},{\"field\":\"handover\",\"label\":\"交接安排\",\"type\":\"textarea\",\"required\":false}]',1,1,1,1,'2026-10-20 14:42:08',2,'2026-10-20 14:42:08','2026-10-20 14:42:08',NULL,0,0,NULL,NULL),(14,'项目采购申请','project_purchase',1,'项目内采购：部门主管确认需求，项目经理审核项目预算，财务审批后付款',NULL,NULL,'[{\"field\":\"project_name\",\"label\":\"所属项目\",\"type\":\"text\",\"required\":true,\"isPrimary\":true},{\"field\":\"item_name\",\"label\":\"采购物品\",\"type\":\"text\",\"required\":true},{\"field\":\"amount\",\"label\":\"采购金额(元)\",\"type\":\"number\",\"required\":true},{\"field\":\"reason\",\"label\":\"采购理由\",\"type\":\"textarea\",\"required\":true},{\"field\":\"expect_date\",\"label\":\"期望到货日期\",\"type\":\"date\",\"required\":false}]',1,1,1,1,'2026-10-21 15:16:22',8,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL,1,0,NULL,NULL),(15,'项目费用报销','project_expense',1,'项目内费用报销：部门主管确认，项目经理审核项目成本，财务审批后付款',NULL,NULL,'[{\"field\":\"project_name\",\"label\":\"所属项目\",\"type\":\"text\",\"required\":true,\"isPrimary\":true},{\"field\":\"amount\",\"label\":\"报销金额(元)\",\"type\":\"number\",\"required\":true},{\"field\":\"reason\",\"label\":\"费用说明\",\"type\":\"textarea\",\"required\":true},{\"field\":\"expense_date\",\"label\":\"发生日期\",\"type\":\"date\",\"required\":false}]',1,1,1,1,'2026-10-21 15:35:46',9,'2026-10-21 15:35:46','2026-10-23 20:16:36',NULL,1,0,NULL,NULL);
/*!40000 ALTER TABLE `flow_definitions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `flow_delegates`
--

DROP TABLE IF EXISTS `flow_delegates`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flow_delegates` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `delegator_id` bigint NOT NULL COMMENT '委托人（原审核人）',
  `agent_id` bigint NOT NULL COMMENT '代理人',
  `start_time` datetime NOT NULL COMMENT '生效时间',
  `end_time` datetime NOT NULL COMMENT '失效时间',
  `reason` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '委托原因',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1生效 0已取消',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_flow_delegates_agent` (`agent_id`,`status`,`end_time`),
  KEY `idx_flow_delegates_delegator` (`delegator_id`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程审批委托代理';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `flow_delegates`
--

LOCK TABLES `flow_delegates` WRITE;
/*!40000 ALTER TABLE `flow_delegates` DISABLE KEYS */;
/*!40000 ALTER TABLE `flow_delegates` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `flow_instances`
--

DROP TABLE IF EXISTS `flow_instances`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flow_instances` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '流程实例ID',
  `definition_id` bigint NOT NULL COMMENT '流程定义ID',
  `instance_no` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '流程编号（如 FLOW-20260911-0001）',
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '流程标题（如：张三的请假申请）',
  `creator_id` bigint NOT NULL COMMENT '发起人ID',
  `current_step_id` bigint DEFAULT NULL COMMENT '当前步骤ID',
  `current_step_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '当前步骤名称（冗余字段）',
  `current_auditor_id` bigint DEFAULT NULL COMMENT '当前审核人ID（冗余字段，便于待办查询）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0草稿 1审批中 2已通过 3已驳回 4已撤回 5已取消',
  `business_data` text COLLATE utf8mb4_unicode_ci COMMENT '业务数据（JSON格式，存储申请表单数据）',
  `attachment_id` bigint DEFAULT NULL COMMENT '附件ID',
  `urgency` tinyint NOT NULL DEFAULT '0' COMMENT '紧急程度：0普通 1紧急',
  `submit_time` datetime DEFAULT NULL COMMENT '提交时间',
  `finish_time` datetime DEFAULT NULL COMMENT '完成时间（通过/驳回/撤回时间）',
  `approve_duration` int DEFAULT NULL COMMENT '审批耗时（分钟，从提交到完成）',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间（软删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `instance_no` (`instance_no`),
  KEY `idx_flow_instances_definition_status` (`definition_id`,`status`),
  KEY `idx_flow_instances_creator_created` (`creator_id`,`created_at`),
  KEY `idx_flow_instances_auditor_status` (`current_auditor_id`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=83 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程实例表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `flow_instances`
--

LOCK TABLES `flow_instances` WRITE;
/*!40000 ALTER TABLE `flow_instances` DISABLE KEYS */;
INSERT INTO `flow_instances` VALUES (2,1,'FLOW-20260929-161036-7446','请假',4,1,'已完成',5,2,'{\"startDate\":\"2026-09-29\",\"endDate\":\"2026-10-01\",\"days\":3,\"reason\":\"火星移民\"}',NULL,0,'2026-09-29 16:10:37','2026-09-29 17:22:38',NULL,'2026-09-29 16:10:37','2026-09-29 17:22:38',NULL),(4,3,'FLOW-20260929-175438-5888','张三-因事请假',1,3,'部门主管审批',5,1,'{\"reason\":\"张三要变成奥特曼，拯救世界\"}',NULL,0,'2026-09-29 17:54:39',NULL,NULL,'2026-09-29 17:54:39','2026-10-21 08:56:45',NULL),(5,5,'FLOW-20260929-185749-1652','赵六-报销',4,4,'已驳回',6,3,'{\"field_mumk22px\":\"周五请甲方吃饭\"}',NULL,0,'2026-09-29 18:57:50','2026-09-29 19:10:13',NULL,'2026-09-29 18:57:50','2026-09-29 19:10:13',NULL),(6,5,'FLOW-20260930-154011-1992','赵六-报销',4,4,'已完成',6,2,'{\"field_mumk22px\":\"周五请甲方吃饭\"}',NULL,0,'2026-09-30 15:40:12','2026-09-30 16:07:23',NULL,'2026-09-30 15:40:12','2026-09-30 16:07:23',NULL),(7,5,'FLOW-20260930-165510-7217','赵六-报销',4,4,'已撤回',6,4,'{\"field_mumk22px\":\"周五请甲方吃饭\"}',NULL,0,'2026-09-30 16:55:11','2026-09-30 16:55:23',NULL,'2026-09-30 16:55:11','2026-09-30 16:55:23',NULL),(9,6,'FLOW-20260930-180541-8163','张三-巨额资金报销',1,5,'已完成',6,2,'{\"field_munxmncx\":\"周五公司的商用车保养的消费\"}',NULL,1,'2026-09-30 18:05:41','2026-09-30 18:06:36',NULL,'2026-09-30 18:05:41','2026-09-30 18:06:36',NULL),(13,6,'FLOW-20261015-161519-1634','李员工的大额差旅报销（2000元）',4,NULL,'已完成',NULL,2,'{\"field_munxmncx\":\"差旅报销 2000 元，发票齐全\"}',NULL,1,'2026-10-15 16:15:20','2026-10-15 16:15:20',NULL,'2026-10-15 16:15:20','2026-10-15 16:15:20',NULL),(17,10,'FLOW-20261019-0001','雷先生-器材采购',9,NULL,'已驳回',NULL,3,'{\"item_name\":\"雷管\",\"amount\":200000,\"reason\":\"要拆掉公司\",\"expect_date\":\"2026-10-31\"}',NULL,0,'2026-10-19 11:23:54','2026-10-19 11:25:51',NULL,'2026-10-19 11:23:54','2026-10-19 11:25:51',NULL);
/*!40000 ALTER TABLE `flow_instances` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `flow_step_transitions`
--

DROP TABLE IF EXISTS `flow_step_transitions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flow_step_transitions` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `step_id` bigint NOT NULL COMMENT '当前步骤ID',
  `target_step_id` bigint NOT NULL COMMENT '目标步骤ID',
  `condition_expr` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '跳转条件表达式（如 amount>1000 && type==‘expense‘）',
  `condition_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '条件名称（如：金额大于1000）',
  `priority` int NOT NULL DEFAULT '0' COMMENT '条件优先级（数字越小越优先，先匹配先走）',
  `is_default` tinyint NOT NULL DEFAULT '0' COMMENT '是否默认路径：0否 1是（所有条件不匹配时走默认）',
  `transition_type` tinyint NOT NULL DEFAULT '1' COMMENT '跳转类型：1正常通过 2驳回 3转交 4撤回',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间（软删除）',
  PRIMARY KEY (`id`),
  KEY `idx_flow_transitions_step` (`step_id`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程跳转条件表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `flow_step_transitions`
--

LOCK TABLES `flow_step_transitions` WRITE;
/*!40000 ALTER TABLE `flow_step_transitions` DISABLE KEYS */;
INSERT INTO `flow_step_transitions` VALUES (1,7,8,NULL,'主管通过',0,1,1,'2026-09-30 18:35:48','2026-10-20 17:43:43',NULL),(2,5,6,'amount >= 1500','金额满1500转总经理',1,0,1,'2026-10-15 17:06:17','2026-10-19 10:47:11',NULL),(3,10,11,NULL,'部门主管通过',0,1,1,'2026-10-19 10:47:11','2026-10-19 10:47:11',NULL),(4,11,12,NULL,'财务通过转总经理',0,1,1,'2026-10-19 10:47:11','2026-10-20 17:41:46',NULL),(5,13,14,NULL,'人事审核通过转总经理',0,1,1,'2026-10-19 10:47:11','2026-10-19 10:47:11',NULL),(6,1,21,NULL,'部门主管通过转人事备案',0,1,1,'2026-10-20 17:41:46','2026-10-20 17:41:46',NULL),(7,22,5,NULL,'部门主管通过转财务主管',0,1,1,'2026-10-20 17:41:46','2026-10-20 17:41:46',NULL),(8,15,16,NULL,'部门主管通过转人事主管',0,1,1,'2026-10-20 17:41:46','2026-10-20 17:41:46',NULL),(9,17,18,NULL,'部门主管通过转人事主管',0,1,1,'2026-10-20 17:41:46','2026-10-20 17:41:46',NULL),(10,18,19,NULL,'人事主管通过转总经理',0,1,1,'2026-10-20 17:41:46','2026-10-20 17:41:46',NULL),(11,30,31,NULL,'部门主管通过转项目经理',0,1,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(12,31,32,NULL,'项目经理通过转财务',0,1,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(13,33,34,NULL,'部门主管通过转项目经理',0,1,1,'2026-10-21 15:35:46','2026-10-21 15:35:46',NULL),(14,34,35,NULL,'项目经理通过转财务',0,1,1,'2026-10-21 15:35:46','2026-10-21 15:35:46',NULL);
/*!40000 ALTER TABLE `flow_step_transitions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `flow_steps`
--

DROP TABLE IF EXISTS `flow_steps`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flow_steps` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '步骤ID',
  `definition_id` bigint NOT NULL COMMENT '流程定义ID',
  `step_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '步骤名称',
  `step_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '步骤编码',
  `prev_step_id` bigint NOT NULL DEFAULT '0' COMMENT '前置步骤ID（0表示第一步）',
  `step_type` tinyint NOT NULL DEFAULT '1' COMMENT '步骤类型：1审批节点 2抄送节点 3条件网关节点 4开始节点 5结束节点',
  `approver_type` tinyint NOT NULL DEFAULT '1' COMMENT '审批人类型：1指定用户 2指定角色 3部门主管 4职位匹配 5发起人自选',
  `approver_config` text COLLATE utf8mb4_unicode_ci COMMENT '审批人配置（JSON格式，根据approver_type存储不同内容）',
  `step_form` text COLLATE utf8mb4_unicode_ci COMMENT '步骤表单配置（JSON格式，审批时需要填写的字段）',
  `timeout_hours` int NOT NULL DEFAULT '0' COMMENT '超时时间（小时，0表示不超时）',
  `timeout_action` tinyint NOT NULL DEFAULT '0' COMMENT '超时处理：0不处理 1自动催办 2自动通过 3自动驳回',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '步骤顺序号',
  `sign_type` tinyint NOT NULL DEFAULT '1' COMMENT '会签类型：1或签（任意一人通过） 2会签（全部通过） 3按比例通过',
  `sign_ratio` int DEFAULT NULL COMMENT '会签通过比例（sign_type=3时使用，如80表示80%）',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间（软删除）',
  PRIMARY KEY (`id`),
  KEY `idx_flow_steps_definition_sort` (`definition_id`,`sort_order`)
) ENGINE=InnoDB AUTO_INCREMENT=52 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程步骤表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `flow_steps`
--

LOCK TABLES `flow_steps` WRITE;
/*!40000 ALTER TABLE `flow_steps` DISABLE KEYS */;
INSERT INTO `flow_steps` VALUES (1,1,'部门主管审批','dept_approve',0,1,3,NULL,NULL,0,0,1,1,NULL,'2026-09-28 19:09:16','2026-10-21 08:53:33',NULL),(2,2,'部门主管审批','',0,1,3,'',NULL,0,0,1,1,NULL,'2026-09-29 14:59:17','2026-09-29 14:59:17',NULL),(3,3,'部门主管审批','dept_approve',0,1,3,NULL,NULL,0,0,1,1,NULL,'2026-09-29 17:52:38','2026-10-21 08:53:33',NULL),(4,5,'部门主管审批','',0,1,3,NULL,NULL,0,0,1,1,NULL,'2026-09-29 18:55:03','2026-10-21 08:53:33',NULL),(5,6,'财务主管审批','',0,1,2,'{\"roleId\":5}',NULL,0,0,2,1,NULL,'2026-09-30 18:03:10','2026-10-20 17:37:39',NULL),(6,6,'经理审批','',0,1,2,'{\"roleId\":6}',NULL,0,0,3,1,NULL,'2026-09-30 18:03:10','2026-10-20 17:37:39',NULL),(7,7,'主管审批','step1',0,1,1,'{\"userId\": 3}',NULL,0,0,1,1,NULL,'2026-09-30 18:35:48','2026-09-30 18:35:48',NULL),(8,7,'经理审批','step2',0,1,1,'{\"userId\": 1}',NULL,0,0,2,1,NULL,'2026-09-30 18:35:48','2026-09-30 18:35:48',NULL),(9,9,'部门主管审批','',0,1,3,NULL,NULL,0,0,1,1,NULL,'2026-10-03 14:51:59','2026-10-21 08:53:33',NULL),(10,10,'部门主管审批','step1',0,1,3,NULL,NULL,0,0,1,1,NULL,'2026-10-19 10:47:11','2026-10-21 08:53:33',NULL),(11,10,'财务审批','step2',0,1,2,'{\"roleId\":5}',NULL,0,0,2,1,NULL,'2026-10-19 10:47:11','2026-10-19 10:47:11',NULL),(12,10,'总经理审批','step3',0,1,2,'{\"roleId\":6}',NULL,0,0,3,1,NULL,'2026-10-19 10:47:11','2026-10-19 10:47:11',NULL),(13,11,'人事专员审核','step1',0,1,2,'{\"roleId\":4}',NULL,0,0,1,1,NULL,'2026-10-19 10:47:11','2026-10-19 10:47:11',NULL),(14,11,'总经理审批','step2',0,1,2,'{\"roleId\":6}',NULL,0,0,2,1,NULL,'2026-10-19 10:47:11','2026-10-19 10:47:11',NULL),(15,12,'部门主管审批','step1',0,1,3,NULL,NULL,0,0,1,1,NULL,'2026-10-20 14:42:08','2026-10-21 08:53:33',NULL),(16,12,'人事主管审批','step2',0,1,2,'{\"roleId\":7}',NULL,0,0,2,1,NULL,'2026-10-20 14:42:08','2026-10-20 14:42:08',NULL),(17,13,'部门主管审批','step1',0,1,3,NULL,NULL,0,0,1,1,NULL,'2026-10-20 14:42:08','2026-10-21 08:53:33',NULL),(18,13,'人事主管审批','step2',0,1,2,'{\"roleId\":7}',NULL,0,0,2,1,NULL,'2026-10-20 14:42:08','2026-10-20 14:42:08',NULL),(19,13,'总经理审批','step3',0,1,2,'{\"roleId\":6}',NULL,0,0,3,1,NULL,'2026-10-20 14:42:08','2026-10-20 14:42:08',NULL),(21,1,'人事主管备案','hr_confirm',0,1,2,'{\"roleId\":7}',NULL,0,0,2,1,NULL,'2026-10-20 17:37:39','2026-10-20 17:37:39',NULL),(22,6,'部门主管审批','dept_approve',0,1,3,NULL,NULL,0,0,1,1,NULL,'2026-10-20 17:37:39','2026-10-21 08:53:33',NULL),(30,14,'部门主管审批','dept_approve',0,1,3,NULL,NULL,0,0,1,1,100,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(31,14,'项目经理审批','pm_approve',0,1,2,'{\"roleId\":8}',NULL,0,0,2,1,100,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(32,14,'财务审批','finance_approve',0,1,2,'{\"roleId\":5}',NULL,0,0,3,1,100,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(33,15,'部门主管审批','dept_approve',0,1,3,NULL,NULL,0,0,1,1,NULL,'2026-10-21 15:35:46','2026-10-21 15:35:46',NULL),(34,15,'项目经理审批','pm_approve',0,1,2,'{\"roleId\":8}',NULL,0,0,2,1,NULL,'2026-10-21 15:35:46','2026-10-21 15:35:46',NULL),(35,15,'财务审批','finance_approve',0,1,2,'{\"roleId\":5}',NULL,0,0,3,1,NULL,'2026-10-21 15:35:46','2026-10-21 15:35:46',NULL);
/*!40000 ALTER TABLE `flow_steps` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `holidays`
--

DROP TABLE IF EXISTS `holidays`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `holidays` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `year` int DEFAULT NULL,
  `holiday_date` date NOT NULL COMMENT '节假日日期',
  `holiday_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '节假日名称',
  `holiday_type` tinyint NOT NULL DEFAULT '1' COMMENT '类型：1法定假日 2调休上班日 3周末',
  `is_workday` tinyint NOT NULL DEFAULT '0' COMMENT '是否工作日：0否 1是（调休上班日为1）',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间（软删除）',
  PRIMARY KEY (`id`),
  KEY `idx_holidays_year_date` (`year`,`holiday_date`)
) ENGINE=InnoDB AUTO_INCREMENT=43 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='法定节假日表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `holidays`
--

LOCK TABLES `holidays` WRITE;
/*!40000 ALTER TABLE `holidays` DISABLE KEYS */;
INSERT INTO `holidays` VALUES (4,2026,'2026-01-01','元旦',1,0,1,'2026-10-14 16:23:54','2026-10-14 16:23:54',NULL),(5,2026,'2026-01-02','元旦',1,0,2,'2026-10-14 16:23:54','2026-10-14 16:23:54',NULL),(6,2026,'2026-01-03','元旦',1,0,3,'2026-10-14 16:23:54','2026-10-14 16:23:54',NULL),(7,2026,'2026-01-04','元旦调休上班',2,1,4,'2026-10-14 16:23:54','2026-10-14 16:23:54',NULL),(8,2026,'2026-02-14','春节调休上班',2,1,5,'2026-10-14 16:23:54','2026-10-14 16:23:54',NULL),(9,2026,'2026-02-15','春节',1,0,6,'2026-10-14 16:23:54','2026-10-14 16:23:54',NULL),(10,2026,'2026-02-16','春节',1,0,7,'2026-10-14 16:23:54','2026-10-14 16:23:54',NULL),(11,2026,'2026-02-17','春节',1,0,8,'2026-10-14 16:23:54','2026-10-14 16:23:54',NULL),(12,2026,'2026-02-18','春节',1,0,9,'2026-10-14 16:23:54','2026-10-14 16:23:54',NULL),(13,2026,'2026-02-19','春节',1,0,10,'2026-10-14 16:23:54','2026-10-14 16:23:54',NULL),(14,2026,'2026-02-20','春节',1,0,11,'2026-10-14 16:23:54','2026-10-14 16:23:54',NULL),(15,2026,'2026-02-21','春节',1,0,12,'2026-10-14 16:23:54','2026-10-14 16:23:54',NULL),(16,2026,'2026-02-22','春节',1,0,13,'2026-10-14 16:23:54','2026-10-14 16:23:54',NULL),(17,2026,'2026-02-23','春节',1,0,14,'2026-10-14 16:23:54','2026-10-14 16:23:54',NULL),(18,2026,'2026-02-28','春节调休上班',2,1,15,'2026-10-14 16:23:54','2026-10-14 16:23:54',NULL),(19,2026,'2026-04-04','清明节',1,0,16,'2026-10-14 16:23:54','2026-10-14 16:23:54',NULL),(20,2026,'2026-04-05','清明节',1,0,17,'2026-10-14 16:23:54','2026-10-14 16:23:54',NULL),(21,2026,'2026-04-06','清明节',1,0,18,'2026-10-14 16:23:54','2026-10-14 16:23:54',NULL),(22,2026,'2026-05-01','劳动节',1,0,19,'2026-10-14 16:23:54','2026-10-14 16:23:54',NULL),(23,2026,'2026-05-02','劳动节',1,0,20,'2026-10-14 16:23:54','2026-10-14 16:23:54',NULL),(24,2026,'2026-05-03','劳动节',1,0,21,'2026-10-14 16:23:54','2026-10-14 16:23:54',NULL),(25,2026,'2026-05-04','劳动节',1,0,22,'2026-10-14 16:23:54','2026-10-14 16:23:54',NULL),(26,2026,'2026-05-05','劳动节',1,0,23,'2026-10-14 16:23:54','2026-10-14 16:23:54',NULL),(27,2026,'2026-05-09','劳动节调休上班',2,1,24,'2026-10-14 16:23:54','2026-10-14 16:23:54',NULL),(28,2026,'2026-06-19','端午节',1,0,25,'2026-10-14 16:23:54','2026-10-14 16:23:54',NULL),(29,2026,'2026-06-20','端午节',1,0,26,'2026-10-14 16:23:54','2026-10-14 16:23:54',NULL),(30,2026,'2026-06-21','端午节',1,0,27,'2026-10-14 16:23:54','2026-10-14 16:23:54',NULL),(31,2026,'2026-09-20','国庆节调休上班',2,1,28,'2026-10-14 16:23:54','2026-10-14 16:23:54',NULL),(32,2026,'2026-09-25','中秋节',1,0,29,'2026-10-14 16:23:54','2026-10-14 16:23:54',NULL),(33,2026,'2026-09-26','中秋节',1,0,30,'2026-10-14 16:23:54','2026-10-14 16:23:54',NULL),(34,2026,'2026-09-27','中秋节',1,0,31,'2026-10-14 16:23:54','2026-10-14 16:23:54',NULL),(35,2026,'2026-10-01','国庆节',1,0,32,'2026-10-14 16:23:54','2026-10-14 16:23:54',NULL),(36,2026,'2026-10-02','国庆节',1,0,33,'2026-10-14 16:23:54','2026-10-14 16:23:54',NULL),(37,2026,'2026-10-03','国庆节',1,0,34,'2026-10-14 16:23:54','2026-10-14 16:23:54',NULL),(38,2026,'2026-10-04','国庆节',1,0,35,'2026-10-14 16:23:54','2026-10-14 16:23:54',NULL),(39,2026,'2026-10-05','国庆节',1,0,36,'2026-10-14 16:23:54','2026-10-14 16:23:54',NULL),(40,2026,'2026-10-06','国庆节',1,0,37,'2026-10-14 16:23:54','2026-10-14 16:23:54',NULL),(41,2026,'2026-10-07','国庆节',1,0,38,'2026-10-14 16:23:54','2026-10-14 16:23:54',NULL),(42,2026,'2026-10-10','国庆节调休上班',2,1,39,'2026-10-14 16:23:54','2026-10-14 16:23:54',NULL);
/*!40000 ALTER TABLE `holidays` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `login_logs`
--

DROP TABLE IF EXISTS `login_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `login_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID（登录成功时记录）',
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '登录用户名',
  `login_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  `ip_address` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '登录IP',
  `location` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '登录地点（IP解析）',
  `device` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '设备类型（如 Windows, iOS, Android）',
  `browser` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '浏览器信息',
  `os` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作系统',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '登录状态：0失败 1成功',
  `fail_reason` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '失败原因（如密码错误、账号锁定）',
  `logout_time` datetime DEFAULT NULL COMMENT '登出时间',
  `session_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '会话ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间（软删除）',
  PRIMARY KEY (`id`),
  KEY `idx_login_logs_user_time` (`user_id`,`login_time`),
  KEY `idx_login_logs_ip_time` (`ip_address`,`login_time`)
) ENGINE=InnoDB AUTO_INCREMENT=496 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `login_logs`
--

LOCK TABLES `login_logs` WRITE;
/*!40000 ALTER TABLE `login_logs` DISABLE KEYS */;
INSERT INTO `login_logs` VALUES (1,1,'admin','2026-09-22 09:07:55','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-22 09:07:55','2026-09-22 09:07:54',NULL),(3,1,'admin','2026-09-22 09:10:02','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-22 09:10:02','2026-09-22 09:10:01',NULL),(6,1,'admin','2026-09-26 14:50:31','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-26 14:50:31','2026-09-26 14:50:31',NULL),(8,1,'admin','2026-09-26 15:03:28','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-26 15:03:28','2026-09-26 15:03:27',NULL),(9,1,'admin','2026-09-26 15:40:28','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-26 15:40:28','2026-09-26 15:40:27',NULL),(10,2,'hhf','2026-09-26 16:46:43','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-26 16:46:43','2026-09-26 16:46:43',NULL),(11,2,'hhf','2026-09-26 16:48:52','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-26 16:48:52','2026-09-26 16:48:51',NULL),(12,1,'admin','2026-09-26 17:26:04','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-26 17:26:04','2026-09-26 17:26:03',NULL),(13,1,'admin','2026-09-26 17:27:45','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-26 17:27:45','2026-09-26 17:27:44',NULL),(14,1,'admin','2026-09-26 17:50:47','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-26 17:50:47','2026-09-26 17:50:46',NULL),(15,1,'admin','2026-09-26 17:55:36','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-26 17:55:36','2026-09-26 17:55:35',NULL),(16,1,'admin','2026-09-26 18:06:03','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-26 18:06:03','2026-09-26 18:06:03',NULL),(17,1,'admin','2026-09-28 14:23:03','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-28 14:23:03','2026-09-28 14:23:02',NULL),(18,4,'employee01','2026-09-28 14:25:24','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-28 14:25:24','2026-09-28 14:25:24',NULL),(19,1,'admin','2026-09-28 14:27:39','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-28 14:27:39','2026-09-28 14:27:38',NULL),(20,4,'employee01','2026-09-28 14:28:04','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-28 14:28:04','2026-09-28 14:28:03',NULL),(21,1,'admin','2026-09-28 14:37:40','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-28 14:37:40','2026-09-28 14:37:40',NULL),(22,1,'admin','2026-09-28 15:05:32','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-28 15:05:32','2026-09-28 15:05:31',NULL),(23,1,'admin','2026-09-28 15:05:43','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-28 15:05:43','2026-09-28 15:05:43',NULL),(24,1,'admin','2026-09-28 15:05:54','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-28 15:05:54','2026-09-28 15:05:54',NULL),(25,1,'admin','2026-09-28 15:06:06','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-28 15:06:06','2026-09-28 15:06:06',NULL),(26,1,'admin','2026-09-28 15:10:59','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-28 15:10:59','2026-09-28 15:10:58',NULL),(28,4,'employee01','2026-09-28 15:45:45','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-28 15:45:45','2026-09-28 15:45:45',NULL),(29,1,'admin','2026-09-28 15:47:50','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-28 15:47:50','2026-09-28 15:47:50',NULL),(30,4,'employee01','2026-09-28 15:58:46','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-28 15:58:46','2026-09-28 15:58:45',NULL),(31,4,'employee01','2026-09-28 15:59:31','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-28 15:59:31','2026-09-28 15:59:30',NULL),(32,4,'employee01','2026-09-28 16:17:50','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-28 16:17:50','2026-09-28 16:17:50',NULL),(33,1,'admin','2026-09-28 16:28:49','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-28 16:28:49','2026-09-28 16:28:49',NULL),(34,1,'admin','2026-09-28 16:50:14','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-28 16:50:14','2026-09-28 16:50:14',NULL),(35,1,'admin','2026-09-28 17:19:18','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-28 17:19:18','2026-09-28 17:19:18',NULL),(36,1,'admin','2026-09-28 18:10:55','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-28 18:10:55','2026-09-28 18:10:55',NULL),(37,1,'admin','2026-09-28 18:27:04','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-28 18:27:04','2026-09-28 18:27:03',NULL),(38,4,'employee01','2026-09-28 18:30:21','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-28 18:30:21','2026-09-28 18:30:21',NULL),(39,1,'admin','2026-09-28 18:30:45','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-28 18:30:45','2026-09-28 18:30:45',NULL),(40,1,'admin','2026-09-28 18:57:54','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-28 18:57:54','2026-09-28 18:57:54',NULL),(41,1,'admin','2026-09-28 19:53:49','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-28 19:53:49','2026-09-28 19:53:48',NULL),(42,4,'employee01','2026-09-28 19:55:12','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-28 19:55:12','2026-09-28 19:55:12',NULL),(43,1,'admin','2026-09-28 19:55:39','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-28 19:55:39','2026-09-28 19:55:38',NULL),(44,1,'admin','2026-09-29 14:16:14','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-29 14:16:14','2026-09-29 14:16:14',NULL),(45,4,'employee01','2026-09-29 15:07:26','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-29 15:07:26','2026-09-29 15:07:25',NULL),(46,5,'hr01','2026-09-29 15:08:27','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-29 15:08:27','2026-09-29 15:08:26',NULL),(47,5,'hr01','2026-09-29 15:09:57','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-29 15:09:57','2026-09-29 15:09:56',NULL),(48,4,'employee01','2026-09-29 16:09:05','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-29 16:09:05','2026-09-29 16:09:05',NULL),(49,5,'hr01','2026-09-29 17:20:42','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-29 17:20:42','2026-09-29 17:20:42',NULL),(50,5,'hr01','2026-09-29 17:43:55','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-29 17:43:55','2026-09-29 17:43:54',NULL),(52,1,'admin','2026-09-29 17:46:05','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-29 17:46:05','2026-09-29 17:46:04',NULL),(53,1,'admin','2026-09-29 17:48:47','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-29 17:48:47','2026-09-29 17:48:46',NULL),(54,4,'employee01','2026-09-29 18:16:18','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-29 18:16:18','2026-09-29 18:16:18',NULL),(55,5,'hr01','2026-09-29 18:17:17','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-29 18:17:17','2026-09-29 18:17:17',NULL),(56,1,'admin','2026-09-29 18:18:09','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-29 18:18:09','2026-09-29 18:18:09',NULL),(57,6,'finance01','2026-09-29 18:26:10','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-29 18:26:10','2026-09-29 18:26:09',NULL),(58,6,'finance01','2026-09-29 18:27:10','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-29 18:27:10','2026-09-29 18:27:10',NULL),(59,5,'hr01','2026-09-29 18:28:17','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-29 18:28:17','2026-09-29 18:28:16',NULL),(60,1,'admin','2026-09-29 18:29:31','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-29 18:29:31','2026-09-29 18:29:30',NULL),(61,6,'finance01','2026-09-29 18:55:20','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-29 18:55:20','2026-09-29 18:55:20',NULL),(62,4,'employee01','2026-09-29 18:55:34','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-29 18:55:34','2026-09-29 18:55:34',NULL),(63,1,'admin','2026-09-29 18:56:01','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-29 18:56:01','2026-09-29 18:56:00',NULL),(64,1,'admin','2026-09-30 14:00:17','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-30 14:00:17','2026-09-30 14:00:17',NULL),(65,1,'admin','2026-09-30 14:43:57','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-30 14:43:57','2026-09-30 14:43:56',NULL),(66,1,'admin','2026-09-30 15:30:38','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-30 15:30:38','2026-09-30 15:30:37',NULL),(67,4,'employee01','2026-09-30 15:35:43','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-30 15:35:43','2026-09-30 15:35:43',NULL),(68,6,'finance01','2026-09-30 15:40:39','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-30 15:40:39','2026-09-30 15:40:38',NULL),(69,4,'employee01','2026-09-30 16:54:56','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-30 16:54:56','2026-09-30 16:54:55',NULL),(70,3,'manager01','2026-09-30 18:38:38','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-30 18:38:38','2026-09-30 18:38:37',NULL),(71,3,'manager01','2026-09-30 18:39:32','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-30 18:39:32','2026-09-30 18:39:31',NULL),(72,1,'admin','2026-09-30 19:41:46','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-30 19:41:46','2026-09-30 19:41:45',NULL),(73,1,'admin','2026-09-30 19:45:12','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-30 19:45:12','2026-09-30 19:45:12',NULL),(74,1,'admin','2026-09-30 19:51:43','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-30 19:51:43','2026-09-30 19:51:42',NULL),(75,1,'admin','2026-09-30 19:52:00','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-09-30 19:52:00','2026-09-30 19:51:59',NULL),(83,1,'admin','2026-10-01 14:18:38','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-01 14:18:38','2026-10-01 14:18:37',NULL),(84,1,'admin','2026-10-01 15:44:18','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-01 15:44:18','2026-10-01 15:44:17',NULL),(85,1,'admin','2026-10-03 14:13:45','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-03 14:13:45','2026-10-03 14:13:44',NULL),(87,1,'admin','2026-10-03 15:06:04','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-03 15:06:04','2026-10-03 15:06:04',NULL),(88,1,'admin','2026-10-03 15:15:31','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-03 15:15:31','2026-10-03 15:15:30',NULL),(89,3,'manager01','2026-10-03 15:24:43','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-03 15:24:43','2026-10-03 15:24:42',NULL),(90,3,'manager01','2026-10-03 15:48:11','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-03 15:48:11','2026-10-03 15:48:11',NULL),(91,1,'admin','2026-10-03 15:48:47','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-03 15:48:47','2026-10-03 15:48:46',NULL),(105,1,'admin','2026-10-04 11:01:41','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-04 11:01:41','2026-10-04 11:01:41',NULL),(118,1,'admin','2026-10-04 14:58:56','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-04 14:58:56','2026-10-04 14:58:56',NULL),(121,1,'admin','2026-10-04 15:00:26','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-04 15:00:26','2026-10-04 15:00:25',NULL),(124,4,'employee01','2026-10-04 15:18:27','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-04 15:18:27','2026-10-04 15:18:27',NULL),(125,1,'admin','2026-10-04 15:41:22','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-04 15:41:22','2026-10-04 15:41:22',NULL),(126,1,'admin','2026-10-04 15:41:40','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-04 15:41:40','2026-10-04 15:41:39',NULL),(127,1,'admin','2026-10-04 15:42:01','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-04 15:42:01','2026-10-04 15:42:01',NULL),(128,3,'manager01','2026-10-04 15:47:29','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-04 15:47:29','2026-10-04 15:47:28',NULL),(129,3,'manager01','2026-10-04 15:49:06','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-04 15:49:06','2026-10-04 15:49:06',NULL),(130,3,'manager01','2026-10-04 15:49:25','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-04 15:49:25','2026-10-04 15:49:25',NULL),(131,3,'manager01','2026-10-04 15:49:26','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-04 15:49:26','2026-10-04 15:49:25',NULL),(132,3,'manager01','2026-10-04 15:49:38','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-04 15:49:38','2026-10-04 15:49:38',NULL),(133,1,'admin','2026-10-04 15:49:55','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-04 15:49:55','2026-10-04 15:49:54',NULL),(134,3,'manager01','2026-10-04 15:50:05','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-04 15:50:05','2026-10-04 15:50:04',NULL),(135,1,'admin','2026-10-04 15:50:24','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-04 15:50:24','2026-10-04 15:50:24',NULL),(136,1,'admin','2026-10-04 15:51:19','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-04 15:51:19','2026-10-04 15:51:18',NULL),(137,3,'manager01','2026-10-04 15:51:33','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-04 15:51:33','2026-10-04 15:51:32',NULL),(138,3,'manager01','2026-10-04 15:54:08','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-04 15:54:08','2026-10-04 15:54:07',NULL),(139,3,'manager01','2026-10-04 15:56:13','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-04 15:56:13','2026-10-04 15:56:13',NULL),(140,3,'manager01','2026-10-04 15:58:13','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-04 15:58:13','2026-10-04 15:58:12',NULL),(141,1,'admin','2026-10-04 16:02:35','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-04 16:02:35','2026-10-04 16:02:34',NULL),(142,3,'manager01','2026-10-04 16:02:48','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-04 16:02:48','2026-10-04 16:02:47',NULL),(143,3,'manager01','2026-10-04 16:03:06','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-04 16:03:06','2026-10-04 16:03:06',NULL),(144,3,'manager01','2026-10-04 16:05:56','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-04 16:05:56','2026-10-04 16:05:55',NULL),(145,1,'admin','2026-10-04 16:08:22','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-04 16:08:22','2026-10-04 16:08:22',NULL),(146,3,'manager01','2026-10-04 16:09:12','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-04 16:09:12','2026-10-04 16:09:11',NULL),(147,3,'manager01','2026-10-04 16:28:15','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-04 16:28:15','2026-10-04 16:28:15',NULL),(148,3,'manager01','2026-10-04 16:28:36','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-04 16:28:36','2026-10-04 16:28:36',NULL),(149,1,'admin','2026-10-04 16:28:56','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-04 16:28:56','2026-10-04 16:28:56',NULL),(150,3,'manager01','2026-10-04 16:30:51','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-04 16:30:51','2026-10-04 16:30:50',NULL),(151,3,'manager01','2026-10-04 16:31:07','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-04 16:31:07','2026-10-04 16:31:06',NULL),(152,1,'admin','2026-10-04 16:37:57','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-04 16:37:57','2026-10-04 16:37:56',NULL),(153,1,'admin','2026-10-04 16:41:10','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-04 16:41:10','2026-10-04 16:41:10',NULL),(156,1,'admin','2026-10-04 16:52:15','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-04 16:52:15','2026-10-04 16:52:15',NULL),(168,1,'admin','2026-10-04 17:13:41','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-04 17:13:41','2026-10-04 17:13:40',NULL),(169,1,'admin','2026-10-04 17:14:02','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-04 17:14:02','2026-10-04 17:14:01',NULL),(170,1,'admin','2026-10-04 17:14:41','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-04 17:14:41','2026-10-04 17:14:41',NULL),(173,1,'admin','2026-10-05 08:49:30','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-05 08:49:30','2026-10-05 08:49:29',NULL),(174,1,'admin','2026-10-05 14:17:51','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-05 14:17:51','2026-10-05 14:17:50',NULL),(175,1,'admin','2026-10-05 14:22:07','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-05 14:22:07','2026-10-05 14:22:06',NULL),(176,4,'employee01','2026-10-05 14:22:07','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-05 14:22:07','2026-10-05 14:22:06',NULL),(177,1,'admin','2026-10-05 14:46:55','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-05 14:46:55','2026-10-05 14:46:54',NULL),(178,4,'employee01','2026-10-05 14:46:55','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-05 14:46:55','2026-10-05 14:46:54',NULL),(179,1,'admin','2026-10-05 14:48:04','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-05 14:48:04','2026-10-05 14:48:03',NULL),(180,4,'employee01','2026-10-05 14:48:04','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-05 14:48:04','2026-10-05 14:48:03',NULL),(181,1,'admin','2026-10-05 14:49:09','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-05 14:49:09','2026-10-05 14:49:08',NULL),(182,4,'employee01','2026-10-05 14:49:09','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-05 14:49:09','2026-10-05 14:49:08',NULL),(183,1,'admin','2026-10-05 14:49:47','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-05 14:49:47','2026-10-05 14:49:47',NULL),(184,4,'employee01','2026-10-05 14:49:48','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-05 14:49:48','2026-10-05 14:49:47',NULL),(185,1,'admin','2026-10-05 14:54:29','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-05 14:54:29','2026-10-05 14:54:28',NULL),(186,1,'admin','2026-10-05 15:36:20','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-05 15:36:20','2026-10-05 15:36:20',NULL),(187,1,'admin','2026-10-05 15:37:15','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-05 15:37:15','2026-10-05 15:37:15',NULL),(188,4,'employee01','2026-10-05 15:37:15','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-05 15:37:15','2026-10-05 15:37:15',NULL),(189,1,'admin','2026-10-05 15:38:39','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-05 15:38:39','2026-10-05 15:38:39',NULL),(190,4,'employee01','2026-10-05 15:38:39','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-05 15:38:39','2026-10-05 15:38:39',NULL),(191,1,'admin','2026-10-05 15:39:45','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-05 15:39:45','2026-10-05 15:39:45',NULL),(192,4,'employee01','2026-10-05 15:39:45','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-05 15:39:45','2026-10-05 15:39:45',NULL),(193,1,'admin','2026-10-05 15:40:38','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-05 15:40:38','2026-10-05 15:40:38',NULL),(194,4,'employee01','2026-10-05 15:40:38','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-05 15:40:38','2026-10-05 15:40:38',NULL),(195,4,'employee01','2026-10-05 15:43:38','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-05 15:43:38','2026-10-05 15:43:37',NULL),(196,4,'employee01','2026-10-05 15:45:50','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-05 15:45:50','2026-10-05 15:45:49',NULL),(197,1,'admin','2026-10-05 15:47:15','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-05 15:47:15','2026-10-05 15:47:14',NULL),(198,4,'employee01','2026-10-05 15:47:15','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-05 15:47:15','2026-10-05 15:47:15',NULL),(199,1,'admin','2026-10-05 15:48:36','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-05 15:48:36','2026-10-05 15:48:36',NULL),(200,4,'employee01','2026-10-05 15:48:36','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-05 15:48:36','2026-10-05 15:48:36',NULL),(201,1,'admin','2026-10-05 16:02:26','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-05 16:02:26','2026-10-05 16:02:26',NULL),(202,1,'admin','2026-10-05 16:03:22','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-05 16:03:22','2026-10-05 16:03:21',NULL),(203,4,'employee01','2026-10-05 16:03:22','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-05 16:03:22','2026-10-05 16:03:21',NULL),(204,4,'employee01','2026-10-05 16:05:12','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-05 16:05:12','2026-10-05 16:05:12',NULL),(205,4,'employee01','2026-10-05 16:22:39','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-05 16:22:39','2026-10-05 16:22:38',NULL),(206,1,'admin','2026-10-05 16:23:44','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-05 16:23:44','2026-10-05 16:23:44',NULL),(207,3,'manager01','2026-10-05 16:30:14','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-05 16:30:14','2026-10-05 16:30:14',NULL),(212,3,'manager01','2026-10-05 16:30:54','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-05 16:30:54','2026-10-05 16:30:54',NULL),(213,3,'manager01','2026-10-05 16:32:16','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-05 16:32:16','2026-10-05 16:32:15',NULL),(214,3,'manager01','2026-10-05 16:32:57','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-05 16:32:57','2026-10-05 16:32:56',NULL),(215,1,'admin','2026-10-05 17:21:19','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-05 17:21:19','2026-10-05 17:21:19',NULL),(216,4,'employee01','2026-10-05 17:21:20','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-05 17:21:20','2026-10-05 17:21:19',NULL),(217,1,'admin','2026-10-05 17:21:45','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-05 17:21:45','2026-10-05 17:21:45',NULL),(218,4,'employee01','2026-10-05 17:21:46','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-05 17:21:46','2026-10-05 17:21:45',NULL),(219,1,'admin','2026-10-08 09:05:40','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-08 09:05:40','2026-10-08 09:05:39',NULL),(220,1,'admin','2026-10-08 11:35:05','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-08 11:35:05','2026-10-08 11:35:05',NULL),(221,1,'admin','2026-10-09 10:10:26','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-09 10:10:26','2026-10-09 10:10:26',NULL),(222,1,'admin','2026-10-09 14:15:30','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-09 14:15:30','2026-10-09 14:15:30',NULL),(233,1,'admin','2026-10-09 14:49:34','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-09 14:49:34','2026-10-09 14:49:34',NULL),(234,4,'employee01','2026-10-09 14:57:19','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-09 14:57:19','2026-10-09 14:57:19',NULL),(235,4,'employee01','2026-10-09 14:58:35','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-09 14:58:35','2026-10-09 14:58:35',NULL),(236,3,'manager01','2026-10-09 15:09:45','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-09 15:09:45','2026-10-09 15:09:44',NULL),(245,4,'employee01','2026-10-09 15:53:21','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-09 15:53:21','2026-10-09 15:53:21',NULL),(248,1,'admin','2026-10-09 15:59:44','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-09 15:59:44','2026-10-09 15:59:43',NULL),(249,1,'admin','2026-10-09 16:07:01','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-09 16:07:01','2026-10-09 16:07:01',NULL),(250,1,'admin','2026-10-13 14:41:54','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-13 14:41:54','2026-10-13 14:41:54',NULL),(251,1,'admin','2026-10-13 14:42:04','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-13 14:42:04','2026-10-13 14:42:03',NULL),(252,1,'admin','2026-10-13 14:44:01','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-13 14:44:01','2026-10-13 14:44:01',NULL),(254,1,'admin','2026-10-13 15:11:43','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-13 15:11:43','2026-10-13 15:11:42',NULL),(255,1,'admin','2026-10-13 16:31:43','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-13 16:31:43','2026-10-13 16:31:43',NULL),(256,1,'admin','2026-10-13 16:35:41','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-13 16:35:41','2026-10-13 16:35:40',NULL),(257,4,'employee01','2026-10-13 16:35:41','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-13 16:35:41','2026-10-13 16:35:40',NULL),(258,1,'admin','2026-10-13 16:36:42','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-13 16:36:42','2026-10-13 16:36:41',NULL),(259,4,'employee01','2026-10-13 16:36:42','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-13 16:36:42','2026-10-13 16:36:41',NULL),(260,1,'admin','2026-10-13 16:42:51','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-13 16:42:51','2026-10-13 16:42:50',NULL),(261,4,'employee01','2026-10-13 16:42:51','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-13 16:42:51','2026-10-13 16:42:50',NULL),(262,1,'admin','2026-10-13 16:44:26','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-13 16:44:26','2026-10-13 16:44:26',NULL),(263,4,'employee01','2026-10-13 16:44:26','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-13 16:44:26','2026-10-13 16:44:26',NULL),(264,1,'admin','2026-10-13 16:45:10','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-13 16:45:10','2026-10-13 16:45:09',NULL),(265,4,'employee01','2026-10-13 16:45:10','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-13 16:45:10','2026-10-13 16:45:09',NULL),(266,1,'admin','2026-10-13 16:47:21','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-13 16:47:21','2026-10-13 16:47:20',NULL),(267,4,'employee01','2026-10-13 16:47:21','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-13 16:47:21','2026-10-13 16:47:21',NULL),(268,1,'admin','2026-10-13 16:55:37','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-13 16:55:37','2026-10-13 16:55:37',NULL),(269,1,'admin','2026-10-13 17:02:20','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-13 17:02:20','2026-10-13 17:02:20',NULL),(270,4,'employee01','2026-10-13 17:14:54','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-13 17:14:54','2026-10-13 17:14:54',NULL),(271,4,'employee01','2026-10-13 17:22:34','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-13 17:22:34','2026-10-13 17:22:33',NULL),(272,4,'employee01','2026-10-13 17:31:50','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-13 17:31:50','2026-10-13 17:31:49',NULL),(273,1,'admin','2026-10-13 17:39:20','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-13 17:39:20','2026-10-13 17:39:19',NULL),(274,4,'employee01','2026-10-13 17:39:20','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-13 17:39:20','2026-10-13 17:39:20',NULL),(275,1,'admin','2026-10-13 18:03:58','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-13 18:03:58','2026-10-13 18:03:58',NULL),(276,1,'admin','2026-10-13 18:03:59','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-13 18:03:59','2026-10-13 18:03:59',NULL),(277,1,'admin','2026-10-13 18:04:49','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-13 18:04:49','2026-10-13 18:04:48',NULL),(278,1,'admin','2026-10-13 18:04:50','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-13 18:04:50','2026-10-13 18:04:49',NULL),(279,1,'admin','2026-10-13 18:05:47','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-13 18:05:47','2026-10-13 18:05:46',NULL),(280,1,'admin','2026-10-13 18:09:14','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-13 18:09:14','2026-10-13 18:09:14',NULL),(281,1,'admin','2026-10-13 20:23:17','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-13 20:23:17','2026-10-13 20:23:16',NULL),(282,1,'admin','2026-10-13 20:24:41','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-13 20:24:41','2026-10-13 20:24:41',NULL),(283,4,'employee01','2026-10-13 20:24:41','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-13 20:24:41','2026-10-13 20:24:41',NULL),(284,3,'manager01','2026-10-13 20:24:42','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-13 20:24:42','2026-10-13 20:24:41',NULL),(285,3,'manager01','2026-10-13 20:25:05','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-13 20:25:05','2026-10-13 20:25:04',NULL),(286,3,'manager01','2026-10-13 20:25:42','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-13 20:25:42','2026-10-13 20:25:42',NULL),(287,1,'admin','2026-10-13 20:25:43','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-13 20:25:43','2026-10-13 20:25:42',NULL),(288,1,'admin','2026-10-13 20:28:12','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-13 20:28:12','2026-10-13 20:28:12',NULL),(289,4,'employee01','2026-10-13 20:28:13','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-13 20:28:13','2026-10-13 20:28:12',NULL),(290,3,'manager01','2026-10-13 20:28:13','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-13 20:28:13','2026-10-13 20:28:13',NULL),(291,3,'manager01','2026-10-13 20:28:37','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-13 20:28:37','2026-10-13 20:28:36',NULL),(292,3,'manager01','2026-10-13 20:28:49','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-13 20:28:49','2026-10-13 20:28:49',NULL),(293,3,'manager01','2026-10-13 20:29:19','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-13 20:29:19','2026-10-13 20:29:19',NULL),(294,1,'admin','2026-10-13 20:29:40','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-13 20:29:40','2026-10-13 20:29:40',NULL),(295,4,'employee01','2026-10-13 20:29:41','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-13 20:29:41','2026-10-13 20:29:40',NULL),(296,3,'manager01','2026-10-13 20:29:41','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-13 20:29:41','2026-10-13 20:29:40',NULL),(297,1,'admin','2026-10-13 20:30:04','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-13 20:30:04','2026-10-13 20:30:03',NULL),(298,4,'employee01','2026-10-13 20:30:04','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-13 20:30:04','2026-10-13 20:30:03',NULL),(299,1,'admin','2026-10-13 20:30:16','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-13 20:30:16','2026-10-13 20:30:15',NULL),(300,4,'employee01','2026-10-13 20:30:16','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-13 20:30:16','2026-10-13 20:30:15',NULL),(301,1,'admin','2026-10-13 20:31:02','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-13 20:31:02','2026-10-13 20:31:01',NULL),(302,4,'employee01','2026-10-13 20:31:02','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-13 20:31:02','2026-10-13 20:31:01',NULL),(303,4,'employee01','2026-10-13 20:31:16','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-13 20:31:16','2026-10-13 20:31:15',NULL),(304,1,'admin','2026-10-13 20:38:23','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-13 20:38:23','2026-10-13 20:38:23',NULL),(305,1,'admin','2026-10-13 20:48:23','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-13 20:48:23','2026-10-13 20:48:22',NULL),(306,4,'employee01','2026-10-13 20:48:23','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-13 20:48:23','2026-10-13 20:48:23',NULL),(307,1,'admin','2026-10-13 20:48:38','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-13 20:48:38','2026-10-13 20:48:37',NULL),(308,4,'employee01','2026-10-13 20:48:38','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-13 20:48:38','2026-10-13 20:48:37',NULL),(309,1,'admin','2026-10-13 20:49:18','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-13 20:49:18','2026-10-13 20:49:17',NULL),(310,1,'admin','2026-10-14 08:47:41','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 08:47:41','2026-10-14 08:47:40',NULL),(311,4,'employee01','2026-10-14 08:47:41','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 08:47:41','2026-10-14 08:47:40',NULL),(312,1,'admin','2026-10-14 08:47:53','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 08:47:53','2026-10-14 08:47:53',NULL),(313,1,'admin','2026-10-14 08:48:02','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 08:48:02','2026-10-14 08:48:01',NULL),(314,1,'admin','2026-10-14 08:48:20','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 08:48:20','2026-10-14 08:48:20',NULL),(315,1,'admin','2026-10-14 08:51:04','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 08:51:04','2026-10-14 08:51:03',NULL),(316,1,'admin','2026-10-14 09:07:20','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 09:07:20','2026-10-14 09:07:20',NULL),(317,1,'admin','2026-10-14 09:07:40','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 09:07:40','2026-10-14 09:07:39',NULL),(318,1,'admin','2026-10-14 09:44:52','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 09:44:52','2026-10-14 09:44:51',NULL),(319,4,'employee01','2026-10-14 09:45:48','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 09:45:48','2026-10-14 09:45:48',NULL),(320,4,'employee01','2026-10-14 09:46:58','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 09:46:58','2026-10-14 09:46:58',NULL),(321,3,'manager01','2026-10-14 09:46:59','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 09:46:59','2026-10-14 09:46:58',NULL),(322,1,'admin','2026-10-14 09:46:59','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 09:46:59','2026-10-14 09:46:58',NULL),(323,4,'employee01','2026-10-14 09:47:59','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 09:47:59','2026-10-14 09:47:59',NULL),(324,3,'manager01','2026-10-14 09:47:59','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 09:47:59','2026-10-14 09:47:59',NULL),(325,1,'admin','2026-10-14 09:48:00','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 09:48:00','2026-10-14 09:47:59',NULL),(326,1,'admin','2026-10-14 10:27:42','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 10:27:42','2026-10-14 10:27:41',NULL),(327,4,'employee01','2026-10-14 10:28:24','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 10:28:24','2026-10-14 10:28:24',NULL),(328,3,'manager01','2026-10-14 10:29:32','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 10:29:32','2026-10-14 10:29:32',NULL),(329,1,'admin','2026-10-14 10:45:10','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 10:45:10','2026-10-14 10:45:09',NULL),(330,1,'admin','2026-10-14 10:52:19','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 10:52:19','2026-10-14 10:52:19',NULL),(331,4,'employee01','2026-10-14 11:01:08','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 11:01:08','2026-10-14 11:01:08',NULL),(332,3,'manager01','2026-10-14 11:03:42','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 11:03:42','2026-10-14 11:03:42',NULL),(333,3,'manager01','2026-10-14 11:05:42','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 11:05:42','2026-10-14 11:05:42',NULL),(334,3,'manager01','2026-10-14 11:05:53','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 11:05:53','2026-10-14 11:05:53',NULL),(335,3,'manager01','2026-10-14 11:06:15','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 11:06:15','2026-10-14 11:06:14',NULL),(336,1,'admin','2026-10-14 15:00:50','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 15:00:50','2026-10-14 15:00:50',NULL),(337,5,'hr01','2026-10-14 15:00:51','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 15:00:51','2026-10-14 15:00:50',NULL),(338,3,'manager01','2026-10-14 15:00:51','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 15:00:51','2026-10-14 15:00:50',NULL),(339,4,'employee01','2026-10-14 15:00:51','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 15:00:51','2026-10-14 15:00:51',NULL),(340,1,'admin','2026-10-14 15:05:53','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 15:05:53','2026-10-14 15:05:53',NULL),(341,1,'admin','2026-10-14 15:06:21','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 15:06:21','2026-10-14 15:06:21',NULL),(342,5,'hr01','2026-10-14 15:06:22','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 15:06:22','2026-10-14 15:06:21',NULL),(343,3,'manager01','2026-10-14 15:06:22','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 15:06:22','2026-10-14 15:06:21',NULL),(344,4,'employee01','2026-10-14 15:06:22','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 15:06:22','2026-10-14 15:06:21',NULL),(345,1,'admin','2026-10-14 15:06:59','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 15:06:59','2026-10-14 15:06:59',NULL),(346,1,'admin','2026-10-14 15:08:58','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 15:08:58','2026-10-14 15:08:58',NULL),(347,5,'hr01','2026-10-14 15:08:58','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 15:08:58','2026-10-14 15:08:58',NULL),(348,1,'admin','2026-10-14 15:12:28','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 15:12:28','2026-10-14 15:12:27',NULL),(349,1,'admin','2026-10-14 15:13:07','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 15:13:07','2026-10-14 15:13:06',NULL),(350,5,'hr01','2026-10-14 15:13:07','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 15:13:07','2026-10-14 15:13:06',NULL),(351,1,'admin','2026-10-14 15:17:17','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 15:17:17','2026-10-14 15:17:17',NULL),(352,1,'admin','2026-10-14 15:31:21','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 15:31:21','2026-10-14 15:31:20',NULL),(353,5,'hr01','2026-10-14 15:38:39','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 15:38:39','2026-10-14 15:38:38',NULL),(354,3,'manager01','2026-10-14 15:41:41','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 15:41:41','2026-10-14 15:41:41',NULL),(355,3,'manager01','2026-10-14 15:42:23','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 15:42:23','2026-10-14 15:42:22',NULL),(356,4,'employee01','2026-10-14 15:44:34','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 15:44:34','2026-10-14 15:44:33',NULL),(357,1,'admin','2026-10-14 15:48:17','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 15:48:17','2026-10-14 15:48:16',NULL),(358,1,'admin','2026-10-14 15:48:33','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 15:48:33','2026-10-14 15:48:32',NULL),(359,1,'admin','2026-10-14 16:26:16','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 16:26:16','2026-10-14 16:26:16',NULL),(360,4,'employee01','2026-10-14 16:26:17','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 16:26:17','2026-10-14 16:26:16',NULL),(361,1,'admin','2026-10-14 16:27:07','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 16:27:07','2026-10-14 16:27:07',NULL),(362,1,'admin','2026-10-14 16:27:33','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 16:27:33','2026-10-14 16:27:33',NULL),(363,4,'employee01','2026-10-14 16:43:18','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 16:43:18','2026-10-14 16:43:18',NULL),(364,1,'admin','2026-10-14 16:44:26','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 16:44:26','2026-10-14 16:44:26',NULL),(365,1,'admin','2026-10-14 17:37:15','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 17:37:15','2026-10-14 17:37:14',NULL),(366,1,'admin','2026-10-14 17:37:53','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 17:37:53','2026-10-14 17:37:52',NULL),(367,1,'admin','2026-10-14 17:38:24','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 17:38:24','2026-10-14 17:38:23',NULL),(368,1,'admin','2026-10-14 17:40:48','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 17:40:48','2026-10-14 17:40:48',NULL),(369,1,'admin','2026-10-14 17:46:54','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 17:46:54','2026-10-14 17:46:53',NULL),(370,4,'employee01','2026-10-14 18:00:40','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 18:00:40','2026-10-14 18:00:39',NULL),(371,1,'admin','2026-10-14 19:10:17','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-14 19:10:17','2026-10-14 19:10:16',NULL),(372,7,'gm01','2026-10-15 10:46:48','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-15 10:46:48','2026-10-15 10:46:48',NULL),(373,7,'gm01','2026-10-15 10:48:13','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-15 10:48:13','2026-10-15 10:48:13',NULL),(374,7,'gm01','2026-10-15 10:48:37','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-15 10:48:37','2026-10-15 10:48:37',NULL),(375,7,'gm01','2026-10-15 10:55:09','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-15 10:55:09','2026-10-15 10:55:09',NULL),(376,7,'gm01','2026-10-15 11:03:45','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-15 11:03:45','2026-10-15 11:03:45',NULL),(377,7,'gm01','2026-10-15 11:07:27','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-15 11:07:27','2026-10-15 11:07:26',NULL),(378,4,'employee01','2026-10-15 11:13:48','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-15 11:13:48','2026-10-15 11:13:48',NULL),(379,7,'gm01','2026-10-15 11:13:49','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-15 11:13:49','2026-10-15 11:13:48',NULL),(380,7,'gm01','2026-10-15 11:21:03','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-15 11:21:03','2026-10-15 11:21:03',NULL),(381,7,'gm01','2026-10-15 11:21:03','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-15 11:21:03','2026-10-15 11:21:03',NULL),(382,1,'admin','2026-10-15 11:21:03','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-15 11:21:03','2026-10-15 11:21:03',NULL),(383,7,'gm01','2026-10-15 11:21:44','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-15 11:21:44','2026-10-15 11:21:44',NULL),(384,7,'gm01','2026-10-15 11:22:57','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-15 11:22:57','2026-10-15 11:22:56',NULL),(385,7,'gm01','2026-10-15 11:22:57','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-15 11:22:57','2026-10-15 11:22:57',NULL),(386,1,'admin','2026-10-15 11:22:57','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-15 11:22:57','2026-10-15 11:22:57',NULL),(387,7,'gm01','2026-10-15 11:25:07','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-15 11:25:07','2026-10-15 11:25:06',NULL),(388,1,'admin','2026-10-15 11:25:07','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-15 11:25:07','2026-10-15 11:25:07',NULL),(389,1,'admin','2026-10-15 11:29:03','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-15 11:29:03','2026-10-15 11:29:02',NULL),(390,7,'gm01','2026-10-15 11:29:03','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-15 11:29:03','2026-10-15 11:29:03',NULL),(391,7,'gm01','2026-10-15 11:36:00','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-15 11:36:00','2026-10-15 11:36:00',NULL),(398,8,'gm02','2026-10-15 15:40:42','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-15 15:40:42','2026-10-15 15:40:41',NULL),(399,8,'gm02','2026-10-15 15:41:09','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-15 15:41:09','2026-10-15 15:41:09',NULL),(400,8,'gm02','2026-10-15 15:43:27','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-15 15:43:27','2026-10-15 15:43:27',NULL),(402,4,'employee01','2026-10-15 16:15:20','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-15 16:15:20','2026-10-15 16:15:19',NULL),(403,6,'finance01','2026-10-15 16:15:20','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-15 16:15:20','2026-10-15 16:15:19',NULL),(404,8,'gm02','2026-10-15 16:15:20','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-15 16:15:20','2026-10-15 16:15:20',NULL),(405,4,'employee01','2026-10-16 10:37:21','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-16 10:37:21','2026-10-16 10:37:20',NULL),(406,3,'manager01','2026-10-16 10:47:21','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-16 10:47:21','2026-10-16 10:47:20',NULL),(407,5,'hr01','2026-10-16 10:51:49','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-16 10:51:49','2026-10-16 10:51:48',NULL),(408,4,'employee01','2026-10-16 10:55:19','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-16 10:55:19','2026-10-16 10:55:18',NULL),(409,3,'manager01','2026-10-16 10:56:57','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-16 10:56:57','2026-10-16 10:56:56',NULL),(412,1,'admin','2026-10-19 10:12:40','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-19 10:12:40','2026-10-19 10:12:39',NULL),(413,3,'manager01','2026-10-19 10:24:03','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-19 10:24:03','2026-10-19 10:24:02',NULL),(414,3,'manager01','2026-10-19 10:25:02','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-19 10:25:02','2026-10-19 10:25:02',NULL),(415,1,'admin','2026-10-19 11:16:09','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-19 11:16:09','2026-10-19 11:16:09',NULL),(416,9,'gm03','2026-10-19 11:19:35','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-19 11:19:35','2026-10-19 11:19:35',NULL),(417,9,'gm03','2026-10-19 11:20:54','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-19 11:20:54','2026-10-19 11:20:54',NULL),(418,3,'manager01','2026-10-19 15:16:27','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-19 15:16:27','2026-10-19 15:16:26',NULL),(419,1,'admin','2026-10-19 15:16:43','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-19 15:16:43','2026-10-19 15:16:43',NULL),(428,1,'admin','2026-10-19 15:29:38','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-19 15:29:38','2026-10-19 15:29:37',NULL),(430,1,'admin','2026-10-19 15:29:55','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-19 15:29:55','2026-10-19 15:29:54',NULL),(431,6,'finance01','2026-10-19 15:30:34','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-19 15:30:34','2026-10-19 15:30:33',NULL),(432,6,'finance01','2026-10-19 15:31:50','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-19 15:31:50','2026-10-19 15:31:49',NULL),(433,1,'admin','2026-10-19 15:50:42','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-19 15:50:42','2026-10-19 15:50:41',NULL),(434,1,'admin','2026-10-20 11:15:09','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-20 11:15:09','2026-10-20 11:15:08',NULL),(435,6,'finance01','2026-10-20 14:12:32','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-20 14:12:32','2026-10-20 14:12:32',NULL),(437,5,'hr01','2026-10-20 14:28:16','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-20 14:28:16','2026-10-20 14:28:16',NULL),(438,5,'hr01','2026-10-20 14:29:03','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-20 14:29:03','2026-10-20 14:29:02',NULL),(440,6,'finance01','2026-10-20 15:57:04','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-20 15:57:04','2026-10-20 15:57:03',NULL),(441,1,'admin','2026-10-20 16:02:03','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-20 16:02:03','2026-10-20 16:02:02',NULL),(442,10,'hrmanager01','2026-10-20 16:03:30','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-20 16:03:30','2026-10-20 16:03:30',NULL),(443,10,'hrmanager01','2026-10-20 16:04:16','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-20 16:04:16','2026-10-20 16:04:16',NULL),(444,1,'admin','2026-10-21 14:33:58','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-21 14:33:58','2026-10-21 14:33:58',NULL),(445,5,'hr01','2026-10-21 14:35:52','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-21 14:35:52','2026-10-21 14:35:52',NULL),(446,1,'admin','2026-10-21 14:36:17','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-21 14:36:17','2026-10-21 14:36:16',NULL),(447,1,'admin','2026-10-21 14:36:48','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-21 14:36:48','2026-10-21 14:36:48',NULL),(448,12,'hr02','2026-10-21 14:39:39','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-21 14:39:39','2026-10-21 14:39:39',NULL),(449,12,'hr02','2026-10-21 14:40:27','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-21 14:40:27','2026-10-21 14:40:27',NULL),(450,12,'hr02','2026-10-21 15:04:54','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-21 15:04:54','2026-10-21 15:04:54',NULL),(451,13,'pm01','2026-10-21 15:34:45','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-21 15:34:45','2026-10-21 15:34:45',NULL),(452,13,'pm01','2026-10-21 15:35:05','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-21 15:35:05','2026-10-21 15:35:05',NULL),(453,13,'pm01','2026-10-21 15:36:24','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-21 15:36:24','2026-10-21 15:36:23',NULL),(454,13,'pm01','2026-10-21 15:36:42','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-21 15:36:42','2026-10-21 15:36:41',NULL),(455,1,'admin','2026-10-21 16:57:26','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-21 16:57:26','2026-10-21 16:57:26',NULL),(456,13,'pm01','2026-10-21 17:01:48','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-21 17:01:48','2026-10-21 17:01:48',NULL),(458,13,'pm01','2026-10-21 17:02:47','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-21 17:02:47','2026-10-21 17:02:47',NULL),(459,1,'admin','2026-10-21 17:07:19','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-21 17:07:19','2026-10-21 17:07:18',NULL),(462,1,'admin','2026-10-21 17:09:03','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-21 17:09:03','2026-10-21 17:09:02',NULL),(463,9,'gm03','2026-10-21 17:09:44','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-21 17:09:44','2026-10-21 17:09:44',NULL),(464,9,'gm03','2026-10-21 17:10:17','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-21 17:10:17','2026-10-21 17:10:16',NULL),(465,9,'gm03','2026-10-21 17:12:35','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-21 17:12:35','2026-10-21 17:12:35',NULL),(466,9,'gm03','2026-10-22 09:45:03','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-22 09:45:03','2026-10-22 09:45:03',NULL),(467,1,'admin','2026-10-22 09:45:30','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-22 09:45:30','2026-10-22 09:45:30',NULL),(472,12,'hr02','2026-10-22 10:34:51','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-22 10:34:51','2026-10-22 10:34:50',NULL),(473,9,'gm03','2026-10-22 10:35:43','0:0:0:0:0:0:0:1',NULL,NULL,NULL,NULL,1,NULL,NULL,NULL,'2026-10-22 10:35:43','2026-10-22 10:35:42',NULL);
/*!40000 ALTER TABLE `login_logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `menus`
--

DROP TABLE IF EXISTS `menus`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `menus` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父菜单ID（0表示顶级）',
  `menu_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '菜单名称',
  `menu_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '菜单编码（权限标识，如 system:user:list）',
  `menu_type` tinyint NOT NULL DEFAULT '1' COMMENT '类型：1目录 2菜单 3按钮',
  `path` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '路由路径',
  `component` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '前端组件路径',
  `icon` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '图标',
  `is_nav` tinyint NOT NULL DEFAULT '1' COMMENT '是否在导航栏显示：0否 1是',
  `page_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '页面地址（前端路由路径）',
  `nav_icon` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '导航图标',
  `open_type` tinyint NOT NULL DEFAULT '1' COMMENT '打开方式：1当前窗口 2新窗口',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0停用 1启用',
  `visible` tinyint NOT NULL DEFAULT '1' COMMENT '是否可见：0隐藏 1显示',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间（软删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `menu_code` (`menu_code`),
  KEY `idx_menus_parent` (`parent_id`)
) ENGINE=InnoDB AUTO_INCREMENT=120 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='栏目/菜单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `menus`
--

LOCK TABLES `menus` WRITE;
/*!40000 ALTER TABLE `menus` DISABLE KEYS */;
INSERT INTO `menus` VALUES (1,0,'公告通知','announcement',2,'/announcement','announcement/index','Bell',1,NULL,NULL,1,6,1,1,'2026-09-22 19:35:28','2026-09-22 19:35:28',NULL),(2,1,'公告查看','announcement:list',3,NULL,NULL,NULL,0,NULL,NULL,1,1,1,1,'2026-09-22 19:35:59','2026-09-22 19:35:59',NULL),(3,1,'新增公告','announcement:add',3,NULL,NULL,NULL,0,NULL,NULL,1,2,1,1,'2026-09-22 19:35:59','2026-09-22 19:35:59',NULL),(4,1,'编辑公告','announcement:edit',3,NULL,NULL,NULL,0,NULL,NULL,1,3,1,1,'2026-09-22 19:35:59','2026-09-22 19:35:59',NULL),(5,1,'删除公告','announcement:delete',3,NULL,NULL,NULL,0,NULL,NULL,1,4,1,1,'2026-09-22 19:35:59','2026-09-22 19:35:59',NULL),(6,1,'发布/下线','announcement:publish',3,NULL,NULL,NULL,0,NULL,NULL,1,5,1,1,'2026-09-22 19:35:59','2026-09-22 19:35:59',NULL),(7,1,'公告置顶','announcement:top',3,NULL,NULL,NULL,0,NULL,NULL,1,6,1,1,'2026-09-22 19:35:59','2026-09-22 19:35:59',NULL),(8,1,'已读列表','announcement:reads',3,NULL,NULL,NULL,0,NULL,NULL,1,7,1,1,'2026-09-22 19:35:59','2026-09-22 19:35:59',NULL),(9,1,'类别管理','announcement:category:manage',3,NULL,NULL,NULL,0,NULL,NULL,1,8,1,1,'2026-09-22 19:35:59','2026-09-22 19:35:59',NULL),(10,0,'用户管理','user',2,'/home/user',NULL,NULL,1,NULL,NULL,1,2,1,1,'2026-09-26 16:01:50','2026-09-26 16:01:50',NULL),(11,10,'用户列表','user:list',3,'',NULL,NULL,1,NULL,NULL,1,1,1,1,'2026-09-26 16:01:50','2026-09-26 16:01:50',NULL),(12,10,'新增用户','user:add',3,'',NULL,NULL,1,NULL,NULL,1,2,1,1,'2026-09-26 16:01:50','2026-09-26 16:01:50',NULL),(13,10,'编辑用户','user:edit',3,'',NULL,NULL,1,NULL,NULL,1,3,1,1,'2026-09-26 16:01:50','2026-09-26 16:01:50',NULL),(14,10,'删除用户','user:delete',3,'',NULL,NULL,1,NULL,NULL,1,4,1,1,'2026-09-26 16:01:50','2026-09-26 16:01:50',NULL),(15,10,'重置密码','user:reset',3,'',NULL,NULL,1,NULL,NULL,1,5,1,1,'2026-09-26 16:01:50','2026-09-26 16:01:50',NULL),(16,10,'启用/停用','user:status',3,'',NULL,NULL,1,NULL,NULL,1,6,1,1,'2026-09-26 16:01:50','2026-09-26 16:01:50',NULL),(17,10,'账号解锁','user:unlock',3,'',NULL,NULL,1,NULL,NULL,1,7,1,1,'2026-09-26 16:01:50','2026-09-26 16:01:50',NULL),(18,0,'工作日志','worklog',1,'/home/worklog','worklog/index','Notebook',1,NULL,NULL,1,4,1,1,'2026-09-28 14:53:58','2026-09-28 14:53:58',NULL),(19,18,'日志查看/管理','worklog:view-all',1,NULL,NULL,NULL,1,NULL,NULL,1,1,1,1,'2026-09-28 14:53:58','2026-09-28 14:53:58',NULL),(20,18,'日志类别管理','worklog:category:manage',1,NULL,NULL,NULL,1,NULL,NULL,1,2,1,1,'2026-09-28 14:53:58','2026-09-28 14:53:58',NULL),(21,18,'查看权限配置','worklog:permission-config',1,NULL,NULL,NULL,1,NULL,NULL,1,3,1,1,'2026-09-28 14:53:58','2026-09-28 14:53:58',NULL),(22,18,'日志导出','worklog:export',1,NULL,NULL,NULL,1,NULL,NULL,1,4,1,1,'2026-09-28 14:53:58','2026-09-28 14:53:58',NULL),(23,10,'导出用户','user:export',3,NULL,NULL,NULL,1,NULL,NULL,1,8,1,1,'2026-09-28 16:45:33','2026-09-28 16:45:33',NULL),(24,10,'组织机构管理','org:manage',3,NULL,NULL,NULL,1,NULL,NULL,1,9,1,1,'2026-09-28 17:11:03','2026-09-28 17:11:03',NULL),(25,10,'职位管理','position:manage',3,NULL,NULL,NULL,1,NULL,NULL,1,10,1,1,'2026-09-28 17:59:51','2026-09-28 17:59:51',NULL),(26,10,'职称管理','title:manage',3,NULL,NULL,NULL,1,NULL,NULL,1,11,1,1,'2026-09-28 17:59:51','2026-09-28 17:59:51',NULL),(27,10,'用户群组','group:manage',3,NULL,NULL,NULL,1,NULL,NULL,1,12,1,1,'2026-09-28 18:04:12','2026-09-28 18:04:12',NULL),(28,0,'流程管理','flow:manage',2,NULL,NULL,NULL,1,NULL,NULL,1,5,1,1,'2026-09-28 18:43:39','2026-09-28 18:43:39',NULL),(29,28,'流程定义','flow:def:manage',3,NULL,NULL,NULL,1,NULL,NULL,1,1,1,1,'2026-09-28 18:43:39','2026-09-28 18:43:39',NULL),(30,28,'发起流程','flow:start',3,NULL,NULL,NULL,1,NULL,NULL,1,2,1,1,'2026-09-28 18:43:39','2026-09-28 18:43:39',NULL),(31,28,'待办','flow:todo',3,NULL,NULL,NULL,1,NULL,NULL,1,3,1,1,'2026-09-28 18:43:39','2026-09-28 18:43:39',NULL),(32,28,'已办','flow:done',3,NULL,NULL,NULL,1,NULL,NULL,1,4,1,1,'2026-09-28 18:43:39','2026-09-28 18:43:39',NULL),(33,28,'我发起的','flow:mine',3,NULL,NULL,NULL,1,NULL,NULL,1,5,1,1,'2026-09-28 18:43:39','2026-09-28 18:43:39',NULL),(34,28,'所有流程','flow:all:view',3,'/home/flow-all',NULL,NULL,1,NULL,NULL,1,6,1,1,'2026-10-15 10:32:37','2026-10-15 10:32:37',NULL),(35,0,'在线用户','system:online',3,'/home/online','views/system/online.vue',NULL,0,NULL,NULL,1,90,1,1,'2026-10-19 09:45:37','2026-10-19 09:45:37',NULL),(36,0,'出勤全局查看','attendance:view-all',3,NULL,NULL,NULL,0,NULL,NULL,1,901,1,0,'2026-10-19 10:47:11','2026-10-19 10:47:11',NULL),(37,0,'出勤配置管理','attendance:manage',3,NULL,NULL,NULL,0,NULL,NULL,1,902,1,0,'2026-10-19 10:47:11','2026-10-19 10:47:11',NULL),(38,0,'任务全局查看','task:view-all',3,NULL,NULL,NULL,0,NULL,NULL,1,903,1,0,'2026-10-19 10:47:11','2026-10-19 10:47:11',NULL),(39,0,'报表查看','report:view',3,NULL,NULL,NULL,0,NULL,NULL,1,904,1,0,'2026-10-19 10:47:11','2026-10-19 10:47:11',NULL),(40,0,'财务管理','finance',2,NULL,NULL,'Money',1,NULL,NULL,1,7,1,1,'2026-10-19 11:32:40','2026-10-19 11:32:40',NULL),(41,40,'付款管理','finance:pay',3,'/home/finance/payment','finance/payment/index','Wallet',1,NULL,NULL,1,1,1,1,'2026-10-19 11:32:40','2026-10-19 11:32:40',NULL),(42,40,'付款导出','finance:export',3,NULL,NULL,NULL,0,NULL,NULL,1,2,1,0,'2026-10-19 11:32:40','2026-10-19 11:32:40',NULL),(43,40,'作废付款','finance:pay:void',3,NULL,NULL,NULL,0,NULL,NULL,1,3,1,0,'2026-10-19 11:32:40','2026-10-19 11:32:40',NULL),(44,40,'财务统计','finance:stat',3,'/home/finance/stat','finance/stat/index','DataAnalysis',1,NULL,NULL,1,2,1,1,'2026-10-19 14:19:50','2026-10-19 14:19:50',NULL),(45,40,'费用类别','finance:category:list',3,'/home/finance/category','finance/category/index','Files',1,NULL,NULL,1,3,1,1,'2026-10-19 14:19:50','2026-10-19 14:19:50',NULL),(46,40,'新增类别','finance:category:add',3,NULL,NULL,NULL,0,NULL,NULL,1,4,1,0,'2026-10-19 14:19:50','2026-10-19 14:19:50',NULL),(47,40,'编辑类别','finance:category:edit',3,NULL,NULL,NULL,0,NULL,NULL,1,5,1,0,'2026-10-19 14:19:50','2026-10-19 14:19:50',NULL),(48,40,'删除类别','finance:category:delete',3,NULL,NULL,NULL,0,NULL,NULL,1,6,1,0,'2026-10-19 14:19:50','2026-10-19 14:19:50',NULL),(49,18,'日志回收站管理','worklog:manage',1,NULL,NULL,NULL,1,NULL,NULL,1,5,1,1,'2026-10-20 09:58:39','2026-10-20 09:58:39',NULL),(50,0,'招聘管理','recruitment',2,'/home/recruitment',NULL,'Suitcase',1,NULL,NULL,1,8,1,1,'2026-10-20 14:42:08','2026-10-20 14:42:08',NULL),(51,50,'招聘岗位','recruitment:position:list',3,'/home/recruitment/position',NULL,NULL,1,NULL,NULL,1,1,1,1,'2026-10-20 14:42:08','2026-10-20 14:42:08',NULL),(52,50,'发布岗位','recruitment:position:add',3,NULL,NULL,NULL,0,NULL,NULL,1,2,1,1,'2026-10-20 14:42:08','2026-10-20 14:42:08',NULL),(53,50,'编辑岗位','recruitment:position:edit',3,NULL,NULL,NULL,0,NULL,NULL,1,3,1,1,'2026-10-20 14:42:08','2026-10-20 14:42:08',NULL),(54,50,'删除岗位','recruitment:position:delete',3,NULL,NULL,NULL,0,NULL,NULL,1,4,1,1,'2026-10-20 14:42:08','2026-10-20 14:42:08',NULL),(55,50,'候选人管理','recruitment:candidate:list',3,'/home/recruitment/candidate',NULL,NULL,1,NULL,NULL,1,5,1,1,'2026-10-20 14:42:08','2026-10-20 14:42:08',NULL),(56,50,'新增候选人','recruitment:candidate:add',3,NULL,NULL,NULL,0,NULL,NULL,1,6,1,1,'2026-10-20 14:42:08','2026-10-20 14:42:08',NULL),(57,50,'编辑候选人','recruitment:candidate:edit',3,NULL,NULL,NULL,0,NULL,NULL,1,7,1,1,'2026-10-20 14:42:08','2026-10-20 14:42:08',NULL),(58,50,'候选人状态流转','recruitment:candidate:status',3,NULL,NULL,NULL,0,NULL,NULL,1,8,1,1,'2026-10-20 14:42:08','2026-10-20 14:42:08',NULL),(59,50,'删除候选人','recruitment:candidate:delete',3,NULL,NULL,NULL,0,NULL,NULL,1,9,1,1,'2026-10-20 14:42:08','2026-10-20 14:42:08',NULL),(60,0,'登录日志统计','login:stat',3,'/home/system/login-log',NULL,NULL,1,NULL,NULL,1,9,1,1,'2026-10-20 14:42:08','2026-10-20 14:42:08',NULL),(61,0,'出勤审核（申诉/补签终审）','attendance:audit',3,NULL,NULL,NULL,0,NULL,NULL,1,903,1,1,'2026-10-20 19:08:01','2026-10-20 19:08:01',NULL),(62,10,'入职开通账号','user:onboard',3,NULL,NULL,NULL,1,NULL,NULL,1,70,1,1,'2026-10-20 19:33:31','2026-10-20 19:33:31',NULL),(63,10,'离职交接','user:transfer',3,NULL,NULL,NULL,1,NULL,NULL,1,72,1,1,'2026-10-20 19:33:31','2026-10-20 19:33:31',NULL),(64,18,'日志查看（本部门）','worklog:view-dept',3,NULL,NULL,NULL,1,NULL,NULL,1,6,1,1,'2026-10-21 14:50:17','2026-10-21 14:50:17',NULL),(65,0,'出勤查看（本部门）','attendance:view-dept',3,NULL,NULL,NULL,0,NULL,NULL,1,905,1,1,'2026-10-21 14:50:17','2026-10-21 14:50:17',NULL),(66,0,'项目管理','project',2,'/home/project',NULL,'Folder',1,NULL,NULL,1,3,1,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(67,66,'项目列表','project:list',3,'/home/project',NULL,NULL,1,NULL,NULL,1,1,1,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(68,66,'新建项目','project:add',3,NULL,NULL,NULL,0,NULL,NULL,1,2,1,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(69,66,'编辑项目','project:edit',3,NULL,NULL,NULL,0,NULL,NULL,1,3,1,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(70,66,'删除项目','project:delete',3,NULL,NULL,NULL,0,NULL,NULL,1,4,1,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(71,66,'项目成员管理','project:member:manage',3,NULL,NULL,NULL,0,NULL,NULL,1,5,1,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(72,66,'项目统计','project:stat',3,NULL,NULL,NULL,0,NULL,NULL,1,6,1,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(73,38,'创建任务','task:add',3,NULL,NULL,NULL,0,NULL,NULL,1,1,1,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(74,38,'分配与改派','task:assign',3,NULL,NULL,NULL,0,NULL,NULL,1,2,1,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(75,38,'任务验收','task:accept-check',3,NULL,NULL,NULL,0,NULL,NULL,1,3,1,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(76,38,'任务看板','task:board',3,NULL,NULL,NULL,0,NULL,NULL,1,4,1,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(77,38,'任务统计','task:stat',3,NULL,NULL,NULL,0,NULL,NULL,1,5,1,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(78,38,'添加任务成员','task:member:add',3,NULL,NULL,NULL,0,NULL,NULL,1,6,1,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(79,38,'移除任务成员','task:member:remove',3,NULL,NULL,NULL,0,NULL,NULL,1,7,1,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(80,38,'任务查看（本部门）','task:view-dept',3,NULL,NULL,NULL,0,NULL,NULL,1,8,1,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(81,38,'接受任务','task:accept',3,NULL,NULL,NULL,0,NULL,NULL,1,9,1,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(82,38,'更新任务','task:update',3,NULL,NULL,NULL,0,NULL,NULL,1,10,1,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(83,38,'提交任务','task:submit',3,NULL,NULL,NULL,0,NULL,NULL,1,11,1,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(84,38,'任务评论','task:comment',3,NULL,NULL,NULL,0,NULL,NULL,1,12,1,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(85,28,'项目类流程审批','flow:approve-project',3,NULL,NULL,NULL,0,NULL,NULL,1,7,1,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(86,38,'任务中心访问','task:access',3,NULL,NULL,NULL,0,NULL,NULL,1,0,1,1,'2026-10-21 16:09:47','2026-10-21 16:09:47',NULL),(87,38,'发放任务权限','task:grant',3,NULL,NULL,NULL,0,NULL,NULL,1,13,1,1,'2026-10-21 16:09:47','2026-10-21 16:09:47',NULL),(88,18,'撰写工作日志','worklog:add',3,NULL,NULL,NULL,0,NULL,NULL,1,1,1,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(89,18,'修改当天日志','worklog:edit',3,NULL,NULL,NULL,0,NULL,NULL,1,2,1,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(90,18,'删除日志','worklog:delete',3,NULL,NULL,NULL,0,NULL,NULL,1,3,1,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(91,18,'日志评论','worklog:comment',3,NULL,NULL,NULL,0,NULL,NULL,1,4,1,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(92,18,'日志补写申请','worklog:makeup',3,NULL,NULL,NULL,0,NULL,NULL,1,5,1,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(93,18,'查看被授权日志','worklog:view-auth',3,NULL,NULL,NULL,0,NULL,NULL,1,6,1,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(94,28,'撤回流程','flow:withdraw',3,NULL,NULL,NULL,0,NULL,NULL,1,10,1,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(95,28,'催办流程','flow:urge',3,NULL,NULL,NULL,0,NULL,NULL,1,11,1,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(96,0,'上班签到','attendance:check-in',3,NULL,NULL,NULL,0,NULL,NULL,1,910,1,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(97,0,'下班签退','attendance:check-out',3,NULL,NULL,NULL,0,NULL,NULL,1,911,1,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(98,0,'查看本人出勤','attendance:view-own',3,NULL,NULL,NULL,0,NULL,NULL,1,912,1,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(99,0,'补签申请','attendance:makeup:apply',3,NULL,NULL,NULL,0,NULL,NULL,1,913,1,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(100,0,'出勤异常申诉','attendance:appeal:apply',3,NULL,NULL,NULL,0,NULL,NULL,1,914,1,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(101,0,'外出打卡','attendance:out',3,NULL,NULL,NULL,0,NULL,NULL,1,915,1,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(102,0,'维护个人资料','user:profile:edit',3,NULL,NULL,NULL,0,NULL,NULL,1,920,1,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(103,0,'修改密码','user:change-pwd',3,NULL,NULL,NULL,0,NULL,NULL,1,921,1,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(104,0,'查看系统通知','notify:view',3,NULL,NULL,NULL,0,NULL,NULL,1,922,1,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(105,0,'我的待办汇总','todo:view',3,NULL,NULL,NULL,0,NULL,NULL,1,923,1,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(106,0,'在线用户基础列表','online:view',3,NULL,NULL,NULL,0,NULL,NULL,1,924,1,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(107,0,'权限管理','role:manage',2,'/home/system/role','system/role/index','Lock',1,NULL,NULL,1,95,1,1,'2026-10-23 18:54:26','2026-10-23 18:54:26',NULL),(108,0,'系统管理','system:param',2,'/home/system/manage','system/manage/index','Setting',1,NULL,NULL,1,96,1,1,'2026-10-23 18:54:26','2026-10-23 18:54:26',NULL),(109,107,'分配角色','role:assign',3,NULL,NULL,NULL,0,NULL,NULL,1,951,1,1,'2026-10-23 18:54:26','2026-10-23 18:54:26',NULL),(110,108,'菜单与权限码','system:menu',3,NULL,NULL,NULL,0,NULL,NULL,1,961,1,1,'2026-10-23 18:54:26','2026-10-23 18:54:26',NULL),(111,108,'操作日志查看','log:operation:view',3,NULL,NULL,NULL,0,NULL,NULL,1,962,1,1,'2026-10-23 18:54:26','2026-10-23 18:54:26',NULL),(112,108,'异常日志查看','log:exception:view',3,NULL,NULL,NULL,0,NULL,NULL,1,963,1,1,'2026-10-23 18:54:26','2026-10-23 18:54:26',NULL),(113,108,'通知模板维护','notify:template:manage',3,NULL,NULL,NULL,0,NULL,NULL,1,964,1,1,'2026-10-23 18:54:26','2026-10-23 18:54:26',NULL),(114,108,'推送记录查看','notify:log:view',3,NULL,NULL,NULL,0,NULL,NULL,1,965,1,1,'2026-10-23 18:54:26','2026-10-23 18:54:26',NULL),(115,0,'公开公告查看','announcement:view-public',3,NULL,NULL,NULL,0,NULL,NULL,1,971,1,1,'2026-10-23 18:54:26','2026-10-23 18:54:26',NULL),(116,11,'查看完整联系方式','user:sensitive:view',3,NULL,NULL,NULL,0,NULL,NULL,1,21,1,1,'2026-10-23 19:21:51','2026-10-23 19:21:51',NULL),(117,38,'删除任务','task:delete',3,NULL,NULL,NULL,0,NULL,NULL,1,60,1,1,'2026-10-23 19:53:06','2026-10-23 19:53:06',NULL),(118,28,'设置流程使用权限','flow:perm:grant',3,NULL,NULL,NULL,0,NULL,NULL,1,70,1,1,'2026-10-23 19:53:06','2026-10-23 19:53:06',NULL),(119,11,'查看薪资','user:salary:view',3,NULL,NULL,NULL,0,NULL,NULL,1,22,1,1,'2026-10-24 11:52:37','2026-10-24 11:52:37',NULL);
/*!40000 ALTER TABLE `menus` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `nav_images`
--

DROP TABLE IF EXISTS `nav_images`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `nav_images` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `title` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '图片标题（alt文本）',
  `image_url` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '图片URL',
  `link_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '点击跳转链接',
  `position` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'banner' COMMENT '展示位置：banner首页轮播, nav导航栏, sidebar侧边栏',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0停用 1启用',
  `start_time` datetime DEFAULT NULL COMMENT '展示开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '展示结束时间',
  `click_count` int NOT NULL DEFAULT '0' COMMENT '点击次数',
  `uploader_id` bigint NOT NULL COMMENT '上传人ID',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间（软删除）',
  PRIMARY KEY (`id`),
  KEY `idx_nav_images_status_sort` (`status`,`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='导航图片管理表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `nav_images`
--

LOCK TABLES `nav_images` WRITE;
/*!40000 ALTER TABLE `nav_images` DISABLE KEYS */;
/*!40000 ALTER TABLE `nav_images` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `notification_records`
--

DROP TABLE IF EXISTS `notification_records`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification_records` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `user_id` bigint NOT NULL COMMENT '接收用户ID',
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '通知标题',
  `content` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '通知内容',
  `type` tinyint NOT NULL DEFAULT '1' COMMENT '通知类型：1系统通知 2审批待办 3任务通知 4出勤提醒 5日志提醒',
  `biz_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '关联业务类型（如 flow_instance, task）',
  `biz_id` bigint DEFAULT NULL COMMENT '关联业务ID',
  `jump_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '跳转链接',
  `is_read` tinyint NOT NULL DEFAULT '0' COMMENT '是否已读：0未读 1已读',
  `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
  `push_status` tinyint NOT NULL DEFAULT '0' COMMENT '推送状态：0未推送 1推送成功 2推送失败',
  `push_time` datetime DEFAULT NULL COMMENT '推送时间',
  `push_channel` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '推送渠道：wechat_work, dingtalk, email',
  `push_fail_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '推送失败原因',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间（软删除）',
  `is_top` tinyint DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_notification_user_read` (`user_id`,`is_read`),
  KEY `idx_notification_cleanup` (`is_read`,`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=602 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统通知表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notification_records`
--

LOCK TABLES `notification_records` WRITE;
/*!40000 ALTER TABLE `notification_records` DISABLE KEYS */;
INSERT INTO `notification_records` VALUES (3,5,'新的待办流程：张三-因事请假','您有一条新的待办流程：张三-因事请假',2,'flow_instance',4,'/home/flow-todo',1,'2026-10-20 15:55:56',0,NULL,NULL,NULL,'2026-09-29 17:54:39','2026-10-20 15:55:56',NULL,0),(4,5,'催办提醒：张三-因事请假','发起人催办您审批的流程：张三-因事请假',2,'flow_instance',4,'/home/flow-todo',1,'2026-10-20 15:55:56',0,NULL,NULL,NULL,'2026-09-29 18:16:09','2026-10-20 15:55:56',NULL,0),(7,6,'新的待办流程：赵六-报销','您有一条新的待办流程：赵六-报销',2,'flow_instance',5,'/home/flow-todo',0,NULL,0,NULL,NULL,NULL,'2026-09-29 18:57:50','2026-09-30 16:09:58','2026-09-30 16:09:58',0),(8,4,'流程被驳回：赵六-报销','您发起的流程被驳回：你自己出我们是黑公司',2,'flow_instance',5,'/home/flow-mine',1,'2026-09-30 15:40:20',0,NULL,NULL,NULL,'2026-09-29 19:10:13','2026-09-30 15:40:19',NULL,0),(9,6,'新的待办流程：赵六-报销','您有一条新的待办流程：赵六-报销',2,'flow_instance',6,'/home/flow-todo',1,'2026-09-30 16:07:15',0,NULL,NULL,NULL,'2026-09-30 15:40:12','2026-09-30 16:07:14',NULL,0),(10,6,'催办提醒：赵六-报销','发起人催办您审批的流程：赵六-报销',2,'flow_instance',6,'/home/flow-todo',1,'2026-09-30 16:07:15',0,NULL,NULL,NULL,'2026-09-30 15:40:16','2026-09-30 16:07:14',NULL,0),(11,6,'催办提醒：赵六-报销','发起人催办您审批的流程：赵六-报销',2,'flow_instance',6,'/home/flow-todo',1,'2026-09-30 16:07:15',0,NULL,NULL,NULL,'2026-09-30 15:40:32','2026-09-30 16:07:14',NULL,0),(12,6,'新的待办流程：赵六-报销','您有一条新的待办流程：赵六-报销',2,'flow_instance',7,'/home/flow-todo',1,'2026-09-30 17:05:48',0,NULL,NULL,NULL,'2026-09-30 16:55:11','2026-09-30 17:05:47',NULL,0),(13,6,'催办提醒：赵六-报销','发起人催办您审批的流程：赵六-报销',2,'flow_instance',7,'/home/flow-todo',1,'2026-09-30 17:05:48',0,NULL,NULL,NULL,'2026-09-30 16:55:16','2026-09-30 17:05:47',NULL,0),(16,6,'新的待办流程：张三-巨额资金报销','您有一条新的待办流程：张三-巨额资金报销',2,'flow_instance',9,'/home/flow-todo',1,'2026-09-30 18:06:03',0,NULL,NULL,NULL,'2026-09-30 18:05:41','2026-09-30 18:06:03',NULL,0),(17,1,'流程已完成：张三-巨额资金报销','您发起的流程已审批完成',2,'flow_instance',9,'/home/flow-mine',1,'2026-09-30 18:37:16',0,NULL,NULL,NULL,'2026-09-30 18:06:36','2026-09-30 18:37:16',NULL,0),(25,1,'新的补签申请待审批','王主管 提交了 2026-10-03 的签+退申请，请审批',2,'attendance_makeup',11,'/home/attendance',1,'2026-10-04 11:01:49',0,NULL,NULL,NULL,'2026-10-03 16:12:24','2026-10-04 11:01:48',NULL,0),(26,5,'新的补签申请待审批','王主管 提交了 2026-10-03 的签+退申请，请审批',2,'attendance_makeup',11,'/home/attendance',1,'2026-10-20 15:55:55',0,NULL,NULL,NULL,'2026-10-03 16:12:24','2026-10-20 15:55:54',NULL,0),(27,1,'新的补签申请待审批','王主管 提交了 2026-10-03 的签+退申请，请审批',2,'attendance_makeup',12,'/home/attendance',1,'2026-10-04 11:01:46',0,NULL,NULL,NULL,'2026-10-03 16:12:28','2026-10-04 11:01:45',NULL,0),(28,5,'新的补签申请待审批','王主管 提交了 2026-10-03 的签+退申请，请审批',2,'attendance_makeup',12,'/home/attendance',1,'2026-10-20 15:55:55',0,NULL,NULL,NULL,'2026-10-03 16:12:28','2026-10-20 15:55:54',NULL,0),(51,3,'出勤申诉已通过','您 2026-10-05 的出勤申诉已审批通过，出勤状态已修正为正常',2,'attendance_appeal',10,'/home/attendance',1,'2026-10-09 15:10:26',0,NULL,NULL,NULL,'2026-10-05 16:33:34','2026-10-09 15:10:25',NULL,0),(57,1,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',1,'2026-10-09 14:36:16',0,NULL,NULL,NULL,'2026-10-08 17:00:00','2026-10-09 14:36:15',NULL,0),(58,2,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',0,NULL,0,NULL,NULL,NULL,'2026-10-08 17:00:00','2026-10-08 17:00:00',NULL,0),(59,3,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',1,'2026-10-09 15:10:26',0,NULL,NULL,NULL,'2026-10-08 17:00:00','2026-10-09 15:10:25',NULL,0),(60,4,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',1,'2026-10-09 15:05:01',0,NULL,NULL,NULL,'2026-10-08 17:00:00','2026-10-09 15:05:01',NULL,0),(61,5,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',1,'2026-10-20 15:55:55',0,NULL,NULL,NULL,'2026-10-08 17:00:00','2026-10-20 15:55:54',NULL,0),(62,6,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',1,'2026-10-19 15:51:53',0,NULL,NULL,NULL,'2026-10-08 17:00:00','2026-10-19 15:51:52',NULL,0),(66,2,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',0,NULL,0,NULL,NULL,NULL,'2026-10-09 17:00:00','2026-10-09 17:00:00',NULL,0),(67,3,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',1,'2026-10-19 10:25:46',0,NULL,NULL,NULL,'2026-10-09 17:00:00','2026-10-19 10:25:46',NULL,0),(68,5,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',1,'2026-10-20 15:55:54',0,NULL,NULL,NULL,'2026-10-09 17:00:00','2026-10-20 15:55:54',NULL,0),(69,6,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',1,'2026-10-19 15:51:52',0,NULL,NULL,NULL,'2026-10-09 17:00:00','2026-10-19 15:51:52',NULL,0),(70,1,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',1,'2026-10-13 18:12:56',0,NULL,NULL,NULL,'2026-10-12 17:00:01','2026-10-13 18:12:56',NULL,0),(71,2,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',0,NULL,0,NULL,NULL,NULL,'2026-10-12 17:00:01','2026-10-12 17:00:01',NULL,0),(72,3,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',1,'2026-10-19 10:25:45',0,NULL,NULL,NULL,'2026-10-12 17:00:01','2026-10-19 10:25:45',NULL,0),(73,4,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',0,NULL,0,NULL,NULL,NULL,'2026-10-12 17:00:01','2026-10-12 17:00:01',NULL,0),(74,5,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',1,'2026-10-20 15:55:54',0,NULL,NULL,NULL,'2026-10-12 17:00:01','2026-10-20 15:55:54',NULL,0),(75,6,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',1,'2026-10-19 15:51:52',0,NULL,NULL,NULL,'2026-10-12 17:00:01','2026-10-19 15:51:52',NULL,0),(105,1,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',1,'2026-10-13 18:12:56',0,NULL,NULL,NULL,'2026-10-13 17:00:00','2026-10-13 18:12:56',NULL,0),(106,2,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',0,NULL,0,NULL,NULL,NULL,'2026-10-13 17:00:00','2026-10-13 17:00:00',NULL,0),(107,3,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',1,'2026-10-19 10:25:45',0,NULL,NULL,NULL,'2026-10-13 17:00:00','2026-10-19 10:25:44',NULL,0),(108,4,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',0,NULL,0,NULL,NULL,NULL,'2026-10-13 17:00:00','2026-10-13 17:00:00',NULL,0),(109,5,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',1,'2026-10-20 15:55:54',0,NULL,NULL,NULL,'2026-10-13 17:00:00','2026-10-20 15:55:53',NULL,0),(110,6,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',1,'2026-10-19 15:51:52',0,NULL,NULL,NULL,'2026-10-13 17:00:00','2026-10-19 15:51:51',NULL,0),(112,4,'任务通知','您有新任务：P2??-????',2,'task',NULL,'/home/task?type=assigned',0,NULL,0,NULL,NULL,NULL,'2026-10-13 20:24:42','2026-10-13 20:24:41',NULL,0),(113,1,'任务通知','任务待验收：P2??-????',2,'task',NULL,'/home/task?type=created',1,'2026-10-19 10:14:55',0,NULL,NULL,NULL,'2026-10-13 20:24:42','2026-10-19 10:14:54',NULL,0),(114,4,'任务通知','任务已验收通过：P2??-????',2,'task',NULL,'/home/task?type=assigned',0,NULL,0,NULL,NULL,NULL,'2026-10-13 20:24:42','2026-10-13 20:24:41',NULL,0),(115,4,'任务通知','您有新任务：P2??-????-????',2,'task',NULL,'/home/task?type=assigned',0,NULL,0,NULL,NULL,NULL,'2026-10-13 20:24:42','2026-10-13 20:24:42',NULL,0),(116,4,'任务通知','任务已撤销：P2??-????-????',2,'task',NULL,'/home/task?type=assigned',0,NULL,0,NULL,NULL,NULL,'2026-10-13 20:24:42','2026-10-13 20:24:42',NULL,0),(117,4,'任务通知','任务「P2??-????-????」已恢复，待您接受',2,'task',NULL,'/home/task?type=assigned',0,NULL,0,NULL,NULL,NULL,'2026-10-13 20:24:43','2026-10-13 20:24:42',NULL,0),(118,4,'任务通知','您有新任务：P2??-????',2,'task',NULL,'/home/task?type=assigned',0,NULL,0,NULL,NULL,NULL,'2026-10-13 20:28:13','2026-10-13 20:28:13',NULL,0),(119,1,'任务通知','任务待验收：P2??-????',2,'task',NULL,'/home/task?type=created',1,'2026-10-19 10:14:54',0,NULL,NULL,NULL,'2026-10-13 20:28:13','2026-10-19 10:14:54',NULL,0),(120,4,'任务通知','任务已验收通过：P2??-????',2,'task',NULL,'/home/task?type=assigned',0,NULL,0,NULL,NULL,NULL,'2026-10-13 20:28:13','2026-10-13 20:28:13',NULL,0),(121,4,'任务通知','您有新任务：P2??-????-????',2,'task',NULL,'/home/task?type=assigned',0,NULL,0,NULL,NULL,NULL,'2026-10-13 20:28:14','2026-10-13 20:28:14',NULL,0),(122,4,'任务通知','任务已撤销：P2??-????-????',2,'task',NULL,'/home/task?type=assigned',0,NULL,0,NULL,NULL,NULL,'2026-10-13 20:28:14','2026-10-13 20:28:14',NULL,0),(123,4,'任务通知','任务「P2??-????-????」已恢复，待您接受',2,'task',NULL,'/home/task?type=assigned',0,NULL,0,NULL,NULL,NULL,'2026-10-13 20:28:14','2026-10-13 20:28:14',NULL,0),(124,4,'任务通知','您有新任务：P2??-????',2,'task',NULL,'/home/task?type=assigned',0,NULL,0,NULL,NULL,NULL,'2026-10-13 20:29:41','2026-10-13 20:29:40',NULL,0),(125,1,'任务通知','任务待验收：P2??-????',2,'task',NULL,'/home/task?type=created',1,'2026-10-19 10:14:54',0,NULL,NULL,NULL,'2026-10-13 20:29:41','2026-10-19 10:14:53',NULL,0),(126,4,'任务通知','任务已验收通过：P2??-????',2,'task',NULL,'/home/task?type=assigned',0,NULL,0,NULL,NULL,NULL,'2026-10-13 20:29:41','2026-10-13 20:29:41',NULL,0),(127,4,'任务通知','您有新任务：P2??-????-????',2,'task',NULL,'/home/task?type=assigned',0,NULL,0,NULL,NULL,NULL,'2026-10-13 20:29:41','2026-10-13 20:29:41',NULL,0),(128,4,'任务通知','任务已撤销：P2??-????-????',2,'task',NULL,'/home/task?type=assigned',0,NULL,0,NULL,NULL,NULL,'2026-10-13 20:29:41','2026-10-13 20:29:41',NULL,0),(129,4,'任务通知','任务「P2??-????-????」已恢复，待您接受',2,'task',NULL,'/home/task?type=assigned',0,NULL,0,NULL,NULL,NULL,'2026-10-13 20:29:41','2026-10-13 20:29:41',NULL,0),(130,4,'任务通知','您有新任务：P2??-?????',2,'task',NULL,'/home/task?type=assigned',0,NULL,0,NULL,NULL,NULL,'2026-10-13 20:48:23','2026-10-13 20:48:23',NULL,0),(131,1,'任务通知','任务待验收：P2??-?????',2,'task',NULL,'/home/task?type=created',1,'2026-10-19 10:14:55',0,NULL,NULL,NULL,'2026-10-13 20:48:23','2026-10-19 10:14:55',NULL,0),(132,4,'任务通知','任务已验收通过：P2??-?????',2,'task',NULL,'/home/task?type=assigned',0,NULL,0,NULL,NULL,NULL,'2026-10-13 20:48:23','2026-10-13 20:48:23',NULL,0),(133,4,'任务通知','您有新任务：P2??-?????',2,'task',NULL,'/home/task?type=assigned',0,NULL,0,NULL,NULL,NULL,'2026-10-13 20:48:38','2026-10-13 20:48:37',NULL,0),(134,1,'任务通知','任务待验收：P2??-?????',2,'task',NULL,'/home/task?type=created',1,'2026-10-19 10:14:56',0,NULL,NULL,NULL,'2026-10-13 20:48:38','2026-10-19 10:14:55',NULL,0),(135,4,'任务通知','任务已验收通过：P2??-?????',2,'task',NULL,'/home/task?type=assigned',0,NULL,0,NULL,NULL,NULL,'2026-10-13 20:48:38','2026-10-13 20:48:38',NULL,0),(136,1,'任务通知','任务待验收：P2??-????',2,'task',NULL,'/home/task?type=created',1,'2026-10-19 10:14:56',0,NULL,NULL,NULL,'2026-10-13 20:49:18','2026-10-19 10:14:55',NULL,0),(137,4,'任务通知','您有新任务：UI????-???',2,'task',NULL,'/home/task?type=assigned',0,NULL,0,NULL,NULL,NULL,'2026-10-14 08:47:41','2026-10-14 08:47:40',NULL,0),(138,4,'任务通知','您有新任务：UI????-???',2,'task',NULL,'/home/task?type=assigned',0,NULL,0,NULL,NULL,NULL,'2026-10-14 08:47:41','2026-10-14 08:47:40',NULL,0),(139,4,'任务通知','您有新任务：UI????-???',2,'task',NULL,'/home/task?type=assigned',0,NULL,0,NULL,NULL,NULL,'2026-10-14 08:47:41','2026-10-14 08:47:40',NULL,0),(140,1,'任务通知','任务待验收：UI????-???',2,'task',NULL,'/home/task?type=created',1,'2026-10-19 10:14:56',0,NULL,NULL,NULL,'2026-10-14 08:47:41','2026-10-19 10:14:56',NULL,0),(141,4,'任务通知','任务已验收通过：UI????-???',2,'task',NULL,'/home/task?type=assigned',0,NULL,0,NULL,NULL,NULL,'2026-10-14 08:54:29','2026-10-14 08:54:29',NULL,0),(142,4,'任务提醒','今日待办：待接受 1 项，进行中 1 项，待验收 0 项',3,'task',NULL,'/home/task?type=assigned',0,NULL,0,NULL,NULL,NULL,'2026-10-14 09:00:00','2026-10-14 09:00:00',NULL,0),(143,4,'任务通知','您有新任务：ROLETEST-assigned-to-employee01',2,'task',NULL,'/home/task?type=assigned',0,NULL,0,NULL,NULL,NULL,'2026-10-14 09:44:52','2026-10-14 09:44:52',NULL,0),(144,5,'任务通知','您有新任务：ROLETEST-assigned-to-hr01',2,'task',NULL,'/home/task?type=assigned',1,'2026-10-20 15:55:54',0,NULL,NULL,NULL,'2026-10-14 09:44:52','2026-10-20 15:55:53',NULL,0),(145,6,'任务通知','您有新任务：ROLETEST-assigned-to-finance01',2,'task',NULL,'/home/task?type=assigned',1,'2026-10-19 15:51:51',0,NULL,NULL,NULL,'2026-10-14 09:44:52','2026-10-19 15:51:51',NULL,0),(146,1,'任务通知','任务被认领：ROLETEST-pool-unassigned',2,'task',NULL,'/home/task?type=created',1,'2026-10-19 10:14:56',0,NULL,NULL,NULL,'2026-10-14 09:46:59','2026-10-19 10:14:56',NULL,0),(147,1,'任务通知','您有新任务：ROLETEST-employee-creates-assign-admin',2,'task',NULL,'/home/task?type=assigned',1,'2026-10-19 10:14:56',0,NULL,NULL,NULL,'2026-10-14 09:47:00','2026-10-19 10:14:56',NULL,0),(148,1,'任务通知','任务待验收：ROLETEST-assigned-to-employee01',2,'task',NULL,'/home/task?type=created',1,'2026-10-19 10:14:57',0,NULL,NULL,NULL,'2026-10-14 09:48:00','2026-10-19 10:14:57',NULL,0),(149,4,'任务通知','任务已验收通过：ROLETEST-assigned-to-employee01',2,'task',NULL,'/home/task?type=assigned',0,NULL,0,NULL,NULL,NULL,'2026-10-14 09:48:00','2026-10-14 09:48:00',NULL,0),(150,4,'任务通知','任务待验收：ROLETEST-employee-creates-assign-admin',2,'task',NULL,'/home/task?type=created',0,NULL,0,NULL,NULL,NULL,'2026-10-14 09:48:00','2026-10-14 09:48:00',NULL,0),(151,1,'任务通知','任务已验收通过：ROLETEST-employee-creates-assign-admin',2,'task',NULL,'/home/task?type=assigned',1,'2026-10-19 10:14:57',0,NULL,NULL,NULL,'2026-10-14 09:48:00','2026-10-19 10:14:56',NULL,0),(152,1,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',1,'2026-10-19 10:14:58',0,NULL,NULL,NULL,'2026-10-14 17:00:00','2026-10-19 10:14:57',NULL,0),(153,2,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',0,NULL,0,NULL,NULL,NULL,'2026-10-14 17:00:00','2026-10-14 17:00:00',NULL,0),(154,3,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',1,'2026-10-19 10:25:43',0,NULL,NULL,NULL,'2026-10-14 17:00:00','2026-10-19 10:25:43',NULL,0),(155,4,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',0,NULL,0,NULL,NULL,NULL,'2026-10-14 17:00:00','2026-10-14 17:00:00',NULL,0),(156,5,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',1,'2026-10-20 15:55:53',0,NULL,NULL,NULL,'2026-10-14 17:00:00','2026-10-20 15:55:53',NULL,0),(157,6,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',1,'2026-10-19 15:51:51',0,NULL,NULL,NULL,'2026-10-14 17:00:00','2026-10-19 15:51:51',NULL,0),(158,4,'任务通知','您有新任务：gm????-????',2,'task',NULL,'/home/task?type=assigned',0,NULL,0,NULL,NULL,NULL,'2026-10-15 11:29:03','2026-10-15 11:29:03',NULL,0),(159,6,'新的待办流程：李员工的大额差旅报销（2000元）','您有一条新的待办流程：李员工的大额差旅报销（2000元）',2,'flow_instance',13,'/home/flow-todo',1,'2026-10-15 16:15:20',0,NULL,NULL,NULL,'2026-10-15 16:15:20','2026-10-15 16:15:20',NULL,0),(160,4,'流程已完成：李员工的大额差旅报销（2000元）','您发起的流程已审批完成',2,'flow_instance',13,'/home/flow-mine',0,NULL,0,NULL,NULL,NULL,'2026-10-15 16:15:20','2026-10-15 16:15:20',NULL,0),(161,1,'新的补签申请待审批','李员工 提交了 2026-10-14 的签+退申请，请审批',2,'attendance_makeup',17,'/home/attendance',1,'2026-10-19 10:14:59',0,NULL,NULL,NULL,'2026-10-16 10:46:06','2026-10-19 10:14:58',NULL,0),(162,3,'新的补签申请待审批','李员工 提交了 2026-10-14 的签+退申请，请审批',2,'attendance_makeup',17,'/home/attendance',1,'2026-10-19 10:25:36',0,NULL,NULL,NULL,'2026-10-16 10:46:06','2026-10-19 10:25:35',NULL,0),(163,4,'任务通知','您有新任务：市场部季度数据整理',2,'task',NULL,'/home/task?type=assigned',0,NULL,0,NULL,NULL,NULL,'2026-10-16 10:49:11','2026-10-16 10:49:10',NULL,0),(164,1,'补签申请待人事终审','李员工 的 2026-10-14 补签申请已通过部门主管初审，请人事终审',2,'attendance_makeup',17,'/home/attendance',1,'2026-10-19 10:14:59',0,NULL,NULL,NULL,'2026-10-16 10:50:29','2026-10-19 10:14:58',NULL,0),(165,5,'补签申请待人事终审','李员工 的 2026-10-14 补签申请已通过部门主管初审，请人事终审',2,'attendance_makeup',17,'/home/attendance',1,'2026-10-20 15:55:53',0,NULL,NULL,NULL,'2026-10-16 10:50:29','2026-10-20 15:55:52',NULL,0),(166,4,'补签主管初审通过','您 2026-10-14 的补签申请已通过部门主管初审，等待人事终审',2,'attendance_makeup',17,'/home/attendance',0,NULL,0,NULL,NULL,NULL,'2026-10-16 10:50:29','2026-10-16 10:50:29',NULL,0),(167,4,'补签申请已通过','您 2026-10-14 的补签申请已审批通过',2,'attendance_makeup',17,'/home/attendance',0,NULL,0,NULL,NULL,NULL,'2026-10-16 10:53:49','2026-10-16 10:53:49',NULL,0),(179,3,'新的待办流程：雷先生-器材采购','新的待办流程：雷先生-器材采购，请及时处理',2,'flow_instance',17,'/home/flow-todo',1,'2026-10-19 11:24:21',0,NULL,NULL,NULL,'2026-10-19 11:23:54','2026-10-19 11:24:21',NULL,0),(180,3,'催办提醒：雷先生-器材采购','发起人催办您审批的流程：雷先生-器材采购',2,'flow_instance',17,'/home/flow-todo',1,'2026-10-19 11:24:21',0,NULL,NULL,NULL,'2026-10-19 11:24:15','2026-10-19 11:24:21',NULL,0),(181,9,'流程被驳回：雷先生-器材采购','您发起的流程被驳回：不行，拆公司是违法的行为。',2,'flow_instance',17,'/home/flow-mine',1,'2026-10-21 17:10:34',0,NULL,NULL,NULL,'2026-10-19 11:25:51','2026-10-21 17:10:33',NULL,0),(199,1,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',1,'2026-10-20 11:15:28',0,NULL,NULL,NULL,'2026-10-19 17:00:00','2026-10-20 11:15:28',NULL,0),(200,2,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',0,NULL,0,NULL,NULL,NULL,'2026-10-19 17:00:00','2026-10-19 17:00:00',NULL,0),(201,3,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',0,NULL,0,NULL,NULL,NULL,'2026-10-19 17:00:00','2026-10-19 17:00:00',NULL,0),(202,4,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',0,NULL,0,NULL,NULL,NULL,'2026-10-19 17:00:00','2026-10-19 17:00:00',NULL,0),(203,5,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',1,'2026-10-20 15:55:53',0,NULL,NULL,NULL,'2026-10-19 17:00:00','2026-10-20 15:55:52',NULL,0),(204,6,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',1,'2026-10-20 14:12:37',0,NULL,NULL,NULL,'2026-10-19 17:00:00','2026-10-20 14:12:36',NULL,0),(205,7,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',0,NULL,0,NULL,NULL,NULL,'2026-10-19 17:00:00','2026-10-19 17:00:00',NULL,0),(206,8,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',0,NULL,0,NULL,NULL,NULL,'2026-10-19 17:00:00','2026-10-19 17:00:00',NULL,0),(207,9,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',1,'2026-10-21 17:10:33',0,NULL,NULL,NULL,'2026-10-19 17:00:00','2026-10-21 17:10:33',NULL,0),(208,1,'忘记签退提醒','系统管理员，您今天尚未签退，请在24:00前补签退（补签退不占用补签次数）',2,'attendance_remind',NULL,'/home/attendance',1,'2026-10-20 11:15:28',0,NULL,NULL,NULL,'2026-10-19 23:00:00','2026-10-20 11:15:27',NULL,0),(209,3,'忘记签退提醒','王主管，您今天尚未签退，请在24:00前补签退（补签退不占用补签次数）',2,'attendance_remind',NULL,'/home/attendance',0,NULL,0,NULL,NULL,NULL,'2026-10-19 23:00:00','2026-10-19 23:00:00',NULL,0),(210,2,'上班签到提醒','张三，上班前15分钟了，请及时签到',2,'attendance_remind',NULL,'/home/attendance',0,NULL,0,NULL,NULL,NULL,'2026-10-19 23:45:00','2026-10-19 23:45:00',NULL,0),(211,4,'上班签到提醒','李员工，上班前15分钟了，请及时签到',2,'attendance_remind',NULL,'/home/attendance',0,NULL,0,NULL,NULL,NULL,'2026-10-19 23:45:00','2026-10-19 23:45:00',NULL,0),(212,5,'上班签到提醒','赵专员，上班前15分钟了，请及时签到',2,'attendance_remind',NULL,'/home/attendance',1,'2026-10-20 15:55:52',0,NULL,NULL,NULL,'2026-10-19 23:45:00','2026-10-20 15:55:51',NULL,0),(213,6,'上班签到提醒','钱专员，上班前15分钟了，请及时签到',2,'attendance_remind',NULL,'/home/attendance',1,'2026-10-20 14:12:36',0,NULL,NULL,NULL,'2026-10-19 23:45:00','2026-10-20 14:12:36',NULL,0),(214,7,'上班签到提醒','陈总经理，上班前15分钟了，请及时签到',2,'attendance_remind',NULL,'/home/attendance',0,NULL,0,NULL,NULL,NULL,'2026-10-19 23:45:00','2026-10-19 23:45:00',NULL,0),(215,8,'上班签到提醒','赵总经理，上班前15分钟了，请及时签到',2,'attendance_remind',NULL,'/home/attendance',0,NULL,0,NULL,NULL,NULL,'2026-10-19 23:45:00','2026-10-19 23:45:00',NULL,0),(216,9,'上班签到提醒','雷先生，上班前15分钟了，请及时签到',2,'attendance_remind',NULL,'/home/attendance',1,'2026-10-21 17:10:33',0,NULL,NULL,NULL,'2026-10-19 23:45:00','2026-10-21 17:10:33',NULL,0),(217,4,'任务提醒','今日待办：待接受 1 项，进行中 0 项，待验收 0 项',3,'task',NULL,'/home/task?type=assigned',0,NULL,0,NULL,NULL,NULL,'2026-10-20 08:59:55','2026-10-20 08:59:55',NULL,0),(218,1,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',1,'2026-10-21 14:34:10',0,NULL,NULL,NULL,'2026-10-20 17:00:00','2026-10-21 14:34:09',NULL,0),(219,2,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',0,NULL,0,NULL,NULL,NULL,'2026-10-20 17:00:00','2026-10-20 17:00:00',NULL,0),(220,3,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',0,NULL,0,NULL,NULL,NULL,'2026-10-20 17:00:00','2026-10-20 17:00:00',NULL,0),(221,4,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',0,NULL,0,NULL,NULL,NULL,'2026-10-20 17:00:00','2026-10-20 17:00:00',NULL,0),(222,5,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',0,NULL,0,NULL,NULL,NULL,'2026-10-20 17:00:00','2026-10-20 17:00:00',NULL,0),(223,6,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',0,NULL,0,NULL,NULL,NULL,'2026-10-20 17:00:00','2026-10-20 17:00:00',NULL,0),(224,7,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',0,NULL,0,NULL,NULL,NULL,'2026-10-20 17:00:00','2026-10-20 17:00:00',NULL,0),(225,8,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',0,NULL,0,NULL,NULL,NULL,'2026-10-20 17:00:00','2026-10-20 17:00:00',NULL,0),(226,9,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',1,'2026-10-21 17:10:33',0,NULL,NULL,NULL,'2026-10-20 17:00:00','2026-10-21 17:10:32',NULL,0),(227,10,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',1,'2026-10-21 08:44:32',0,NULL,NULL,NULL,'2026-10-20 17:00:00','2026-10-21 08:44:32',NULL,0),(230,1,'流程已完成：张三-因事请假','您发起的流程已审批完成',2,'flow_instance',4,'/home/flow-mine',1,'2026-10-21 14:34:10',0,NULL,NULL,NULL,'2026-10-20 17:40:12','2026-10-21 14:34:09',NULL,0),(256,1,'补签申请待人事终审','赵总经理 的 2026-10-19 补签申请已通过部门主管初审，请人事终审',2,'attendance_makeup',18,'/home/attendance',1,'2026-10-21 14:34:10',0,NULL,NULL,NULL,'2026-10-20 19:46:30','2026-10-21 14:34:09',NULL,0),(257,10,'补签申请待人事终审','赵总经理 的 2026-10-19 补签申请已通过部门主管初审，请人事终审',2,'attendance_makeup',18,'/home/attendance',1,'2026-10-21 08:44:31',0,NULL,NULL,NULL,'2026-10-20 19:46:30','2026-10-21 08:44:31',NULL,0),(258,8,'补签主管初审通过','您 2026-10-19 的补签申请已通过部门主管初审，等待人事终审',2,'attendance_makeup',18,'/home/attendance',0,NULL,0,NULL,NULL,NULL,'2026-10-20 19:46:30','2026-10-20 19:46:30',NULL,0),(259,8,'补签申请已通过','您 2026-10-19 的补签申请已审批通过',2,'attendance_makeup',18,'/home/attendance',0,NULL,0,NULL,NULL,NULL,'2026-10-20 19:46:30','2026-10-20 19:46:30',NULL,0),(260,1,'上班签到提醒','系统管理员，上班前15分钟了，请及时签到',2,'attendance_remind',NULL,'/home/attendance',1,'2026-10-21 14:34:10',0,NULL,NULL,NULL,'2026-10-20 23:45:00','2026-10-21 14:34:09',NULL,0),(261,2,'上班签到提醒','张三，上班前15分钟了，请及时签到',2,'attendance_remind',NULL,'/home/attendance',0,NULL,0,NULL,NULL,NULL,'2026-10-20 23:45:00','2026-10-20 23:45:00',NULL,0),(262,3,'上班签到提醒','王主管，上班前15分钟了，请及时签到',2,'attendance_remind',NULL,'/home/attendance',0,NULL,0,NULL,NULL,NULL,'2026-10-20 23:45:00','2026-10-20 23:45:00',NULL,0),(263,4,'上班签到提醒','李员工，上班前15分钟了，请及时签到',2,'attendance_remind',NULL,'/home/attendance',0,NULL,0,NULL,NULL,NULL,'2026-10-20 23:45:00','2026-10-20 23:45:00',NULL,0),(264,5,'上班签到提醒','赵专员，上班前15分钟了，请及时签到',2,'attendance_remind',NULL,'/home/attendance',0,NULL,0,NULL,NULL,NULL,'2026-10-20 23:45:00','2026-10-20 23:45:00',NULL,0),(265,6,'上班签到提醒','钱专员，上班前15分钟了，请及时签到',2,'attendance_remind',NULL,'/home/attendance',0,NULL,0,NULL,NULL,NULL,'2026-10-20 23:45:00','2026-10-20 23:45:00',NULL,0),(266,7,'上班签到提醒','陈总经理，上班前15分钟了，请及时签到',2,'attendance_remind',NULL,'/home/attendance',0,NULL,0,NULL,NULL,NULL,'2026-10-20 23:45:00','2026-10-20 23:45:00',NULL,0),(267,8,'上班签到提醒','赵总经理，上班前15分钟了，请及时签到',2,'attendance_remind',NULL,'/home/attendance',0,NULL,0,NULL,NULL,NULL,'2026-10-20 23:45:00','2026-10-20 23:45:00',NULL,0),(268,9,'上班签到提醒','雷先生，上班前15分钟了，请及时签到',2,'attendance_remind',NULL,'/home/attendance',1,'2026-10-21 17:10:33',0,NULL,NULL,NULL,'2026-10-20 23:45:00','2026-10-21 17:10:32',NULL,0),(269,10,'上班签到提醒','陈主管，上班前15分钟了，请及时签到',2,'attendance_remind',NULL,'/home/attendance',1,'2026-10-21 08:44:31',0,NULL,NULL,NULL,'2026-10-20 23:45:00','2026-10-21 08:44:31',NULL,0),(276,4,'任务提醒','今日待办：待接受 1 项，进行中 0 项，待验收 0 项',3,'task',NULL,'/home/task?type=assigned',0,NULL,0,NULL,NULL,NULL,'2026-10-21 09:00:00','2026-10-21 09:00:00',NULL,0),(279,4,'任务通知','您有新任务：需求梳理与接口设计',2,'task',NULL,'/home/task?type=assigned',0,NULL,0,NULL,NULL,NULL,'2026-10-21 15:25:17','2026-10-21 15:25:17',NULL,0),(280,13,'任务通知','任务待验收：需求梳理与接口设计',2,'task',NULL,'/home/task?type=created',0,NULL,0,NULL,NULL,NULL,'2026-10-21 15:25:18','2026-10-21 15:25:17',NULL,0),(281,4,'任务通知','任务已验收通过：需求梳理与接口设计',2,'task',NULL,'/home/task?type=assigned',0,NULL,0,NULL,NULL,NULL,'2026-10-21 15:25:18','2026-10-21 15:25:17',NULL,0),(296,1,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',1,'2026-10-21 17:09:12',0,NULL,NULL,NULL,'2026-10-21 17:00:00','2026-10-21 17:09:12',NULL,0),(297,2,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',0,NULL,0,NULL,NULL,NULL,'2026-10-21 17:00:00','2026-10-21 17:00:00',NULL,0),(298,3,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',0,NULL,0,NULL,NULL,NULL,'2026-10-21 17:00:00','2026-10-21 17:00:00',NULL,0),(299,4,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',0,NULL,0,NULL,NULL,NULL,'2026-10-21 17:00:00','2026-10-21 17:00:00',NULL,0),(300,5,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',0,NULL,0,NULL,NULL,NULL,'2026-10-21 17:00:00','2026-10-21 17:00:00',NULL,0),(301,6,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',0,NULL,0,NULL,NULL,NULL,'2026-10-21 17:00:00','2026-10-21 17:00:00',NULL,0),(302,7,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',0,NULL,0,NULL,NULL,NULL,'2026-10-21 17:00:00','2026-10-21 17:00:00',NULL,0),(303,8,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',0,NULL,0,NULL,NULL,NULL,'2026-10-21 17:00:00','2026-10-21 17:00:00',NULL,0),(304,9,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',1,'2026-10-21 17:10:32',0,NULL,NULL,NULL,'2026-10-21 17:00:00','2026-10-21 17:10:32',NULL,0),(305,10,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',0,NULL,0,NULL,NULL,NULL,'2026-10-21 17:00:00','2026-10-21 17:00:00',NULL,0),(306,12,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',0,NULL,0,NULL,NULL,NULL,'2026-10-21 17:00:00','2026-10-21 17:00:00',NULL,0),(307,13,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',0,NULL,0,NULL,NULL,NULL,'2026-10-21 17:00:00','2026-10-21 17:00:00',NULL,0),(326,1,'任务通知','任务被认领：JdbcPro',2,'task',NULL,'/home/task?type=created',0,NULL,0,NULL,NULL,NULL,'2026-10-22 09:54:25','2026-10-22 09:54:25',NULL,0),(327,14,'任务通知','您有新任务：HANDOVER-TASK-A',2,'task',NULL,'/home/task?type=assigned',0,NULL,0,NULL,NULL,NULL,'2026-10-22 09:59:51','2026-10-22 09:59:50',NULL,0),(331,15,'任务通知','您有新任务：HANDOVER-TASK-A',2,'task',NULL,'/home/task?type=assigned',0,NULL,0,NULL,NULL,NULL,'2026-10-22 10:06:22','2026-10-22 10:06:21',NULL,0),(335,1,'任务通知','任务待验收：HANDOVER-TASK-A',2,'task',NULL,'/home/task?type=created',0,NULL,0,NULL,NULL,NULL,'2026-10-22 10:06:47','2026-10-22 10:06:46',NULL,0),(339,17,'项目通知','您被加入项目：审计交接项目',2,'project',NULL,'/home/project?id=8',0,NULL,0,NULL,NULL,NULL,'2026-10-22 10:25:53','2026-10-22 10:25:53',NULL,0),(340,17,'任务通知','您有新任务：交接前任务',2,'task',NULL,'/home/task?type=assigned',0,NULL,0,NULL,NULL,NULL,'2026-10-22 10:25:53','2026-10-22 10:25:53',NULL,0),(345,1,'新的补签申请待审批','市场部测试员 提交了 2026-10-21 的签+退申请，请审批',2,'attendance_makeup',19,'/home/attendance',0,NULL,0,NULL,NULL,NULL,'2026-10-22 10:43:37','2026-10-22 10:43:37',NULL,0),(346,3,'新的补签申请待审批','市场部测试员 提交了 2026-10-21 的签+退申请，请审批',2,'attendance_makeup',19,'/home/attendance',0,NULL,0,NULL,NULL,NULL,'2026-10-22 10:43:37','2026-10-22 10:43:37',NULL,0),(348,3,'任务通知','任务待验收：部门级任务（非项目）',2,'task',NULL,'/home/task?type=created',0,NULL,0,NULL,NULL,NULL,'2026-10-22 10:43:38','2026-10-22 10:43:37',NULL,0),(355,1,'补签申请待人事终审','市场部测试员 的 2026-10-21 补签申请已通过部门主管初审，请人事终审',2,'attendance_makeup',19,'/home/attendance',0,NULL,0,NULL,NULL,NULL,'2026-10-22 10:53:34','2026-10-22 10:53:34',NULL,0),(356,10,'补签申请待人事终审','市场部测试员 的 2026-10-21 补签申请已通过部门主管初审，请人事终审',2,'attendance_makeup',19,'/home/attendance',0,NULL,0,NULL,NULL,NULL,'2026-10-22 10:53:34','2026-10-22 10:53:34',NULL,0),(358,19,'项目通知','您被加入项目：JDBC PRO',2,'project',NULL,'/home/project?id=6',0,NULL,0,NULL,NULL,NULL,'2026-10-22 11:12:19','2026-10-22 11:12:19',NULL,0),(359,19,'任务通知','您有新任务：审计-交接测试任务',2,'task',NULL,'/home/task?type=assigned',0,NULL,0,NULL,NULL,NULL,'2026-10-22 11:12:19','2026-10-22 11:12:19',NULL,0),(364,13,'任务通知','任务待验收：审计-交接测试任务',2,'task',NULL,'/home/task?type=created',0,NULL,0,NULL,NULL,NULL,'2026-10-22 11:12:20','2026-10-22 11:12:20',NULL,0),(365,4,'任务通知','任务已验收通过：审计-交接测试任务',2,'task',NULL,'/home/task?type=assigned',0,NULL,0,NULL,NULL,NULL,'2026-10-22 11:12:21','2026-10-22 11:12:20',NULL,0),(479,1,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',0,NULL,0,NULL,NULL,NULL,'2026-10-22 18:07:59','2026-10-22 18:07:59',NULL,0),(480,2,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',0,NULL,0,NULL,NULL,NULL,'2026-10-22 18:07:59','2026-10-22 18:07:59',NULL,0),(481,3,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',0,NULL,0,NULL,NULL,NULL,'2026-10-22 18:07:59','2026-10-22 18:07:59',NULL,0),(482,4,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',0,NULL,0,NULL,NULL,NULL,'2026-10-22 18:07:59','2026-10-22 18:07:59',NULL,0),(483,5,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',0,NULL,0,NULL,NULL,NULL,'2026-10-22 18:07:59','2026-10-22 18:07:59',NULL,0),(484,6,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',0,NULL,0,NULL,NULL,NULL,'2026-10-22 18:07:59','2026-10-22 18:07:59',NULL,0),(485,7,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',0,NULL,0,NULL,NULL,NULL,'2026-10-22 18:07:59','2026-10-22 18:07:59',NULL,0),(486,8,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',0,NULL,0,NULL,NULL,NULL,'2026-10-22 18:07:59','2026-10-22 18:07:59',NULL,0),(487,9,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',0,NULL,0,NULL,NULL,NULL,'2026-10-22 18:07:59','2026-10-22 18:07:59',NULL,0),(488,10,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',0,NULL,0,NULL,NULL,NULL,'2026-10-22 18:07:59','2026-10-22 18:07:59',NULL,0),(489,12,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',0,NULL,0,NULL,NULL,NULL,'2026-10-22 18:07:59','2026-10-22 18:07:59',NULL,0),(490,13,'日志提醒','您今天还未撰写工作日志，请在18:00前完成，以免影响工作统计。',5,'worklog_remind',NULL,'/home/worklog',0,NULL,0,NULL,NULL,NULL,'2026-10-22 18:07:59','2026-10-22 18:07:59',NULL,0),(491,1,'上班签到提醒','系统管理员，上班前15分钟了，请及时签到',2,'attendance_remind',NULL,'/home/attendance',0,NULL,0,NULL,NULL,NULL,'2026-10-22 23:45:00','2026-10-22 23:45:00',NULL,0),(492,2,'上班签到提醒','张三，上班前15分钟了，请及时签到',2,'attendance_remind',NULL,'/home/attendance',0,NULL,0,NULL,NULL,NULL,'2026-10-22 23:45:00','2026-10-22 23:45:00',NULL,0),(493,3,'上班签到提醒','王主管，上班前15分钟了，请及时签到',2,'attendance_remind',NULL,'/home/attendance',0,NULL,0,NULL,NULL,NULL,'2026-10-22 23:45:00','2026-10-22 23:45:00',NULL,0),(494,4,'上班签到提醒','李员工，上班前15分钟了，请及时签到',2,'attendance_remind',NULL,'/home/attendance',0,NULL,0,NULL,NULL,NULL,'2026-10-22 23:45:00','2026-10-22 23:45:00',NULL,0),(495,5,'上班签到提醒','赵专员，上班前15分钟了，请及时签到',2,'attendance_remind',NULL,'/home/attendance',0,NULL,0,NULL,NULL,NULL,'2026-10-22 23:45:00','2026-10-22 23:45:00',NULL,0),(496,6,'上班签到提醒','钱专员，上班前15分钟了，请及时签到',2,'attendance_remind',NULL,'/home/attendance',0,NULL,0,NULL,NULL,NULL,'2026-10-22 23:45:00','2026-10-22 23:45:00',NULL,0),(497,7,'上班签到提醒','陈总经理，上班前15分钟了，请及时签到',2,'attendance_remind',NULL,'/home/attendance',0,NULL,0,NULL,NULL,NULL,'2026-10-22 23:45:00','2026-10-22 23:45:00',NULL,0),(498,8,'上班签到提醒','赵总经理，上班前15分钟了，请及时签到',2,'attendance_remind',NULL,'/home/attendance',0,NULL,0,NULL,NULL,NULL,'2026-10-22 23:45:00','2026-10-22 23:45:00',NULL,0),(499,9,'上班签到提醒','雷先生，上班前15分钟了，请及时签到',2,'attendance_remind',NULL,'/home/attendance',0,NULL,0,NULL,NULL,NULL,'2026-10-22 23:45:00','2026-10-22 23:45:00',NULL,0),(500,10,'上班签到提醒','陈主管，上班前15分钟了，请及时签到',2,'attendance_remind',NULL,'/home/attendance',0,NULL,0,NULL,NULL,NULL,'2026-10-22 23:45:00','2026-10-22 23:45:00',NULL,0),(501,12,'上班签到提醒','张先生，上班前15分钟了，请及时签到',2,'attendance_remind',NULL,'/home/attendance',0,NULL,0,NULL,NULL,NULL,'2026-10-22 23:45:00','2026-10-22 23:45:00',NULL,0),(502,13,'上班签到提醒','项目经理，上班前15分钟了，请及时签到',2,'attendance_remind',NULL,'/home/attendance',0,NULL,0,NULL,NULL,NULL,'2026-10-22 23:45:00','2026-10-22 23:45:00',NULL,0),(521,5,'催办提醒：张三-因事请假','流程【张三-因事请假】在您这里已停留 566 小时，请及时处理',2,'flow_instance',4,'/home/flow-todo',0,NULL,0,NULL,NULL,NULL,'2026-10-23 09:00:00','2026-10-23 09:00:00',NULL,0),(522,4,'任务提醒','今日待办：待接受 1 项，进行中 0 项，待验收 0 项',3,'task',NULL,'/home/task?type=assigned',0,NULL,0,NULL,NULL,NULL,'2026-10-23 09:00:00','2026-10-23 09:00:00',NULL,0),(523,9,'任务提醒','今日待办：待接受 0 项，进行中 1 项，待验收 0 项',3,'task',NULL,'/home/task?type=assigned',0,NULL,0,NULL,NULL,NULL,'2026-10-23 09:00:00','2026-10-23 09:00:00',NULL,0),(531,4,'任务通知','您有新任务：员工自测任务A',2,'task',NULL,'/home/task?type=assigned',0,NULL,0,NULL,NULL,NULL,'2026-10-23 09:35:21','2026-10-23 09:35:21',NULL,0),(532,1,'任务通知','任务待验收：员工自测任务A',2,'task',NULL,'/home/task?type=created',0,NULL,0,NULL,NULL,NULL,'2026-10-23 09:35:22','2026-10-23 09:35:21',NULL,0),(533,4,'任务通知','您有新任务：员工自测任务B',2,'task',NULL,'/home/task?type=assigned',0,NULL,0,NULL,NULL,NULL,'2026-10-23 09:35:22','2026-10-23 09:35:21',NULL,0),(534,1,'任务通知','任务被拒绝：员工自测任务B',2,'task',NULL,'/home/task?type=created',0,NULL,0,NULL,NULL,NULL,'2026-10-23 09:35:22','2026-10-23 09:35:21',NULL,0),(535,1,'新的补签申请待审批','李员工 提交了 2026-10-22 的签+退申请，请审批',2,'attendance_makeup',20,'/home/attendance',0,NULL,0,NULL,NULL,NULL,'2026-10-23 09:36:49','2026-10-23 09:36:49',NULL,0),(536,3,'新的补签申请待审批','李员工 提交了 2026-10-22 的签+退申请，请审批',2,'attendance_makeup',20,'/home/attendance',0,NULL,0,NULL,NULL,NULL,'2026-10-23 09:36:49','2026-10-23 09:36:49',NULL,0),(537,12,'日志新评论','李员工 在你的日志《HR02 的日志（授权自测）》中评论：员工01 已阅读，谢谢分享',5,'worklog_comment',11,'/home/worklog',0,NULL,0,NULL,NULL,NULL,'2026-10-23 09:40:34','2026-10-23 09:40:34',NULL,0),(538,4,'日志新评论','王主管 在你的日志《审计-员工日志-改过》中评论：主管评一句：继续保持',5,'worklog_comment',12,'/home/worklog',0,NULL,0,NULL,NULL,NULL,'2026-10-23 18:34:23','2026-10-23 18:34:23',NULL,0),(544,1,'新的补签申请待审批','李员工 提交了 2026-10-22 的补签到申请，请审批',2,'attendance_makeup',21,'/home/attendance',0,NULL,0,NULL,NULL,NULL,'2026-10-23 18:37:03','2026-10-23 18:37:03',NULL,0),(545,3,'新的补签申请待审批','李员工 提交了 2026-10-22 的补签到申请，请审批',2,'attendance_makeup',21,'/home/attendance',0,NULL,0,NULL,NULL,NULL,'2026-10-23 18:37:03','2026-10-23 18:37:03',NULL,0),(546,1,'补签申请待人事终审','李员工 的 2026-10-22 补签申请已通过部门主管初审，请人事终审',2,'attendance_makeup',21,'/home/attendance',0,NULL,0,NULL,NULL,NULL,'2026-10-23 18:37:03','2026-10-23 18:37:03',NULL,0),(547,10,'补签申请待人事终审','李员工 的 2026-10-22 补签申请已通过部门主管初审，请人事终审',2,'attendance_makeup',21,'/home/attendance',0,NULL,0,NULL,NULL,NULL,'2026-10-23 18:37:03','2026-10-23 18:37:03',NULL,0),(548,4,'补签主管初审通过','您 2026-10-22 的补签申请已通过部门主管初审，等待人事终审',2,'attendance_makeup',21,'/home/attendance',0,NULL,0,NULL,NULL,NULL,'2026-10-23 18:37:03','2026-10-23 18:37:03',NULL,0),(549,4,'补签申请已通过','您 2026-10-22 的补签申请已审批通过',2,'attendance_makeup',21,'/home/attendance',0,NULL,0,NULL,NULL,NULL,'2026-10-23 18:37:03','2026-10-23 18:37:03',NULL,0),(550,3,'任务通知','任务待验收：市场部季度数据整理',2,'task',NULL,'/home/task?type=created',0,NULL,0,NULL,NULL,NULL,'2026-10-23 18:37:03','2026-10-23 18:37:03',NULL,0),(551,4,'任务通知','任务已验收通过：市场部季度数据整理',2,'task',NULL,'/home/task?type=assigned',0,NULL,0,NULL,NULL,NULL,'2026-10-23 18:37:03','2026-10-23 18:37:03',NULL,0),(552,26,'任务通知','您有新任务：审计-交接任务',2,'task',NULL,'/home/task?type=assigned',0,NULL,0,NULL,NULL,NULL,'2026-10-23 18:38:55','2026-10-23 18:38:54',NULL,0),(558,4,'项目通知','您被加入项目：审计-项目四',2,'project',NULL,'/home/project?id=9',0,NULL,0,NULL,NULL,NULL,'2026-10-23 18:40:05','2026-10-23 18:40:04',NULL,0),(559,4,'任务通知','您有新任务：审计-项目任务',2,'task',NULL,'/home/task?type=assigned',0,NULL,0,NULL,NULL,NULL,'2026-10-23 18:40:05','2026-10-23 18:40:04',NULL,0),(560,13,'任务通知','任务待验收：审计-项目任务',2,'task',NULL,'/home/task?type=created',0,NULL,0,NULL,NULL,NULL,'2026-10-23 18:40:05','2026-10-23 18:40:04',NULL,0),(561,4,'任务通知','任务已验收通过：审计-项目任务',2,'task',NULL,'/home/task?type=assigned',0,NULL,0,NULL,NULL,NULL,'2026-10-23 18:40:05','2026-10-23 18:40:04',NULL,0),(562,4,'项目通知','项目状态更新：审计-项目四 → 已完成',2,'project',NULL,'/home/project?id=9',0,NULL,0,NULL,NULL,NULL,'2026-10-23 18:40:05','2026-10-23 18:40:05',NULL,0),(571,4,'任务通知','您有新任务：删除测试任务Z',2,'task',NULL,'/home/task?type=assigned',0,NULL,0,NULL,NULL,NULL,'2026-10-23 20:04:52','2026-10-23 20:04:51',NULL,0),(573,4,'任务通知','您有新任务：删除测试任务Z',2,'task',NULL,'/home/task?type=assigned',0,NULL,0,NULL,NULL,NULL,'2026-10-23 20:05:11','2026-10-23 20:05:10',NULL,0),(574,1,'上班签到提醒','系统管理员，上班前15分钟了，请及时签到',2,'attendance_remind',NULL,'/home/attendance',0,NULL,0,NULL,NULL,NULL,'2026-10-23 23:45:00','2026-10-23 23:45:00',NULL,0),(575,2,'上班签到提醒','张三，上班前15分钟了，请及时签到',2,'attendance_remind',NULL,'/home/attendance',0,NULL,0,NULL,NULL,NULL,'2026-10-23 23:45:00','2026-10-23 23:45:00',NULL,0),(576,3,'上班签到提醒','王主管，上班前15分钟了，请及时签到',2,'attendance_remind',NULL,'/home/attendance',0,NULL,0,NULL,NULL,NULL,'2026-10-23 23:45:00','2026-10-23 23:45:00',NULL,0),(577,4,'上班签到提醒','李员工，上班前15分钟了，请及时签到',2,'attendance_remind',NULL,'/home/attendance',0,NULL,0,NULL,NULL,NULL,'2026-10-23 23:45:00','2026-10-23 23:45:00',NULL,0),(578,5,'上班签到提醒','赵专员，上班前15分钟了，请及时签到',2,'attendance_remind',NULL,'/home/attendance',0,NULL,0,NULL,NULL,NULL,'2026-10-23 23:45:00','2026-10-23 23:45:00',NULL,0),(579,6,'上班签到提醒','钱专员，上班前15分钟了，请及时签到',2,'attendance_remind',NULL,'/home/attendance',0,NULL,0,NULL,NULL,NULL,'2026-10-23 23:45:00','2026-10-23 23:45:00',NULL,0),(580,7,'上班签到提醒','陈总经理，上班前15分钟了，请及时签到',2,'attendance_remind',NULL,'/home/attendance',0,NULL,0,NULL,NULL,NULL,'2026-10-23 23:45:00','2026-10-23 23:45:00',NULL,0),(581,8,'上班签到提醒','赵总经理，上班前15分钟了，请及时签到',2,'attendance_remind',NULL,'/home/attendance',0,NULL,0,NULL,NULL,NULL,'2026-10-23 23:45:00','2026-10-23 23:45:00',NULL,0),(582,9,'上班签到提醒','雷先生，上班前15分钟了，请及时签到',2,'attendance_remind',NULL,'/home/attendance',0,NULL,0,NULL,NULL,NULL,'2026-10-23 23:45:00','2026-10-23 23:45:00',NULL,0),(583,10,'上班签到提醒','陈主管，上班前15分钟了，请及时签到',2,'attendance_remind',NULL,'/home/attendance',0,NULL,0,NULL,NULL,NULL,'2026-10-23 23:45:00','2026-10-23 23:45:00',NULL,0),(584,12,'上班签到提醒','张先生，上班前15分钟了，请及时签到',2,'attendance_remind',NULL,'/home/attendance',0,NULL,0,NULL,NULL,NULL,'2026-10-23 23:45:00','2026-10-23 23:45:00',NULL,0),(585,13,'上班签到提醒','项目经理，上班前15分钟了，请及时签到',2,'attendance_remind',NULL,'/home/attendance',0,NULL,0,NULL,NULL,NULL,'2026-10-23 23:45:00','2026-10-23 23:45:00',NULL,0),(586,4,'任务提醒','今日待办：待接受 1 项，进行中 0 项，待验收 0 项',3,'task',NULL,'/home/task?type=assigned',0,NULL,0,NULL,NULL,NULL,'2026-10-24 08:59:55','2026-10-24 08:59:55',NULL,0),(587,9,'任务提醒','今日待办：待接受 0 项，进行中 1 项，待验收 0 项',3,'task',NULL,'/home/task?type=assigned',0,NULL,0,NULL,NULL,NULL,'2026-10-24 08:59:55','2026-10-24 08:59:55',NULL,0);
/*!40000 ALTER TABLE `notification_records` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `notification_templates`
--

DROP TABLE IF EXISTS `notification_templates`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification_templates` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `template_code` varchar(64) NOT NULL COMMENT '模板编码',
  `template_name` varchar(100) NOT NULL COMMENT '模板名称',
  `biz_type` varchar(32) DEFAULT NULL COMMENT '业务类型',
  `title_template` varchar(200) NOT NULL COMMENT '标题模板，支持 {变量}',
  `content_template` text COMMENT '内容模板',
  `channels` varchar(100) DEFAULT 'system' COMMENT '推送渠道，逗号分隔',
  `status` tinyint DEFAULT '1' COMMENT '0停用 1启用',
  `remark` varchar(255) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_template_code` (`template_code`),
  KEY `idx_biz_type` (`biz_type`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知模板';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notification_templates`
--

LOCK TABLES `notification_templates` WRITE;
/*!40000 ALTER TABLE `notification_templates` DISABLE KEYS */;
INSERT INTO `notification_templates` VALUES (1,'flow_arrive','待办流程到达','flow_instance','新的待办流程：{title}','流程 {instanceNo} 已到达「{stepName}」，请及时处理。','system',1,'流程流转到下一个审批节点时使用','2026-10-23 18:54:26','2026-10-23 18:54:26',NULL),(2,'flow_result','流程审批结果','flow_instance','流程{result}：{title}','您发起的流程 {instanceNo} 已被{approver}处理，处理结果：{result}。','system',1,'审批通过/驳回后通知发起人','2026-10-23 18:54:26','2026-10-23 18:54:26',NULL),(3,'task_assign','任务分配','task','您有新任务：{title}','{assigner} 把任务「{title}」指派给您，计划完成时间 {planEndTime}。','system',1,'任务指派时使用','2026-10-23 18:54:26','2026-10-23 18:54:26',NULL),(4,'attendance_abnormal','出勤异常提醒','attendance','出勤异常提醒：{date}','您 {date} 的考勤状态为「{status}」，如有异议请在出勤管理里提交申诉。','system',1,'迟到/早退/缺勤自动提醒','2026-10-23 18:54:26','2026-10-23 18:54:26',NULL),(5,'system_notice','系统通知','system','{title}','{content}','system',1,'系统级公告与提醒的通用模板','2026-10-23 18:54:26','2026-10-23 18:54:26',NULL);
/*!40000 ALTER TABLE `notification_templates` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `online_users`
--

DROP TABLE IF EXISTS `online_users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `online_users` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` bigint NOT NULL COMMENT '用户ID（同一用户只保留最新会话）',
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
  `session_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '会话ID',
  `login_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  `last_active_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后活跃时间',
  `ip_address` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'IP地址',
  `browser` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '浏览器',
  `os` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作系统',
  `device` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '设备类型',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间（软删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_id` (`user_id`),
  KEY `idx_online_users_last_active` (`last_active_time`)
) ENGINE=InnoDB AUTO_INCREMENT=176 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='在线用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `online_users`
--

LOCK TABLES `online_users` WRITE;
/*!40000 ALTER TABLE `online_users` DISABLE KEYS */;
INSERT INTO `online_users` VALUES (173,1,'系统管理员','c4fd8138793e47658d2f5ee1db6cdc2f','2026-10-22 09:45:30','2026-10-24 12:11:06','0:0:0:0:0:0:0:1','Edge','Windows','电脑','2026-10-22 09:45:30','2026-10-22 09:45:30',NULL);
/*!40000 ALTER TABLE `online_users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `operation_logs`
--

DROP TABLE IF EXISTS `operation_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `operation_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` bigint NOT NULL COMMENT '操作用户ID',
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '操作用户名（冗余）',
  `module` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '操作模块（如 user, role, announcement）',
  `action` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '操作类型（如 create, update, delete, login, export）',
  `method` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '请求方法（GET, POST, PUT, DELETE）',
  `url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '请求URL',
  `ip_address` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作IP',
  `user_agent` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'User-Agent',
  `target_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作对象类型（如 user, role）',
  `target_id` bigint DEFAULT NULL COMMENT '操作对象ID',
  `request_params` text COLLATE utf8mb4_unicode_ci COMMENT '请求参数（JSON格式，敏感字段脱敏）',
  `response_result` text COLLATE utf8mb4_unicode_ci COMMENT '响应结果摘要',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '操作状态：0失败 1成功',
  `error_msg` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '错误信息（失败时记录）',
  `cost_time` int DEFAULT NULL COMMENT '耗时（毫秒）',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间（软删除）',
  PRIMARY KEY (`id`),
  KEY `idx_operation_logs_user_created` (`user_id`,`created_at`),
  KEY `idx_operation_logs_target` (`target_type`,`target_id`)
) ENGINE=InnoDB AUTO_INCREMENT=45 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `operation_logs`
--

LOCK TABLES `operation_logs` WRITE;
/*!40000 ALTER TABLE `operation_logs` DISABLE KEYS */;
INSERT INTO `operation_logs` VALUES (1,1,'admin','user','export','GET','/api/users/export','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/153.0.0.0 Safari/537.36 Edg/153.0.0.0','user',NULL,'{\"keyword\":null,\"status\":null,\"orgId\":null}','导出6条',1,NULL,1052,'2026-09-28 16:51:17','2026-09-28 16:51:17',NULL),(2,1,'admin','user','export','GET','/api/users/export','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/153.0.0.0 Safari/537.36 Edg/153.0.0.0','user',NULL,'{\"keyword\":null,\"status\":null,\"orgId\":null}','导出6条',1,NULL,699,'2026-09-28 17:20:51','2026-09-28 17:20:51',NULL),(3,1,'admin','user','export','GET','/api/users/export','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/154.0.0.0 Safari/537.36','user',NULL,'{\"keyword\":null,\"status\":null,\"orgId\":null}','导出6条',1,NULL,773,'2026-10-04 15:04:11','2026-10-04 15:04:11',NULL),(19,4,'employee01','attendance','recheck_in','POST','/api/attendance/check-in','0:0:0:0:0:0:0:1',NULL,'attendance',35,'original=2026-10-05T15:48:37 -> 2026-10-05T16:03:21.714158','ok',1,NULL,NULL,'2026-10-05 16:03:21','2026-10-05 16:03:21',NULL),(20,4,'employee01','attendance','recheck_out','POST','/api/attendance/check-out','0:0:0:0:0:0:0:1',NULL,'attendance',35,'original=2026-10-05T15:48:37 -> 2026-10-05T16:03:21.734832100','ok',1,NULL,NULL,'2026-10-05 16:03:21','2026-10-05 16:03:21',NULL),(21,1,'admin','工作日志','下载附件','GET','/api/worklogs/attachments/10/download',NULL,NULL,'worklog_attachment',10,NULL,NULL,1,NULL,NULL,'2026-10-05 17:21:45','2026-10-05 17:21:45',NULL),(22,1,'admin','attendance','export_statistics','GET','/api/attendance/statistics/export','0:0:0:0:0:0:0:1',NULL,'attendance',NULL,'month=2026-10;deptId=null','导出1条',1,NULL,NULL,'2026-10-08 11:35:37','2026-10-08 11:35:37',NULL),(23,1,'admin','attendance','recheck_out','POST','/api/attendance/check-out','0:0:0:0:0:0:0:1',NULL,'attendance',49,'original=2026-10-08T11:35:25 -> 2026-10-08T11:38:07.579326400','ok',1,NULL,NULL,'2026-10-08 11:38:07','2026-10-08 11:38:07',NULL),(24,1,'admin','attendance','recheck_out','POST','/api/attendance/check-out','0:0:0:0:0:0:0:1',NULL,'attendance',49,'original=2026-10-08T11:38:08 -> 2026-10-08T11:38:07.579326400','ok',1,NULL,NULL,'2026-10-08 11:38:07','2026-10-08 11:38:07',NULL),(25,1,'admin','工作日志','下载附件','GET','/api/worklogs/attachments/11/download',NULL,NULL,'worklog_attachment',11,NULL,NULL,1,NULL,NULL,'2026-10-09 10:43:42','2026-10-09 10:43:42',NULL),(26,1,'admin','report','export','GET','/api/report/export/task',NULL,NULL,'task',NULL,'{\"start\":\"2026-07-17\",\"end\":\"2026-10-14\",\"deptId\":null,\"userId\":null,\"projectId\":null}','导出9行',1,NULL,0,'2026-10-14 15:00:51','2026-10-14 15:00:51',NULL),(27,1,'admin','report','export','GET','/api/report/export/task',NULL,NULL,'task',NULL,'{\"start\":\"2026-07-17\",\"end\":\"2026-10-14\",\"deptId\":null,\"userId\":null,\"projectId\":null}','导出16行',1,NULL,0,'2026-10-14 15:08:58','2026-10-14 15:08:58',NULL),(28,1,'admin','report','export','GET','/api/report/export/task',NULL,NULL,'task',NULL,'{\"start\":\"2026-07-17\",\"end\":\"2026-10-14\",\"deptId\":null,\"userId\":null,\"projectId\":null}','导出16行',1,NULL,0,'2026-10-14 15:12:28','2026-10-14 15:12:28',NULL),(29,1,'admin','report','export','GET','/api/report/export/task',NULL,NULL,'task',NULL,'{\"start\":\"2026-07-17\",\"end\":\"2026-10-14\",\"deptId\":null,\"userId\":null,\"projectId\":null}','导出16行',1,NULL,0,'2026-10-14 15:13:07','2026-10-14 15:13:07',NULL),(30,1,'admin','report','export','GET','/api/report/export/attendance',NULL,NULL,'attendance',NULL,'{\"start\":\"2026-07-17\",\"end\":\"2026-10-14\",\"deptId\":null,\"userId\":null,\"projectId\":null}','导出21行',1,NULL,0,'2026-10-14 15:13:07','2026-10-14 15:13:07',NULL),(31,1,'admin','report','export','GET','/api/report/export/worklog',NULL,NULL,'worklog',NULL,'{\"start\":\"2026-07-17\",\"end\":\"2026-10-14\",\"deptId\":null,\"userId\":null,\"projectId\":null}','导出18行',1,NULL,0,'2026-10-14 15:13:08','2026-10-14 15:13:08',NULL),(32,1,'admin','report','export','GET','/api/report/export/flow',NULL,NULL,'flow',NULL,'{\"start\":\"2026-07-17\",\"end\":\"2026-10-14\",\"deptId\":null,\"userId\":null,\"projectId\":null}','导出13行',1,NULL,0,'2026-10-14 15:13:08','2026-10-14 15:13:08',NULL),(33,5,'hr01','report','export','GET','/api/report/export/attendance',NULL,NULL,'attendance',NULL,'{\"start\":\"2026-07-17\",\"end\":\"2026-10-14\",\"deptId\":null,\"userId\":null,\"projectId\":null}','导出21行',1,NULL,0,'2026-10-14 15:13:08','2026-10-14 15:13:08',NULL),(34,1,'admin','report','export','GET','/api/report/export/task',NULL,NULL,'task',NULL,'{\"start\":\"2026-07-17\",\"end\":\"2026-10-14\",\"deptId\":null,\"userId\":null,\"projectId\":null}','导出16行',1,NULL,0,'2026-10-14 15:17:17','2026-10-14 15:17:17',NULL),(35,1,'admin','report','export','GET','/api/report/export/flow',NULL,NULL,'flow',NULL,'{\"start\":\"2026-07-17\",\"end\":\"2026-10-14\",\"deptId\":null,\"userId\":null,\"projectId\":null}','导出13行',1,NULL,0,'2026-10-14 15:17:18','2026-10-14 15:17:18',NULL),(36,1,'admin','report','export','GET','/api/report/export/attendance',NULL,NULL,'attendance',NULL,'{\"start\":\"2026-07-17\",\"end\":\"2026-10-14\",\"deptId\":null,\"userId\":null,\"projectId\":null}','导出21行',1,NULL,0,'2026-10-14 15:17:18','2026-10-14 15:17:18',NULL),(37,1,'admin','report','export','GET','/api/report/export/worklog',NULL,NULL,'worklog',NULL,'{\"start\":\"2026-07-17\",\"end\":\"2026-10-14\",\"deptId\":null,\"userId\":null,\"projectId\":null}','导出18行',1,NULL,0,'2026-10-14 15:37:04','2026-10-14 15:37:04',NULL),(38,1,'admin','report','export','GET','/api/report/export/worklog',NULL,NULL,'worklog',NULL,'{\"start\":\"2026-07-17\",\"end\":\"2026-10-14\",\"deptId\":null,\"userId\":null,\"projectId\":null}','导出18行',1,NULL,0,'2026-10-14 15:37:37','2026-10-14 15:37:37',NULL),(39,3,'manager01','attendance','export','GET','/api/attendance/export','0:0:0:0:0:0:0:1',NULL,'attendance',NULL,'startDate=2026-10-01;endDate=2026-10-31;deptId=null','导出12条',1,NULL,NULL,'2026-10-22 10:48:46','2026-10-22 10:48:46',NULL),(40,4,'employee01','attendance','recheck_in','POST','/api/attendance/check-in','0:0:0:0:0:0:0:1',NULL,'attendance',92,'original=2026-10-23T18:34:23 -> 2026-10-23T18:34:22.819080800','ok',1,NULL,NULL,'2026-10-23 18:34:22','2026-10-23 18:34:22',NULL),(41,4,'employee01','attendance','recheck_in','POST','/api/attendance/check-in','0:0:0:0:0:0:0:1',NULL,'attendance',92,'original=2026-10-23T18:34:23 -> 2026-10-23T18:34:22.856003900','ok',1,NULL,NULL,'2026-10-23 18:34:22','2026-10-23 18:34:22',NULL),(42,1,'admin','report','export','GET','/api/report/export/worklog',NULL,NULL,'worklog',NULL,'{\"start\":\"2026-10-01\",\"end\":\"2026-10-31\"}','导出15行',1,NULL,0,'2026-10-23 20:04:53','2026-10-23 20:04:53',NULL),(43,1,'admin','report','export','GET','/api/report/export/worklog',NULL,NULL,'worklog',NULL,'{\"start\":\"2026-10-01\",\"end\":\"2026-10-31\"}','导出14行',1,NULL,0,'2026-10-23 20:05:11','2026-10-23 20:05:11',NULL),(44,1,'admin','attendance','export','GET','/api/attendance/export','0:0:0:0:0:0:0:1',NULL,'attendance',NULL,'startDate=2026-10-23;endDate=2026-10-23;deptId=null','导出1条',1,NULL,NULL,'2026-10-23 20:05:28','2026-10-23 20:05:28',NULL);
/*!40000 ALTER TABLE `operation_logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `organizations`
--

DROP TABLE IF EXISTS `organizations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `organizations` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '组织ID',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父组织ID（0表示顶级）',
  `org_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '组织名称',
  `org_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '组织编码',
  `org_type` tinyint NOT NULL DEFAULT '1' COMMENT '组织类型：1公司 2部门 3小组',
  `leader_id` bigint DEFAULT NULL COMMENT '负责人用户ID',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0停用 1启用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间（软删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `org_code` (`org_code`),
  KEY `idx_organizations_parent` (`parent_id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='组织机构表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `organizations`
--

LOCK TABLES `organizations` WRITE;
/*!40000 ALTER TABLE `organizations` DISABLE KEYS */;
INSERT INTO `organizations` VALUES (1,0,'董事会','HQ',1,7,1,1,'2026-09-26 16:22:28','2026-10-21 08:53:33',NULL),(2,1,'人事部','HR',1,NULL,1,1,'2026-09-26 16:22:28','2026-10-20 19:13:38',NULL),(3,1,'财务部','FIN',1,NULL,2,1,'2026-09-26 16:22:28','2026-09-26 16:22:28',NULL),(4,1,'市场部','MKT',1,3,3,1,'2026-09-26 16:22:28','2026-10-13 20:22:47',NULL),(5,4,'销售部','SALES',1,NULL,1,1,'2026-09-26 16:22:28','2026-09-26 16:22:28',NULL),(6,4,'技术部','TECH',1,NULL,2,1,'2026-09-26 16:22:28','2026-10-22 11:19:51',NULL),(7,4,'客服部','CS',1,NULL,3,1,'2026-09-26 16:22:28','2026-09-26 16:22:28',NULL),(8,2,'人事部测试子部门','HRTEST999',2,NULL,0,1,'2026-10-20 19:13:38','2026-10-20 19:13:37','2026-10-20 19:13:37'),(9,1,'管理员测试部门','ADMTEST999',2,NULL,0,1,'2026-10-20 19:13:38','2026-10-20 19:13:37','2026-10-20 19:13:37');
/*!40000 ALTER TABLE `organizations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `page_controls`
--

DROP TABLE IF EXISTS `page_controls`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `page_controls` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '控件ID',
  `page_id` bigint NOT NULL COMMENT '所属页面/菜单ID（关联menus表）',
  `control_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '控件编码（前端控件唯一标识，如 user_list_add_btn）',
  `control_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '控件名称（如：新增用户按钮：）',
  `control_type` tinyint NOT NULL DEFAULT '1' COMMENT '控件类型：1按钮 2链接 3输入框 4表格列 5其他',
  `parent_control_id` bigint DEFAULT NULL COMMENT '父控件ID（用于控件层级）',
  `parent_control_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '父控件类别',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0停用 1启用',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间（软删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `control_code` (`control_code`),
  KEY `idx_page_controls_page` (`page_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='页面控件管理表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `page_controls`
--

LOCK TABLES `page_controls` WRITE;
/*!40000 ALTER TABLE `page_controls` DISABLE KEYS */;
/*!40000 ALTER TABLE `page_controls` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `password_history`
--

DROP TABLE IF EXISTS `password_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `password_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `password` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `password_history`
--

LOCK TABLES `password_history` WRITE;
/*!40000 ALTER TABLE `password_history` DISABLE KEYS */;
INSERT INTO `password_history` VALUES (1,1,'$2a$10$0Su0iK6M1a0/yHZCfM/wieftVLa8AKck5L5M/m0mKqx6G0L/V9Iai','2026-09-22 09:09:05'),(2,2,'$2a$10$H/fpK58zMXrDuqlQ3ujGWeLCb4skYrdWZsFJulEM3GmR.w3BzN/Ea','2026-09-26 16:48:39'),(3,4,'$2a$10$T.wNRvrhnpc4i1QbORtVwuTuRdvB58DzDCHnm2lfuHxwKSB0sm6aO','2026-09-28 14:27:13'),(4,5,'$2a$10$ZLY7/AgI7YIlLDadf1f5VO1mxBXteLQCdN6wfSxpZ8q.y07O3YWGG','2026-09-29 15:09:28'),(5,6,'$2a$10$lq3VdqSd8ruHzDJXW5B7K.oiAe70v/v6foMHt93hwaDluiz5f4neO','2026-09-29 18:26:43'),(6,3,'$2a$10$pVf1uF27MTRE/Tvkk66Pa.TEJ2EjCU22G4Sonq8628BgV9W8TVBMi','2026-09-30 18:39:16'),(7,3,'$2a$10$jFBtUbVyU4UXRD4NCi2E8u3C.h43v.E8xNNyWImDXVf5ofkjsvNr2','2026-10-04 15:49:25'),(8,3,'$2a$10$yPn4L2IjbIhllY7E4wI1eeeMpUm7wbV2tsYm..AmVq0UNW3.oASNm','2026-10-04 15:56:13'),(9,4,'$2a$10$eJCcjGqglxBhvWyW4o0LwuucxAvyeW3ZBHxPHmS2tJnpWwAsgklAG','2026-10-09 14:58:09'),(10,9,'$2a$10$QSc6ibJgFomQq74/ScwYsOXeGx7FNza5DfqnDqH3leStJGCO.GMPW','2026-10-19 11:20:34'),(11,6,'$2a$10$RVKLfF4arJ7dTIUgpGq79OcRW7ed38N8gVOj8zsgbGx0.kffvtlGS','2026-10-19 15:31:27'),(12,5,'$2a$10$MHdMptja4csVxo4cE1VPNugG2gfxP0aMl7npOA7DZD/kbeV/Pueya','2026-10-20 14:28:37'),(13,10,'$2a$10$Li.27BT0c18tf1ZRWt9qCuqs5YEbdpQncxaLDGXs7N/DPd.10Da36','2026-10-20 16:04:00'),(14,12,'$2a$10$EeLGlKxlTM2ra0WuiNErPOwqEq/shlJygGwSpxiQiqAh8AA323XcO','2026-10-21 14:40:09'),(15,13,'$2a$10$BjFFYvSz.Ng2aipNrHL/3OomdS7Wu7zmmAe8N51lx7UE25bSgHCg2','2026-10-21 17:02:24'),(16,9,'$2a$10$VGr..ub9/.RLoQXthWoI4u2TIRhcDPRaObUmkl4PV7pveHUTir3Sa','2026-10-21 17:10:05');
/*!40000 ALTER TABLE `password_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `payment_records`
--

DROP TABLE IF EXISTS `payment_records`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_records` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '付款记录ID',
  `pay_no` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '付款单号（PAY-20261019-0001）',
  `instance_id` bigint NOT NULL COMMENT '流程实例ID',
  `instance_no` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '流程编号（冗余，列表直接展示）',
  `definition_id` bigint DEFAULT NULL COMMENT '流程定义ID',
  `flow_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '流程名称（冗余）',
  `title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '流程标题',
  `applicant_id` bigint DEFAULT NULL COMMENT '申请人ID',
  `category_id` bigint DEFAULT NULL COMMENT '费用类别ID（付款时确认）',
  `category_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '费用类别名称（冗余，统计直接用）',
  `amount` decimal(12,2) NOT NULL COMMENT '付款金额',
  `payee_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '收款人',
  `payee_account` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '收款账号',
  `pay_method` tinyint NOT NULL DEFAULT '1' COMMENT '付款方式：1银行转账 2现金 3微信 4支付宝 5支票',
  `pay_time` datetime NOT NULL COMMENT '付款时间',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1已付款 2已作废',
  `operator_id` bigint NOT NULL COMMENT '付款操作人ID',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '付款备注',
  `void_reason` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '作废原因',
  `void_by` bigint DEFAULT NULL COMMENT '作废操作人ID',
  `void_time` datetime DEFAULT NULL COMMENT '作废时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间（软删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_pay_no` (`pay_no`),
  KEY `idx_payment_status_time` (`status`,`pay_time`),
  KEY `idx_payment_payee` (`payee_name`),
  KEY `idx_payment_category` (`category_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='付款记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `payment_records`
--

LOCK TABLES `payment_records` WRITE;
/*!40000 ALTER TABLE `payment_records` DISABLE KEYS */;
/*!40000 ALTER TABLE `payment_records` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `positions`
--

DROP TABLE IF EXISTS `positions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `positions` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '职位ID',
  `position_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '职位名称',
  `position_code` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '职位编码',
  `level` tinyint NOT NULL DEFAULT '0' COMMENT '职位级别（1-10，用于审批流自动匹配）',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0停用 1启用',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间（软删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `position_code` (`position_code`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='职位表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `positions`
--

LOCK TABLES `positions` WRITE;
/*!40000 ALTER TABLE `positions` DISABLE KEYS */;
INSERT INTO `positions` VALUES (1,'测试工程师','test_eng_archived',1,0,1,'','2026-10-04 15:09:45','2026-10-21 08:48:45','2026-10-04 15:10:42'),(8,'前端开发工程师','fe_eng',0,10,1,NULL,'2026-10-21 08:48:33','2026-10-21 08:48:45',NULL),(9,'Java 后端开发工程师','java_eng',0,20,1,NULL,'2026-10-21 08:48:33','2026-10-21 08:48:45',NULL),(10,'测试工程师','qa_eng',0,30,1,NULL,'2026-10-21 08:48:33','2026-10-21 08:48:45',NULL),(11,'产品经理','product_mgr',0,40,1,NULL,'2026-10-21 08:48:33','2026-10-21 08:48:33',NULL),(12,'人事专员','hr_spec',0,50,1,NULL,'2026-10-21 08:48:33','2026-10-21 08:48:33',NULL),(13,'财务专员','fin_spec',0,60,1,NULL,'2026-10-21 08:48:33','2026-10-21 08:48:33',NULL);
/*!40000 ALTER TABLE `positions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `project_members`
--

DROP TABLE IF EXISTS `project_members`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_members` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id` bigint NOT NULL COMMENT '项目ID',
  `user_id` bigint NOT NULL COMMENT '成员用户ID',
  `role` tinyint NOT NULL DEFAULT '2' COMMENT '项目内角色：1项目经理 2成员 3关注人',
  `join_time` datetime DEFAULT NULL COMMENT '加入时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted_at` datetime DEFAULT NULL COMMENT '软删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_user` (`project_id`,`user_id`),
  KEY `idx_project_members_user` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=28 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目成员';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `project_members`
--

LOCK TABLES `project_members` WRITE;
/*!40000 ALTER TABLE `project_members` DISABLE KEYS */;
INSERT INTO `project_members` VALUES (11,6,13,1,'2026-10-21 17:06:49','2026-10-21 17:06:48','2026-10-21 17:06:48',NULL),(12,6,4,2,'2026-10-21 17:25:55','2026-10-21 17:06:48','2026-10-22 11:12:19',NULL),(13,6,9,2,'2026-10-21 17:06:49','2026-10-21 17:06:48','2026-10-21 17:06:48',NULL),(14,6,12,2,'2026-10-21 17:06:49','2026-10-21 17:06:48','2026-10-21 17:06:48',NULL),(15,6,5,2,'2026-10-21 17:25:30','2026-10-21 17:22:12','2026-10-21 17:22:12','2026-10-21 17:25:42'),(18,6,15,2,'2026-10-22 10:06:22','2026-10-22 10:06:21','2026-10-22 10:06:21','2026-10-22 10:06:21');
/*!40000 ALTER TABLE `project_members` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `projects`
--

DROP TABLE IF EXISTS `projects`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `projects` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '项目ID',
  `project_no` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '项目编号（如 PRJ-2026-001）',
  `name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '项目名称',
  `description` text COLLATE utf8mb4_unicode_ci COMMENT '项目描述',
  `project_type` tinyint NOT NULL DEFAULT '1' COMMENT '项目类型：1内部项目 2客户项目 3研发项目',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0未启动 1进行中 2已暂停 3已完成 4已取消',
  `priority` tinyint NOT NULL DEFAULT '2' COMMENT '优先级：1低 2中 3高 4紧急',
  `manager_id` bigint NOT NULL COMMENT '项目经理ID',
  `creator_id` bigint NOT NULL COMMENT '创建人ID',
  `plan_start_date` date DEFAULT NULL COMMENT '计划开始日期',
  `plan_end_date` date DEFAULT NULL COMMENT '计划结束日期',
  `actual_start_date` date DEFAULT NULL COMMENT '实际开始日期',
  `actual_end_date` date DEFAULT NULL COMMENT '实际结束日期',
  `progress` tinyint NOT NULL DEFAULT '0' COMMENT '进度百分比（0-100）',
  `color` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '项目标识颜色（看板视图使用）',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间（软删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `project_no` (`project_no`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `projects`
--

LOCK TABLES `projects` WRITE;
/*!40000 ALTER TABLE `projects` DISABLE KEYS */;
INSERT INTO `projects` VALUES (6,'PRJ-20261021-001','JDBC PRO','创建只属于公司的原生态JDBC强化版框架',3,1,3,13,13,'2026-10-21','2027-03-11','2026-10-21',NULL,25,NULL,'2026-10-21 17:06:48','2026-10-21 17:06:57',NULL);
/*!40000 ALTER TABLE `projects` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `recruitment_candidates`
--

DROP TABLE IF EXISTS `recruitment_candidates`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `recruitment_candidates` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '候选人ID',
  `position_id` bigint NOT NULL COMMENT '应聘岗位',
  `candidate_name` varchar(50) NOT NULL COMMENT '姓名',
  `gender` tinyint NOT NULL DEFAULT '0' COMMENT '0未知 1男 2女',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `education` varchar(50) DEFAULT NULL COMMENT '学历',
  `experience_years` int DEFAULT NULL COMMENT '工作年限',
  `source` varchar(50) DEFAULT NULL COMMENT '来源：简历投递/内推/校招/猎头',
  `resume_path` varchar(255) DEFAULT NULL COMMENT '简历附件路径',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1待筛选 2待面试 3面试通过 4已入职 5已淘汰',
  `interview_time` datetime DEFAULT NULL COMMENT '面试时间',
  `interviewer` varchar(50) DEFAULT NULL COMMENT '面试官',
  `evaluation` text COMMENT '面试评价',
  `expected_salary` decimal(10,2) DEFAULT NULL COMMENT '期望薪资',
  `creator_id` bigint NOT NULL COMMENT '录入人',
  `remark` varchar(500) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_candidate_position` (`position_id`),
  KEY `idx_candidate_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='招聘候选人';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `recruitment_candidates`
--

LOCK TABLES `recruitment_candidates` WRITE;
/*!40000 ALTER TABLE `recruitment_candidates` DISABLE KEYS */;
INSERT INTO `recruitment_candidates` VALUES (2,2,'张伟',1,'13800001111',NULL,'本科',3,'BOSS直聘',NULL,1,NULL,NULL,NULL,15000.00,10,NULL,'2026-10-20 14:58:17','2026-10-20 14:58:17',NULL),(3,3,'李娜',2,'13800002222',NULL,'大专',4,'内推',NULL,3,'2026-10-22 10:00:00','陈主管',NULL,13000.00,10,NULL,'2026-10-20 14:58:17','2026-10-21 08:44:10',NULL),(4,2,'王强',1,'13800003333',NULL,'硕士及以上',5,'校招',NULL,4,'2026-10-21 15:00:00','陈主管','基础扎实，沟通清晰',18000.00,10,NULL,'2026-10-20 14:58:17','2026-10-20 14:58:17',NULL);
/*!40000 ALTER TABLE `recruitment_candidates` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `recruitment_positions`
--

DROP TABLE IF EXISTS `recruitment_positions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `recruitment_positions` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '岗位ID',
  `position_name` varchar(100) NOT NULL COMMENT '招聘岗位名称',
  `dept_id` bigint DEFAULT NULL COMMENT '用人部门',
  `position_id` bigint DEFAULT NULL COMMENT '关联职位表，用于定级',
  `headcount` int NOT NULL DEFAULT '1' COMMENT '招聘人数',
  `education` varchar(50) DEFAULT NULL COMMENT '学历要求',
  `experience_years` int DEFAULT NULL COMMENT '经验要求（年）',
  `salary_range` varchar(50) DEFAULT NULL COMMENT '薪资范围',
  `work_place` varchar(100) DEFAULT NULL COMMENT '工作地点',
  `duty` text COMMENT '岗位职责',
  `requirement` text COMMENT '任职要求',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '0已停招 1在招 2已结束',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `deadline` date DEFAULT NULL COMMENT '截止日期',
  `creator_id` bigint NOT NULL COMMENT '创建人',
  `remark` varchar(500) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_recruit_position_status` (`status`),
  KEY `idx_recruit_position_dept` (`dept_id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='招聘岗位';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `recruitment_positions`
--

LOCK TABLES `recruitment_positions` WRITE;
/*!40000 ALTER TABLE `recruitment_positions` DISABLE KEYS */;
INSERT INTO `recruitment_positions` VALUES (2,'Java 后端开发工程师',2,NULL,2,'本科',2,'12k-18k','广州天河','负责 OA 系统后端接口与业务模块开发','熟悉 SpringBoot / MyBatis / MySQL',1,'2026-10-20 14:58:16','2026-11-30',10,NULL,'2026-10-20 14:58:16','2026-10-20 14:58:16',NULL),(3,'前端开发工程师',2,NULL,1,'大专',3,'10k-15k','广州天河','负责 Vue3 管理端页面开发','熟悉 Vue3 / TypeScript / Element Plus',1,'2026-10-20 14:58:17',NULL,10,NULL,'2026-10-20 14:58:17','2026-10-20 14:58:17',NULL),(4,'测试工程师',2,NULL,1,'本科',1,'8k-12k','广州天河','负责功能测试与接口测试','熟悉测试用例设计',0,NULL,NULL,10,NULL,'2026-10-20 14:58:17','2026-10-20 14:58:17',NULL),(5,'管理员测试岗',2,NULL,1,NULL,NULL,NULL,NULL,NULL,NULL,1,'2026-10-20 19:18:18',NULL,1,NULL,'2026-10-20 19:18:18','2026-10-21 08:48:17','2026-10-21 08:48:17');
/*!40000 ALTER TABLE `recruitment_positions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `role_permissions`
--

DROP TABLE IF EXISTS `role_permissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `role_permissions` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `menu_id` bigint NOT NULL COMMENT '菜单/权限ID',
  `permission_type` tinyint NOT NULL DEFAULT '1' COMMENT '权限类型：1菜单 2按钮',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间（软删除）',
  PRIMARY KEY (`id`),
  KEY `idx_role_permissions_role` (`role_id`),
  KEY `idx_role_permissions_menu` (`menu_id`)
) ENGINE=InnoDB AUTO_INCREMENT=563 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `role_permissions`
--

LOCK TABLES `role_permissions` WRITE;
/*!40000 ALTER TABLE `role_permissions` DISABLE KEYS */;
INSERT INTO `role_permissions` VALUES (1,1,1,1,'2026-09-26 17:19:53','2026-09-26 17:19:53',NULL),(2,1,10,1,'2026-09-26 17:19:53','2026-09-26 17:19:53',NULL),(3,1,2,1,'2026-09-26 17:19:53','2026-09-26 17:19:53',NULL),(4,1,3,1,'2026-09-26 17:19:53','2026-09-26 17:19:53',NULL),(5,1,4,1,'2026-09-26 17:19:53','2026-09-26 17:19:53',NULL),(6,1,5,1,'2026-09-26 17:19:53','2026-09-26 17:19:53',NULL),(7,1,6,1,'2026-09-26 17:19:53','2026-09-26 17:19:53',NULL),(8,1,7,1,'2026-09-26 17:19:53','2026-09-26 17:19:53',NULL),(9,1,8,1,'2026-09-26 17:19:53','2026-09-26 17:19:53',NULL),(10,1,9,1,'2026-09-26 17:19:53','2026-09-26 17:19:53',NULL),(11,1,11,1,'2026-09-26 17:19:53','2026-09-26 17:19:53',NULL),(12,1,12,1,'2026-09-26 17:19:53','2026-09-26 17:19:53',NULL),(13,1,13,1,'2026-09-26 17:19:53','2026-09-26 17:19:53',NULL),(14,1,14,1,'2026-09-26 17:19:53','2026-09-26 17:19:53',NULL),(15,1,15,1,'2026-09-26 17:19:53','2026-09-26 17:19:53',NULL),(16,1,16,1,'2026-09-26 17:19:53','2026-09-26 17:19:53',NULL),(17,1,17,1,'2026-09-26 17:19:53','2026-09-26 17:19:53',NULL),(32,2,2,1,'2026-09-26 17:19:53','2026-09-26 17:19:53',NULL),(34,3,2,1,'2026-09-26 17:19:53','2026-09-26 17:19:53',NULL),(35,4,2,1,'2026-09-26 17:19:53','2026-09-26 17:19:53',NULL),(36,4,11,1,'2026-09-26 17:19:53','2026-09-26 17:19:53',NULL),(37,4,12,1,'2026-09-26 17:19:53','2026-09-26 17:19:53',NULL),(38,4,13,1,'2026-09-26 17:19:53','2026-09-26 17:19:53',NULL),(39,4,15,1,'2026-09-26 17:19:53','2026-09-26 17:19:53',NULL),(40,5,2,1,'2026-09-26 17:19:53','2026-09-26 17:19:53',NULL),(41,1,18,1,'2026-09-28 14:53:58','2026-09-28 14:53:58',NULL),(42,1,20,1,'2026-09-28 14:53:58','2026-09-28 14:53:58',NULL),(43,1,22,1,'2026-09-28 14:53:58','2026-09-28 14:53:58',NULL),(44,1,21,1,'2026-09-28 14:53:58','2026-09-28 14:53:58',NULL),(45,1,19,1,'2026-09-28 14:53:58','2026-09-28 14:53:58',NULL),(48,2,18,1,'2026-09-28 14:53:58','2026-09-28 14:53:58',NULL),(51,1,23,1,'2026-09-28 16:45:33','2026-09-28 16:45:33',NULL),(52,1,24,1,'2026-09-28 17:11:03','2026-09-28 17:11:03',NULL),(53,1,25,1,'2026-09-28 17:59:51','2026-09-28 17:59:51',NULL),(54,1,26,1,'2026-09-28 17:59:51','2026-09-28 17:59:51',NULL),(56,1,27,1,'2026-09-28 18:04:12','2026-09-28 18:04:12',NULL),(57,1,29,1,'2026-09-28 18:43:39','2026-09-28 18:43:39',NULL),(58,1,30,1,'2026-09-28 18:43:39','2026-09-28 18:43:39',NULL),(59,1,31,1,'2026-09-28 18:43:39','2026-09-28 18:43:39',NULL),(60,1,32,1,'2026-09-28 18:43:39','2026-09-28 18:43:39',NULL),(61,1,33,1,'2026-09-28 18:43:39','2026-09-28 18:43:39',NULL),(62,6,2,1,'2026-10-15 10:32:37','2026-10-15 10:32:37',NULL),(63,6,34,1,'2026-10-15 10:32:37','2026-10-15 10:32:37',NULL),(64,6,32,1,'2026-10-15 10:32:37','2026-10-15 10:32:37',NULL),(65,6,33,1,'2026-10-15 10:32:37','2026-10-15 10:32:37',NULL),(66,6,30,1,'2026-10-15 10:32:37','2026-10-15 10:32:37',NULL),(67,6,31,1,'2026-10-15 10:32:37','2026-10-15 10:32:37',NULL),(68,6,19,1,'2026-10-15 10:32:37','2026-10-15 10:32:37',NULL),(69,6,36,2,'2026-10-19 10:47:11','2026-10-19 10:47:11',NULL),(70,6,39,2,'2026-10-19 10:47:11','2026-10-19 10:47:11',NULL),(71,6,38,2,'2026-10-19 10:47:11','2026-10-19 10:47:11',NULL),(72,2,31,2,'2026-10-19 10:47:11','2026-10-19 10:47:11',NULL),(73,2,30,2,'2026-10-19 10:47:11','2026-10-19 10:47:11',NULL),(74,2,33,2,'2026-10-19 10:47:11','2026-10-19 10:47:11',NULL),(75,2,32,2,'2026-10-19 10:47:11','2026-10-19 10:47:11',NULL),(76,3,31,2,'2026-10-19 10:47:11','2026-10-19 10:47:11',NULL),(77,3,30,2,'2026-10-19 10:47:11','2026-10-19 10:47:11',NULL),(78,3,33,2,'2026-10-19 10:47:11','2026-10-19 10:47:11',NULL),(79,3,32,2,'2026-10-19 10:47:11','2026-10-19 10:47:11',NULL),(80,4,31,2,'2026-10-19 10:47:11','2026-10-19 10:47:11',NULL),(81,4,30,2,'2026-10-19 10:47:11','2026-10-19 10:47:11',NULL),(82,4,33,2,'2026-10-19 10:47:11','2026-10-19 10:47:11',NULL),(83,4,32,2,'2026-10-19 10:47:11','2026-10-19 10:47:11',NULL),(84,5,31,2,'2026-10-19 10:47:11','2026-10-19 10:47:11',NULL),(85,5,30,2,'2026-10-19 10:47:11','2026-10-19 10:47:11',NULL),(86,5,33,2,'2026-10-19 10:47:11','2026-10-19 10:47:11',NULL),(87,5,32,2,'2026-10-19 10:47:11','2026-10-19 10:47:11',NULL),(103,5,41,1,'2026-10-19 11:32:40','2026-10-19 11:32:40',NULL),(104,5,42,1,'2026-10-19 11:32:40','2026-10-19 11:32:40',NULL),(105,1,41,1,'2026-10-19 11:32:40','2026-10-19 11:32:40',NULL),(106,1,42,1,'2026-10-19 11:32:40','2026-10-19 11:32:40',NULL),(107,1,43,1,'2026-10-19 11:32:40','2026-10-19 11:32:40',NULL),(108,5,44,1,'2026-10-19 14:19:50','2026-10-19 14:19:50',NULL),(109,1,44,1,'2026-10-19 14:19:50','2026-10-19 14:19:50',NULL),(110,5,45,1,'2026-10-19 14:19:50','2026-10-19 14:19:50',NULL),(111,1,45,1,'2026-10-19 14:19:50','2026-10-19 14:19:50',NULL),(112,5,46,1,'2026-10-19 14:19:50','2026-10-19 14:19:50',NULL),(113,1,46,1,'2026-10-19 14:19:50','2026-10-19 14:19:50',NULL),(114,5,47,1,'2026-10-19 14:19:50','2026-10-19 14:19:50',NULL),(115,1,47,1,'2026-10-19 14:19:50','2026-10-19 14:19:50',NULL),(116,5,48,1,'2026-10-19 14:19:50','2026-10-19 14:19:50',NULL),(117,1,48,1,'2026-10-19 14:19:50','2026-10-19 14:19:50',NULL),(126,1,37,1,'2026-10-20 09:40:42','2026-10-20 09:40:42',NULL),(127,1,35,1,'2026-10-20 09:40:42','2026-10-20 09:40:42',NULL),(128,4,37,1,'2026-10-20 09:40:42','2026-10-20 09:40:42',NULL),(131,1,49,1,'2026-10-20 09:58:39','2026-10-20 09:58:39',NULL),(132,5,43,1,'2026-10-20 10:01:57','2026-10-20 10:01:57',NULL),(133,7,2,1,'2026-10-20 14:42:08','2026-10-20 14:42:08',NULL),(135,7,36,1,'2026-10-20 14:42:08','2026-10-20 14:42:08',NULL),(136,7,32,1,'2026-10-20 14:42:08','2026-10-20 14:42:08',NULL),(137,7,33,1,'2026-10-20 14:42:08','2026-10-20 14:42:08',NULL),(138,7,30,1,'2026-10-20 14:42:08','2026-10-20 14:42:08',NULL),(139,7,31,1,'2026-10-20 14:42:08','2026-10-20 14:42:08',NULL),(140,7,60,1,'2026-10-20 14:42:08','2026-10-20 14:42:08',NULL),(141,7,24,1,'2026-10-20 14:42:08','2026-10-20 14:42:08',NULL),(142,7,25,1,'2026-10-20 14:42:08','2026-10-20 14:42:08',NULL),(143,7,56,1,'2026-10-20 14:42:08','2026-10-20 14:42:08',NULL),(144,7,59,1,'2026-10-20 14:42:08','2026-10-20 14:42:08',NULL),(145,7,57,1,'2026-10-20 14:42:08','2026-10-20 14:42:08',NULL),(146,7,55,1,'2026-10-20 14:42:08','2026-10-20 14:42:08',NULL),(147,7,58,1,'2026-10-20 14:42:08','2026-10-20 14:42:08',NULL),(148,7,52,1,'2026-10-20 14:42:08','2026-10-20 14:42:08',NULL),(149,7,54,1,'2026-10-20 14:42:08','2026-10-20 14:42:08',NULL),(150,7,53,1,'2026-10-20 14:42:08','2026-10-20 14:42:08',NULL),(151,7,51,1,'2026-10-20 14:42:08','2026-10-20 14:42:08',NULL),(154,7,26,1,'2026-10-20 14:42:08','2026-10-20 14:42:08',NULL),(156,7,13,1,'2026-10-20 14:42:08','2026-10-20 14:42:08',NULL),(158,7,11,1,'2026-10-20 14:42:08','2026-10-20 14:42:08',NULL),(159,7,15,1,'2026-10-20 14:42:08','2026-10-20 14:42:08',NULL),(165,4,52,1,'2026-10-20 14:42:08','2026-10-20 14:42:08',NULL),(166,4,51,1,'2026-10-20 14:42:08','2026-10-20 14:42:08',NULL),(167,4,16,1,'2026-10-20 14:42:08','2026-10-20 14:42:08',NULL),(171,4,53,1,'2026-10-20 16:21:53','2026-10-20 16:21:53',NULL),(172,4,61,1,'2026-10-20 19:08:01','2026-10-20 19:08:01',NULL),(173,7,61,1,'2026-10-20 19:08:01','2026-10-20 19:08:01',NULL),(175,4,62,1,'2026-10-20 19:33:31','2026-10-20 19:33:31',NULL),(176,4,63,1,'2026-10-20 19:33:31','2026-10-20 19:33:31',NULL),(179,2,65,1,'2026-10-21 14:50:17','2026-10-21 14:50:17',NULL),(180,2,64,1,'2026-10-21 14:50:17','2026-10-21 14:50:17',NULL),(182,1,85,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(183,1,66,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(184,1,68,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(185,1,70,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(186,1,69,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(187,1,67,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(188,1,71,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(189,1,72,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(190,1,81,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(191,1,75,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(192,1,73,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(193,1,74,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(194,1,76,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(195,1,84,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(196,1,78,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(197,1,79,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(198,1,77,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(199,1,83,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(200,1,82,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(201,1,80,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(213,8,2,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(214,8,85,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(215,8,32,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(216,8,33,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(217,8,30,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(218,8,31,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(219,8,66,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(220,8,68,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(221,8,70,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(222,8,69,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(223,8,67,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(224,8,71,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(225,8,72,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(226,8,75,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(227,8,73,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(228,8,74,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(229,8,76,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(230,8,78,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(231,8,79,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(232,8,77,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(244,2,73,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(245,2,74,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(246,2,77,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(247,2,80,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(251,3,81,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(252,3,84,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(253,3,83,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(254,3,82,1,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(258,1,86,2,'2026-10-21 16:09:47','2026-10-21 16:09:47',NULL),(259,6,86,2,'2026-10-21 16:09:47','2026-10-21 16:09:47',NULL),(260,2,86,2,'2026-10-21 16:09:47','2026-10-21 16:09:47',NULL),(261,8,86,2,'2026-10-21 16:09:47','2026-10-21 16:09:47',NULL),(265,1,87,2,'2026-10-21 16:09:47','2026-10-21 16:09:47',NULL),(266,8,100,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(267,7,100,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(268,6,100,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(269,5,100,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(270,4,100,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(271,3,100,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(272,2,100,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(273,1,100,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(274,8,96,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(275,7,96,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(276,6,96,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(277,5,96,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(278,4,96,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(279,3,96,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(280,2,96,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(281,1,96,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(282,8,97,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(283,7,97,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(284,6,97,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(285,5,97,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(286,4,97,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(287,3,97,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(288,2,97,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(289,1,97,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(290,8,99,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(291,7,99,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(292,6,99,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(293,5,99,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(294,4,99,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(295,3,99,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(296,2,99,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(297,1,99,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(298,8,101,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(299,7,101,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(300,6,101,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(301,5,101,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(302,4,101,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(303,3,101,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(304,2,101,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(305,1,101,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(306,8,98,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(307,7,98,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(308,6,98,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(309,5,98,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(310,4,98,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(311,3,98,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(312,2,98,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(313,1,98,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(314,8,95,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(315,7,95,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(316,6,95,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(317,5,95,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(318,4,95,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(319,3,95,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(320,2,95,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(321,1,95,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(322,8,94,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(323,7,94,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(324,6,94,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(325,5,94,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(326,4,94,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(327,3,94,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(328,2,94,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(329,1,94,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(330,8,104,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(331,7,104,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(332,6,104,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(333,5,104,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(334,4,104,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(335,3,104,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(336,2,104,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(337,1,104,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(338,8,106,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(339,7,106,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(340,6,106,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(341,5,106,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(342,4,106,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(343,3,106,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(344,2,106,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(345,1,106,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(346,8,105,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(347,7,105,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(348,6,105,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(349,5,105,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(350,4,105,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(351,3,105,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(352,2,105,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(353,1,105,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(354,8,103,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(355,7,103,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(356,6,103,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(357,5,103,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(358,4,103,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(359,3,103,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(360,2,103,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(361,1,103,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(362,8,102,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(363,7,102,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(364,6,102,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(365,5,102,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(366,4,102,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(367,3,102,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(368,2,102,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(369,1,102,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(370,8,88,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(371,7,88,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(372,6,88,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(373,5,88,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(374,4,88,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(375,3,88,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(376,2,88,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(377,1,88,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(378,8,91,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(379,7,91,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(380,6,91,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(381,5,91,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(382,4,91,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(383,3,91,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(384,2,91,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(385,1,91,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(386,8,90,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(387,7,90,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(388,6,90,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(389,5,90,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(390,4,90,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(391,3,90,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(392,2,90,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(393,1,90,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(394,8,89,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(395,7,89,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(396,6,89,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(397,5,89,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(398,4,89,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(399,3,89,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(400,2,89,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(401,1,89,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(402,8,92,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(403,7,92,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(404,6,92,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(405,5,92,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(406,4,92,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(407,3,92,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(408,2,92,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(409,1,92,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(410,8,93,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(411,7,93,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(412,6,93,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(413,5,93,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(414,4,93,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(415,3,93,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(416,2,93,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(417,1,93,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(521,2,8,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(522,3,8,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(523,4,8,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(524,5,8,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(525,6,8,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(526,7,8,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(527,8,8,1,'2026-10-23 09:30:44','2026-10-23 09:30:44',NULL),(528,1,115,2,'2026-10-23 18:54:26','2026-10-23 18:54:26',NULL),(529,1,112,2,'2026-10-23 18:54:26','2026-10-23 18:54:26',NULL),(530,1,111,2,'2026-10-23 18:54:26','2026-10-23 18:54:26',NULL),(531,1,114,2,'2026-10-23 18:54:26','2026-10-23 18:54:26',NULL),(532,1,113,2,'2026-10-23 18:54:26','2026-10-23 18:54:26',NULL),(533,1,109,2,'2026-10-23 18:54:26','2026-10-23 18:54:26',NULL),(534,1,107,1,'2026-10-23 18:54:26','2026-10-23 18:54:26',NULL),(535,1,110,2,'2026-10-23 18:54:26','2026-10-23 18:54:26',NULL),(536,1,108,1,'2026-10-23 18:54:26','2026-10-23 18:54:26',NULL),(543,10,115,2,'2026-10-23 18:54:26','2026-10-23 18:54:26',NULL),(547,7,116,1,'2026-10-23 19:21:51','2026-10-23 19:21:51',NULL),(549,6,40,1,'2026-10-23 19:21:51','2026-10-23 19:21:51',NULL),(550,6,44,1,'2026-10-23 19:21:51','2026-10-23 19:21:51',NULL),(552,4,116,1,'2026-10-23 19:23:51','2026-10-23 19:23:51',NULL),(553,1,118,1,'2026-10-23 19:53:06','2026-10-23 19:53:06',NULL),(554,1,117,1,'2026-10-23 19:53:06','2026-10-23 19:53:06',NULL),(556,5,119,1,'2026-10-24 11:52:37','2026-10-24 11:52:37',NULL),(557,6,119,1,'2026-10-24 11:52:37','2026-10-24 11:52:37',NULL),(558,4,119,1,'2026-10-24 11:52:37','2026-10-24 11:52:37',NULL),(559,7,119,1,'2026-10-24 11:52:37','2026-10-24 11:52:37',NULL);
/*!40000 ALTER TABLE `role_permissions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `roles`
--

DROP TABLE IF EXISTS `roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `roles` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色名称',
  `role_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色编码（如 admin, employee, dept_manager）',
  `role_type` tinyint NOT NULL DEFAULT '1' COMMENT '角色类型：1系统角色 2业务角色 3动态角色',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '角色描述',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0停用 1启用',
  `is_system` tinyint NOT NULL DEFAULT '0' COMMENT '是否系统内置：0否 1是（内置不可删除）',
  `data_scope` tinyint NOT NULL DEFAULT '1' COMMENT '数据权限范围：1全部 2本部门 3本部门及以下 4本人 5自定义',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间（软删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `role_code` (`role_code`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `roles`
--

LOCK TABLES `roles` WRITE;
/*!40000 ALTER TABLE `roles` DISABLE KEYS */;
INSERT INTO `roles` VALUES (1,'系统管理员','admin',1,'系统内置管理员',1,1,1,1,'2026-09-23 08:48:12','2026-09-23 08:48:12',NULL),(2,'部门主管','manager',1,'负责本部门员工管理',2,1,0,3,'2026-09-26 17:19:53','2026-10-22 10:48:04',NULL),(3,'普通员工','employee',1,'查看公告与个人信息',3,1,0,4,'2026-09-26 17:19:53','2026-10-20 17:37:39',NULL),(4,'人事专员','hr',1,'负责人事相关事务',4,1,0,2,'2026-09-26 17:19:53','2026-09-26 17:19:53',NULL),(5,'财务专员','finance',1,'负责财务相关事务',5,1,0,2,'2026-09-26 17:19:53','2026-09-26 17:19:53',NULL),(6,'总经理/高管','gm',1,'全局数据查看与高级别流程审批',6,1,0,1,'2026-10-15 10:32:37','2026-10-15 10:32:37',NULL),(7,'人事/行政主管','hr_manager',1,'人事模块审批权，全局出勤/人事数据查看，负责补签终审与申诉审核',7,1,1,3,'2026-10-20 14:42:08','2026-10-20 14:42:08',NULL),(8,'项目经理','project_manager',1,'负责项目立项、成员管理、任务分派与验收，只能看自己项目的数据',8,1,0,5,'2026-10-21 15:16:22','2026-10-21 15:16:22',NULL),(10,'访客','guest',3,'可选角色，默认不启用；只能查看公开公告，不能进入内部业务模块',99,1,0,4,'2026-10-23 18:54:26','2026-10-24 11:52:37',NULL);
/*!40000 ALTER TABLE `roles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shift_schedules`
--

DROP TABLE IF EXISTS `shift_schedules`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `shift_schedules` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `schedule_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `schedule_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'once指定日期 weekly每周 range日期范围',
  `schedule_date` date DEFAULT NULL COMMENT 'once时指定日期',
  `week_day` int DEFAULT NULL COMMENT 'weekly时星期几0=周日1=周一',
  `start_date` date DEFAULT NULL,
  `end_date` date DEFAULT NULL,
  `work_start` time DEFAULT NULL,
  `work_end` time DEFAULT NULL,
  `late_threshold` time DEFAULT NULL,
  `early_leave_threshold` time DEFAULT NULL,
  `is_active` tinyint DEFAULT '1',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `lunar_month` int DEFAULT NULL,
  `lunar_day` int DEFAULT NULL,
  `yearly_month` int DEFAULT NULL,
  `yearly_day` int DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shift_schedules`
--

LOCK TABLES `shift_schedules` WRITE;
/*!40000 ALTER TABLE `shift_schedules` DISABLE KEYS */;
INSERT INTO `shift_schedules` VALUES (1,'中秋模板','once','2027-10-26',NULL,NULL,NULL,'09:00:00','18:00:00','09:30:00','17:30:00',1,'2026-10-03 14:32:36',NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `shift_schedules` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shift_templates`
--

DROP TABLE IF EXISTS `shift_templates`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `shift_templates` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `template_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `work_start` time DEFAULT NULL,
  `work_end` time DEFAULT NULL,
  `late_threshold` time DEFAULT NULL,
  `early_leave_threshold` time DEFAULT NULL,
  `shift_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'day',
  `is_all_day` tinyint NOT NULL DEFAULT '0',
  `is_active` tinyint DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=27 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shift_templates`
--

LOCK TABLES `shift_templates` WRITE;
/*!40000 ALTER TABLE `shift_templates` DISABLE KEYS */;
INSERT INTO `shift_templates` VALUES (1,'标准班(默认)','09:00:00','18:00:00','09:30:00','17:30:00','day',0,0,'2026-10-01 15:47:46'),(8,'','23:00:00','23:40:00','23:59:00','22:59:00','day',0,0,'2026-10-03 16:18:12'),(9,'','04:00:00','23:00:00','22:31:00','22:00:00','day',0,0,'2026-10-03 16:20:31'),(10,'','16:20:00','16:30:00','16:26:00','16:23:00','day',0,0,'2026-10-03 16:23:12'),(11,'','09:00:00','18:00:00','09:30:00','17:30:00','day',0,0,'2026-10-03 16:24:40'),(26,'全天可打卡','00:00:00','23:59:00','23:59:00','00:00:00','day',1,1,'2026-10-05 16:02:26');
/*!40000 ALTER TABLE `shift_templates` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `system_config`
--

DROP TABLE IF EXISTS `system_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `system_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `config_key` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `config_value` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `config_key` (`config_key`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `system_config`
--

LOCK TABLES `system_config` WRITE;
/*!40000 ALTER TABLE `system_config` DISABLE KEYS */;
INSERT INTO `system_config` VALUES (1,'work_start','09:00','上班时间'),(2,'work_end','18:00','下班时间'),(3,'late_threshold','09:30','迟到判定线'),(4,'early_leave_threshold','17:30','严重早退判定线'),(5,'re_sign_window_hours','2','重新签到窗口(小时)'),(6,'max_re_sign','3','每天最多重新签到次数');
/*!40000 ALTER TABLE `system_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `system_params`
--

DROP TABLE IF EXISTS `system_params`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `system_params` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '参数ID',
  `param_key` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '参数键（如 system.name, file.max_size）',
  `param_value` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '参数值',
  `param_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '参数名称',
  `param_group` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '参数分组（如：基础配置：：通知配置：：出勤配置：：文件配置：）',
  `param_type` tinyint NOT NULL DEFAULT '1' COMMENT '参数类型：1字符串 2数字 3布尔 4JSON',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '同组内排序号',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0停用 1启用',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '参数说明',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间（软删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `param_key` (`param_key`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统参数表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `system_params`
--

LOCK TABLES `system_params` WRITE;
/*!40000 ALTER TABLE `system_params` DISABLE KEYS */;
INSERT INTO `system_params` VALUES (1,'geo_check_enable','0','签到地理校验开关（0关闭 1开启）','出勤管理',2,0,1,'0关闭 1开启','2026-10-04 14:32:58','2026-10-23 19:09:12',NULL),(2,'geo_lat','23.1320','公司纬度','出勤管理',2,0,1,'签到范围校验用','2026-10-04 14:32:58','2026-10-23 19:09:12',NULL),(3,'geo_lng','113.3220','公司经度','出勤管理',2,0,1,'签到范围校验用','2026-10-04 14:32:58','2026-10-23 19:09:12',NULL),(4,'geo_radius','500','签到允许半径(米)','出勤管理',2,0,1,'默认500米','2026-10-04 14:32:58','2026-10-23 19:09:12',NULL),(5,'attendance_weekend_count','0','周末是否计入出勤（0否 1是）','出勤管理',2,0,1,NULL,'2026-10-05 15:37:15','2026-10-23 19:09:12',NULL);
/*!40000 ALTER TABLE `system_params` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `task_comments`
--

DROP TABLE IF EXISTS `task_comments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `task_comments` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `task_id` bigint NOT NULL COMMENT '任务ID',
  `user_id` bigint NOT NULL COMMENT '评论人ID',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父评论ID（0表示顶级评论）',
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '评论内容',
  `mention_users` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '@用户ID列表（逗号分隔，用于通知）',
  `attachment_id` bigint DEFAULT NULL COMMENT '附件ID',
  `comment_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
  `is_edited` tinyint NOT NULL DEFAULT '0' COMMENT '是否已编辑：0否 1是',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间（软删除）',
  PRIMARY KEY (`id`),
  KEY `idx_task_comments_task` (`task_id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务评论表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `task_comments`
--

LOCK TABLES `task_comments` WRITE;
/*!40000 ALTER TABLE `task_comments` DISABLE KEYS */;
/*!40000 ALTER TABLE `task_comments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `task_members`
--

DROP TABLE IF EXISTS `task_members`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `task_members` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `task_id` bigint NOT NULL COMMENT '任务ID',
  `user_id` bigint NOT NULL COMMENT '成员用户ID',
  `role` tinyint NOT NULL DEFAULT '1' COMMENT '成员角色：1负责人 2参与人 3关注人 4验收人',
  `join_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间（软删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_members_task_user` (`task_id`,`user_id`),
  KEY `idx_task_members_user` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=102 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务成员表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `task_members`
--

LOCK TABLES `task_members` WRITE;
/*!40000 ALTER TABLE `task_members` DISABLE KEYS */;
INSERT INTO `task_members` VALUES (70,990011,4,1,'2026-10-16 10:49:11','2026-10-16 10:49:10','2026-10-16 10:49:10',NULL),(71,990011,3,3,'2026-10-16 10:49:11','2026-10-16 10:49:10','2026-10-16 10:49:10',NULL),(74,990013,1,3,'2026-10-22 09:53:22','2026-10-22 09:53:22','2026-10-22 09:53:22',NULL),(75,990013,9,1,'2026-10-22 09:54:25','2026-10-22 09:54:25','2026-10-22 09:54:25',NULL);
/*!40000 ALTER TABLE `task_members` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `task_operation_logs`
--

DROP TABLE IF EXISTS `task_operation_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `task_operation_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `task_id` bigint NOT NULL COMMENT '任务ID',
  `operator_id` bigint NOT NULL COMMENT '操作人ID',
  `action` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '操作类型（如 create, assign, accept, submit, approve, reject, update_status）',
  `old_value` text COLLATE utf8mb4_unicode_ci COMMENT '旧值（JSON格式，记录变更前的字段）',
  `new_value` text COLLATE utf8mb4_unicode_ci COMMENT '新值（JSON格式，记录变更后的字段）',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作备注',
  `operation_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间（软删除）',
  PRIMARY KEY (`id`),
  KEY `idx_task_logs_task` (`task_id`)
) ENGINE=InnoDB AUTO_INCREMENT=175 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务操作日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `task_operation_logs`
--

LOCK TABLES `task_operation_logs` WRITE;
/*!40000 ALTER TABLE `task_operation_logs` DISABLE KEYS */;
INSERT INTO `task_operation_logs` VALUES (131,990012,13,'create',NULL,'状态=1','创建任务','2026-10-21 15:25:17','2026-10-21 15:25:17','2026-10-21 15:25:17',NULL),(132,990012,4,'accept','1','2','接受任务','2026-10-21 15:25:17','2026-10-21 15:25:17','2026-10-21 15:25:17',NULL),(133,990012,4,'submit','2','3','提交验收','2026-10-21 15:25:18','2026-10-21 15:25:17','2026-10-21 15:25:17',NULL),(134,990012,13,'approve','3','4','验收通过','2026-10-21 15:25:18','2026-10-21 15:25:17','2026-10-21 15:25:17',NULL),(135,990013,1,'create',NULL,'状态=0','创建任务','2026-10-22 09:53:22','2026-10-22 09:53:22','2026-10-22 09:53:22',NULL),(136,990013,9,'claim','0','2','认领任务','2026-10-22 09:54:25','2026-10-22 09:54:25','2026-10-22 09:54:25',NULL),(137,990013,9,'update_progress','0','25','更新进度为 25%','2026-10-22 09:54:46','2026-10-22 09:54:45','2026-10-22 09:54:45',NULL),(138,990014,1,'create',NULL,'状态=1','创建任务','2026-10-22 09:59:51','2026-10-22 09:59:50','2026-10-22 09:59:50',NULL),(139,990015,1,'create',NULL,'状态=1','创建任务','2026-10-22 10:06:22','2026-10-22 10:06:21','2026-10-22 10:06:21',NULL),(140,990015,4,'accept','1','2','接受任务','2026-10-22 10:06:47','2026-10-22 10:06:46','2026-10-22 10:06:46',NULL),(141,990015,4,'submit','2','3','提交验收','2026-10-22 10:06:47','2026-10-22 10:06:46','2026-10-22 10:06:46',NULL),(142,990013,9,'update_progress','25','40','更新进度为 40%：audit probe','2026-10-22 10:22:05','2026-10-22 10:22:04','2026-10-22 10:22:04',NULL),(143,990013,9,'update_progress','40','25','更新进度为 25%：audit probe restore','2026-10-22 10:22:05','2026-10-22 10:22:04','2026-10-22 10:22:04',NULL),(144,990016,13,'create',NULL,'状态=1','创建任务','2026-10-22 10:25:53','2026-10-22 10:25:53','2026-10-22 10:25:53',NULL),(145,990017,3,'create',NULL,'状态=1','创建任务','2026-10-22 10:43:38','2026-10-22 10:43:37','2026-10-22 10:43:37',NULL),(146,990017,18,'accept','1','2','接受任务','2026-10-22 10:43:38','2026-10-22 10:43:37','2026-10-22 10:43:37',NULL),(147,990017,18,'update_progress','0','100','更新进度为 100%：完成','2026-10-22 10:43:38','2026-10-22 10:43:37','2026-10-22 10:43:37',NULL),(148,990017,18,'submit','2','3','提交验收','2026-10-22 10:43:38','2026-10-22 10:43:37','2026-10-22 10:43:37',NULL),(149,990017,3,'approve','3','4','验收通过','2026-10-22 10:43:38','2026-10-22 10:43:37','2026-10-22 10:43:37',NULL),(150,990018,13,'create',NULL,'状态=1','创建任务','2026-10-22 11:12:19','2026-10-22 11:12:19','2026-10-22 11:12:19',NULL),(151,990018,4,'accept','1','2','接受任务','2026-10-22 11:12:20','2026-10-22 11:12:20','2026-10-22 11:12:20',NULL),(152,990018,4,'update_progress','0','100','更新进度为 100%','2026-10-22 11:12:20','2026-10-22 11:12:20','2026-10-22 11:12:20',NULL),(153,990018,4,'submit','2','3','提交验收','2026-10-22 11:12:20','2026-10-22 11:12:20','2026-10-22 11:12:20',NULL),(154,990018,13,'approve','3','4','验收通过','2026-10-22 11:12:21','2026-10-22 11:12:20','2026-10-22 11:12:20',NULL),(155,990019,1,'create',NULL,'状态=1','创建任务','2026-10-23 09:35:21','2026-10-23 09:35:21','2026-10-23 09:35:21',NULL),(156,990019,4,'accept','1','2','接受任务','2026-10-23 09:35:22','2026-10-23 09:35:21','2026-10-23 09:35:21',NULL),(157,990019,4,'update_progress','0','40','更新进度为 40%','2026-10-23 09:35:22','2026-10-23 09:35:21','2026-10-23 09:35:21',NULL),(158,990019,4,'submit','2','3','提交验收','2026-10-23 09:35:22','2026-10-23 09:35:21','2026-10-23 09:35:21',NULL),(159,990019,4,'comment',NULL,NULL,'发表评论','2026-10-23 09:35:22','2026-10-23 09:35:21','2026-10-23 09:35:21',NULL),(160,990020,1,'create',NULL,'状态=1','创建任务','2026-10-23 09:35:22','2026-10-23 09:35:21','2026-10-23 09:35:21',NULL),(161,990020,4,'reject','1','6','拒绝任务：当前手上有三个紧急需求，来不及','2026-10-23 09:35:22','2026-10-23 09:35:21','2026-10-23 09:35:21',NULL),(173,990023,1,'create',NULL,'状态=1','创建任务','2026-10-23 20:04:52','2026-10-23 20:04:51','2026-10-23 20:04:51',NULL),(174,990024,1,'create',NULL,'状态=1','创建任务','2026-10-23 20:05:11','2026-10-23 20:05:10','2026-10-23 20:05:10',NULL);
/*!40000 ALTER TABLE `task_operation_logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tasks`
--

DROP TABLE IF EXISTS `tasks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tasks` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `task_no` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务编号（如 TASK-20260911-0001）',
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务名称',
  `description` text COLLATE utf8mb4_unicode_ci COMMENT '任务描述',
  `project_id` bigint DEFAULT NULL COMMENT '所属项目ID',
  `task_type` tinyint NOT NULL DEFAULT '1' COMMENT '任务类型：1开发任务 2测试任务 3运维任务 4设计任务 5其他',
  `priority` tinyint NOT NULL DEFAULT '2' COMMENT '优先级：1低 2中 3高 4紧急',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0待分配 1待接受 2进行中 3待验收 4已完成 5已撤销 6已拒绝',
  `creator_id` bigint NOT NULL COMMENT '创建人ID',
  `assignee_id` bigint DEFAULT NULL COMMENT '负责人ID（被指派人）',
  `plan_start_time` datetime DEFAULT NULL COMMENT '计划开始时间',
  `plan_end_time` datetime DEFAULT NULL COMMENT '计划结束时间',
  `actual_start_time` datetime DEFAULT NULL COMMENT '实际开始时间',
  `actual_end_time` datetime DEFAULT NULL COMMENT '实际结束时间',
  `plan_work_hours` decimal(6,2) DEFAULT NULL COMMENT '计划工时（小时）',
  `actual_work_hours` decimal(6,2) DEFAULT NULL COMMENT '实际工时（小时）',
  `progress` tinyint NOT NULL DEFAULT '0' COMMENT '进度百分比（0-100）',
  `attachment_id` bigint DEFAULT NULL COMMENT '附件ID',
  `tags` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '标签（逗号分隔，如：前端,紧急）',
  `accept_time` datetime DEFAULT NULL COMMENT '接受时间',
  `submit_time` datetime DEFAULT NULL COMMENT '提交验收时间',
  `reject_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '拒绝原因',
  `complete_note` text COLLATE utf8mb4_unicode_ci COMMENT '完成说明（提交完成时填写）',
  `is_overdue` tinyint NOT NULL DEFAULT '0' COMMENT '是否逾期：0否 1是（定时任务自动标记）',
  `due_reminded` tinyint NOT NULL DEFAULT '0' COMMENT '到期前24小时是否已提醒：0否1是',
  `archive_status` tinyint NOT NULL DEFAULT '0' COMMENT '归档状态：0未归档 1已归档（完成超过30天自动归档）',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间（软删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `task_no` (`task_no`),
  KEY `idx_tasks_assignee_status` (`assignee_id`,`status`),
  KEY `idx_tasks_creator_status` (`creator_id`,`status`),
  KEY `idx_tasks_project_status` (`project_id`,`status`),
  KEY `idx_tasks_plan_end_status` (`plan_end_time`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=990025 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务主表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tasks`
--

LOCK TABLES `tasks` WRITE;
/*!40000 ALTER TABLE `tasks` DISABLE KEYS */;
INSERT INTO `tasks` VALUES (990011,'TASK-20261016-104910649','市场部季度数据整理','',0,1,2,1,3,4,NULL,NULL,NULL,NULL,NULL,NULL,0,NULL,NULL,NULL,NULL,NULL,NULL,0,0,0,'2026-10-16 10:49:10','2026-10-23 18:44:37',NULL),(990013,'TASK-20261022-095322904','JdbcPro','开发一款为本公司量身的数据连接技术框架',6,1,3,2,1,9,'2026-10-22 00:00:00','2027-05-31 00:00:00','2026-10-22 09:54:25',NULL,1200.00,NULL,25,NULL,NULL,'2026-10-22 09:54:25',NULL,NULL,NULL,0,0,0,'2026-10-22 09:53:22','2026-10-22 09:53:22',NULL);
/*!40000 ALTER TABLE `tasks` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `titles`
--

DROP TABLE IF EXISTS `titles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `titles` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '职称ID',
  `title_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '职称名称',
  `code` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '职称编号',
  `title_level` tinyint NOT NULL DEFAULT '1' COMMENT '职称等级：1初级 2中级 3高级 4正高',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0停用 1启用',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注说明',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间（软删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='职称表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `titles`
--

LOCK TABLES `titles` WRITE;
/*!40000 ALTER TABLE `titles` DISABLE KEYS */;
INSERT INTO `titles` VALUES (1,'测试职称','test_title',1,1,0,'','2026-10-04 15:20:22','2026-10-04 15:21:25','2026-10-04 15:21:25');
/*!40000 ALTER TABLE `titles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tool_downloads`
--

DROP TABLE IF EXISTS `tool_downloads`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tool_downloads` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '工具名称（如：VPN客户端：：Git安装包：：项目模板：）',
  `description` text COLLATE utf8mb4_unicode_ci COMMENT '工具描述（功能说明、使用方法、版本信息）',
  `category_id` bigint DEFAULT NULL COMMENT '分类ID',
  `file_id` bigint DEFAULT NULL COMMENT '附件ID（关联attachments表）',
  `download_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '外部下载链接（如官方下载页）',
  `version` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '版本号',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小（字节）',
  `platform` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '适用平台：Windows, macOS, Linux, All',
  `download_count` int NOT NULL DEFAULT '0' COMMENT '下载次数',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0下架 1上架',
  `uploader_id` bigint NOT NULL COMMENT '上传人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间（软删除）',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工具下载表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tool_downloads`
--

LOCK TABLES `tool_downloads` WRITE;
/*!40000 ALTER TABLE `tool_downloads` DISABLE KEYS */;
/*!40000 ALTER TABLE `tool_downloads` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_group_members`
--

DROP TABLE IF EXISTS `user_group_members`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_group_members` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `group_id` bigint NOT NULL COMMENT '群组ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `join_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间（软删除）',
  PRIMARY KEY (`id`),
  KEY `idx_user_group_members_group` (`group_id`),
  KEY `idx_user_group_members_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='群组成员关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_group_members`
--

LOCK TABLES `user_group_members` WRITE;
/*!40000 ALTER TABLE `user_group_members` DISABLE KEYS */;
/*!40000 ALTER TABLE `user_group_members` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_groups`
--

DROP TABLE IF EXISTS `user_groups`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_groups` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '群组ID',
  `group_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '群组名称',
  `group_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '群组编码',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '群组描述',
  `creator_id` bigint NOT NULL COMMENT '创建人ID',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0停用 1启用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间（软删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `group_code` (`group_code`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户群组表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_groups`
--

LOCK TABLES `user_groups` WRITE;
/*!40000 ALTER TABLE `user_groups` DISABLE KEYS */;
INSERT INTO `user_groups` VALUES (1,'前端组','fe_team','负责前端项目',1,1,'2026-09-28 18:30:00','2026-09-28 18:30:00',NULL),(2,'测试组','test_grp','走查测试',1,1,'2026-10-04 15:06:19','2026-10-04 15:22:57','2026-10-04 15:22:57');
/*!40000 ALTER TABLE `user_groups` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_handovers`
--

DROP TABLE IF EXISTS `user_handovers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_handovers` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '离职人',
  `receiver_id` bigint DEFAULT NULL COMMENT '接收人，保留模式下为空',
  `operator_id` bigint NOT NULL COMMENT '办理人',
  `reason` varchar(500) DEFAULT NULL COMMENT '交接原因',
  `effective_date` date DEFAULT NULL COMMENT '离职生效日期',
  `todo_count` int NOT NULL DEFAULT '0' COMMENT '转交的待办流程数',
  `running_count` int NOT NULL DEFAULT '0' COMMENT '转交的审批中发起流程数',
  `task_count` int NOT NULL DEFAULT '0',
  `project_count` int NOT NULL DEFAULT '0',
  `done_count` int NOT NULL DEFAULT '0' COMMENT '历史已办流程数（只统计不转交）',
  `item_summary` text COMMENT '交接内容清单',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime DEFAULT NULL,
  `handle_mode` varchar(20) NOT NULL DEFAULT 'transfer' COMMENT 'transfer 批量转交 / keep 保留待办 / auto 自动转直属上级',
  PRIMARY KEY (`id`),
  KEY `idx_handover_user` (`user_id`),
  KEY `idx_handover_receiver` (`receiver_id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='离职交接记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_handovers`
--

LOCK TABLES `user_handovers` WRITE;
/*!40000 ALTER TABLE `user_handovers` DISABLE KEYS */;
/*!40000 ALTER TABLE `user_handovers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_notification_preferences`
--

DROP TABLE IF EXISTS `user_notification_preferences`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_notification_preferences` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `pref_system` tinyint NOT NULL DEFAULT '1' COMMENT '系统通知开关(type=1)',
  `pref_flow` tinyint NOT NULL DEFAULT '1' COMMENT '审批待办开关(type=2)',
  `pref_task` tinyint NOT NULL DEFAULT '1' COMMENT '任务通知开关(type=3)',
  `pref_attendance` tinyint NOT NULL DEFAULT '1' COMMENT '出勤提醒开关(type=4)',
  `pref_worklog` tinyint NOT NULL DEFAULT '1' COMMENT '日志提醒开关(type=5)',
  `pref_sound` tinyint NOT NULL DEFAULT '1' COMMENT '新消息声音/标题提醒开关',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户通知偏好设置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_notification_preferences`
--

LOCK TABLES `user_notification_preferences` WRITE;
/*!40000 ALTER TABLE `user_notification_preferences` DISABLE KEYS */;
INSERT INTO `user_notification_preferences` VALUES (1,1,1,1,1,1,1,1,'2026-10-14 17:37:53','2026-10-14 17:53:56');
/*!40000 ALTER TABLE `user_notification_preferences` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_permissions`
--

DROP TABLE IF EXISTS `user_permissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_permissions` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` bigint NOT NULL COMMENT '被授权的用户',
  `menu_id` bigint NOT NULL COMMENT '权限（menus.id）',
  `granted_by` bigint DEFAULT NULL COMMENT '授权人',
  `source` tinyint NOT NULL DEFAULT '1' COMMENT '来源：1管理员授予 2派发任务自动开通',
  `reason` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '授予原因',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间（软删除）',
  PRIMARY KEY (`id`),
  KEY `idx_user_permissions_user` (`user_id`),
  KEY `idx_user_permissions_menu` (`menu_id`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户级额外权限（角色之外单独授予）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_permissions`
--

LOCK TABLES `user_permissions` WRITE;
/*!40000 ALTER TABLE `user_permissions` DISABLE KEYS */;
INSERT INTO `user_permissions` VALUES (1,4,86,NULL,2,'按已有任务补开','2026-10-21 16:13:31','2026-10-21 16:13:31',NULL),(2,3,86,NULL,2,'按已有任务补开','2026-10-21 16:13:31','2026-10-21 16:13:31',NULL),(7,12,86,13,2,'加入项目自动开通','2026-10-21 17:22:12','2026-10-21 17:22:12',NULL),(11,9,86,1,2,'认领任务：JdbcPro','2026-10-22 09:54:25','2026-10-22 09:54:25',NULL);
/*!40000 ALTER TABLE `user_permissions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_roles`
--

DROP TABLE IF EXISTS `user_roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_roles` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `grant_type` tinyint NOT NULL DEFAULT '1' COMMENT '授权类型：1直接授权 2继承授权',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间（软删除）',
  PRIMARY KEY (`id`),
  KEY `idx_user_roles_user` (`user_id`),
  KEY `idx_user_roles_role` (`role_id`)
) ENGINE=InnoDB AUTO_INCREMENT=31 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_roles`
--

LOCK TABLES `user_roles` WRITE;
/*!40000 ALTER TABLE `user_roles` DISABLE KEYS */;
INSERT INTO `user_roles` VALUES (1,1,1,1,'2026-09-23 08:48:12','2026-09-23 08:48:12',NULL),(2,2,1,1,'2026-09-26 16:46:27','2026-09-26 16:46:27',NULL),(3,3,2,1,'2026-09-26 17:27:52','2026-09-26 17:27:52',NULL),(4,4,3,1,'2026-09-26 17:27:52','2026-09-26 17:27:52',NULL),(5,5,4,1,'2026-09-26 17:27:52','2026-09-26 17:27:52',NULL),(6,6,5,1,'2026-09-26 17:27:52','2026-09-26 17:27:52',NULL),(7,7,6,1,'2026-10-15 10:32:37','2026-10-15 10:32:37',NULL),(8,8,6,1,'2026-10-15 14:50:29','2026-10-20 14:23:03','2026-10-20 14:23:03'),(9,9,6,1,'2026-10-19 11:18:48','2026-10-19 11:18:48',NULL),(10,8,6,1,'2026-10-20 14:23:03','2026-10-20 14:23:03',NULL),(11,10,7,1,'2026-10-20 14:42:08','2026-10-20 14:42:08',NULL),(13,12,4,1,'2026-10-21 14:39:11','2026-10-21 14:39:11',NULL),(14,13,8,1,'2026-10-21 15:24:32','2026-10-21 15:24:32',NULL);
/*!40000 ALTER TABLE `user_roles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_third_party_bindings`
--

DROP TABLE IF EXISTS `user_third_party_bindings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_third_party_bindings` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '本系统用户',
  `platform` varchar(20) NOT NULL COMMENT 'wecom=企业微信 dingtalk=钉钉',
  `external_id` varchar(64) NOT NULL COMMENT '第三方账号标识（企业微信 UserID / 钉钉 userId）',
  `external_name` varchar(50) DEFAULT NULL COMMENT '第三方显示名，仅用于本人核对',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1已绑定 0已解绑',
  `bind_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_binding_user` (`user_id`,`platform`),
  KEY `idx_binding_external` (`platform`,`external_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='第三方账号绑定（企业微信/钉钉）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_third_party_bindings`
--

LOCK TABLES `user_third_party_bindings` WRITE;
/*!40000 ALTER TABLE `user_third_party_bindings` DISABLE KEYS */;
/*!40000 ALTER TABLE `user_third_party_bindings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名（登录账号）',
  `password` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码（BCrypt加密）',
  `real_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '真实姓名',
  `gender` tinyint NOT NULL DEFAULT '0' COMMENT '性别：0未知 1男 2女',
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '手机号',
  `email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '邮箱',
  `avatar` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '头像URL',
  `org_id` bigint NOT NULL COMMENT '所属组织/部门ID',
  `position_id` bigint DEFAULT NULL COMMENT '职位ID',
  `title_id` bigint DEFAULT NULL COMMENT '职称ID',
  `entry_date` date DEFAULT NULL COMMENT '入职日期',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0禁用 1正常 2锁定',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '最后登录IP',
  `last_logout_time` datetime DEFAULT NULL COMMENT '最后退出时间',
  `login_fail_count` int NOT NULL DEFAULT '0' COMMENT '连续登录失败次数',
  `lock_time` datetime DEFAULT NULL COMMENT '账号锁定时间',
  `wechat_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '企业微信/钉钉用户ID（用于推送绑定）',
  `emergency_contact` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `emergency_phone` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `bank_account` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `bank_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `salary` decimal(12,2) DEFAULT NULL COMMENT '月薪（仅HR/财务/总经理/管理员可见）',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间（软删除）',
  `must_change_password` tinyint NOT NULL DEFAULT '1' COMMENT '是否要修改密码：1是 0否',
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `phone` (`phone`),
  UNIQUE KEY `email` (`email`),
  KEY `idx_users_org_status` (`org_id`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=28 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'admin','$2a$10$ZL4gIXedodslBNs4JM2a.OAfi3wxZ5vKVqHiq8bhP5sEWouRQsMGK','系统管理员',1,'13800138000','3631339132@qq.com',NULL,1,NULL,NULL,NULL,1,'2026-10-22 09:45:30','0:0:0:0:0:0:0:1','2026-10-22 09:45:01',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-09-21 17:33:09','2026-10-14 17:38:25',NULL,0),(2,'hhf','$2a$10$vQCIedPwwkXuvSefBhdk5u7NsTVlti.NpHxxmEC8EODW21K2Z0KIa','张三',1,'18064658331','admin',NULL,0,NULL,NULL,NULL,1,NULL,NULL,'2026-09-26 17:50:23',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-09-26 16:46:27','2026-09-26 16:46:27',NULL,0),(3,'manager01','$2a$10$SC8.OJ8lJISl6W5SzaN0o.OIfp.F3PJveUH8gxaOOTer2Ve/iubTu','王主管',1,'13800000002','manager@oa.com',NULL,4,NULL,NULL,'2024-03-01',1,'2026-10-19 15:16:27','0:0:0:0:0:0:0:1','2026-10-19 15:16:36',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-09-26 17:27:52','2026-10-14 15:47:07',NULL,0),(4,'employee01','$2a$10$b3Sa4TNSV0ltl/Q.jmRRwe8l5I523/45HidB/vFAODXWeePsi5ijS','李员工',2,'13800000001','employee@oa.com',NULL,6,NULL,NULL,'2024-06-15',1,'2026-10-16 10:55:19','0:0:0:0:0:0:0:1','2026-10-16 10:56:47',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-09-26 17:27:52','2026-10-23 18:37:04',NULL,0),(5,'hr01','$2a$10$6Y4RIqK8yIOHOKPaSKYxgulrb4LGARKBejmnnZpz1sOY4E4ItM0dO','赵专员',2,'13800000004','hr@oa.com',NULL,2,NULL,NULL,'2024-02-10',1,'2026-10-21 14:35:52','0:0:0:0:0:0:0:1','2026-10-21 16:57:13',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-09-26 17:27:52','2026-10-20 14:28:09',NULL,0),(6,'finance01','$2a$10$rXOouGuJf7Iy45pFqlVRyOBEkySmwUSY0IOeNPQTR0LrXkSP.VrCW','钱专员',1,'13800000005','finance@oa.com',NULL,3,NULL,NULL,'2024-05-20',1,'2026-10-20 15:57:04','0:0:0:0:0:0:0:1','2026-10-20 16:01:45',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-09-26 17:27:52','2026-10-19 15:30:13',NULL,0),(7,'gm01','$2a$10$ZL4gIXedodslBNs4JM2a.OAfi3wxZ5vKVqHiq8bhP5sEWouRQsMGK','陈总经理',1,'13800000006','gm@oa.com',NULL,1,NULL,NULL,'2024-01-01',1,'2026-10-15 11:36:00','0:0:0:0:0:0:0:1',NULL,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-10-15 10:32:37','2026-10-15 10:32:37',NULL,0),(8,'gm02','$2a$10$b3Sa4TNSV0ltl/Q.jmRRwe8l5I523/45HidB/vFAODXWeePsi5ijS','赵总经理',1,NULL,'18064658331@qq.com',NULL,2,NULL,NULL,'2026-10-13',1,'2026-10-15 16:15:20','0:0:0:0:0:0:0:1',NULL,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-10-15 14:50:29','2026-10-20 14:23:03',NULL,0),(9,'gm03','$2a$10$Keuk4atRR3eHkTgtyMXhAuaLV62QuMxdWYFAW7rkRh8vyWtjPRoF6','雷先生',0,'18064658332','huangshaohuangshaogui@hotmail.com ',NULL,1,NULL,NULL,NULL,1,'2026-10-22 10:35:43','0:0:0:0:0:0:0:1','2026-10-22 09:45:26',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-10-19 11:18:48','2026-10-21 17:08:08',NULL,0),(10,'hrmanager01','$2a$10$yU3oDxAkFqOrUUtsHcF8Q.BicjLm09Gl4BvSEHZ2DHvTIEpVncKCu','陈主管',2,'13900000007','hrmanager01@example.com',NULL,2,NULL,NULL,NULL,1,'2026-10-20 16:04:16','0:0:0:0:0:0:0:1',NULL,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-10-20 14:42:08','2026-10-20 16:02:56',NULL,0),(12,'hr02','$2a$10$Si4.n/vV5YMyhsYA0MU2hOHpb0sKf2KMPJm3no4jbLBIv2ChKbUtu','张先生',0,'18000000000','hr02@861202',NULL,2,NULL,NULL,'2026-10-21',1,'2026-10-22 10:34:51','0:0:0:0:0:0:0:1','2026-10-22 10:35:39',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-10-21 14:39:11','2026-10-24 12:10:40',NULL,0),(13,'pm01','$2a$10$Cs8a/XWFe1whH6nQZoADO.jT7C7LZhLsbIBc4s34tlBYeHZlzpZkq','项目经理',0,NULL,NULL,NULL,6,NULL,NULL,'2026-10-21',1,'2026-10-21 17:02:47','0:0:0:0:0:0:0:1','2026-10-21 17:07:08',0,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'2026-10-21 15:24:32','2026-10-21 17:01:27',NULL,0);
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `worklog_categories`
--

DROP TABLE IF EXISTS `worklog_categories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `worklog_categories` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '类别ID',
  `category_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '类别名称',
  `category_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '类别编码',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '类别描述',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0停用 1启用',
  `creator_id` bigint NOT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间（软删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `category_code` (`category_code`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作日志类别表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `worklog_categories`
--

LOCK TABLES `worklog_categories` WRITE;
/*!40000 ALTER TABLE `worklog_categories` DISABLE KEYS */;
INSERT INTO `worklog_categories` VALUES (1,'日报','DAILY','日常工作记录',1,1,1,'2026-09-28 14:53:58','2026-09-28 14:53:58',NULL),(2,'周报','WEEKLY','本周工作汇总',2,1,1,'2026-09-28 14:53:58','2026-09-28 14:53:58',NULL),(3,'月报','MONTHLY','本月工作总结',3,1,1,'2026-09-28 14:53:58','2026-09-28 14:53:58',NULL);
/*!40000 ALTER TABLE `worklog_categories` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `worklog_comments`
--

DROP TABLE IF EXISTS `worklog_comments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `worklog_comments` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `worklog_id` bigint NOT NULL COMMENT '日志ID',
  `user_id` bigint NOT NULL COMMENT '评论人ID',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父评论ID（0表示顶级评论）',
  `content` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '评论内容',
  `comment_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间（软删除）',
  PRIMARY KEY (`id`),
  KEY `idx_worklog_comments_worklog` (`worklog_id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作日志评论表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `worklog_comments`
--

LOCK TABLES `worklog_comments` WRITE;
/*!40000 ALTER TABLE `worklog_comments` DISABLE KEYS */;
INSERT INTO `worklog_comments` VALUES (1,1,1,0,'管理员点评：完成得很好','2026-09-28 15:06:06','2026-09-28 15:06:06','2026-09-28 15:10:59','2026-09-28 15:10:59'),(2,1,1,0,'一般般','2026-10-01 15:17:13','2026-10-01 15:17:13','2026-10-01 15:17:13',NULL);
/*!40000 ALTER TABLE `worklog_comments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `worklog_supplement_approvals`
--

DROP TABLE IF EXISTS `worklog_supplement_approvals`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `worklog_supplement_approvals` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '被授权员工',
  `start_date` date NOT NULL COMMENT '可补写起始日期',
  `end_date` date NOT NULL COMMENT '可补写结束日期',
  `reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '申请原因',
  `approved_by` bigint DEFAULT NULL COMMENT '审批人',
  `expire_at` datetime DEFAULT NULL COMMENT '授权过期时间（审批通过后才设置）',
  `status` tinyint DEFAULT '1' COMMENT '1生效 0作废',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `worklog_supplement_approvals`
--

LOCK TABLES `worklog_supplement_approvals` WRITE;
/*!40000 ALTER TABLE `worklog_supplement_approvals` DISABLE KEYS */;
INSERT INTO `worklog_supplement_approvals` VALUES (1,4,'2026-10-01','2026-10-09','AAAAAA',NULL,NULL,0,'2026-10-09 15:36:54'),(2,4,'2026-10-01','2026-10-09','AAAAAA',NULL,NULL,0,'2026-10-09 15:36:55'),(3,4,'2026-09-30','2026-10-09','',NULL,NULL,0,'2026-10-09 15:37:26'),(4,4,'2026-09-30','2026-10-09','AAAAA',NULL,NULL,0,'2026-10-09 15:37:32'),(5,4,'2026-09-30','2026-10-09','AAAAA',NULL,NULL,0,'2026-10-09 15:40:57'),(6,4,'2026-09-30','2026-10-09','',NULL,NULL,0,'2026-10-09 15:41:05'),(7,4,'2026-09-01','2026-09-05','出差忘记写日志',NULL,NULL,0,'2026-10-09 15:57:56');
/*!40000 ALTER TABLE `worklog_supplement_approvals` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `worklog_view_permissions`
--

DROP TABLE IF EXISTS `worklog_view_permissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `worklog_view_permissions` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `target_id` bigint NOT NULL COMMENT '被查看人ID（谁的日志）',
  `viewer_id` bigint NOT NULL DEFAULT '0' COMMENT '查看人ID（0表示按群组/部门授权）',
  `group_id` bigint DEFAULT NULL COMMENT '用户群组ID（按群组授权时使用）',
  `dept_id` bigint DEFAULT NULL COMMENT '部门ID（按部门授权时使用，扩展）',
  `permission_type` tinyint NOT NULL DEFAULT '1' COMMENT '授权类型：1按用户 2按群组 3按部门',
  `grant_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '授权时间',
  `granter_id` bigint NOT NULL COMMENT '授权人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间（软删除）',
  PRIMARY KEY (`id`),
  KEY `idx_worklog_view_viewer_target` (`viewer_id`,`target_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日志查看权限表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `worklog_view_permissions`
--

LOCK TABLES `worklog_view_permissions` WRITE;
/*!40000 ALTER TABLE `worklog_view_permissions` DISABLE KEYS */;
INSERT INTO `worklog_view_permissions` VALUES (1,3,1,NULL,NULL,1,'2026-10-09 14:41:12',1,'2026-10-09 14:41:12','2026-10-09 14:41:12',NULL),(2,4,1,NULL,NULL,1,'2026-10-09 15:04:24',1,'2026-10-09 15:04:24','2026-10-09 15:04:24',NULL);
/*!40000 ALTER TABLE `worklog_view_permissions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `worklogs`
--

DROP TABLE IF EXISTS `worklogs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `worklogs` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` bigint NOT NULL COMMENT '撰写人ID',
  `log_date` date NOT NULL COMMENT '日志日期',
  `category_id` bigint DEFAULT NULL COMMENT '日志类别ID',
  `title` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '日志标题',
  `content` longtext COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '日志内容（富文本）',
  `work_summary` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '工作总结',
  `next_plan` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '明日计划',
  `attachment_id` bigint DEFAULT NULL COMMENT '附件ID',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0草稿 1已提交 2已撤回',
  `is_supplement` tinyint NOT NULL DEFAULT '0' COMMENT '是否补写：0正常 1补写',
  `submit_time` datetime DEFAULT NULL COMMENT '提交时间',
  `view_count` int NOT NULL DEFAULT '0' COMMENT '被查看次数',
  `comment_count` int NOT NULL DEFAULT '0' COMMENT '评论数',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间（软删除）',
  PRIMARY KEY (`id`),
  KEY `idx_worklogs_user_created` (`user_id`,`created_at`),
  KEY `idx_worklogs_category_status` (`category_id`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `worklogs`
--

LOCK TABLES `worklogs` WRITE;
/*!40000 ALTER TABLE `worklogs` DISABLE KEYS */;
INSERT INTO `worklogs` VALUES (1,1,'2026-09-28',1,'测试-工作日志模块联调','今天完成了工作日志模块的后端实现与前端页面开发，编译通过。','后端接口全部完成','继续完善统计图表',NULL,1,0,'2026-09-28 15:05:43',2,2,'2026-09-28 15:05:43','2026-10-01 15:16:55',NULL),(2,1,'2026-10-01',NULL,'前端项目工作日志','今天完成了，工作日志模块的，并对它添加筛选功能是页面数据更加整洁','','',NULL,1,0,'2026-10-01 15:24:32',0,0,'2026-10-01 15:24:32','2026-10-01 15:24:32',NULL),(3,1,'2026-10-04',1,'走查测试日志','测试撰写日志闭环，验证工作日志模块功能。','','',NULL,1,0,'2026-10-04 15:29:42',0,0,'2026-10-04 15:29:42','2026-10-04 15:31:36','2026-10-04 15:31:36'),(5,4,'2026-10-05',NULL,'员工附件验证','员工自己上传附件测试。','','',NULL,1,0,'2026-10-05 17:21:46',0,0,'2026-10-05 17:21:46','2026-10-05 17:24:48','2026-10-05 17:24:48'),(8,4,'2026-10-09',1,'AAAAAAAAAAAA','AAAAAAAAAAAAA','AAAAAAAAAAA','AAAAAAAAAA',12,1,0,'2026-10-09 15:04:54',0,0,'2026-10-09 15:04:54','2026-10-09 15:04:54',NULL);
/*!40000 ALTER TABLE `worklogs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'oa_system'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-24 17:25:06
