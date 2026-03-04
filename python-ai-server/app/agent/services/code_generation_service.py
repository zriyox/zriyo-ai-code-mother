"""
代码生成服务。

职责：
- 组装生成提示词消息
- 流式调用 LLM 并输出分片
- 清洗代码围栏并返回生成结果
"""

from dataclasses import dataclass, field
from typing import Any, AsyncGenerator, Dict, List, Optional

from app.llm.client import LlmClient
from app.models.llm import LlmConfig, LlmMessage
from app.prompts.code_agent_prompts import build_single_file_user_prompt


@dataclass
class CodeGenerationResult:
    """代码生成节点执行结果。"""

    messages: List[LlmMessage] = field(default_factory=list)
    response_text: str = ""
    code: str = ""


class CodeGenerationService:
    """代码生成节点服务。"""

    def __init__(self, system_prompt_builder):
        self._system_prompt_builder = system_prompt_builder

    def build_generation_messages(
        self,
        skills_context: Dict[str, str],
        file_path: str,
        requirement: str,
        plan_summary: Optional[str],
        snippets: List[Dict[str, Any]],
    ) -> List[LlmMessage]:
        system_prompt = self._system_prompt_builder(skills_context)
        user_prompt = build_single_file_user_prompt(
            file_path=file_path,
            requirement=requirement,
            plan_summary=plan_summary,
            snippets=snippets,
        )
        return [
            LlmMessage(role="system", content=system_prompt),
            LlmMessage(role="user", content=user_prompt),
        ]

    async def stream_tokens(
        self,
        llm_config: LlmConfig,
        messages: List[LlmMessage],
    ) -> AsyncGenerator[str, None]:
        """
        流式生成 token。
        """
        client = LlmClient(llm_config)
        async for token in client.astream(
            messages,
            temperature=llm_config.temperature,
            max_tokens=llm_config.max_tokens,
        ):
            yield token

    def finalize_generation(
        self,
        messages: List[LlmMessage],
        response_text: str,
    ) -> CodeGenerationResult:
        code = self.strip_code_fence(response_text)
        return CodeGenerationResult(messages=messages, response_text=response_text, code=code)

    @staticmethod
    def strip_code_fence(text: str) -> str:
        """去掉 ```lang ... ``` 包裹，得到纯代码文本。"""
        content = text.strip()
        if content.startswith("```"):
            content = content.split("\n", 1)[1] if "\n" in content else ""
        if content.endswith("```"):
            content = content.rsplit("```", 1)[0]
        return content.strip()
