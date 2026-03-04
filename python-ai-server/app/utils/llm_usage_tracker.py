"""
LLM 调用统计追踪（请求级 ContextVar）。

用于在一次 Agent 请求内聚合：
- 外部 LLM 调用次数
- usage token 汇总（prompt/completion/total）
"""

from __future__ import annotations

from contextvars import ContextVar
from dataclasses import dataclass, field
from typing import Any, Dict, Optional


@dataclass
class LlmUsageSession:
    """单次请求内的 LLM usage 聚合状态。"""

    trace_id: Optional[str] = None
    task_id: Optional[str] = None

    call_count: int = 0
    calls_with_usage: int = 0

    prompt_tokens: int = 0
    completion_tokens: int = 0
    total_tokens: int = 0

    estimated: bool = False
    provider_calls: Dict[str, int] = field(default_factory=dict)

    def snapshot(self) -> Dict[str, Any]:
        return {
            "trace_id": self.trace_id,
            "task_id": self.task_id,
            "call_count": self.call_count,
            "calls_with_usage": self.calls_with_usage,
            "prompt_tokens": self.prompt_tokens,
            "completion_tokens": self.completion_tokens,
            "total_tokens": self.total_tokens,
            "estimated": self.estimated,
            "provider_calls": dict(self.provider_calls),
        }


_SESSION_VAR: ContextVar[Optional[LlmUsageSession]] = ContextVar("llm_usage_session", default=None)


def begin_llm_usage_session(trace_id: Optional[str] = None, task_id: Optional[str] = None):
    """开始一次新的请求级 usage 聚合会话。"""
    session = LlmUsageSession(trace_id=trace_id, task_id=task_id)
    return _SESSION_VAR.set(session)


def end_llm_usage_session(token=None) -> None:
    """结束 usage 会话并清理 ContextVar。"""
    if token is not None:
        _SESSION_VAR.reset(token)
        return
    _SESSION_VAR.set(None)


def snapshot_llm_usage_session() -> Dict[str, Any]:
    """获取当前会话快照（无会话时返回 0 值快照）。"""
    session = _SESSION_VAR.get()
    if session is None:
        return {
            "trace_id": None,
            "task_id": None,
            "call_count": 0,
            "calls_with_usage": 0,
            "prompt_tokens": 0,
            "completion_tokens": 0,
            "total_tokens": 0,
            "estimated": False,
            "provider_calls": {},
        }
    return session.snapshot()


def diff_llm_usage(before: Dict[str, Any], after: Dict[str, Any]) -> Dict[str, Any]:
    """计算两个 usage 快照之间的增量。"""
    return {
        "call_count": _to_int(after.get("call_count")) - _to_int(before.get("call_count")),
        "calls_with_usage": _to_int(after.get("calls_with_usage")) - _to_int(before.get("calls_with_usage")),
        "prompt_tokens": _to_int(after.get("prompt_tokens")) - _to_int(before.get("prompt_tokens")),
        "completion_tokens": _to_int(after.get("completion_tokens")) - _to_int(before.get("completion_tokens")),
        "total_tokens": _to_int(after.get("total_tokens")) - _to_int(before.get("total_tokens")),
        "estimated": bool(after.get("estimated", False)),
    }


def record_llm_call(
    provider: str,
    usage: Optional[Dict[str, Any]] = None,
    estimated: bool = False,
    external_call: bool = True,
) -> None:
    """记录一次 LLM 调用（仅在会话存在时生效）。"""
    session = _SESSION_VAR.get()
    if session is None:
        return

    if external_call:
        session.call_count += 1

    provider_key = (provider or "unknown").lower()
    session.provider_calls[provider_key] = session.provider_calls.get(provider_key, 0) + 1

    normalized_usage = _normalize_usage(usage)
    if normalized_usage is not None:
        session.calls_with_usage += 1
        session.prompt_tokens += normalized_usage["prompt_tokens"]
        session.completion_tokens += normalized_usage["completion_tokens"]
        session.total_tokens += normalized_usage["total_tokens"]
        session.estimated = session.estimated or normalized_usage["estimated"]

    if estimated:
        session.estimated = True


def _normalize_usage(usage: Optional[Dict[str, Any]]) -> Optional[Dict[str, Any]]:
    if not usage:
        return None

    prompt_tokens = _to_int(
        usage.get("prompt_tokens", usage.get("input_tokens", usage.get("prompt_token_count", 0)))
    )
    completion_tokens = _to_int(
        usage.get("completion_tokens", usage.get("output_tokens", usage.get("completion_token_count", 0)))
    )
    total_tokens = _to_int(usage.get("total_tokens", usage.get("total_token_count", 0)))
    if total_tokens <= 0:
        total_tokens = prompt_tokens + completion_tokens

    return {
        "prompt_tokens": prompt_tokens,
        "completion_tokens": completion_tokens,
        "total_tokens": total_tokens,
        "estimated": bool(usage.get("estimated", False)),
    }


def _to_int(value: Any) -> int:
    if value is None:
        return 0
    try:
        return int(value)
    except (TypeError, ValueError):
        return 0
