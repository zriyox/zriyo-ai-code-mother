"""
项目上下文工具
负责 Skills 读取、选择与代码生成提示词组装。
"""

import json
import os
import re
from pathlib import Path
from typing import Optional, Dict, List, Any, Set, Tuple
from loguru import logger
from langchain_core.messages import SystemMessage, HumanMessage, ToolMessage

from app.llm.client import LlmClient
from app.models.llm import LlmConfig, LlmMessage
from app.utils.llm_usage_tracker import record_llm_call
from app.prompts.project_prompts import (
    build_codegen_system_prompt,
    build_skill_selector_prompts,
)


class ProjectGenerator:
    """项目上下文生成器（Skills + Prompt）"""

    MCP_TOOL_PATTERN = re.compile(r"\bmcp/[a-zA-Z0-9_.:/-]+\b")
    SECTION_HEADING_PATTERN = re.compile(r"^(#{2,4})\s+(.+?)\s*$")

    def __init__(self):
        """初始化生成器。"""
        self.docs_path = self._get_skills_root()
        self.skills_path = self.docs_path

    def _get_skills_root(self) -> Path:
        """获取技能根目录（repo/skills/codeagent）"""
        return Path(__file__).resolve().parents[4] / "skills" / "codeagent"

    def _load_skills_context(
        self,
        requirement: str,
        llm_config: Optional[LlmConfig] = None,
        context: Optional[str] = None,
        allowed_capabilities: Optional[List[str]] = None,
        capability_catalog_version: Optional[str] = None,
    ) -> Dict[str, str]:
        """
        根据需求加载相关的 Skill 文档

        Returns:
            {skill_name: skill_content}
        """
        skills = {}

        # 基础规范（默认需要）
        system_content = self._read_skill("SYSTEM.md")
        if system_content:
            skills["SYSTEM.md"] = system_content

        skills_index = self._read_skill("SKILLS_INDEX.md")

        # LLM 自动选择能力（Skill + MCP）
        selected: List[str] = []
        if llm_config and skills_index:
            try:
                selected = self._select_skills_by_llm(
                    requirement=requirement,
                    context=context,
                    skills_index=skills_index,
                    llm_config=llm_config,
                    allowed_capabilities=allowed_capabilities,
                    capability_catalog_version=capability_catalog_version,
                )
            except Exception as e:
                logger.warning(f"Skill selection failed, fallback to base only: {e}")

        progressive_context, _ = self._build_progressive_capability_context(
            requirement=requirement,
            selected_capabilities=selected,
        )
        skills.update(progressive_context)

        logger.info(f"Loaded {len(skills)} skill documents")
        return skills

    def _list_available_skills(self) -> List[str]:
        """列出可用技能 ID（code/<skill>）"""
        skills_dir = self.skills_path / "code"
        if not skills_dir.exists():
            return []
        items = []
        for p in skills_dir.iterdir():
            if p.is_dir():
                items.append(f"code/{p.name}")
        return sorted(items)

    def _select_skills_by_llm(
        self,
        requirement: str,
        context: Optional[str],
        skills_index: str,
        llm_config: LlmConfig,
        max_skills: int = 3,
        allowed_capabilities: Optional[List[str]] = None,
        capability_catalog_version: Optional[str] = None,
    ) -> List[str]:
        """使用 LLM 选择需要加载的能力列表（Skill + MCP）。"""
        available = self._list_available_capabilities(skills_index)
        effective_available = self._resolve_allowed_capabilities(
            available=available,
            allowed_capabilities=allowed_capabilities,
        )
        if not effective_available:
            return []

        selected_by_tool_calling = self._select_capabilities_by_tool_calling(
            requirement=requirement,
            context=context,
            skills_index=skills_index,
            llm_config=llm_config,
            available=effective_available,
            max_skills=max_skills,
            capability_catalog_version=capability_catalog_version,
        )
        if selected_by_tool_calling is not None:
            return selected_by_tool_calling

        available_text = "\n".join(f"- {s}" for s in effective_available)
        system_prompt, user_prompt = build_skill_selector_prompts(
            requirement=requirement,
            context=context,
            available_text=available_text,
            skills_index=skills_index,
            max_skills=max_skills,
        )
        if capability_catalog_version:
            system_prompt += (
                f"\n\ncapability_catalog_version: {capability_catalog_version}\n"
                "仅允许从已下发 capability 列表中选择。"
            )

        client = LlmClient(llm_config)
        response = client.call(
            [
                LlmMessage(role="system", content=system_prompt),
                LlmMessage(role="user", content=user_prompt),
            ],
            temperature=0.1,
            max_tokens=300,
        )

        selected = self._parse_skill_list(response)
        if not selected:
            return []

        return self._filter_selected_capabilities(selected, effective_available, max_skills)

    def _select_capabilities_by_tool_calling(
        self,
        requirement: str,
        context: Optional[str],
        skills_index: str,
        llm_config: LlmConfig,
        available: List[str],
        max_skills: int,
        capability_catalog_version: Optional[str] = None,
    ) -> Optional[List[str]]:
        """
        使用 LangChain 工具调用能力进行选择。

        返回：
        - `list`：选择结果（可为空列表）
        - `None`：本轮失败，调用方可回退旧逻辑
        """
        client = LlmClient(llm_config)
        try:
            chat_model = client.build_chat_model(streaming=False, temperature=0.1, max_tokens=500)
            if not hasattr(chat_model, "bind_tools"):
                return None

            enum_values = list(available)
            tools = [
                {
                    "type": "function",
                    "function": {
                        "name": "read_capability",
                        "description": "读取某个 capability（code/* 或 mcp/*）的说明内容，便于判断是否需要。",
                        "parameters": {
                            "type": "object",
                            "properties": {
                                "name": {
                                    "type": "string",
                                    "enum": enum_values,
                                    "description": "capability 名称（必须来自 allowed_capabilities）",
                                }
                            },
                            "required": ["name"],
                        },
                    },
                },
                {
                    "type": "function",
                    "function": {
                        "name": "select_capabilities",
                        "description": "最终确认本次需要加载的 capability 列表（最多 3 个）。",
                        "parameters": {
                            "type": "object",
                            "properties": {
                                "capabilities": {
                                    "type": "array",
                                    "items": {"type": "string", "enum": enum_values},
                                    "description": "最终选择的 capability 列表",
                                }
                            },
                            "required": ["capabilities"],
                        },
                    },
                },
            ]
            model = chat_model.bind_tools(tools)

            available_text = "\n".join(f"- {s}" for s in available)
            system_prompt, user_prompt = build_skill_selector_prompts(
                requirement=requirement,
                context=context,
                available_text=available_text,
                skills_index=skills_index,
                max_skills=max_skills,
            )
            system_prompt += (
                "\n\n你必须优先通过工具读取 capability，再调用 select_capabilities 输出最终列表。"
                "不要输出无关文本。"
            )
            if capability_catalog_version:
                system_prompt += f"\ncapability_catalog_version: {capability_catalog_version}"

            messages = [
                SystemMessage(content=system_prompt),
                HumanMessage(content=user_prompt),
            ]

            available_set = set(available)
            alias_map = {item.split("/", 1)[1]: item for item in available if "/" in item}

            for _ in range(6):
                ai_message = model.invoke(messages)
                record_llm_call(provider=llm_config.provider, usage=self._extract_usage_from_message(ai_message))
                messages.append(ai_message)

                tool_calls = getattr(ai_message, "tool_calls", None) or []
                if not tool_calls:
                    parsed = self._parse_skill_list(self._stringify_message_content(ai_message))
                    return self._filter_selected_capabilities(parsed, available, max_skills) if parsed else []

                final_selected: Optional[List[str]] = None
                for tool_call in tool_calls:
                    tool_name = tool_call.get("name")
                    tool_args = tool_call.get("args") if isinstance(tool_call.get("args"), dict) else {}
                    tool_call_id = tool_call.get("id")

                    if tool_name == "read_capability":
                        requested = str(tool_args.get("name", "")).strip()
                        normalized = self._normalize_capability_id(requested, available_set, alias_map)
                        if not normalized:
                            payload = {"ok": False, "error": "capability_not_allowed", "requested": requested}
                        else:
                            content = self._read_capability(normalized)
                            overview_payload = self._build_capability_overview_payload(
                                capability_name=normalized,
                                content=content or "",
                            )
                            payload = {
                                "ok": bool(content),
                                "name": normalized,
                                "overview": overview_payload.get("overview", ""),
                                "section_titles": overview_payload.get("section_titles", []),
                            }
                        if tool_call_id:
                            messages.append(ToolMessage(tool_call_id=tool_call_id, content=json.dumps(payload, ensure_ascii=False)))
                        continue

                    if tool_name == "select_capabilities":
                        raw = tool_args.get("capabilities")
                        if raw is None:
                            raw = tool_args.get("skills")
                        if not isinstance(raw, list):
                            raw = [raw] if raw else []
                        final_selected = self._filter_selected_capabilities([str(item) for item in raw], available, max_skills)
                        payload = {"accepted": final_selected}
                        if tool_call_id:
                            messages.append(ToolMessage(tool_call_id=tool_call_id, content=json.dumps(payload, ensure_ascii=False)))
                        continue

                    if tool_call_id:
                        messages.append(
                            ToolMessage(
                                tool_call_id=tool_call_id,
                                content=json.dumps({"ok": False, "error": f"unsupported_tool:{tool_name}"}, ensure_ascii=False),
                            )
                        )

                if final_selected is not None:
                    return final_selected

            return []
        except Exception as e:
            logger.warning(f"LangChain tool-calling selector failed, fallback to plain mode: {e}")
            return None

    def _resolve_allowed_capabilities(
        self,
        available: List[str],
        allowed_capabilities: Optional[List[str]],
    ) -> List[str]:
        """按 Java 侧下发的白名单约束可选能力集合。"""
        if not available:
            return []
        if not allowed_capabilities:
            return available

        available_set = set(available)
        alias_map = {item.split("/", 1)[1]: item for item in available if "/" in item}
        result: List[str] = []
        for raw in allowed_capabilities:
            normalized = self._normalize_capability_id(str(raw), available_set, alias_map)
            if not normalized:
                logger.warning(f"Ignore unsupported capability from orchestrator: {raw}")
                continue
            if normalized not in result:
                result.append(normalized)
        return result

    def _build_progressive_capability_context(
        self,
        requirement: str,
        selected_capabilities: List[str],
        max_sections_per_capability: int = 2,
        max_overview_chars: int = 600,
        max_section_chars: int = 1200,
    ) -> Tuple[Dict[str, str], Dict[str, Any]]:
        """
        渐进式能力披露：
        1) 先给 overview
        2) 再按 requirement 选择少量章节
        """
        context_docs: Dict[str, str] = {}
        disclosure_meta: Dict[str, Any] = {"capabilities": []}

        for capability_name in selected_capabilities:
            raw_content = self._read_capability(capability_name)
            if not raw_content:
                logger.warning(f"Capability not found: {capability_name}")
                continue

            overview, sections = self._split_capability_content(raw_content)
            selected_sections = self._pick_relevant_sections(
                requirement=requirement,
                sections=sections,
                max_sections=max_sections_per_capability,
            )

            context_docs[capability_name] = self._compose_progressive_context_text(
                capability_name=capability_name,
                overview=overview,
                sections=selected_sections,
                max_overview_chars=max_overview_chars,
                max_section_chars=max_section_chars,
            )

            disclosure_meta["capabilities"].append(
                {
                    "id": capability_name,
                    "section_candidates": [item["title"] for item in sections],
                    "selected_sections": [item["title"] for item in selected_sections],
                    "selected_section_count": len(selected_sections),
                }
            )

        return context_docs, disclosure_meta

    def _build_capability_overview_payload(self, capability_name: str, content: str) -> Dict[str, Any]:
        overview, sections = self._split_capability_content(content)
        return {
            "name": capability_name,
            "overview": overview[:600],
            "section_titles": [item["title"] for item in sections[:20]],
        }

    def _split_capability_content(self, content: str) -> Tuple[str, List[Dict[str, str]]]:
        lines = content.splitlines()
        preamble: List[str] = []
        sections: List[Dict[str, str]] = []
        current_title: Optional[str] = None
        current_lines: List[str] = []

        def flush_current():
            if current_title is None:
                return
            body = "\n".join(current_lines).strip()
            if body:
                sections.append({"title": current_title, "content": body})

        for line in lines:
            heading_match = self.SECTION_HEADING_PATTERN.match(line.strip())
            if heading_match:
                flush_current()
                current_title = heading_match.group(2).strip()
                current_lines = []
                continue

            if current_title is None:
                preamble.append(line)
            else:
                current_lines.append(line)

        flush_current()

        overview = "\n".join(preamble).strip()
        if not overview:
            if sections:
                overview = sections[0]["content"][:800]
            else:
                overview = content[:800]

        return overview, sections

    def _pick_relevant_sections(
        self,
        requirement: str,
        sections: List[Dict[str, str]],
        max_sections: int,
    ) -> List[Dict[str, str]]:
        if not sections or max_sections <= 0:
            return []

        terms = self._extract_query_terms(requirement)
        scored: List[Tuple[int, int, Dict[str, str]]] = []
        for index, section in enumerate(sections):
            title = (section.get("title") or "").lower()
            content_preview = (section.get("content") or "")[:1200].lower()
            score = 0
            for term in terms:
                if term in title:
                    score += 4
                if term in content_preview:
                    score += 1
            scored.append((score, -index, section))

        scored.sort(key=lambda item: (item[0], item[1]), reverse=True)
        top = [item[2] for item in scored[:max_sections]]
        if all(item[0] <= 0 for item in scored[:max_sections]):
            return sections[:max_sections]
        return top

    def _extract_query_terms(self, text: str) -> Set[str]:
        if not text:
            return set()
        raw_terms = re.findall(r"[a-zA-Z][a-zA-Z0-9_-]{2,}|[\u4e00-\u9fff]{2,}", text.lower())
        stop_words = {"the", "and", "with", "for", "this", "that", "file", "code", "页面", "功能", "实现"}
        return {term for term in raw_terms if term not in stop_words}

    def _compose_progressive_context_text(
        self,
        capability_name: str,
        overview: str,
        sections: List[Dict[str, str]],
        max_overview_chars: int,
        max_section_chars: int,
    ) -> str:
        parts = [f"# {capability_name}", "## 概览", (overview or "")[:max_overview_chars]]
        if sections:
            parts.append("## 选中章节")
            for section in sections:
                title = section.get("title", "未命名章节")
                content = (section.get("content") or "")[:max_section_chars]
                parts.append(f"### {title}\n{content}")
        return "\n\n".join(parts).strip()

    def _filter_selected_capabilities(
        self,
        selected: List[str],
        available: List[str],
        max_skills: int,
    ) -> List[str]:
        available_set = set(available)
        alias_map = {s.split("/", 1)[1]: s for s in available}

        filtered: List[str] = []
        for raw in selected:
            name = raw.strip()
            if name in available_set:
                filtered.append(name)
            elif name in alias_map:
                filtered.append(alias_map[name])

        # 去重 + 截断（沿用 max_skills 命名，语义为“最多加载能力数”）
        result: List[str] = []
        for s in filtered:
            if s not in result:
                result.append(s)
            if len(result) >= max_skills:
                break
        return result

    def _list_available_capabilities(self, skills_index: Optional[str] = None) -> List[str]:
        capabilities = set(self._list_available_skills())
        capabilities.update(self._list_available_mcp_tools(skills_index))
        return sorted(capabilities)

    def _list_available_mcp_tools(self, skills_index: Optional[str] = None) -> List[str]:
        """
        列出可用 MCP 能力：
        1) 环境变量 CODEAGENT_MCP_TOOLS_JSON / CODEAGENT_MCP_TOOLS
        2) skills/codeagent/mcp 目录结构
        3) SKILLS_INDEX.md 中显式出现的 mcp/*
        """
        result: Set[str] = set()

        env_json = os.getenv("CODEAGENT_MCP_TOOLS_JSON", "").strip()
        if env_json:
            try:
                values = json.loads(env_json)
                if isinstance(values, list):
                    for item in values:
                        normalized = self._normalize_mcp_id(str(item))
                        if normalized:
                            result.add(normalized)
            except Exception as e:
                logger.warning(f"Parse CODEAGENT_MCP_TOOLS_JSON failed: {e}")

        env_csv = os.getenv("CODEAGENT_MCP_TOOLS", "").strip()
        if env_csv:
            for part in env_csv.split(","):
                normalized = self._normalize_mcp_id(part)
                if normalized:
                    result.add(normalized)

        mcp_dir = self.skills_path / "mcp"
        if mcp_dir.exists() and mcp_dir.is_dir():
            for child in mcp_dir.iterdir():
                if child.is_dir():
                    result.add(f"mcp/{child.name}")
                elif child.is_file() and child.suffix.lower() == ".md":
                    result.add(f"mcp/{child.stem}")

        if skills_index:
            result.update(self._extract_mcp_ids_from_text(skills_index))

        return sorted(result)

    def _extract_mcp_ids_from_text(self, text: str) -> Set[str]:
        return {item.strip() for item in self.MCP_TOOL_PATTERN.findall(text or "") if item.strip()}

    def _parse_skill_list(self, response: str) -> List[str]:
        """解析 LLM 返回的技能列表 JSON"""
        text = response.strip()
        if text.startswith("```json"):
            text = text[7:]
        elif text.startswith("```"):
            text = text[3:]
        if text.endswith("```"):
            text = text[:-3]
        text = text.strip()
        try:
            data = json.loads(text)
            if isinstance(data, list):
                return [str(x) for x in data]
        except Exception:
            logger.warning(f"Failed to parse skill list JSON: {response[:200]}")
        return []

    def _read_skill(self, filename: str) -> Optional[str]:
        """读取 Skill 文档内容（支持 SKILL.md 目录结构）"""
        search_roots = [self.skills_path, self.docs_path]

        for root in search_roots:
            for candidate in self._resolve_skill_candidates(root, filename):
                if candidate.exists() and candidate.is_file():
                    with open(candidate, "r", encoding="utf-8") as f:
                        return self._strip_front_matter(f.read())
        return None

    def _read_capability(self, capability_name: str) -> Optional[str]:
        """
        读取 capability 内容：
        - code/*：读取本地 skill 文档
        - mcp/*：读取 mcp 描述（目录或兜底描述）
        """
        normalized = self._normalize_skill_name(capability_name)
        if not normalized:
            return None
        if normalized.startswith("mcp/"):
            return self._read_mcp_capability(normalized)
        return self._read_skill(normalized)

    def _read_mcp_capability(self, capability_name: str) -> str:
        mcp_name = capability_name.split("/", 1)[1] if "/" in capability_name else capability_name
        mcp_dir = self.skills_path / "mcp"
        candidates = [
            mcp_dir / mcp_name / "SKILL.md",
            mcp_dir / f"{mcp_name}.md",
            mcp_dir / mcp_name / "README.md",
        ]
        for candidate in candidates:
            if candidate.exists() and candidate.is_file():
                with open(candidate, "r", encoding="utf-8") as f:
                    return self._strip_front_matter(f.read())
        return (
            f"# {capability_name}\n"
            "该能力由 MCP 运行时提供。\n"
            "当前阶段仅作为能力标识加入上下文，真正调用由编排层执行。"
        )

    def _resolve_skill_candidates(self, root: Path, name: str) -> list[Path]:
        """解析技能路径候选（支持旧文件名与新目录结构）"""
        candidates: list[Path] = []
        clean = name.strip().replace("\\", "/")

        # 直接路径（文件或子路径）
        candidates.append(root / clean)
        if clean and not clean.endswith(".md"):
            candidates.append(root / clean / "SKILL.md")

        # 解析为 skill id
        if clean.endswith(".md"):
            stem = Path(clean).stem  # e.g. axios-skill / SKILL
            skill_id = stem[:-6] if stem.endswith("-skill") else stem
        else:
            skill_id = clean[:-6] if clean.endswith("-skill") else clean

        if skill_id and skill_id.lower() != "skill":
            candidates.append(root / skill_id / "SKILL.md")
            candidates.append(root / f"{skill_id}-skill" / "SKILL.md")

            # 支持按 Agent 分组目录：skills/<agent>/<skill>/SKILL.md
            if root.exists():
                for agent_dir in root.iterdir():
                    if agent_dir.is_dir():
                        candidates.append(agent_dir / skill_id / "SKILL.md")
                        candidates.append(agent_dir / f"{skill_id}-skill" / "SKILL.md")

        return candidates

    def _strip_front_matter(self, content: str) -> str:
        """去除 YAML front matter，仅返回正文"""
        if not content.startswith("---"):
            return content

        lines = content.splitlines()
        if not lines or lines[0].strip() != "---":
            return content

        end_index = None
        for i in range(1, len(lines)):
            if lines[i].strip() == "---":
                end_index = i
                break

        if end_index is None:
            return content

        body = "\n".join(lines[end_index + 1 :])
        return body.lstrip("\n")

    def _normalize_skill_name(self, name: str) -> Optional[str]:
        """规范化技能文件名，防止路径穿越"""
        if not name:
            return None
        clean = name.strip().replace("\\", "/")
        if clean.startswith("/"):
            return None
        parts = [p for p in clean.split("/") if p]
        if any(p == ".." for p in parts):
            return None
        if not clean:
            return None
        return clean

    def _normalize_mcp_id(self, value: str) -> Optional[str]:
        normalized = self._normalize_skill_name(value)
        if not normalized:
            return None
        if not normalized.startswith("mcp/"):
            normalized = f"mcp/{normalized}"
        return normalized

    def _normalize_capability_id(
        self,
        value: str,
        available_set: Set[str],
        alias_map: Dict[str, str],
    ) -> Optional[str]:
        normalized = self._normalize_skill_name(value)
        if not normalized:
            return None
        if normalized in available_set:
            return normalized
        if normalized in alias_map:
            return alias_map[normalized]
        maybe_mcp = self._normalize_mcp_id(normalized)
        if maybe_mcp and maybe_mcp in available_set:
            return maybe_mcp
        return None

    def _stringify_message_content(self, message: Any) -> str:
        content = getattr(message, "content", "")
        if isinstance(content, str):
            return content
        if isinstance(content, list):
            parts: List[str] = []
            for item in content:
                if isinstance(item, str):
                    parts.append(item)
                elif isinstance(item, dict):
                    text = item.get("text")
                    if text:
                        parts.append(str(text))
            return "\n".join(parts)
        return str(content)

    def _extract_usage_from_message(self, payload: Any) -> Optional[Dict[str, Any]]:
        raw = None
        if hasattr(payload, "usage_metadata"):
            raw = getattr(payload, "usage_metadata", None)
        if not raw and hasattr(payload, "response_metadata"):
            response_metadata = getattr(payload, "response_metadata", None)
            if isinstance(response_metadata, dict):
                raw = response_metadata.get("token_usage") or response_metadata.get("usage")
        if not raw or not isinstance(raw, dict):
            return None
        prompt_tokens = self._to_int(raw.get("prompt_tokens", raw.get("input_tokens", 0)))
        completion_tokens = self._to_int(raw.get("completion_tokens", raw.get("output_tokens", 0)))
        total_tokens = self._to_int(raw.get("total_tokens", 0))
        if total_tokens <= 0:
            total_tokens = prompt_tokens + completion_tokens
        return {
            "prompt_tokens": prompt_tokens,
            "completion_tokens": completion_tokens,
            "total_tokens": total_tokens,
            "estimated": bool(raw.get("estimated", False)),
        }

    def _to_int(self, value: Any) -> int:
        if value is None:
            return 0
        try:
            return int(value)
        except (TypeError, ValueError):
            return 0

    def _build_system_prompt(self, skills_context: Dict[str, str]) -> str:
        """构建给 LLM 的系统提示词"""
        return build_codegen_system_prompt(skills_context)


# 单例
_generator: Optional[ProjectGenerator] = None


def get_generator() -> ProjectGenerator:
    """获取项目生成器单例"""
    global _generator
    if _generator is None:
        _generator = ProjectGenerator()
    return _generator
