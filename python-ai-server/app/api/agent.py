"""
模块职责：Agent 执行相关 API 路由层，负责把请求转交给执行链路。
Java 对照：可类比 Spring MVC Controller（参数校验 + 响应封装）。
"""

from fastapi import APIRouter, Response
from fastapi.responses import StreamingResponse, JSONResponse
from typing import AsyncGenerator

from app.models.request import AgentRequest, ChatMessage
from app.models.result import AgentResult
from app.models.enums import IntentType
from app.utils.sse import create_sse_event
from app.utils.errors import AgentError

router = APIRouter(prefix="/api/v1", tags=["agent"])


@router.post(
    "/agent-run",
    responses={
        200: {"description": "Agent 执行成功（非流式）", "model": AgentResult},
        202: {"description": "Agent 执行中（流式 SSE）"},
        400: {"description": "请求参数错误"},
        500: {"description": "服务器内部错误"},
    },
    summary="执行 Agent 任务",
    description="""
执行 Agent 任务，支持代码生成、文档分析、图表绘制等功能。

**请求头：**
- `Content-Type: application/json`

**任务类型 (task_type)：**
- `code_gen`: 代码生成
- `code_fix`: 代码修复
- `code_explain`: 代码解释
- `code_refactor`: 代码重构
- `code_review`: 代码审查
- `doc_analyze`: 文档分析
- `doc_summarize`: 文档摘要
- `doc_extract`: 文档提取
- `chart_draw`: 图表绘制
- `data_analyze`: 数据分析
- `deploy`: 部署
- `rollback`: 回滚
- `debug`: 调试
- `test`: 测试

**流式响应：**
设置 `stream: true` 时返回 SSE 流，否则返回完整结果。
"""
)
async def run_agent(request: AgentRequest, stream: bool = False) -> Response:
    """
    执行 Agent 任务

    Args:
        request: Agent 执行请求
        stream: 是否使用 SSE 流式返回

    Returns:
        AgentResult (JSON) 或 StreamingResponse (SSE)
    """
    # TODO: 实现 Agent 执行逻辑
    # 1. 根据 task_type 路由到对应的 Agent
    # 2. 执行 LangChain Agent
    # 3. 返回结果（流式或非流式）

    if stream:
        # SSE 流式响应
        async def event_stream() -> AsyncGenerator[str, None]:
            yield create_sse_event(
                "request_start",
                {"request_id": request.request_id, "task_type": request.task_type},
                request.trace_id
            )
            # TODO: 实际执行逻辑
            yield create_sse_event(
                "request_complete",
                {"request_id": request.request_id, "status": "completed"},
                request.trace_id
            )

        return StreamingResponse(
            event_stream(),
            media_type="text/event-stream",
            headers={
                "Cache-Control": "no-cache",
                "Connection": "keep-alive",
                "X-Accel-Buffering": "no",  # 禁用 Nginx 缓冲
            }
        )

    # 非流式响应
    return JSONResponse(
        content=AgentResult(
            request_id=request.request_id,
            trace_id=request.trace_id,
            status="pending",
            data={"message": "Agent execution not implemented yet"}
        ).model_dump()
    )


@router.post(
    "/chat",
    summary="聊天对话",
    description="""
简单的聊天对话接口，用于直接与 LLM 交互。

不使用 Agent 编排，直接将消息转发给 LLM。
适合简单的对话场景。

**请求体：**
```json
{
  "messages": [
    {"role": "user", "content": "Hello!"}
  ],
  "llm_config": {
    "provider": "openai",
    "model": "gpt-4",
    "api_key": "sk-..."
  },
  "stream": false
}
```
""",
)
async def chat(
    messages: list[ChatMessage],
    llm_config: dict,
    stream: bool = False,
) -> Response:
    """
    聊天对话

    Args:
        messages: 对话消息列表
        llm_config: LLM 配置
        stream: 是否使用流式响应

    Returns:
        JSON 响应或 StreamingResponse (SSE)
    """
    # TODO: 实现聊天逻辑
    return JSONResponse(content={"message": "Chat not implemented yet"})


@router.post(
    "/stop",
    summary="停止 Agent 执行",
    description="""
停止正在执行的 Agent 任务。

**请求参数：**
- `trace_id`: 追踪 ID (query parameter)
- `request_id`: 请求 ID (optional query parameter)
""",
)
async def stop_agent(
    trace_id: str,
    request_id: str = None
) -> dict:
    """
    停止 Agent 执行

    Args:
        trace_id: 追踪 ID
        request_id: 请求 ID（可选）

    Returns:
        停止结果
    """
    # TODO: 实现停止逻辑
    return {
        "trace_id": trace_id,
        "request_id": request_id,
        "stopped": True,
        "message": "Stop not implemented yet"
    }
