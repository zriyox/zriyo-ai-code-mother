"""
模块职责：统一 LLM 访问适配层，屏蔽不同提供商 SDK 差异。
Java 对照：可类比多实现 Client 的 Facade/Adapter。
"""

from typing import List, Dict, Any, Iterator, AsyncIterator, Optional
from loguru import logger

from app.models.llm import LlmConfig, LlmMessage
from app.utils.llm_usage_tracker import record_llm_call


class LlmClient:
    """
    LLM 客户端（适配层）。

    作用：屏蔽不同提供商 SDK 差异，让上层统一用 `call/stream/astream`。
    """

    def __init__(self, config: LlmConfig):
        """
        初始化 LLM 客户端

        Args:
            config: LLM 配置
        """
        self.config = config

    def call(self, messages: List[LlmMessage], **kwargs) -> str:
        """
        调用 LLM（非流式）

        Args:
            messages: 消息列表
            **kwargs: 额外参数，覆盖配置

        Returns:
            LLM 响应内容
        """
        details = self.call_with_details(messages, **kwargs)
        return details["content"]

    def build_chat_model(self, streaming: bool = False, **kwargs):
        """
        暴露底层 LangChain ChatModel（用于 tool-calling/structured-output）。
        """
        return self._build_model(streaming=streaming, **kwargs)

    def call_with_details(self, messages: List[LlmMessage], **kwargs) -> Dict[str, Any]:
        """
        调用 LLM（非流式）并返回 usage 细节。

        Returns:
            {"content": str, "usage": dict|None, "request_id": str|None}
        """
        try:
            model = self._build_model(streaming=False, **kwargs)
            lc_messages = self._to_langchain_messages(messages)
            response = model.invoke(lc_messages)
            content = getattr(response, "content", str(response))
            usage = self._extract_usage(response)
            request_id = self._extract_request_id(response)
            record_llm_call(provider=self.config.provider, usage=usage)
            return {
                "content": content,
                "usage": usage,
                "request_id": request_id,
            }
        except Exception as e:
            logger.error(f"LLM 调用失败: {e}")
            raise

    def stream(self, messages: List[LlmMessage], **kwargs) -> Iterator[str]:
        """
        流式调用 LLM（同步生成器）

        Yields:
            文本增量内容
        """
        usage: Optional[Dict[str, Any]] = None
        request_started = False
        try:
            model = self._build_model(streaming=True, **kwargs)
            lc_messages = self._to_langchain_messages(messages)
            request_started = True
            for chunk in model.stream(lc_messages):
                usage = self._merge_usage(usage, self._extract_usage(chunk))
                content = getattr(chunk, "content", None)
                if content:
                    yield content
        except Exception as e:
            logger.error(f"LLM 流式调用失败: {e}")
            raise
        finally:
            if request_started:
                record_llm_call(provider=self.config.provider, usage=usage)

    async def astream(self, messages: List[LlmMessage], **kwargs) -> AsyncIterator[str]:
        """
        流式调用 LLM（异步生成器）

        Yields:
            文本增量内容
        """
        usage: Optional[Dict[str, Any]] = None
        request_started = False
        try:
            model = self._build_model(streaming=True, **kwargs)
            lc_messages = self._to_langchain_messages(messages)
            request_started = True
            async for chunk in model.astream(lc_messages):
                usage = self._merge_usage(usage, self._extract_usage(chunk))
                content = getattr(chunk, "content", None)
                if content:
                    yield content
        except Exception as e:
            logger.error(f"LLM 异步流式调用失败: {e}")
            raise
        finally:
            if request_started:
                record_llm_call(provider=self.config.provider, usage=usage)

    def _build_model(self, streaming: bool, **kwargs):
        """
        根据 provider 构建对应 LangChain ChatModel 实例。

        当前策略：
        - anthropic/claude -> `ChatAnthropic`
        - gemini/google -> `ChatGoogleGenerativeAI`
        - 其他默认走 OpenAI 兼容协议 -> `ChatOpenAI`
        """
        provider = (self.config.provider or "").lower()

        temperature = kwargs.get("temperature", self.config.temperature)
        max_tokens = kwargs.get("max_tokens", self.config.max_tokens)
        timeout = kwargs.get("timeout", self.config.timeout)

        extra_params: Dict[str, Any] = {}
        if self.config.extra_params:
            extra_params.update(self.config.extra_params)
        extra_params.update(kwargs.get("extra_params", {}) or {})

        if provider in ["anthropic", "claude"]:
            try:
                from langchain_anthropic import ChatAnthropic

                return ChatAnthropic(
                    model=self.config.model,
                    api_key=self.config.api_key,
                    temperature=temperature,
                    max_tokens=max_tokens,
                    timeout=timeout,
                    streaming=streaming,
                )
            except ImportError:
                raise RuntimeError("langchain-anthropic 未安装，请运行: pip install langchain-anthropic")

        if provider in ["gemini", "google"]:
            try:
                from langchain_community.chat_models import ChatGoogleGenerativeAI

                return ChatGoogleGenerativeAI(
                    model=self.config.model,
                    google_api_key=self.config.api_key,
                    temperature=temperature,
                    max_output_tokens=max_tokens,
                    timeout=timeout,
                    streaming=streaming,
                )
            except ImportError:
                raise RuntimeError("langchain-community 未安装或缺少 Google 依赖")

        # 默认 OpenAI 兼容（OpenAI / DeepSeek / GLM / Zhipu / BigModel / Ollama 等）
        try:
            from langchain_openai import ChatOpenAI

            base_url = self._resolve_base_url(provider)
            model_kwargs = extra_params

            return ChatOpenAI(
                model=self.config.model,
                api_key=self.config.api_key,
                base_url=base_url,
                temperature=temperature,
                max_tokens=max_tokens,
                timeout=timeout,
                streaming=streaming,
                model_kwargs=model_kwargs,
            )
        except ImportError:
            raise RuntimeError("langchain-openai 未安装，请运行: pip install langchain-openai")

    def _resolve_base_url(self, provider: str) -> str:
        """解析不同 provider 的默认 base_url（可被 config.base_url 覆盖）。"""
        if self.config.base_url:
            return self.config.base_url
        if provider in ["ollama"]:
            return "http://localhost:11434/v1"
        if provider in ["glm", "zhipu", "bigmodel"]:
            return "https://open.bigmodel.cn/api/paas/v4"
        return "https://api.openai.com/v1"

    def _extract_usage(self, payload: Any) -> Optional[Dict[str, Any]]:
        """从 LangChain message/chunk 中提取 usage。"""
        raw = None
        if hasattr(payload, "usage_metadata"):
            raw = getattr(payload, "usage_metadata", None)
        if not raw and hasattr(payload, "response_metadata"):
            response_metadata = getattr(payload, "response_metadata", None)
            if isinstance(response_metadata, dict):
                raw = response_metadata.get("token_usage") or response_metadata.get("usage")
        if not raw and isinstance(payload, dict):
            raw = payload.get("usage") or payload.get("token_usage")
        if not raw or not isinstance(raw, dict):
            return None
        return self._normalize_usage(raw)

    def _extract_request_id(self, payload: Any) -> Optional[str]:
        """提取 provider request id（可选）。"""
        if hasattr(payload, "response_metadata"):
            response_metadata = getattr(payload, "response_metadata", None)
            if isinstance(response_metadata, dict):
                request_id = response_metadata.get("request_id") or response_metadata.get("id")
                if request_id:
                    return str(request_id)
        if isinstance(payload, dict):
            request_id = payload.get("request_id") or payload.get("id")
            if request_id:
                return str(request_id)
        return None

    def _normalize_usage(self, usage: Dict[str, Any]) -> Dict[str, Any]:
        prompt_tokens = self._to_int(
            usage.get("prompt_tokens", usage.get("input_tokens", usage.get("prompt_token_count", 0)))
        )
        completion_tokens = self._to_int(
            usage.get("completion_tokens", usage.get("output_tokens", usage.get("completion_token_count", 0)))
        )
        total_tokens = self._to_int(usage.get("total_tokens", usage.get("total_token_count", 0)))
        if total_tokens <= 0:
            total_tokens = prompt_tokens + completion_tokens
        return {
            "prompt_tokens": prompt_tokens,
            "completion_tokens": completion_tokens,
            "total_tokens": total_tokens,
            "estimated": bool(usage.get("estimated", False)),
        }

    def _merge_usage(
        self,
        base_usage: Optional[Dict[str, Any]],
        new_usage: Optional[Dict[str, Any]],
    ) -> Optional[Dict[str, Any]]:
        if not new_usage:
            return base_usage
        if not base_usage:
            return dict(new_usage)
        return {
            "prompt_tokens": max(self._to_int(base_usage.get("prompt_tokens")), self._to_int(new_usage.get("prompt_tokens"))),
            "completion_tokens": max(
                self._to_int(base_usage.get("completion_tokens")),
                self._to_int(new_usage.get("completion_tokens"))
            ),
            "total_tokens": max(self._to_int(base_usage.get("total_tokens")), self._to_int(new_usage.get("total_tokens"))),
            "estimated": bool(base_usage.get("estimated", False) or new_usage.get("estimated", False)),
        }

    def _to_int(self, value: Any) -> int:
        if value is None:
            return 0
        try:
            return int(value)
        except (TypeError, ValueError):
            return 0

    def _to_langchain_messages(self, messages: List[LlmMessage]):
        """将统一消息结构转换为 LangChain 的消息对象。"""
        try:
            from langchain_core.messages import SystemMessage, HumanMessage, AIMessage
        except ImportError:
            raise RuntimeError("langchain-core 未安装，请运行: pip install langchain-core")

        lc_messages = []
        for m in messages:
            role = (m.role or "").lower()
            if role == "system":
                lc_messages.append(SystemMessage(content=m.content))
            elif role == "assistant":
                lc_messages.append(AIMessage(content=m.content))
            else:
                lc_messages.append(HumanMessage(content=m.content))
        return lc_messages
