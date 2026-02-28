"""
模块职责：统一 LLM 访问适配层，屏蔽不同提供商 SDK 差异。
Java 对照：可类比多实现 Client 的 Facade/Adapter。
"""

from typing import List, Dict, Any, Iterator, AsyncIterator
from loguru import logger

from app.models.llm import LlmConfig, LlmMessage


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
        try:
            model = self._build_model(streaming=False, **kwargs)
            lc_messages = self._to_langchain_messages(messages)
            response = model.invoke(lc_messages)
            return getattr(response, "content", str(response))
        except Exception as e:
            logger.error(f"LLM 调用失败: {e}")
            raise

    def stream(self, messages: List[LlmMessage], **kwargs) -> Iterator[str]:
        """
        流式调用 LLM（同步生成器）

        Yields:
            文本增量内容
        """
        try:
            model = self._build_model(streaming=True, **kwargs)
            lc_messages = self._to_langchain_messages(messages)
            for chunk in model.stream(lc_messages):
                content = getattr(chunk, "content", None)
                if content:
                    yield content
        except Exception as e:
            logger.error(f"LLM 流式调用失败: {e}")
            raise

    async def astream(self, messages: List[LlmMessage], **kwargs) -> AsyncIterator[str]:
        """
        流式调用 LLM（异步生成器）

        Yields:
            文本增量内容
        """
        try:
            model = self._build_model(streaming=True, **kwargs)
            lc_messages = self._to_langchain_messages(messages)
            async for chunk in model.astream(lc_messages):
                content = getattr(chunk, "content", None)
                if content:
                    yield content
        except Exception as e:
            logger.error(f"LLM 异步流式调用失败: {e}")
            raise

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
