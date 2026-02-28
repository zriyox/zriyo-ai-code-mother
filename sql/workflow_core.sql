-- 工作流核心表结构（MySQL）

-- 1) 工作流定义（可版本化）
CREATE TABLE workflow_def (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  workflow_id VARCHAR(64) NOT NULL COMMENT '工作流标识',
  version INT NOT NULL DEFAULT 1 COMMENT '版本号',
  name VARCHAR(128) NOT NULL COMMENT '名称',
  spec_json JSON NOT NULL COMMENT 'WorkflowSpec JSON',
  enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_workflow_version (workflow_id, version),
  KEY idx_workflow_id (workflow_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流定义';

-- 2) 项目会话（单 active plan 指针）
CREATE TABLE project_session (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  app_id BIGINT NOT NULL COMMENT '应用 ID',
  title VARCHAR(128) DEFAULT NULL COMMENT '项目标题',
  description TEXT COMMENT '项目描述',
  conversation_summary MEDIUMTEXT COMMENT '对话摘要',
  project_structure JSON COMMENT '项目结构摘要',
  active_plan_id VARCHAR(64) DEFAULT NULL COMMENT '当前生效计划',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_app_id (app_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目会话';

-- 3) 规划（Plan A + Plan B）
CREATE TABLE plan (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  plan_id VARCHAR(64) NOT NULL COMMENT '计划 ID',
  app_id BIGINT NOT NULL COMMENT '应用 ID',
  workflow_id VARCHAR(64) NOT NULL COMMENT '工作流标识',
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/ARCHIVED',
  parent_plan_id VARCHAR(64) DEFAULT NULL COMMENT '上一份计划 ID',
  source VARCHAR(32) DEFAULT NULL COMMENT 'auto/user',
  human_plan_md MEDIUMTEXT COMMENT 'Plan A（Markdown）',
  exec_plan_json JSON COMMENT 'Plan B（执行计划 JSON）',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_plan_id (plan_id),
  KEY idx_app_id (app_id),
  KEY idx_workflow_id (workflow_id),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='计划';

-- 4) 规划步骤（用于前端打勾）
CREATE TABLE plan_step (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  plan_id VARCHAR(64) NOT NULL COMMENT '计划 ID',
  step_id VARCHAR(32) NOT NULL COMMENT '步骤 ID（S1/S2）',
  title VARCHAR(256) DEFAULT NULL COMMENT '步骤标题',
  status VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT 'pending/in_progress/completed/failed',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_plan_step (plan_id, step_id),
  KEY idx_plan_id (plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='计划步骤';

-- 5) 任务（状态机驱动）
CREATE TABLE task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  task_id VARCHAR(64) NOT NULL COMMENT '任务 ID',
  app_id BIGINT NOT NULL COMMENT '应用 ID',
  workflow_id VARCHAR(64) NOT NULL COMMENT '工作流标识',
  status VARCHAR(32) NOT NULL COMMENT '任务状态',
  stage VARCHAR(64) DEFAULT NULL COMMENT '当前阶段',
  attempt INT NOT NULL DEFAULT 0 COMMENT '当前尝试次数',
  max_attempt INT NOT NULL DEFAULT 0 COMMENT '最大重试次数',
  retry_count INT NOT NULL DEFAULT 0 COMMENT '已重试次数',
  next_retry_at DATETIME DEFAULT NULL COMMENT '下次重试时间',
  last_error TEXT COMMENT '最近错误',
  trace_id VARCHAR(64) COMMENT '追踪 ID',
  payload_json JSON COMMENT '输入参数',
  result_json JSON COMMENT '输出结果',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  started_at DATETIME DEFAULT NULL COMMENT '开始时间',
  finished_at DATETIME DEFAULT NULL COMMENT '结束时间',
  UNIQUE KEY uk_task_id (task_id),
  KEY idx_app_id (app_id),
  KEY idx_status (status),
  KEY idx_workflow (workflow_id),
  KEY idx_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务';

-- 6) 任务事件（审计 + SSE 来源）
CREATE TABLE task_event (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  task_id VARCHAR(64) NOT NULL COMMENT '任务 ID',
  event_type VARCHAR(64) NOT NULL COMMENT '事件类型',
  stage VARCHAR(64) DEFAULT NULL COMMENT '关联阶段',
  status_from VARCHAR(32) DEFAULT NULL COMMENT '原状态',
  status_to VARCHAR(32) DEFAULT NULL COMMENT '新状态',
  attempt INT NOT NULL DEFAULT 0 COMMENT '尝试次数',
  message VARCHAR(512) DEFAULT NULL COMMENT '描述',
  data_json JSON COMMENT '事件数据',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_task_id (task_id),
  KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务事件';
