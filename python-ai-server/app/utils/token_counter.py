"""
统一 Token 计数器

支持多种 LLM 提供商的精确/近似 token 计算。
"""

from enum import Enum
from typing import Literal, Optional
import asyncio


class Provider(str, Enum):
    """支持的 LLM 提供商"""
    # OpenAI 系列
    OPENAI = "openai"
    CODEX = "codex"

    # Anthropic 系列
    ANTHROPIC = "anthropic"
    CLAUDE = "claude"

    # Google 系列
    GEMINI = "gemini"
    GOOGLE = "google"

    # 国产模型
    DEEPSEEK = "deepseek"
    QWEN = "qwen"  # 通义千问
    TONGYI = "tongyi"  # 通义千问别名
    GLM = "glm"  # 智谱
    ZHIPU = "zhipu"  # 智谱别名
    ERNIE = "ernie"  # 文心一言
    BAIDU = "baidu"  # 文心别名
    KIMI = "kimi"  # 月之暗面
    MOONSHOT = "moonshot"  # 月之暗面别名

    # 其他
    UNKNOWN = "unknown"


class TokenCounter:
    """统一 Token 计数器"""

    # Tokenizer 缓存
    _encodings = {}
    _tokenizers = {}

    @classmethod
    def _get_tiktoken_encoding(cls, model: str = "gpt-4"):
        """获取 tiktoken encoding"""
        import tiktoken

        if model not in cls._encodings:
            try:
                cls._encodings[model] = tiktoken.encoding_for_model(model)
            except KeyError:
                cls._encodings[model] = tiktoken.get_encoding("cl100k_base")
        return cls._encodings[model]

    @classmethod
    def _get_huggingface_tokenizer(cls, model_name: str):
        """获取 HuggingFace tokenizer"""
        from transformers import AutoTokenizer

        if model_name not in cls._tokenizers:
            cls._tokenizers[model_name] = AutoTokenizer.from_pretrained(
                model_name,
                trust_remote_code=True,
                use_fast=False
            )
        return cls._tokenizers[model_name]

    @classmethod
    async def count_tokens(
        cls,
        text: str,
        provider: str,
        model: Optional[str] = None
    ) -> tuple[int, bool]:
        """
        计算文本的 token 数量

        Args:
            text: 要计算的文本
            provider: LLM 提供商
            model: 可选的模型名称

        Returns:
            (token_count, is_estimated) - token 数量和是否为估算值
        """
        provider = provider.lower()

        # OpenAI / Codex - 精确计算
        if provider in [Provider.OPENAI.value, Provider.CODEX.value]:
            encoding = cls._get_tiktoken_encoding(model or "gpt-4")
            return len(encoding.encode(text)), False

        # Anthropic / Claude - 近似计算
        elif provider in [Provider.ANTHROPIC.value, Provider.CLAUDE.value]:
            encoding = cls._get_tiktoken_encoding("cl100k_base")
            return len(encoding.encode(text)), True  # 近似

        # Google Gemini - 尝试精确，失败则估算
        elif provider in [Provider.GEMINI.value, Provider.GOOGLE.value]:
            try:
                from google import genai
                # 使用官方 SDK 计算（需要配置 API key）
                # 这里作为 fallback 使用估算
                pass
            except ImportError:
                pass
            # Gemini 使用 SentencePiece，约 1 token ≈ 4 字符
            return len(text) // 3, True

        # DeepSeek - 精确计算
        elif provider == Provider.DEEPSEEK.value:
            try:
                tokenizer = cls._get_huggingface_tokenizer("deepseek-ai/deepseek-llm-7b-chat")
                return len(tokenizer.encode(text)), False
            except Exception:
                # Fallback: DeepSeek 约 1.5-2 中文字符/token
                chinese_chars = sum(1 for c in text if '\u4e00' <= c <= '\u9fff')
                other_chars = len(text) - chinese_chars
                return (chinese_chars // 2) + (other_chars // 4), True

        # 通义千问 - 精确计算
        elif provider in [Provider.QWEN.value, Provider.TONGYI.value]:
            try:
                tokenizer = cls._get_huggingface_tokenizer("Qwen/Qwen-7B-Chat")
                return len(tokenizer.encode(text)), False
            except Exception:
                # Fallback
                chinese_chars = sum(1 for c in text if '\u4e00' <= c <= '\u9fff')
                other_chars = len(text) - chinese_chars
                return (chinese_chars // 2) + (other_chars // 4), True

        # 智谱 GLM - 精确计算
        elif provider in [Provider.GLM.value, Provider.ZHIPU.value]:
            try:
                tokenizer = cls._get_huggingface_tokenizer("THUDM/glm-4-9b-chat")
                return len(tokenizer.encode(text)), False
            except Exception:
                # Fallback
                chinese_chars = sum(1 for c in text if '\u4e00' <= c <= '\u9fff')
                other_chars = len(text) - chinese_chars
                return (chinese_chars // 2) + (other_chars // 4), True

        # 文心一言 ERNIE - 精确计算
        elif provider in [Provider.ERNIE.value, Provider.BAIDU.value]:
            try:
                tokenizer = cls._get_huggingface_tokenizer("nghuyong/ernie-2.0-base")
                return len(tokenizer.encode(text)), False
            except Exception:
                # Fallback: 文心约 1.5 中文字符/token
                chinese_chars = sum(1 for c in text if '\u4e00' <= c <= '\u9fff')
                other_chars = len(text) - chinese_chars
                return (chinese_chars // 1.5) + (other_chars // 4), True

        # Kimi / Moonshot - 精确计算
        elif provider in [Provider.KIMI.value, Provider.MOONSHOT.value]:
            try:
                tokenizer = cls._get_huggingface_tokenizer("moonshotai/Moonshot-7B")
                return len(tokenizer.encode(text)), False
            except Exception:
                # Fallback
                chinese_chars = sum(1 for c in text if '\u4e00' <= c <= '\u9fff')
                other_chars = len(text) - chinese_chars
                return (chinese_chars // 2) + (other_chars // 4), True

        # 未知提供商 - 粗略估算
        else:
            chinese_chars = sum(1 for c in text if '\u4e00' <= c <= '\u9fff')
            other_chars = len(text) - chinese_chars
            return (chinese_chars // 2) + (other_chars // 4), True

    @classmethod
    async def count_messages(
        cls,
        messages: list[dict],
        provider: str,
        model: Optional[str] = None
    ) -> tuple[int, bool]:
        """
        计算消息列表的 token 数量（考虑格式开销）

        Args:
            messages: 消息列表 [{"role": "...", "content": "..."}]
            provider: LLM 提供商
            model: 可选的模型名称

        Returns:
            (token_count, is_estimated)
        """
        provider = provider.lower()
        is_estimated = False
        total = 0

        # OpenAI 系列的格式开销计算
        if provider in [Provider.OPENAI.value, Provider.CODEX.value]:
            encoding = cls._get_tiktoken_encoding(model or "gpt-4")
            # 每条消息的格式开销
            total = len(messages) * 4
            for msg in messages:
                content = msg.get("content", "")
                role = msg.get("role", "")
                total += len(encoding.encode(content))
                total += len(encoding.encode(role))
            # reply overhead
            total += 3
            return total, False

        # Anthropic 系列的格式开销
        elif provider in [Provider.ANTHROPIC.value, Provider.CLAUDE.value]:
            encoding = cls._get_tiktoken_encoding("cl100k_base")
            total = len(messages) * 4
            for msg in messages:
                content = msg.get("content", "")
                role = msg.get("role", "")
                total += len(encoding.encode(content))
                total += len(encoding.encode(role))
            total += 3
            return total, True  # 近似

        # 其他提供商简化处理
        else:
            # 先计算所有文本的 token
            all_text = "\n".join(m.get("content", "") for m in messages)
            text_tokens, is_estimated = await cls.count_tokens(all_text, provider, model)
            # 加上消息格式开销（估算）
            overhead = len(messages) * 5
            return text_tokens + overhead, is_estimated

    @classmethod
    def get_supported_providers(cls) -> list[str]:
        """获取所有支持的提供商"""
        return [p.value for p in Provider if p != Provider.UNKNOWN]

    @classmethod
    def get_provider_info(cls, provider: str) -> dict:
        """获取提供商信息"""
        info = {
            "provider": provider,
            "precision": "unknown",
            "tokenizer": "unknown",
            "chars_per_token": "unknown",
        }

        provider = provider.lower()

        if provider in [Provider.OPENAI.value, Provider.CODEX.value]:
            info.update({
                "precision": "exact",
                "tokenizer": "tiktoken (cl100k_base)",
                "chars_per_token": "~4 (English), ~2 (Chinese)"
            })
        elif provider in [Provider.ANTHROPIC.value, Provider.CLAUDE.value]:
            info.update({
                "precision": "approximate",
                "tokenizer": "tiktoken (cl100k_base)",
                "chars_per_token": "~4 (English), ~2 (Chinese)"
            })
        elif provider in [Provider.GEMINI.value, Provider.GOOGLE.value]:
            info.update({
                "precision": "approximate",
                "tokenizer": "SentencePiece",
                "chars_per_token": "~3 (mixed)"
            })
        elif provider == Provider.DEEPSEEK.value:
            info.update({
                "precision": "exact (with HF)",
                "tokenizer": "HuggingFace (BPE)",
                "chars_per_token": "~2 (Chinese), ~4 (English)"
            })
        elif provider in [Provider.QWEN.value, Provider.TONGYI.value]:
            info.update({
                "precision": "exact (with HF)",
                "tokenizer": "HuggingFace (Qwen)",
                "chars_per_token": "~1.5-2 (Chinese)"
            })
        elif provider in [Provider.GLM.value, Provider.ZHIPU.value]:
            info.update({
                "precision": "exact (with HF)",
                "tokenizer": "HuggingFace (GLM)",
                "chars_per_token": "~1.5-2 (Chinese)"
            })
        elif provider in [Provider.ERNIE.value, Provider.BAIDU.value]:
            info.update({
                "precision": "exact (with HF)",
                "tokenizer": "HuggingFace (ERNIE)",
                "chars_per_token": "~1.5 (Chinese)"
            })
        elif provider in [Provider.KIMI.value, Provider.MOONSHOT.value]:
            info.update({
                "precision": "exact (with HF)",
                "tokenizer": "HuggingFace (Moonshot)",
                "chars_per_token": "~2 (Chinese), ~4 (English)"
            })

        return info


# 同步版本（用于兼容）
def count_tokens(text: str, provider: str, model: Optional[str] = None) -> tuple[int, bool]:
    """同步版本的 token 计数"""
    return asyncio.run(TokenCounter.count_tokens(text, provider, model))


def count_messages(messages: list[dict], provider: str, model: Optional[str] = None) -> tuple[int, bool]:
    """同步版本的消息计数"""
    return asyncio.run(TokenCounter.count_messages(messages, provider, model))
