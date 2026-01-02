/*
 Navicat Premium Dump SQL

 Source Server         : localhost_3306
 Source Server Type    : MySQL
 Source Server Version : 80043 (8.0.43)
 Source Host           : localhost:3306
 Source Schema         : zriyo_ai_code_mother

 Target Server Type    : MySQL
 Target Server Version : 80043 (8.0.43)
 File Encoding         : 65001

 Date: 23/12/2025 03:16:08
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ai_code_gen_record
-- ----------------------------
DROP TABLE IF EXISTS `ai_code_gen_record`;
CREATE TABLE `ai_code_gen_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `app_id` bigint NOT NULL COMMENT '关联的应用ID（即项目ID）',
  `user_id` bigint NOT NULL COMMENT '发起用户的ID',
  `status` varchar(20) NOT NULL DEFAULT 'RUNNING' COMMENT '状态：RUNNING / SUCCESS / FAILED / CANCELLED',
  `stage` varchar(50) NOT NULL DEFAULT 'INIT' COMMENT '当前阶段：INIT / SKELETON / FILE_GENERATION / BUILD / DONE',
  `error_message` text COMMENT '失败时的错误信息',
  `project_dir` varchar(255) DEFAULT NULL COMMENT '生成的项目目录名（如 vue_project_123）',
  `file_count` int DEFAULT '0' COMMENT '生成的文件数量',
  `start_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间（可为空）',
  `duration_ms` bigint DEFAULT NULL COMMENT '耗时（毫秒）',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `message_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_app_id` (`app_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_stage` (`stage`),
  KEY `idx_start_time` (`start_time`)
) ENGINE=InnoDB AUTO_INCREMENT=1166 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI代码生成调用记录表';

-- ----------------------------
-- Table structure for ai_tool_log
-- ----------------------------
DROP TABLE IF EXISTS `ai_tool_log`;
CREATE TABLE `ai_tool_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `ai_message_id` bigint NOT NULL COMMENT '对应的 AI 消息 ID（哪次对话调用的）',
  `tool_name` varchar(50) NOT NULL COMMENT '工具名称（writeFile、createFolder 等）',
  `file_path` varchar(500) NOT NULL COMMENT '工具操作的文件路径',
  `action` varchar(50) NOT NULL COMMENT '工具执行动作（写入/读取/创建）',
  `summary` text COMMENT '对这次操作的简短描述',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `cost_time` bigint DEFAULT NULL COMMENT '耗时(ms)',
  PRIMARY KEY (`id`),
  KEY `idx_ai_tool_log_ai_message_id` (`ai_message_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2592 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 工具调用记录表';

-- ----------------------------
-- Table structure for app
-- ----------------------------
DROP TABLE IF EXISTS `app`;
CREATE TABLE `app` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `appName` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '应用名称',
  `cover` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '应用封面',
  `initPrompt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '应用初始化的 prompt',
  `codeGenType` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '代码生成类型（枚举）',
  `deployKey` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '部署标识',
  `deployedTime` datetime DEFAULT NULL COMMENT '部署时间',
  `priority` int NOT NULL DEFAULT '0' COMMENT '优先级',
  `userId` bigint NOT NULL COMMENT '创建用户id',
  `editTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '编辑时间',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `isDelete` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除',
  `latestDeploymentId` bigint DEFAULT NULL COMMENT '关联历史表',
  `is_published` tinyint DEFAULT '0' COMMENT '是否发布',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_deployKey` (`deployKey`),
  KEY `idx_appName` (`appName`),
  KEY `idx_userId` (`userId`)
) ENGINE=InnoDB AUTO_INCREMENT=360701425803771905 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='应用';

-- ----------------------------
-- Table structure for chat_history
-- ----------------------------
DROP TABLE IF EXISTS `chat_history`;
CREATE TABLE `chat_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息',
  `messageType` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'user/ai',
  `appId` bigint NOT NULL COMMENT '应用id',
  `userId` bigint NOT NULL COMMENT '创建用户id',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `isDelete` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除',
  `user_visible` tinyint NOT NULL DEFAULT '1' COMMENT '用户是否可见',
  `meta_data` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '修改操作 json',
  PRIMARY KEY (`id`),
  KEY `idx_appId` (`appId`),
  KEY `idx_createTime` (`createTime`),
  KEY `idx_appId_createTime` (`appId`,`createTime`),
  KEY `idx_chat_history_app_user_id` (`appId`,`userId`,`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4116 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话历史';

-- ----------------------------
-- Table structure for deployment_history
-- ----------------------------
DROP TABLE IF EXISTS `deployment_history`;
CREATE TABLE `deployment_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `app_id` bigint NOT NULL COMMENT '应用ID',
  `deploy_name` varchar(100) NOT NULL COMMENT '发布名称',
  `deploy_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '部署时间',
  `version` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_deploy_cover` (`deploy_time`,`id`,`app_id`,`deploy_name`,`version`)
) ENGINE=InnoDB AUTO_INCREMENT=43 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='应用部署历史记录';

-- ----------------------------
-- Table structure for points_code
-- ----------------------------
DROP TABLE IF EXISTS `points_code`;
CREATE TABLE `points_code` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code` varchar(32) NOT NULL COMMENT '积分兑换码，全局唯一（如：XP7K9Q），由系统生成',
  `points` int NOT NULL DEFAULT '0' COMMENT '该码可兑换的积分数（固定值）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '使用状态：0=未使用，1=已使用，2=已过期',
  `used_by_user_id` bigint DEFAULT NULL COMMENT '兑换用户ID，逻辑外键，关联 users.id；为空表示未兑换',
  `used_at` datetime DEFAULT NULL COMMENT '实际兑换时间，NULL 表示未兑换',
  `expired_at` datetime NOT NULL COMMENT '码的过期时间，超过此时间不可再使用',
  `created_by` bigint DEFAULT NULL COMMENT '创建者用户ID（通常为运营人员），逻辑外键，关联 users.id',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '码的创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='积分兑换码管理表（逻辑外键，支持批量发放与追踪）';

-- ----------------------------
-- Table structure for points_code_copy1
-- ----------------------------
DROP TABLE IF EXISTS `points_code_copy1`;
CREATE TABLE `points_code_copy1` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code` varchar(32) NOT NULL COMMENT '积分兑换码，全局唯一（如：XP7K9Q），由系统生成',
  `points` int NOT NULL DEFAULT '0' COMMENT '该码可兑换的积分数（固定值）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '使用状态：0=未使用，1=已使用，2=已过期',
  `used_by_user_id` bigint DEFAULT NULL COMMENT '兑换用户ID，逻辑外键，关联 users.id；为空表示未兑换',
  `used_at` datetime DEFAULT NULL COMMENT '实际兑换时间，NULL 表示未兑换',
  `expired_at` datetime NOT NULL COMMENT '码的过期时间，超过此时间不可再使用',
  `created_by` bigint DEFAULT NULL COMMENT '创建者用户ID（通常为运营人员），逻辑外键，关联 users.id',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '码的创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='积分兑换码管理表（逻辑外键，支持批量发放与追踪）';

-- ----------------------------
-- Table structure for points_code_copy2
-- ----------------------------
DROP TABLE IF EXISTS `points_code_copy2`;
CREATE TABLE `points_code_copy2` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code` varchar(32) NOT NULL COMMENT '积分兑换码，全局唯一（如：XP7K9Q），由系统生成',
  `points` int NOT NULL DEFAULT '0' COMMENT '该码可兑换的积分数（固定值）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '使用状态：0=未使用，1=已使用，2=已过期',
  `used_by_user_id` bigint DEFAULT NULL COMMENT '兑换用户ID，逻辑外键，关联 users.id；为空表示未兑换',
  `used_at` datetime DEFAULT NULL COMMENT '实际兑换时间，NULL 表示未兑换',
  `expired_at` datetime NOT NULL COMMENT '码的过期时间，超过此时间不可再使用',
  `created_by` bigint DEFAULT NULL COMMENT '创建者用户ID（通常为运营人员），逻辑外键，关联 users.id',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '码的创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='积分兑换码管理表（逻辑外键，支持批量发放与追踪）';

-- ----------------------------
-- Table structure for points_log
-- ----------------------------
DROP TABLE IF EXISTS `points_log`;
CREATE TABLE `points_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID，逻辑外键，关联 users.id',
  `change_amount` int NOT NULL COMMENT '本次积分变动值（正数为增加，负数为扣除）',
  `balance_after` int NOT NULL COMMENT '本次变动后的可用积分余额',
  `reason` varchar(50) NOT NULL COMMENT '变动原因编码，如：daily_sign（签到）、redeem_code（兑换码）、purchase（消费返积分）等',
  `related_id` varchar(64) DEFAULT NULL COMMENT '关联业务ID，如订单号、签到记录ID、积分码ID等，便于追溯来源',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=123 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='积分变动流水日志（逻辑外键，全量可追溯）';

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `userAccount` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '账号',
  `userPassword` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码',
  `userName` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户昵称',
  `userAvatar` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户头像',
  `userProfile` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户简介',
  `userRole` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'user' COMMENT '用户角色：user/admin',
  `editTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `isDelete` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除',
  `authing_sub` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `phone` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_userAccount` (`userAccount`),
  KEY `idx_userName` (`userName`)
) ENGINE=InnoDB AUTO_INCREMENT=356594122376790017 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户';

-- ----------------------------
-- Table structure for user_points
-- ----------------------------
DROP TABLE IF EXISTS `user_points`;
CREATE TABLE `user_points` (
  `user_id` bigint NOT NULL COMMENT '用户ID，逻辑外键，关联 users.id',
  `total_points` int NOT NULL DEFAULT '0' COMMENT '累计获得总积分（不可减少）',
  `available_points` int NOT NULL DEFAULT '0' COMMENT '当前可用积分（可用于兑换或扣减）',
  `used_points` int NOT NULL DEFAULT '0' COMMENT '已使用/已消耗积分（total - available = used）',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号，用于防止并发更新覆盖',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户积分账户（逻辑外键 + 乐观锁）';

-- ----------------------------
-- Table structure for user_sign_in
-- ----------------------------
DROP TABLE IF EXISTS `user_sign_in`;
CREATE TABLE `user_sign_in` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID，逻辑外键，关联 users.id',
  `sign_date` date NOT NULL COMMENT '签到日期（格式：YYYY-MM-DD）',
  `continuous_days` int NOT NULL DEFAULT '1' COMMENT '截至当日的连续签到天数（由应用层计算）',
  `reward_points` int NOT NULL DEFAULT '0' COMMENT '本次签到获得的积分数',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户签到历史记录（逻辑外键，支持连续签到）';

SET FOREIGN_KEY_CHECKS = 1;
