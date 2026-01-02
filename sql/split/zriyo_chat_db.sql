/*
 Source Schema         : zriyo_chat_db
 Target Server Type    : MySQL 8.0
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1. 聊天历史表
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
  KEY `idx_chat_history_app_user_id` (`appId`,`userId`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话历史';

-- ----------------------------
-- 2. AI工具调用日志表
-- ----------------------------
DROP TABLE IF EXISTS `ai_tool_log`;
CREATE TABLE `ai_tool_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `ai_message_id` bigint NOT NULL COMMENT '对应的 AI 消息 ID',
  `tool_name` varchar(50) NOT NULL COMMENT '工具名称',
  `file_path` varchar(500) NOT NULL COMMENT '工具操作的文件路径',
  `action` varchar(50) NOT NULL COMMENT '工具执行动作',
  `summary` text COMMENT '操作简述',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `cost_time` bigint DEFAULT NULL COMMENT '耗时(ms)',
  PRIMARY KEY (`id`),
  KEY `idx_ai_tool_log_ai_message_id` (`ai_message_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 工具调用记录表';

SET FOREIGN_KEY_CHECKS = 1;
