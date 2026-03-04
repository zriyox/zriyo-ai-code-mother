"""
Capability 上下文服务。

职责：
- 读取基础规范文档（SYSTEM.md / SKILLS_INDEX.md）
- 按需求选择 capability（code/* + mcp/*）
- 组装渐进式披露上下文
"""

import asyncio
from dataclasses import dataclass, field
from typing import Any, Dict, List, Optional, Tuple

from app.models.llm import LlmConfig
from app.tools.project import ProjectGenerator


@dataclass
class CapabilityContextResult:
    """能力上下文节点执行结果。"""

    skills_context: Dict[str, str] = field(default_factory=dict)
    selected_capabilities: List[str] = field(default_factory=list)
    disclosure_meta: Dict[str, Any] = field(default_factory=lambda: {"capabilities": []})
    warnings: List[str] = field(default_factory=list)


class CapabilityContextService:
    """能力上下文加载与选择服务。"""

    def __init__(self, generator: ProjectGenerator):
        self.generator = generator

    async def read_system_skill(self) -> Optional[str]:
        return await asyncio.to_thread(self.generator._read_skill, "SYSTEM.md")

    async def read_skills_index(self) -> Optional[str]:
        return await asyncio.to_thread(self.generator._read_skill, "SKILLS_INDEX.md")

    async def select_capabilities(
        self,
        requirement: str,
        context: str,
        skills_index: str,
        llm_config: LlmConfig,
        allowed_capabilities: Optional[List[str]] = None,
        capability_catalog_version: Optional[str] = None,
        max_capabilities: int = 3,
    ) -> List[str]:
        return await asyncio.to_thread(
            self.generator._select_skills_by_llm,
            requirement,
            context,
            skills_index,
            llm_config,
            max_capabilities,
            allowed_capabilities,
            capability_catalog_version,
        )

    async def build_progressive_context(
        self,
        requirement: str,
        selected_capabilities: List[str],
    ) -> Tuple[Dict[str, str], Dict[str, Any]]:
        return await asyncio.to_thread(
            self.generator._build_progressive_capability_context,
            requirement,
            selected_capabilities,
        )

    async def build_context_for_requirement(
        self,
        requirement: str,
        context: str,
        llm_config: LlmConfig,
        allowed_capabilities: Optional[List[str]] = None,
        capability_catalog_version: Optional[str] = None,
        max_capabilities: int = 3,
    ) -> CapabilityContextResult:
        """
        能力节点聚合方法：
        1) 读取 SYSTEM.md
        2) 读取 SKILLS_INDEX.md 并进行能力选择
        3) 读取渐进式能力上下文
        """
        result = CapabilityContextResult()

        system_content = await self.read_system_skill()
        if system_content:
            result.skills_context["SYSTEM.md"] = system_content
        else:
            result.warnings.append("SYSTEM.md not found")

        skills_index = await self.read_skills_index()
        if not skills_index:
            result.warnings.append("SKILLS_INDEX.md not found")
            return result

        try:
            selected = await self.select_capabilities(
                requirement=requirement,
                context=context,
                skills_index=skills_index,
                llm_config=llm_config,
                allowed_capabilities=allowed_capabilities,
                capability_catalog_version=capability_catalog_version,
                max_capabilities=max_capabilities,
            )
        except Exception as exc:
            result.warnings.append(f"skill selection failed: {exc}")
            return result

        result.selected_capabilities = selected
        if not selected:
            return result

        progressive_context, disclosure = await self.build_progressive_context(
            requirement=requirement,
            selected_capabilities=selected,
        )
        result.disclosure_meta = disclosure or {"capabilities": []}

        for capability_name in selected:
            content = progressive_context.get(capability_name)
            if content:
                result.skills_context[capability_name] = content
            else:
                result.warnings.append(f"Skill not found: {capability_name}")

        return result
