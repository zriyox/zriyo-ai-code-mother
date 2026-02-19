"""
Agent 执行 API
"""

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import Optional, List, Dict, Any

router = APIRouter(prefix="/api/v1", tags=["agent"])


class LlmConfig(BaseModel):
    """LLM 配置"""
    provider: str
    model: str
    api_key: str
    temperature: float = 0.7
    max_tokens: int = 4000


class AgentRequest(BaseModel):
    """Agent 执行请求"""
    request_id: str
    trace_id: str
    user_id: int
    app_id: int

    task_type: str                 # "CODE_GEN" | "DOC_PARSE" | "CHART_DRAW" | "CODE_ANALYZE"
    message: str

    # 可选：指定操作的文件
    target_files: Optional[List[str]] = None

    # 项目上下文
    project_context: Optional[Dict[str, Any]] = None
    conversation: Optional[List[Dict[str, str]]] = None

    llm_config: LlmConfig


@router.post("/agent-run")
async def run_agent(request: AgentRequest):
    """
    执行 Agent 任务

    TODO: 待实现
    - Agent 路由
    - LangChain 执行
    - SSE 流式返回
    """
    return {
        "request_id": request.request_id,
        "trace_id": request.trace_id,
        "status": "pending",
        "message": "Agent execution not implemented yet"
    }
