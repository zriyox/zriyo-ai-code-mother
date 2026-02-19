"""
枚举定义 - 与 Java 侧保持一致
"""

from enum import Enum


# ==================== SSE 事件类型 ====================

class SseEventType(str, Enum):
    """SSE 事件类型 - 与 Java SseEventType 对齐"""

    # 请求生命周期
    REQUEST_START = "request_start"
    REQUEST_COMPLETE = "request_complete"
    REQUEST_CANCEL = "request_cancel"

    # Agent 编排
    AGENT_THOUGHT = "agent_thought"
    AGENT_SWITCH = "agent_switch"
    AGENT_START = "agent_start"
    AGENT_COMPLETE = "agent_complete"

    # Tool 执行
    TOOL_CALL = "tool_call"
    TOOL_RESULT = "tool_result"
    TOOL_ERROR = "tool_error"

    # 进度更新
    PROGRESS = "progress"
    STAGE = "stage"

    # 文件操作
    FILE_CREATED = "file_created"
    FILE_UPDATED = "file_updated"

    # 流式输出
    TEXT_CHUNK = "text_chunk"
    CODE_CHUNK = "code_chunk"
    MARKDOWN_CHUNK = "markdown_chunk"

    # 错误与警告
    ERROR = "error"
    WARNING = "warning"

    # 系统事件
    HEARTBEAT = "heartbeat"
    LOG = "log"


# ==================== 意图类型 ====================

class IntentType(str, Enum):
    """意图类型 - 与 Java IntentType 对齐"""

    # 代码相关
    CODE_GEN = "code_gen"
    CODE_FIX = "code_fix"
    CODE_EXPLAIN = "code_explain"
    CODE_REFACTOR = "code_refactor"
    CODE_REVIEW = "code_review"

    # 文档相关
    DOC_ANALYZE = "doc_analyze"
    DOC_SUMMARIZE = "doc_summarize"
    DOC_EXTRACT = "doc_extract"

    # 图表相关
    CHART_DRAW = "chart_draw"
    DATA_ANALYZE = "data_analyze"

    # 部署相关
    DEPLOY = "deploy"
    ROLLBACK = "rollback"

    # 调试相关
    DEBUG = "debug"
    TEST = "test"

    # 其他
    UNKNOWN = "unknown"


# ==================== Agent 类型 ====================

class AgentType(str, Enum):
    """Agent 类型 - 与 Java AgentType 对齐"""

    ORCHESTRATOR = "orchestrator"
    CODE = "code"
    DOC = "doc"
    CHART = "chart"
    DEPLOY = "deploy"
    DEBUG = "debug"
    TEST = "test"
    EXPLAIN = "explain"
    SEARCH = "search"
    GENERAL = "general"


# ==================== 执行步骤状态 ====================

class StepStatus(str, Enum):
    """执行步骤状态 - 与 Java StepStatus 对齐"""

    PENDING = "pending"
    RUNNING = "running"
    COMPLETED = "completed"
    FAILED = "failed"
    CANCELLED = "cancelled"
    SKIPPED = "skipped"


# ==================== 执行阶段 ====================

class ExecutionStage(str, Enum):
    """执行阶段 - 与 Java ExecutionStage 对齐"""

    INITIALIZING = "initializing"
    INTENT_RECOGNITION = "intent_recognition"
    PLANNING = "planning"
    EXECUTING = "executing"
    VALIDATING = "validating"
    COMPLETED = "completed"
    FAILED = "failed"
    CANCELLED = "cancelled"


# ==================== 兼容旧代码 ====================
# 使用 StrEnum 作为兼容层

from enum import StrEnum


class EventType(StrEnum):
    """兼容旧代码的别名"""

    START = "request_start"
    COMPLETE = "request_complete"
    AGENT_THINKING = "agent_thought"
    FILE_READ = "file_read"
    FILE_WRITTEN = "file_written"

    # 同时兼容新的事件类型
    REQUEST_START = "request_start"
    REQUEST_COMPLETE = "request_complete"
    AGENT_THOUGHT = "agent_thought"
    AGENT_SWITCH = "agent_switch"
    AGENT_START = "agent_start"
    AGENT_COMPLETE = "agent_complete"
    TOOL_CALL = "tool_call"
    TOOL_RESULT = "tool_result"
    TOOL_ERROR = "tool_error"
    PROGRESS = "progress"
    STAGE = "stage"
    FILE_CREATED = "file_created"
    FILE_UPDATED = "file_updated"
    TEXT_CHUNK = "text_chunk"
    CODE_CHUNK = "code_chunk"
    MARKDOWN_CHUNK = "markdown_chunk"
    ERROR = "error"
    WARNING = "warning"
    HEARTBEAT = "heartbeat"
    LOG = "log"
