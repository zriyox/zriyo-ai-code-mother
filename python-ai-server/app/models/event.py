"""
SSE 事件模型定义
"""

from pydantic import BaseModel
from typing import Dict, Any, Optional
from .enums import SseEventType


class SseEvent(BaseModel):
    """统一 SSE 事件格式"""
    trace_id: str
    event_type: SseEventType
    timestamp: int
    data: Dict[str, Any]
    event_id: Optional[str] = None

    def to_sse(self) -> str:
        """转换为 SSE 格式"""
        import json
        import time

        return f"event: {self.event_type.value}\ndata: {json.dumps(self.model_dump())}\n\n"


# 兼容旧代码
AgentEvent = SseEvent
EventType = SseEventType
