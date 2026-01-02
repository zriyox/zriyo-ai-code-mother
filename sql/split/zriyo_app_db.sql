/*
 Source Schema         : zriyo_app_db
 Target Server Type    : MySQL 8.0
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1. 应用基础表
-- ----------------------------
DROP TABLE IF EXISTS `app`;
CREATE TABLE `app` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `appName` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '应用名称',
  `cover` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '应用封面',
  `initPrompt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '应用初始化的 prompt',
  `codeGenType` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '代码生成类型',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='应用';

-- ----------------------------
-- 2. 应用部署历史表
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='应用部署历史记录';

-- ----------------------------
-- 3. AI代码生成记录表
-- ----------------------------
DROP TABLE IF EXISTS `ai_code_gen_record`;
CREATE TABLE `ai_code_gen_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `app_id` bigint NOT NULL COMMENT '关联的应用ID',
  `user_id` bigint NOT NULL COMMENT '发起用户的ID',
  `status` varchar(20) NOT NULL DEFAULT 'RUNNING' COMMENT '状态：RUNNING / SUCCESS / FAILED / CANCELLED',
  `stage` varchar(50) NOT NULL DEFAULT 'INIT' COMMENT '当前阶段',
  `error_message` text COMMENT '失败时的错误信息',
  `project_dir` varchar(255) DEFAULT NULL COMMENT '生成的项目目录名',
  `file_count` int DEFAULT '0' COMMENT '生成的文件数量',
  `start_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `duration_ms` bigint DEFAULT NULL COMMENT '耗时（毫秒）',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `message_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_app_id` (`app_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI代码生成调用记录表';

SET FOREIGN_KEY_CHECKS = 1;
