"""
SSE (Server-Sent Events) 工具
"""

from typing import AsyncGenerator, Dict, Any
import json
import time


async def sse_stream(event_generator):
    """
    将事件生成器转换为 SSE 流

    Args:
        event_generator: 生成 AgentEvent 的异步生成器

    Yields:
        SSE 格式的字节流
    """
    try:
        async for event in event_generator:
            # 确保 event 有 to_sse 方法
            if hasattr(event, "to_sse"):
                yield event.to_sse()
            else:
                # 如果是字典，手动构造 SSE
                event_type = event.get("event_type", "message")
                data = json.dumps(event)
                yield f"event: {event_type}\ndata: {data}\n\n"
    except Exception as e:
        # 发送错误事件
        error_event = {
            "event_type": "error",
            "timestamp": int(time.time() * 1000),
            "data": {"error": str(e)}
        }
        yield f"event: error\ndata: {json.dumps(error_event)}\n\n"


def create_sse_event(event_type: str, data: Dict[str, Any], trace_id: str = None) -> str:
    """
    创建 SSE 事件字符串

    Args:
        event_type: 事件类型
        data: 事件数据
        trace_id: 追踪 ID（可选）

    Returns:
        SSE 格式字符串
    """
    event = {
        "event_type": event_type,
        "timestamp": int(time.time() * 1000),
        "data": data
    }
    if trace_id:
        event["trace_id"] = trace_id

    return f"event: {event_type}\ndata: {json.dumps(event)}\n\n"
