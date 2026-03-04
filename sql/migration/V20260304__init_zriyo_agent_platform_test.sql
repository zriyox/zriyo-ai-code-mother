-- =========================================================
-- 测试环境数据库深度改造脚本（生产级结构）
-- 目标库：zriyo_agent_platform_test
-- 来源库：yu_ai_code_mother
-- 说明：
-- 1) 新建独立测试库，不影响现网库
-- 2) 表/字段均带可见 COMMENT（可在数据库工具中直接查看）
-- 3) 保留旧业务字段，新增链路追踪字段（conversation_id/trace_id/task_id）
-- 4) 引入 workflow 核心编排表（workflow_def/project_session/plan/task...）
-- 5) 脚本可重复执行（会重建目标表并重新迁移数据）
-- =========================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `zriyo_agent_platform_test`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

USE `zriyo_agent_platform_test`;

-- 先清理目标表（可重复执行）
DROP TABLE IF EXISTS `task_event`;
DROP TABLE IF EXISTS `task`;
DROP TABLE IF EXISTS `plan_step`;
DROP TABLE IF EXISTS `plan`;
DROP TABLE IF EXISTS `project_session`;
DROP TABLE IF EXISTS `workflow_def`;
DROP TABLE IF EXISTS `ai_code_gen_record`;
DROP TABLE IF EXISTS `ai_tool_log`;
DROP TABLE IF EXISTS `chat_history`;
DROP TABLE IF EXISTS `deployment_history`;
DROP TABLE IF EXISTS `app`;
DROP TABLE IF EXISTS `user_sign_in`;
DROP TABLE IF EXISTS `points_code`;
DROP TABLE IF EXISTS `points_log`;
DROP TABLE IF EXISTS `user_points`;
DROP TABLE IF EXISTS `user`;

-- =========================================================
-- 基础业务表
-- =========================================================

CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID（建议应用层雪花ID）',
  `userAccount` varchar(256) DEFAULT NULL COMMENT '登录账号',
  `userPassword` varchar(512) NOT NULL COMMENT '密码（加密存储）',
  `userName` varchar(256) DEFAULT NULL COMMENT '用户昵称',
  `userAvatar` varchar(1024) DEFAULT NULL COMMENT '头像URL',
  `userProfile` varchar(512) DEFAULT NULL COMMENT '用户简介',
  `userRole` varchar(256) NOT NULL DEFAULT 'user' COMMENT '角色（user/admin）',
  `editTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '编辑时间',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `isDelete` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记（0=否，1=是）',
  `authing_sub` varchar(255) DEFAULT NULL COMMENT '第三方认证主体ID',
  `phone` varchar(255) DEFAULT NULL COMMENT '手机号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_userAccount` (`userAccount`),
  KEY `idx_userName` (`userName`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE `user_points` (
  `user_id` bigint NOT NULL COMMENT '用户ID（逻辑外键）',
  `total_points` int NOT NULL DEFAULT '0' COMMENT '累计总积分',
  `available_points` int NOT NULL DEFAULT '0' COMMENT '可用积分',
  `used_points` int NOT NULL DEFAULT '0' COMMENT '已使用积分',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户积分账户';

CREATE TABLE `points_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `change_amount` int NOT NULL COMMENT '积分变动值（正增负减）',
  `balance_after` int NOT NULL COMMENT '变动后余额',
  `reason` varchar(50) NOT NULL COMMENT '变动原因编码',
  `related_id` varchar(64) DEFAULT NULL COMMENT '关联业务ID（订单号/任务号）',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_points_log_user_id` (`user_id`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='积分流水日志';

CREATE TABLE `points_code` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '兑换码ID',
  `code` varchar(32) NOT NULL COMMENT '兑换码（全局唯一）',
  `points` int NOT NULL DEFAULT '0' COMMENT '可兑换积分',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态（0未使用/1已使用/2过期）',
  `used_by_user_id` bigint DEFAULT NULL COMMENT '兑换用户ID',
  `used_at` datetime DEFAULT NULL COMMENT '兑换时间',
  `expired_at` datetime NOT NULL COMMENT '过期时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建者用户ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_points_code_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='积分兑换码';

CREATE TABLE `user_sign_in` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '签到记录ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `sign_date` date NOT NULL COMMENT '签到日期',
  `continuous_days` int NOT NULL DEFAULT '1' COMMENT '连续签到天数',
  `reward_points` int NOT NULL DEFAULT '0' COMMENT '本次发放积分',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_sign_date` (`user_id`,`sign_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户签到记录';

CREATE TABLE `app` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '应用ID（建议应用层雪花ID）',
  `appName` varchar(256) DEFAULT NULL COMMENT '应用名称',
  `cover` varchar(512) DEFAULT NULL COMMENT '应用封面URL',
  `initPrompt` text COMMENT '初始化提示词',
  `codeGenType` varchar(64) DEFAULT NULL COMMENT '代码生成类型',
  `deployKey` varchar(64) DEFAULT NULL COMMENT '部署标识',
  `deployedTime` datetime DEFAULT NULL COMMENT '最近部署时间',
  `priority` int NOT NULL DEFAULT '0' COMMENT '优先级',
  `userId` bigint NOT NULL COMMENT '创建用户ID',
  `editTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '编辑时间',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `isDelete` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记（0=否，1=是）',
  `latestDeploymentId` bigint DEFAULT NULL COMMENT '最新部署记录ID',
  `is_published` tinyint DEFAULT '0' COMMENT '是否已发布（0/1）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_deployKey` (`deployKey`),
  KEY `idx_appName` (`appName`),
  KEY `idx_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='应用主表';

CREATE TABLE `deployment_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '部署记录ID',
  `app_id` bigint NOT NULL COMMENT '应用ID',
  `deploy_name` varchar(100) NOT NULL COMMENT '发布名称',
  `deploy_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '部署时间',
  `version` varchar(255) NOT NULL COMMENT '版本号',
  PRIMARY KEY (`id`),
  KEY `idx_deploy_cover` (`deploy_time`,`id`,`app_id`,`deploy_name`,`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='部署历史';

-- =========================================================
-- 会话/工具/生成日志（增强链路追踪）
-- =========================================================

CREATE TABLE `chat_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `conversation_id` varchar(64) NOT NULL COMMENT '会话ID（跨多轮稳定）',
  `trace_id` varchar(64) DEFAULT NULL COMMENT '链路追踪ID（单次请求）',
  `task_id` varchar(64) DEFAULT NULL COMMENT '关联任务ID',
  `request_id` varchar(64) DEFAULT NULL COMMENT '请求ID（网关/前端）',
  `agent_run_id` varchar(64) DEFAULT NULL COMMENT 'Agent执行ID',
  `message` text NOT NULL COMMENT '消息内容',
  `messageType` varchar(32) NOT NULL COMMENT '消息类型（user/ai/system）',
  `appId` bigint NOT NULL COMMENT '应用ID',
  `userId` bigint NOT NULL COMMENT '用户ID',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `isDelete` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记（0=否，1=是）',
  `user_visible` tinyint NOT NULL DEFAULT '1' COMMENT '是否对用户可见（0/1）',
  `meta_data` text COMMENT '消息扩展信息（JSON字符串）',
  PRIMARY KEY (`id`),
  KEY `idx_appId` (`appId`),
  KEY `idx_createTime` (`createTime`),
  KEY `idx_appId_createTime` (`appId`,`createTime`),
  KEY `idx_chat_history_app_user_id` (`appId`,`userId`,`id`),
  KEY `idx_chat_history_conversation` (`conversation_id`,`id`),
  KEY `idx_chat_history_trace` (`trace_id`,`id`),
  KEY `idx_chat_history_task` (`task_id`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话消息历史（增强追踪）';

CREATE TABLE `ai_tool_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '工具调用日志ID',
  `ai_message_id` bigint NOT NULL COMMENT '关联AI消息ID（chat_history.id）',
  `conversation_id` varchar(64) DEFAULT NULL COMMENT '会话ID',
  `trace_id` varchar(64) DEFAULT NULL COMMENT '链路追踪ID',
  `task_id` varchar(64) DEFAULT NULL COMMENT '任务ID',
  `agent_run_id` varchar(64) DEFAULT NULL COMMENT 'Agent执行ID',
  `tool_run_id` varchar(64) DEFAULT NULL COMMENT '工具执行ID',
  `tool_name` varchar(50) NOT NULL COMMENT '工具名称',
  `file_path` varchar(500) NOT NULL COMMENT '操作文件路径',
  `action` varchar(50) NOT NULL COMMENT '动作类型（read/write/delete等）',
  `summary` text COMMENT '操作摘要',
  `status` varchar(32) NOT NULL DEFAULT 'SUCCESS' COMMENT '执行状态（SUCCESS/FAILED/CANCELLED）',
  `error_message` text COMMENT '失败错误信息',
  `args_json` json DEFAULT NULL COMMENT '工具入参（JSON）',
  `result_json` json DEFAULT NULL COMMENT '工具出参（JSON）',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `cost_time` bigint DEFAULT NULL COMMENT '耗时（毫秒）',
  PRIMARY KEY (`id`),
  KEY `idx_ai_tool_log_ai_message_id` (`ai_message_id`),
  KEY `idx_ai_tool_log_conversation` (`conversation_id`,`id`),
  KEY `idx_ai_tool_log_trace` (`trace_id`,`id`),
  KEY `idx_ai_tool_log_task` (`task_id`,`id`),
  KEY `idx_ai_tool_log_tool_run` (`tool_run_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI工具调用记录（增强追踪）';

CREATE TABLE `ai_code_gen_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '代码生成记录ID',
  `app_id` bigint NOT NULL COMMENT '应用ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `conversation_id` varchar(64) DEFAULT NULL COMMENT '会话ID',
  `trace_id` varchar(64) DEFAULT NULL COMMENT '链路追踪ID',
  `task_id` varchar(64) DEFAULT NULL COMMENT '任务ID',
  `workflow_id` varchar(64) DEFAULT NULL COMMENT '工作流ID',
  `plan_id` varchar(64) DEFAULT NULL COMMENT '计划ID',
  `request_id` varchar(64) DEFAULT NULL COMMENT '请求ID',
  `status` varchar(20) NOT NULL DEFAULT 'RUNNING' COMMENT '状态（RUNNING/SUCCESS/FAILED/CANCELLED）',
  `stage` varchar(50) NOT NULL DEFAULT 'INIT' COMMENT '阶段（INIT/PLAN/CODEGEN/CHECK/DONE）',
  `error_message` text COMMENT '错误信息',
  `project_dir` varchar(255) DEFAULT NULL COMMENT '项目目录',
  `file_count` int DEFAULT '0' COMMENT '生成文件数',
  `start_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `duration_ms` bigint DEFAULT NULL COMMENT '耗时毫秒',
  `message_id` bigint DEFAULT NULL COMMENT '关联消息ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_app_id` (`app_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_stage` (`stage`),
  KEY `idx_start_time` (`start_time`),
  KEY `idx_codegen_conversation` (`conversation_id`,`id`),
  KEY `idx_codegen_trace` (`trace_id`,`id`),
  KEY `idx_codegen_task` (`task_id`,`id`),
  KEY `idx_codegen_workflow` (`workflow_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI代码生成记录（增强追踪）';

-- =========================================================
-- Workflow 核心编排表
-- =========================================================

CREATE TABLE `workflow_def` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `workflow_id` varchar(64) NOT NULL COMMENT '工作流标识（如 frontend_init）',
  `version` int NOT NULL DEFAULT '1' COMMENT '版本号',
  `name` varchar(128) NOT NULL COMMENT '工作流名称',
  `spec_json` json NOT NULL COMMENT '工作流配置JSON（stages/allowed_actions/retry_policy）',
  `enabled` tinyint NOT NULL DEFAULT '1' COMMENT '是否启用（0/1）',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_workflow_version` (`workflow_id`,`version`),
  KEY `idx_workflow_id` (`workflow_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流定义';

CREATE TABLE `project_session` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `app_id` bigint NOT NULL COMMENT '应用ID',
  `title` varchar(128) DEFAULT NULL COMMENT '项目标题',
  `description` text COMMENT '项目描述',
  `conversation_summary` mediumtext COMMENT '会话摘要',
  `project_structure` json DEFAULT NULL COMMENT '项目结构摘要（JSON）',
  `active_plan_id` varchar(64) DEFAULT NULL COMMENT '当前生效计划ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_app_id` (`app_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目会话';

CREATE TABLE `plan` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `plan_id` varchar(64) NOT NULL COMMENT '计划ID',
  `app_id` bigint NOT NULL COMMENT '应用ID',
  `workflow_id` varchar(64) NOT NULL COMMENT '工作流ID',
  `status` varchar(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态（ACTIVE/ARCHIVED）',
  `parent_plan_id` varchar(64) DEFAULT NULL COMMENT '父计划ID（版本演进）',
  `source` varchar(32) DEFAULT NULL COMMENT '来源（auto/user）',
  `human_plan_md` mediumtext COMMENT 'Plan A（用户可读Markdown）',
  `exec_plan_json` json DEFAULT NULL COMMENT 'Plan B（系统执行JSON）',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plan_id` (`plan_id`),
  KEY `idx_app_id` (`app_id`),
  KEY `idx_workflow_id` (`workflow_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='计划主表';

CREATE TABLE `plan_step` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `plan_id` varchar(64) NOT NULL COMMENT '计划ID',
  `step_id` varchar(32) NOT NULL COMMENT '步骤ID（S1/S2...）',
  `title` varchar(256) DEFAULT NULL COMMENT '步骤标题',
  `status` varchar(32) NOT NULL DEFAULT 'pending' COMMENT '状态（pending/in_progress/completed/failed）',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plan_step` (`plan_id`,`step_id`),
  KEY `idx_plan_id` (`plan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='计划步骤状态';

CREATE TABLE `task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_id` varchar(64) NOT NULL COMMENT '任务ID（建议雪花/UUID）',
  `app_id` bigint NOT NULL COMMENT '应用ID',
  `workflow_id` varchar(64) NOT NULL COMMENT '工作流ID',
  `conversation_id` varchar(64) DEFAULT NULL COMMENT '会话ID',
  `trace_id` varchar(64) DEFAULT NULL COMMENT '链路追踪ID',
  `request_id` varchar(64) DEFAULT NULL COMMENT '请求ID',
  `status` varchar(32) NOT NULL COMMENT '任务状态（PENDING/RUNNING/FAILED...）',
  `stage` varchar(64) DEFAULT NULL COMMENT '当前阶段',
  `attempt` int NOT NULL DEFAULT '0' COMMENT '当前尝试次数',
  `max_attempt` int NOT NULL DEFAULT '0' COMMENT '最大重试次数',
  `retry_count` int NOT NULL DEFAULT '0' COMMENT '已重试次数',
  `next_retry_at` datetime DEFAULT NULL COMMENT '下次重试时间',
  `last_error` text COMMENT '最近错误',
  `payload_json` json DEFAULT NULL COMMENT '输入参数',
  `result_json` json DEFAULT NULL COMMENT '输出结果',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `started_at` datetime DEFAULT NULL COMMENT '开始时间',
  `finished_at` datetime DEFAULT NULL COMMENT '结束时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_id` (`task_id`),
  KEY `idx_task_app` (`app_id`,`created_at`),
  KEY `idx_task_status` (`status`,`updated_at`),
  KEY `idx_task_workflow` (`workflow_id`),
  KEY `idx_task_trace` (`trace_id`,`updated_at`),
  KEY `idx_task_conversation` (`conversation_id`,`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务实例表';

CREATE TABLE `task_event` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `event_id` varchar(64) NOT NULL COMMENT '事件ID（全局唯一）',
  `task_id` varchar(64) NOT NULL COMMENT '任务ID',
  `trace_id` varchar(64) DEFAULT NULL COMMENT '链路追踪ID',
  `event_type` varchar(64) NOT NULL COMMENT '事件类型（stage_started/task_failed...）',
  `stage` varchar(64) DEFAULT NULL COMMENT '关联阶段',
  `status_from` varchar(32) DEFAULT NULL COMMENT '迁移前状态',
  `status_to` varchar(32) DEFAULT NULL COMMENT '迁移后状态',
  `attempt` int NOT NULL DEFAULT '0' COMMENT '尝试次数',
  `message` varchar(512) DEFAULT NULL COMMENT '简要描述',
  `data_json` json DEFAULT NULL COMMENT '事件详情JSON',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_event_id` (`event_id`),
  KEY `idx_task_id` (`task_id`,`created_at`),
  KEY `idx_trace_id` (`trace_id`,`created_at`),
  KEY `idx_event_type` (`event_type`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务事件审计表';

-- =========================================================
-- 数据迁移（源库 -> 新测试库）
-- =========================================================

INSERT INTO `user` (
  `id`, `userAccount`, `userPassword`, `userName`, `userAvatar`, `userProfile`,
  `userRole`, `editTime`, `createTime`, `updateTime`, `isDelete`, `authing_sub`, `phone`
)
SELECT
  `id`, `userAccount`, `userPassword`, `userName`, `userAvatar`, `userProfile`,
  `userRole`, `editTime`, `createTime`, `updateTime`, `isDelete`, `authing_sub`, `phone`
FROM `yu_ai_code_mother`.`user`;

INSERT INTO `user_points` (`user_id`, `total_points`, `available_points`, `used_points`, `version`, `updated_at`)
SELECT `user_id`, `total_points`, `available_points`, `used_points`, `version`, `updated_at`
FROM `yu_ai_code_mother`.`user_points`;

INSERT INTO `points_log` (`id`, `user_id`, `change_amount`, `balance_after`, `reason`, `related_id`, `created_at`)
SELECT `id`, `user_id`, `change_amount`, `balance_after`, `reason`, `related_id`, `created_at`
FROM `yu_ai_code_mother`.`points_log`;

INSERT INTO `points_code` (
  `id`, `code`, `points`, `status`, `used_by_user_id`, `used_at`, `expired_at`, `created_by`, `created_at`
)
SELECT
  `id`, `code`, `points`, `status`, `used_by_user_id`, `used_at`, `expired_at`, `created_by`, `created_at`
FROM `yu_ai_code_mother`.`points_code`;

INSERT INTO `user_sign_in` (`id`, `user_id`, `sign_date`, `continuous_days`, `reward_points`, `created_at`)
SELECT `id`, `user_id`, `sign_date`, `continuous_days`, `reward_points`, `created_at`
FROM `yu_ai_code_mother`.`user_sign_in`;

INSERT INTO `app` (
  `id`, `appName`, `cover`, `initPrompt`, `codeGenType`, `deployKey`, `deployedTime`,
  `priority`, `userId`, `editTime`, `createTime`, `updateTime`, `isDelete`, `latestDeploymentId`, `is_published`
)
SELECT
  `id`, `appName`, `cover`, `initPrompt`, `codeGenType`, `deployKey`, `deployedTime`,
  `priority`, `userId`, `editTime`, `createTime`, `updateTime`, `isDelete`, `latestDeploymentId`, `is_published`
FROM `yu_ai_code_mother`.`app`;

INSERT INTO `deployment_history` (`id`, `app_id`, `deploy_name`, `deploy_time`, `version`)
SELECT `id`, `app_id`, `deploy_name`, `deploy_time`, `version`
FROM `yu_ai_code_mother`.`deployment_history`;

INSERT INTO `chat_history` (
  `id`, `conversation_id`, `trace_id`, `task_id`, `request_id`, `agent_run_id`,
  `message`, `messageType`, `appId`, `userId`, `createTime`, `updateTime`, `isDelete`, `user_visible`, `meta_data`
)
SELECT
  c.`id`,
  CONCAT('conv_', c.`appId`, '_', c.`userId`) AS `conversation_id`,
  CONCAT('hist_trace_', LPAD(HEX(c.`id`), 16, '0')) AS `trace_id`,
  NULL AS `task_id`,
  NULL AS `request_id`,
  NULL AS `agent_run_id`,
  c.`message`, c.`messageType`, c.`appId`, c.`userId`,
  c.`createTime`, c.`updateTime`, c.`isDelete`, c.`user_visible`, c.`meta_data`
FROM `yu_ai_code_mother`.`chat_history` c;

INSERT INTO `ai_tool_log` (
  `id`, `ai_message_id`, `conversation_id`, `trace_id`, `task_id`, `agent_run_id`, `tool_run_id`,
  `tool_name`, `file_path`, `action`, `summary`, `status`, `error_message`, `args_json`, `result_json`,
  `start_time`, `end_time`, `created_at`, `updated_at`, `cost_time`
)
SELECT
  t.`id`, t.`ai_message_id`, ch.`conversation_id`, ch.`trace_id`, ch.`task_id`,
  NULL AS `agent_run_id`, NULL AS `tool_run_id`,
  t.`tool_name`, t.`file_path`, t.`action`, t.`summary`,
  'SUCCESS' AS `status`, NULL AS `error_message`, NULL AS `args_json`, NULL AS `result_json`,
  t.`created_at` AS `start_time`, t.`updated_at` AS `end_time`,
  t.`created_at`, t.`updated_at`, t.`cost_time`
FROM `yu_ai_code_mother`.`ai_tool_log` t
LEFT JOIN `zriyo_agent_platform_test`.`chat_history` ch
  ON ch.`id` = t.`ai_message_id`;

INSERT INTO `ai_code_gen_record` (
  `id`, `app_id`, `user_id`, `conversation_id`, `trace_id`, `task_id`, `workflow_id`, `plan_id`, `request_id`,
  `status`, `stage`, `error_message`, `project_dir`, `file_count`, `start_time`, `end_time`, `duration_ms`,
  `message_id`, `created_at`, `updated_at`
)
SELECT
  r.`id`, r.`app_id`, r.`user_id`,
  ch.`conversation_id`,
  COALESCE(ch.`trace_id`, CONCAT('hist_trace_codegen_', LPAD(HEX(r.`id`), 16, '0'))) AS `trace_id`,
  CONCAT('legacy_task_', r.`id`) AS `task_id`,
  'legacy_codegen' AS `workflow_id`,
  NULL AS `plan_id`,
  CONCAT('legacy_req_', r.`id`) AS `request_id`,
  r.`status`, r.`stage`, r.`error_message`, r.`project_dir`, r.`file_count`,
  r.`start_time`, r.`end_time`, r.`duration_ms`, r.`message_id`, r.`created_at`, r.`updated_at`
FROM `yu_ai_code_mother`.`ai_code_gen_record` r
LEFT JOIN `zriyo_agent_platform_test`.`chat_history` ch
  ON ch.`id` = r.`message_id`;

-- 初始化两条默认工作流（可按需版本化）
INSERT INTO `workflow_def` (`workflow_id`, `version`, `name`, `spec_json`, `enabled`)
VALUES
(
  'frontend_init',
  1,
  '前端初始化工作流',
  JSON_OBJECT(
    'workflow_id', 'frontend_init',
    'version', 1,
    'stages', JSON_ARRAY('prepare', 'init_scaffold', 'finalize'),
    'allowed_actions', JSON_ARRAY('init_project', 'emit_event'),
    'retry_policy', JSON_OBJECT('max', 1, 'backoff_ms', 1000),
    'timeout_ms', 60000
  ),
  1
),
(
  'frontend_iterate',
  1,
  '前端增量修改工作流',
  JSON_OBJECT(
    'workflow_id', 'frontend_iterate',
    'version', 1,
    'stages', JSON_ARRAY('plan', 'codegen', 'check', 'repair'),
    'allowed_actions', JSON_ARRAY('generate_plan', 'generate_file', 'apply_patch', 'delete_file', 'run_check', 'emit_event'),
    'retry_policy', JSON_OBJECT('max', 2, 'backoff_ms', 2000),
    'timeout_ms', 180000
  ),
  1
);

SET FOREIGN_KEY_CHECKS = 1;

-- 迁移完成后可执行核验：
-- SELECT table_name, table_rows FROM information_schema.tables
-- WHERE table_schema='zriyo_agent_platform_test' ORDER BY table_name;
-- =========================================================
-- 多 Agent 可观测性与追踪表升级脚本（测试库）
-- 目标库：zriyo_agent_platform_test
-- 说明：
-- 1) 面向多 Agent 场景新增通用运行追踪表
-- 2) 保留旧表（ai_code_gen_record / ai_tool_log）并进行可回溯映射
-- 3) 所有表/字段均带 COMMENT，可在数据库工具中直接展示
-- 4) 脚本可重复执行（CREATE IF NOT EXISTS + INSERT IGNORE）
-- =========================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

USE `zriyo_agent_platform_test`;

-- =========================================================
-- 1) 会话主表（从 chat_history 聚合的会话维度）
-- =========================================================
CREATE TABLE IF NOT EXISTS `conversation_session` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `conversation_id` varchar(64) NOT NULL COMMENT '会话ID（全局唯一，跨多轮稳定）',
  `app_id` bigint NOT NULL COMMENT '应用ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `title` varchar(255) DEFAULT NULL COMMENT '会话标题',
  `status` varchar(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '会话状态（ACTIVE/ARCHIVED/CLOSED）',
  `active_plan_id` varchar(64) DEFAULT NULL COMMENT '当前生效计划ID',
  `first_message_time` datetime DEFAULT NULL COMMENT '首条消息时间',
  `last_message_time` datetime DEFAULT NULL COMMENT '末条消息时间',
  `message_count` int NOT NULL DEFAULT '0' COMMENT '消息总数',
  `summary_md` mediumtext COMMENT '会话摘要（Markdown）',
  `context_json` json DEFAULT NULL COMMENT '会话上下文（JSON）',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_conversation_id` (`conversation_id`),
  KEY `idx_conversation_app_user` (`app_id`,`user_id`,`updated_at`),
  KEY `idx_conversation_status` (`status`,`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话主表（多轮会话维度）';

-- =========================================================
-- 2) Agent 运行主表（通用，替代仅 codegen 语义）
-- =========================================================
CREATE TABLE IF NOT EXISTS `agent_run_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `agent_run_id` varchar(64) NOT NULL COMMENT 'Agent执行ID（全局唯一）',
  `task_id` varchar(64) DEFAULT NULL COMMENT '任务ID',
  `trace_id` varchar(64) DEFAULT NULL COMMENT '链路追踪ID',
  `conversation_id` varchar(64) DEFAULT NULL COMMENT '会话ID',
  `request_id` varchar(64) DEFAULT NULL COMMENT '请求ID',
  `app_id` bigint NOT NULL COMMENT '应用ID',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `workflow_id` varchar(64) DEFAULT NULL COMMENT '工作流ID',
  `plan_id` varchar(64) DEFAULT NULL COMMENT '计划ID',
  `agent_type` varchar(64) NOT NULL COMMENT 'Agent类型（CODE/DOC/CHART/DEPLOY/...）',
  `agent_name` varchar(128) DEFAULT NULL COMMENT 'Agent名称（如 CodeAgent）',
  `run_mode` varchar(32) NOT NULL DEFAULT 'SYNC' COMMENT '执行模式（SYNC/ASYNC/STREAM）',
  `status` varchar(32) NOT NULL COMMENT '执行状态（PENDING/RUNNING/SUCCESS/FAILED/CANCELLED）',
  `current_stage` varchar(64) DEFAULT NULL COMMENT '当前阶段',
  `input_json` json DEFAULT NULL COMMENT '输入参数（JSON）',
  `output_json` json DEFAULT NULL COMMENT '输出结果（JSON）',
  `error_code` varchar(64) DEFAULT NULL COMMENT '错误码',
  `error_message` text COMMENT '错误信息',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `duration_ms` bigint DEFAULT NULL COMMENT '耗时（毫秒）',
  `source_record_id` bigint DEFAULT NULL COMMENT '来源旧记录ID（ai_code_gen_record.id）',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_agent_run_id` (`agent_run_id`),
  KEY `idx_agent_run_trace` (`trace_id`,`created_at`),
  KEY `idx_agent_run_task` (`task_id`,`created_at`),
  KEY `idx_agent_run_conversation` (`conversation_id`,`created_at`),
  KEY `idx_agent_run_workflow` (`workflow_id`,`created_at`),
  KEY `idx_agent_run_status` (`status`,`updated_at`),
  KEY `idx_agent_run_type` (`agent_type`,`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent执行主记录（通用）';

-- =========================================================
-- 3) Agent 阶段/步骤执行明细
-- =========================================================
CREATE TABLE IF NOT EXISTS `agent_step_run` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `step_run_id` varchar(64) NOT NULL COMMENT '步骤执行ID（全局唯一）',
  `agent_run_id` varchar(64) NOT NULL COMMENT '所属Agent执行ID',
  `task_id` varchar(64) DEFAULT NULL COMMENT '任务ID',
  `trace_id` varchar(64) DEFAULT NULL COMMENT '链路追踪ID',
  `step_id` varchar(64) DEFAULT NULL COMMENT '步骤ID（对应 plan_step.step_id）',
  `stage` varchar(64) DEFAULT NULL COMMENT '阶段名称',
  `action` varchar(64) NOT NULL COMMENT '动作名称（generate_file/apply_patch/...）',
  `attempt` int NOT NULL DEFAULT '0' COMMENT '尝试次数',
  `status` varchar(32) NOT NULL COMMENT '状态（PENDING/RUNNING/SUCCESS/FAILED/CANCELLED）',
  `message` varchar(512) DEFAULT NULL COMMENT '简要说明',
  `input_json` json DEFAULT NULL COMMENT '输入参数（JSON）',
  `output_json` json DEFAULT NULL COMMENT '输出结果（JSON）',
  `error_code` varchar(64) DEFAULT NULL COMMENT '错误码',
  `error_message` text COMMENT '错误信息',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `duration_ms` bigint DEFAULT NULL COMMENT '耗时（毫秒）',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_step_run_id` (`step_run_id`),
  KEY `idx_step_run_agent` (`agent_run_id`,`created_at`),
  KEY `idx_step_run_trace` (`trace_id`,`created_at`),
  KEY `idx_step_run_task` (`task_id`,`created_at`),
  KEY `idx_step_run_status` (`status`,`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent步骤执行明细';

-- =========================================================
-- 4) 工具执行日志（通用，兼容多 Agent）
-- =========================================================
CREATE TABLE IF NOT EXISTS `agent_tool_run` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tool_run_id` varchar(64) NOT NULL COMMENT '工具执行ID（全局唯一）',
  `agent_run_id` varchar(64) DEFAULT NULL COMMENT '所属Agent执行ID',
  `step_run_id` varchar(64) DEFAULT NULL COMMENT '所属步骤执行ID',
  `task_id` varchar(64) DEFAULT NULL COMMENT '任务ID',
  `trace_id` varchar(64) DEFAULT NULL COMMENT '链路追踪ID',
  `conversation_id` varchar(64) DEFAULT NULL COMMENT '会话ID',
  `agent_type` varchar(64) DEFAULT NULL COMMENT 'Agent类型',
  `tool_name` varchar(64) NOT NULL COMMENT '工具名称',
  `tool_provider` varchar(64) DEFAULT NULL COMMENT '工具提供方（internal/python/external）',
  `action` varchar(64) DEFAULT NULL COMMENT '动作类型（read/write/search...）',
  `target` varchar(512) DEFAULT NULL COMMENT '目标资源（文件路径/URL/表名）',
  `status` varchar(32) NOT NULL COMMENT '状态（RUNNING/SUCCESS/FAILED/CANCELLED）',
  `billable` tinyint NOT NULL DEFAULT '0' COMMENT '是否计费（0否1是，工具默认不计费）',
  `external_request_count` int NOT NULL DEFAULT '0' COMMENT '外部请求次数（默认0）',
  `points_cost` int NOT NULL DEFAULT '0' COMMENT '积分消耗（默认0）',
  `summary` text COMMENT '执行摘要',
  `args_json` json DEFAULT NULL COMMENT '输入参数（JSON）',
  `result_json` json DEFAULT NULL COMMENT '输出结果（JSON）',
  `error_code` varchar(64) DEFAULT NULL COMMENT '错误码',
  `error_message` text COMMENT '错误信息',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `duration_ms` bigint DEFAULT NULL COMMENT '耗时（毫秒）',
  `source_tool_log_id` bigint DEFAULT NULL COMMENT '来源旧记录ID（ai_tool_log.id）',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tool_run_id` (`tool_run_id`),
  KEY `idx_agent_tool_trace` (`trace_id`,`created_at`),
  KEY `idx_agent_tool_task` (`task_id`,`created_at`),
  KEY `idx_agent_tool_conversation` (`conversation_id`,`created_at`),
  KEY `idx_agent_tool_agent_run` (`agent_run_id`,`created_at`),
  KEY `idx_agent_tool_name` (`tool_name`,`created_at`),
  KEY `idx_agent_tool_status` (`status`,`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工具执行日志（通用）';

-- =========================================================
-- 5) LLM 调用日志（模型观测）
-- =========================================================
CREATE TABLE IF NOT EXISTS `llm_call_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `llm_call_id` varchar(64) NOT NULL COMMENT '模型调用ID（全局唯一）',
  `agent_run_id` varchar(64) DEFAULT NULL COMMENT '所属Agent执行ID',
  `step_run_id` varchar(64) DEFAULT NULL COMMENT '所属步骤执行ID',
  `task_id` varchar(64) DEFAULT NULL COMMENT '任务ID',
  `trace_id` varchar(64) DEFAULT NULL COMMENT '链路追踪ID',
  `conversation_id` varchar(64) DEFAULT NULL COMMENT '会话ID',
  `provider` varchar(64) NOT NULL COMMENT '模型提供方（openai/anthropic/...）',
  `model` varchar(128) NOT NULL COMMENT '模型名称',
  `provider_request_id` varchar(128) DEFAULT NULL COMMENT '模型侧请求ID（用于对账）',
  `call_count` int NOT NULL DEFAULT '1' COMMENT '外部调用次数（默认1）',
  `billable` tinyint NOT NULL DEFAULT '1' COMMENT '是否计费（1是0否）',
  `estimated` tinyint NOT NULL DEFAULT '0' COMMENT 'token 是否估算值（0否1是）',
  `request_tokens` int DEFAULT NULL COMMENT '输入Token数',
  `response_tokens` int DEFAULT NULL COMMENT '输出Token数',
  `total_tokens` int DEFAULT NULL COMMENT '总Token数',
  `points_cost` int NOT NULL DEFAULT '0' COMMENT '积分消耗（可负值表示扣减）',
  `settle_status` varchar(16) NOT NULL DEFAULT 'PENDING' COMMENT '结算状态（PENDING/SETTLED/REVERSED）',
  `idempotency_key` varchar(128) DEFAULT NULL COMMENT '幂等键（防止重复扣费）',
  `latency_ms` bigint DEFAULT NULL COMMENT '调用耗时（毫秒）',
  `status` varchar(32) NOT NULL COMMENT '调用状态（SUCCESS/FAILED/CANCELLED）',
  `error_code` varchar(64) DEFAULT NULL COMMENT '错误码',
  `error_message` text COMMENT '错误信息',
  `request_preview` text COMMENT '请求摘要（脱敏）',
  `response_preview` text COMMENT '响应摘要（脱敏）',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_llm_call_id` (`llm_call_id`),
  UNIQUE KEY `uk_llm_idempotency_key` (`idempotency_key`),
  KEY `idx_llm_trace` (`trace_id`,`created_at`),
  KEY `idx_llm_task` (`task_id`,`created_at`),
  KEY `idx_llm_agent_run` (`agent_run_id`,`created_at`),
  KEY `idx_llm_provider_model` (`provider`,`model`,`created_at`),
  KEY `idx_llm_status` (`status`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LLM调用日志';

-- =========================================================
-- 6) LLM 积分账本（仅 LLM 计费）
-- =========================================================
CREATE TABLE IF NOT EXISTS `llm_billing_ledger` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `ledger_id` varchar(64) NOT NULL COMMENT '账本流水ID（全局唯一）',
  `llm_call_id` varchar(64) NOT NULL COMMENT '关联 llm_call_log.llm_call_id',
  `trace_id` varchar(64) DEFAULT NULL COMMENT '链路追踪ID',
  `task_id` varchar(64) DEFAULT NULL COMMENT '任务ID',
  `conversation_id` varchar(64) DEFAULT NULL COMMENT '会话ID',
  `app_id` bigint DEFAULT NULL COMMENT '应用ID',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `provider` varchar(64) NOT NULL COMMENT '模型提供方',
  `model` varchar(128) NOT NULL COMMENT '模型名称',
  `request_tokens` int NOT NULL DEFAULT '0' COMMENT '输入 token',
  `response_tokens` int NOT NULL DEFAULT '0' COMMENT '输出 token',
  `total_tokens` int NOT NULL DEFAULT '0' COMMENT '总 token',
  `delta_points` int NOT NULL DEFAULT '0' COMMENT '积分变化（扣减为负）',
  `billing_unit` varchar(32) NOT NULL DEFAULT 'TOKEN' COMMENT '计费单位（TOKEN）',
  `pricing_version` varchar(32) NOT NULL DEFAULT 'v1' COMMENT '计价版本',
  `settle_status` varchar(16) NOT NULL DEFAULT 'PENDING' COMMENT '结算状态（PENDING/SETTLED/REVERSED）',
  `idempotency_key` varchar(128) NOT NULL COMMENT '幂等键（防止重复扣费）',
  `settled_at` datetime DEFAULT NULL COMMENT '结算完成时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ledger_id` (`ledger_id`),
  UNIQUE KEY `uk_ledger_idempotency` (`idempotency_key`),
  KEY `idx_ledger_trace` (`trace_id`,`created_at`),
  KEY `idx_ledger_task` (`task_id`,`created_at`),
  KEY `idx_ledger_user` (`user_id`,`created_at`),
  KEY `idx_ledger_call` (`llm_call_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LLM 积分账本（工具调用默认不计费）';

-- =========================================================
-- 7) Agent 路由切换日志（多 Agent 协作链）
-- =========================================================
CREATE TABLE IF NOT EXISTS `agent_handoff_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `handoff_id` varchar(64) NOT NULL COMMENT '切换事件ID（全局唯一）',
  `task_id` varchar(64) DEFAULT NULL COMMENT '任务ID',
  `trace_id` varchar(64) DEFAULT NULL COMMENT '链路追踪ID',
  `conversation_id` varchar(64) DEFAULT NULL COMMENT '会话ID',
  `from_agent` varchar(128) DEFAULT NULL COMMENT '来源Agent',
  `to_agent` varchar(128) NOT NULL COMMENT '目标Agent',
  `reason` varchar(512) DEFAULT NULL COMMENT '切换原因',
  `metadata_json` json DEFAULT NULL COMMENT '扩展信息（JSON）',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_handoff_id` (`handoff_id`),
  KEY `idx_handoff_trace` (`trace_id`,`created_at`),
  KEY `idx_handoff_task` (`task_id`,`created_at`),
  KEY `idx_handoff_conversation` (`conversation_id`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent切换日志';

-- =========================================================
-- 8) 历史数据回填（幂等）
-- =========================================================

-- 8.1 从 chat_history 聚合会话
INSERT INTO `conversation_session` (
  `conversation_id`, `app_id`, `user_id`, `title`, `status`,
  `first_message_time`, `last_message_time`, `message_count`, `context_json`
)
SELECT
  ch.`conversation_id`,
  MIN(ch.`appId`) AS `app_id`,
  MIN(ch.`userId`) AS `user_id`,
  CONCAT('会话-', ch.`conversation_id`) AS `title`,
  'ACTIVE' AS `status`,
  MIN(ch.`createTime`) AS `first_message_time`,
  MAX(ch.`createTime`) AS `last_message_time`,
  COUNT(*) AS `message_count`,
  JSON_OBJECT('source', 'backfill_chat_history') AS `context_json`
FROM `chat_history` ch
WHERE ch.`conversation_id` IS NOT NULL AND ch.`conversation_id` <> ''
GROUP BY ch.`conversation_id`
ON DUPLICATE KEY UPDATE
  `last_message_time` = VALUES(`last_message_time`),
  `message_count` = VALUES(`message_count`),
  `updated_at` = CURRENT_TIMESTAMP;

-- 8.2 从 ai_code_gen_record 回填通用 Agent 运行记录
INSERT IGNORE INTO `agent_run_record` (
  `agent_run_id`, `task_id`, `trace_id`, `conversation_id`, `request_id`,
  `app_id`, `user_id`, `workflow_id`, `plan_id`, `agent_type`, `agent_name`,
  `run_mode`, `status`, `current_stage`, `error_message`,
  `start_time`, `end_time`, `duration_ms`, `source_record_id`, `created_at`, `updated_at`
)
SELECT
  CONCAT('agr_', r.`id`) AS `agent_run_id`,
  r.`task_id`,
  r.`trace_id`,
  r.`conversation_id`,
  r.`request_id`,
  r.`app_id`,
  r.`user_id`,
  r.`workflow_id`,
  r.`plan_id`,
  CASE
    WHEN r.`workflow_id` LIKE 'frontend_%' OR r.`workflow_id` LIKE 'legacy_%' THEN 'CODE'
    ELSE 'UNKNOWN'
  END AS `agent_type`,
  'CodeAgent' AS `agent_name`,
  'ASYNC' AS `run_mode`,
  r.`status`,
  r.`stage` AS `current_stage`,
  r.`error_message`,
  r.`start_time`,
  r.`end_time`,
  r.`duration_ms`,
  r.`id` AS `source_record_id`,
  r.`created_at`,
  r.`updated_at`
FROM `ai_code_gen_record` r;

-- 8.3 从 ai_tool_log 回填通用工具日志
INSERT IGNORE INTO `agent_tool_run` (
  `tool_run_id`, `agent_run_id`, `task_id`, `trace_id`, `conversation_id`, `agent_type`,
  `tool_name`, `tool_provider`, `action`, `target`, `status`, `summary`,
  `args_json`, `result_json`, `error_message`, `start_time`, `end_time`, `duration_ms`,
  `source_tool_log_id`, `created_at`, `updated_at`
)
SELECT
  COALESCE(t.`tool_run_id`, CONCAT('tr_', t.`id`)) AS `tool_run_id`,
  ar.`agent_run_id`,
  t.`task_id`,
  t.`trace_id`,
  t.`conversation_id`,
  ar.`agent_type`,
  t.`tool_name`,
  'internal' AS `tool_provider`,
  t.`action`,
  t.`file_path` AS `target`,
  t.`status`,
  t.`summary`,
  t.`args_json`,
  t.`result_json`,
  t.`error_message`,
  t.`start_time`,
  t.`end_time`,
  t.`cost_time` AS `duration_ms`,
  t.`id` AS `source_tool_log_id`,
  COALESCE(t.`created_at`, CURRENT_TIMESTAMP),
  COALESCE(t.`updated_at`, CURRENT_TIMESTAMP)
FROM `ai_tool_log` t
LEFT JOIN `agent_run_record` ar ON ar.`task_id` = t.`task_id`;

SET FOREIGN_KEY_CHECKS = 1;

-- 核验建议：
-- SELECT COUNT(*) FROM conversation_session;
-- SELECT COUNT(*) FROM agent_run_record;
-- SELECT COUNT(*) FROM agent_tool_run;
