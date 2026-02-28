"""
LLM 配置模型
"""

from pydantic import BaseModel, Field
from typing import Optional, Dict, Any


class LlmConfig(BaseModel):
    """LLM 配置"""
    provider: str = Field(..., description="提供商: openai, claude, gemini, deepseek, etc.")
    model: str = Field(..., description="模型名称")
    api_key: str = Field(..., description="API Key")
    base_url: Optional[str] = Field(None, description="API 基础 URL")
    temperature: float = Field(default=0.7, description="温度参数")
    max_tokens: int = Field(default=4000, description="最大 token 数")

    # 可选参数
    timeout: int = Field(default=60, description="超时时间（秒）")
    extra_params: Dict[str, Any] = Field(default_factory=dict, description="额外参数")


class LlmMessage(BaseModel):
    """LLM 消息"""
    role: str = Field(..., description="角色: system, user, assistant")
    content: str = Field(..., description="消息内容")
