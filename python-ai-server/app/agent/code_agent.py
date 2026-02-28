"""
Code generation agent (single-file).
"""

import json
import time
import uuid
from typing import List, Optional, Dict, Any, AsyncGenerator
from loguru import logger

from app.llm.client import LlmClient
from app.models.llm import LlmConfig, LlmMessage
from app.models.event import SseEvent
from app.models.enums import SseEventType, AgentType
from app.tools.project import get_generator
from app.tools.file.read import FileReadTool
from app.tools.file.listing import FileListTool
from app.utils.file import ProjectFileSystem
from app.agent.base_agent import CancelableAgent
from app.utils.errors import AgentCancelledError


class CodeAgent(CancelableAgent):
    """Generate and write a single file using LLM + skills."""

    def __init__(self):
        self.generator = get_generator()

    async def generate_and_write(
        self,
        app_id: int,
        file_path: str,
        requirement: str,
        llm_config: LlmConfig,
        plan_summary: Optional[str] = None,
        dependencies: Optional[List[str]] = None,
        file_list: Optional[List[str]] = None,
        task_id: Optional[str] = None,
    ) -> Dict[str, Any]:
        if not file_path.replace("\\", "/").startswith("src/"):
            raise PermissionError("Only src/ is allowed for code generation")
        self._check_cancelled(task_id)
        # 1) Select skills by LLM and load contents
        skills_context = self.generator._load_skills_context(
            requirement=requirement,
            llm_config=llm_config,
            context=self._build_context(file_path, plan_summary),
        )

        # 2) Decide which files to read (LLM-driven)
        candidates = await self._collect_candidates(app_id, dependencies, file_list)
        read_files = []
        snippets: List[Dict[str, Any]] = []
        if candidates:
            read_files = self._select_files_to_read(
                requirement=requirement,
                file_path=file_path,
                plan_summary=plan_summary,
                candidates=candidates,
                llm_config=llm_config,
            )
            for p in read_files:
                self._check_cancelled(task_id)
                try:
                    res = await FileReadTool.run(app_id, p, max_chars=4000)
                    snippets.append({"path": p, "content": res.get("content", "")})
                except Exception as e:
                    logger.warning(f"Read file failed: {p}, {e}")

        # 3) Build prompts
        system_prompt = self.generator._build_system_prompt(skills_context)
        user_prompt = self._build_user_prompt(
            file_path=file_path,
            requirement=requirement,
            plan_summary=plan_summary,
            snippets=snippets,
        )

        # 4) Generate code
        client = LlmClient(llm_config)
        response = client.call(
            [
                LlmMessage(role="system", content=system_prompt),
                LlmMessage(role="user", content=user_prompt),
            ],
            temperature=llm_config.temperature,
            max_tokens=llm_config.max_tokens,
        )
        code = self._strip_code_fence(response)

        # 5) Write file
        self._check_cancelled(task_id)
        await ProjectFileSystem.write_file(app_id, file_path, code)

        return {
            "file_path": file_path,
            "code": code,
            "skills_used": list(skills_context.keys()),
            "read_files": read_files,
        }

    async def generate_and_write_stream(
        self,
        app_id: int,
        file_path: str,
        requirement: str,
        llm_config: LlmConfig,
        plan_summary: Optional[str] = None,
        dependencies: Optional[List[str]] = None,
        file_list: Optional[List[str]] = None,
        trace_id: Optional[str] = None,
        task_id: Optional[str] = None,
    ) -> AsyncGenerator[SseEvent, None]:
        if not file_path.replace("\\", "/").startswith("src/"):
            raise PermissionError("Only src/ is allowed for code generation")

        trace_id = trace_id or uuid.uuid4().hex

        def emit(event_type: SseEventType, data: Dict[str, Any]) -> SseEvent:
            return SseEvent(
                trace_id=trace_id,
                event_type=event_type,
                timestamp=int(time.time() * 1000),
                data=data,
            )

        if task_id:
            yield emit(SseEventType.REQUEST_START, {"app_id": app_id, "file_path": file_path, "task_id": task_id})
        else:
            yield emit(SseEventType.REQUEST_START, {"app_id": app_id, "file_path": file_path})
        yield emit(SseEventType.AGENT_START, {"agent": AgentType.CODE.value})
        yield emit(SseEventType.STAGE, {"stage": "select_skills"})
        try:
            self._check_cancelled(task_id)
        except AgentCancelledError:
            yield emit(SseEventType.REQUEST_CANCEL, {"task_id": task_id})
            yield emit(SseEventType.REQUEST_COMPLETE, {"status": "cancelled", "file_path": file_path})
            return

        # ---- Skill selection (LLM) ----
        skills_context: Dict[str, str] = {}
        selected_skills: List[str] = []

        yield emit(SseEventType.TOOL_CALL, {"tool": "skill_read", "input": {"skill_path": "SYSTEM.md"}})
        system_content = self.generator._read_skill("SYSTEM.md")
        if system_content:
            skills_context["SYSTEM.md"] = system_content
            yield emit(SseEventType.TOOL_RESULT, {"tool": "skill_read", "output": {"skill_path": "SYSTEM.md"}})
        else:
            yield emit(SseEventType.TOOL_ERROR, {"tool": "skill_read", "error": "SYSTEM.md not found"})

        yield emit(SseEventType.TOOL_CALL, {"tool": "skill_read", "input": {"skill_path": "SKILLS_INDEX.md"}})
        skills_index = self.generator._read_skill("SKILLS_INDEX.md")
        if skills_index:
            yield emit(SseEventType.TOOL_RESULT, {"tool": "skill_read", "output": {"skill_path": "SKILLS_INDEX.md"}})
            try:
                selected_skills = self.generator._select_skills_by_llm(
                    requirement=requirement,
                    context=self._build_context(file_path, plan_summary),
                    skills_index=skills_index,
                    llm_config=llm_config,
                )
            except Exception as e:
                yield emit(SseEventType.WARNING, {"message": f"skill selection failed: {e}"})
        else:
            yield emit(SseEventType.TOOL_ERROR, {"tool": "skill_read", "error": "SKILLS_INDEX.md not found"})

        for skill_name in selected_skills:
            try:
                self._check_cancelled(task_id)
            except AgentCancelledError:
                yield emit(SseEventType.REQUEST_CANCEL, {"task_id": task_id})
                yield emit(SseEventType.REQUEST_COMPLETE, {"status": "cancelled", "file_path": file_path})
                return
            yield emit(SseEventType.TOOL_CALL, {"tool": "skill_read", "input": {"skill_path": skill_name}})
            content = self.generator._read_skill(skill_name)
            if content:
                skills_context[skill_name] = content
                yield emit(SseEventType.TOOL_RESULT, {"tool": "skill_read", "output": {"skill_path": skill_name}})
            else:
                yield emit(SseEventType.TOOL_ERROR, {"tool": "skill_read", "error": f"Skill not found: {skill_name}"})

        if selected_skills:
            yield emit(SseEventType.LOG, {"message": "skills_selected", "skills": selected_skills})

        yield emit(SseEventType.PROGRESS, {"percent": 0.25, "message": "skills selected"})

        # ---- Select files to read ----
        yield emit(SseEventType.STAGE, {"stage": "select_files"})
        candidates = await self._collect_candidates(app_id, dependencies, file_list)
        read_files: List[str] = []
        snippets: List[Dict[str, Any]] = []
        if candidates:
            read_files = self._select_files_to_read(
                requirement=requirement,
                file_path=file_path,
                plan_summary=plan_summary,
                candidates=candidates,
                llm_config=llm_config,
            )

        if read_files:
            yield emit(SseEventType.LOG, {"message": "files_selected", "files": read_files})

        for p in read_files:
            try:
                self._check_cancelled(task_id)
            except AgentCancelledError:
                yield emit(SseEventType.REQUEST_CANCEL, {"task_id": task_id})
                yield emit(SseEventType.REQUEST_COMPLETE, {"status": "cancelled", "file_path": file_path})
                return
            yield emit(SseEventType.TOOL_CALL, {"tool": "file_read", "input": {"file_path": p}})
            try:
                res = await FileReadTool.run(app_id, p, max_chars=4000)
                snippets.append({"path": p, "content": res.get("content", "")})
                yield emit(
                    SseEventType.TOOL_RESULT,
                    {"tool": "file_read", "output": {"file_path": p, "total_lines": res.get("total_lines")}},
                )
            except Exception as e:
                yield emit(SseEventType.TOOL_ERROR, {"tool": "file_read", "error": str(e), "file_path": p})

        yield emit(SseEventType.PROGRESS, {"percent": 0.45, "message": "references loaded"})

        # ---- Generate code ----
        yield emit(SseEventType.STAGE, {"stage": "generate_code"})
        system_prompt = self.generator._build_system_prompt(skills_context)
        user_prompt = self._build_user_prompt(
            file_path=file_path,
            requirement=requirement,
            plan_summary=plan_summary,
            snippets=snippets,
        )

        client = LlmClient(llm_config)
        response_parts: List[str] = []
        buffer = ""
        chunk_index = 0

        try:
            async for token in client.astream(
                [
                    LlmMessage(role="system", content=system_prompt),
                    LlmMessage(role="user", content=user_prompt),
                ],
                temperature=llm_config.temperature,
                max_tokens=llm_config.max_tokens,
            ):
                try:
                    self._check_cancelled(task_id)
                except AgentCancelledError:
                    yield emit(SseEventType.REQUEST_CANCEL, {"task_id": task_id})
                    yield emit(SseEventType.REQUEST_COMPLETE, {"status": "cancelled", "file_path": file_path})
                    return
                response_parts.append(token)
                buffer += token
                if len(buffer) >= 200:
                    yield emit(
                        SseEventType.CODE_CHUNK,
                        {"index": chunk_index, "content": buffer},
                    )
                    chunk_index += 1
                    buffer = ""
        except Exception as e:
            yield emit(SseEventType.WARNING, {"message": f"streaming failed, fallback to non-stream: {e}"})
            response = client.call(
                [
                    LlmMessage(role="system", content=system_prompt),
                    LlmMessage(role="user", content=user_prompt),
                ],
                temperature=llm_config.temperature,
                max_tokens=llm_config.max_tokens,
            )
            response_parts = [response]
            buffer = response
            if buffer:
                yield emit(
                    SseEventType.CODE_CHUNK,
                    {"index": chunk_index, "content": buffer},
                )
                chunk_index += 1
                buffer = ""

        if buffer:
            yield emit(
                SseEventType.CODE_CHUNK,
                {"index": chunk_index, "content": buffer},
            )

        response_text = "".join(response_parts)
        code = self._strip_code_fence(response_text)

        yield emit(SseEventType.PROGRESS, {"percent": 0.8, "message": "code generated"})

        # ---- Write file ----
        try:
            self._check_cancelled(task_id)
        except AgentCancelledError:
            yield emit(SseEventType.REQUEST_CANCEL, {"task_id": task_id})
            yield emit(SseEventType.REQUEST_COMPLETE, {"status": "cancelled", "file_path": file_path})
            return
        yield emit(SseEventType.STAGE, {"stage": "write_file"})
        resolved = ProjectFileSystem.resolve_path(app_id, file_path)
        existed = resolved.exists()
        await ProjectFileSystem.write_file(app_id, file_path, code)

        yield emit(
            SseEventType.FILE_UPDATED if existed else SseEventType.FILE_CREATED,
            {"file_path": file_path},
        )

        yield emit(SseEventType.AGENT_COMPLETE, {"agent": AgentType.CODE.value})
        yield emit(
            SseEventType.REQUEST_COMPLETE,
            {"status": "completed", "file_path": file_path, "skills_used": list(skills_context.keys()), "read_files": read_files},
        )

    async def _collect_candidates(
        self,
        app_id: int,
        dependencies: Optional[List[str]],
        file_list: Optional[List[str]],
        max_files: int = 200,
    ) -> List[str]:
        if dependencies:
            return [p for p in dependencies if p.replace("\\", "/").startswith("src/")][:max_files]
        if file_list:
            return [p for p in file_list if p.replace("\\", "/").startswith("src/")][:max_files]

        # Fallback: list src/** (limited)
        try:
            result = await FileListTool.run(app_id, patterns=["src/**"], limit=max_files)
            files = [f.get("path") for f in result.get("files", []) if f.get("path")]
            return files[:max_files]
        except Exception:
            return []

    def _select_files_to_read(
        self,
        requirement: str,
        file_path: str,
        plan_summary: Optional[str],
        candidates: List[str],
        llm_config: LlmConfig,
        max_files: int = 5,
    ) -> List[str]:
        if not candidates:
            return []

        candidate_text = "\n".join(f"- {p}" for p in candidates[:200])
        summary_text = f"\n\n## 规划摘要\n{plan_summary}" if plan_summary else ""

        system_prompt = (
            "你是文件选择器。请从候选文件中选择最需要阅读的文件，"
            f"最多 {max_files} 个。只返回 JSON 数组。"
            "如果不需要任何文件，返回空数组 []。"
        )
        user_prompt = (
            f"## 目标文件\n{file_path}\n\n"
            f"## 用户需求\n{requirement}"
            f"{summary_text}\n\n"
            "## 候选文件\n"
            f"{candidate_text}"
        )

        client = LlmClient(llm_config)
        response = client.call(
            [
                LlmMessage(role="system", content=system_prompt),
                LlmMessage(role="user", content=user_prompt),
            ],
            temperature=0.1,
            max_tokens=200,
        )

        selected = self._parse_json_list(response)
        if not selected:
            return []

        candidate_set = set(candidates)
        filtered: List[str] = []
        for p in selected:
            if p in candidate_set:
                filtered.append(p)
        return filtered[:max_files]

    def _build_context(self, file_path: str, plan_summary: Optional[str]) -> str:
        if plan_summary:
            return f"file_path: {file_path}\nplan_summary: {plan_summary}"
        return f"file_path: {file_path}"

    def _build_user_prompt(
        self,
        file_path: str,
        requirement: str,
        plan_summary: Optional[str],
        snippets: List[Dict[str, Any]],
    ) -> str:
        summary_text = f"\n\n## 规划摘要\n{plan_summary}" if plan_summary else ""
        snippets_text = ""
        if snippets:
            parts = []
            for item in snippets:
                parts.append(f"### {item['path']}\n{item['content']}")
            snippets_text = "\n\n## 参考文件\n" + "\n\n".join(parts)

        return (
            f"## 目标文件\n{file_path}\n\n"
            f"## 用户需求\n{requirement}"
            f"{summary_text}"
            f"{snippets_text}\n\n"
            "请直接输出目标文件的完整内容，不要输出解释。"
        )

    def _strip_code_fence(self, text: str) -> str:
        content = text.strip()
        if content.startswith("```"):
            # remove first fence line
            content = content.split("\n", 1)[1] if "\n" in content else ""
        if content.endswith("```"):
            content = content.rsplit("```", 1)[0]
        return content.strip()

    def _parse_json_list(self, text: str) -> List[str]:
        raw = text.strip()
        if raw.startswith("```json"):
            raw = raw[7:]
        elif raw.startswith("```"):
            raw = raw[3:]
        if raw.endswith("```"):
            raw = raw[:-3]
        raw = raw.strip()
        try:
            data = json.loads(raw)
            if isinstance(data, list):
                return [str(x) for x in data]
        except Exception:
            logger.warning(f"Failed to parse JSON list: {text[:200]}")
        return []
