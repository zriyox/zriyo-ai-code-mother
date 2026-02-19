"""
内部 API - 供 Java 侧调用
"""

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import List, Dict, Any, Optional
import tiktoken

router = APIRouter(prefix="/api/internal", tags=["internal"])


class TokenCountRequest(BaseModel):
    """Token 计数请求"""
    provider: str
    text: str


class TokenCountResponse(BaseModel):
    """Token 计数响应"""
    token_count: int
    provider: str


class MessagesCountRequest(BaseModel):
    """消息列表 Token 计数请求"""
    provider: str
    messages: List[Dict[str, str]]


@router.post("/count-tokens", response_model=TokenCountResponse)
async def count_tokens(request: TokenCountRequest):
    """
    计算文本的 token 数量（供 Java 侧调用）

    Args:
        request: 包含 provider 和 text 的请求

    Returns:
        token 数量
    """
    provider = request.provider.lower()
    text = request.text

    if provider in ["openai", "codex"]:
        # OpenAI 使用 tiktoken 精确计算
        try:
            encoding = tiktoken.encoding_for_model("gpt-4")
            token_count = len(encoding.encode(text))
        except Exception:
            # 降级到 cl100k_base
            encoding = tiktoken.get_encoding("cl100k_base")
            token_count = len(encoding.encode(text))

    elif provider in ["anthropic", "claude"]:
        # Claude 近似使用 cl100k_base
        encoding = tiktoken.get_encoding("cl100k_base")
        token_count = len(encoding.encode(text))

    else:
        # 其他模型粗略估算：中英文混合 1.5 字符 ≈ 1 token
        chinese_chars = sum(1 for c in text if '\u4e00' <= c <= '\u9fff')
        other_chars = len(text) - chinese_chars
        token_count = (chinese_chars // 2) + (other_chars // 4)

    return TokenCountResponse(
        token_count=token_count,
        provider=provider
    )


@router.post("/count-messages")
async def count_messages(request: MessagesCountRequest):
    """
    计算消息列表的 token 数量

    Args:
        request: 包含 provider 和 messages 的请求

    Returns:
        token 数量
    """
    provider = request.provider.lower()
    messages = request.messages

    if provider in ["openai", "codex"]:
        try:
            encoding = tiktoken.encoding_for_model("gpt-4")
        except Exception:
            encoding = tiktoken.get_encoding("cl100k_base")

        # 每条消息的开销（role + 格式）
        total = len(messages) * 4

        for msg in messages:
            content = msg.get("content", "")
            role = msg.get("role", "")
            total += len(encoding.encode(content))
            total += len(encoding.encode(role))

        # reply overhead
        total += 3
        return {"token_count": total}

    # 其他 provider 简化处理
    total_text = sum(len(m.get("content", "")) for m in messages)
    return {"token_count": total_text // 2}


@router.get("/ping")
async def ping():
    """简单的 ping 端点，测试服务连通性"""
    return {"pong": True}
