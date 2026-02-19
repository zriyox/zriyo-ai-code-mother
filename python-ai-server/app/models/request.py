"""
请求模型定义
"""

from pydantic import BaseModel
from typing import Optional, List, Dict, Any


class LlmConfig(BaseModel):
    """LLM 配置"""
    provider: str
    model: str
    api_key: str
    temperature: float = 0.7
    max_tokens: int = 4000
    base_url: Optional[str] = None


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


class ChatMessage(BaseModel):
    """聊天消息"""
    role: str                      # "system" | "user" | "assistant"
    content: str
