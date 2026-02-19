"""
SSE 事件模型定义
"""

from enum import Enum
from pydantic import BaseModel
from typing import Dict, Any


class EventType(str, Enum):
    """事件类型"""
    # 生命周期
    START = "start"
    PROGRESS = "progress"
    COMPLETE = "complete"
    ERROR = "error"

    # Agent 事件
    AGENT_START = "agent_start"
    AGENT_THINKING = "agent_thinking"

    # Tool 事件
    TOOL_CALL = "tool_call"
    TOOL_RESULT = "tool_result"

    # 文件操作事件（通知 Java 记录）
    FILE_READ = "file_read"
    FILE_WRITTEN = "file_written"


class AgentEvent(BaseModel):
    """统一事件格式"""
    trace_id: str
    event_type: EventType
    timestamp: int
    data: Dict[str, Any]

    def to_sse(self) -> str:
        """转换为 SSE 格式"""
        import json
        import time

        return f"event: {self.event_type}\ndata: {json.dumps(self.model_dump())}\n\n"
