"""
单文件代码生成 Agent。

执行链路（可类比 Java Service 编排）：
1. 选择技能文档（skills）
2. 选择并读取参考文件（src/**）
3. 组装提示词并调用 LLM
4. 写回目标文件
"""

import time
import uuid
from typing import List, Optional, Dict, Any, AsyncGenerator

from app.models.llm import LlmConfig, LlmMessage
from app.models.event import SseEvent
from app.models.enums import SseEventType, AgentType
from app.tools.project import get_generator
from app.utils.file import ProjectFileSystem
from app.agent.base_agent import CancelableAgent
from app.agent.services import (
    CapabilityContextService,
    FileContextService,
    DependencyGraphService,
    CodeGenerationService,
    FileWriteService,
)
from app.utils.errors import AgentCancelledError
from app.utils.token_counter import TokenCounter
from app.utils.llm_usage_tracker import (
    begin_llm_usage_session,
    end_llm_usage_session,
    snapshot_llm_usage_session,
    diff_llm_usage,
)


class CodeAgent(CancelableAgent):
    """使用 LLM + Skills 生成并写入单文件。"""

    def __init__(self):
        self.generator = get_generator()
        self.capability_context_service = CapabilityContextService(self.generator)
        self.file_context_service = FileContextService()
        self.dependency_graph_service = DependencyGraphService()
        self.code_generation_service = CodeGenerationService(self.generator._build_system_prompt)
        self.file_write_service = FileWriteService()

    async def generate_and_write_stream(
        self,
        app_id: int,
        file_path: str,
        requirement: str,
        llm_config: LlmConfig,
        plan_summary: Optional[str] = None,
        dependencies: Optional[List[str]] = None,
        file_list: Optional[List[str]] = None,
        allowed_capabilities: Optional[List[str]] = None,
        capability_catalog_version: Optional[str] = None,
        trace_id: Optional[str] = None,
        task_id: Optional[str] = None,
    ) -> AsyncGenerator[SseEvent, None]:
        """
        流式执行：通过 SSE 分阶段推送进度、代码片段和结果。

        适合前端实时展示“正在生成中”的体验。
        """
        try:
            resolved_target = ProjectFileSystem.resolve_path(
                app_id,
                file_path,
                allowed_prefixes=["src"],
            )
            file_path = resolved_target.relative_to(ProjectFileSystem.get_project_path(app_id)).as_posix()
        except Exception as e:
            raise PermissionError(f"Only src/ is allowed for code generation: {e}") from e

        trace_id = trace_id or uuid.uuid4().hex
        session_token = begin_llm_usage_session(trace_id=trace_id, task_id=task_id)

        def emit(event_type: SseEventType, data: Dict[str, Any]) -> SseEvent:
            """统一封装 SSE 事件结构，避免每次手动填 trace/timestamp。"""
            return SseEvent(
                trace_id=trace_id,
                event_type=event_type,
                timestamp=int(time.time() * 1000),
                data=data,
            )

        try:
            if task_id:
                yield emit(SseEventType.REQUEST_START, {"app_id": app_id, "file_path": file_path, "task_id": task_id})
            else:
                yield emit(SseEventType.REQUEST_START, {"app_id": app_id, "file_path": file_path})
            yield emit(SseEventType.AGENT_START, {"agent": AgentType.CODE.value})
            yield emit(SseEventType.STAGE, {"stage": "select_skills"})
            try:
                await self._check_cancelled(task_id)
            except AgentCancelledError:
                yield emit(SseEventType.REQUEST_CANCEL, {"task_id": task_id})
                yield emit(SseEventType.REQUEST_COMPLETE, {"status": "cancelled", "file_path": file_path})
                return

            # ---- Skill selection (LLM) ----
            yield emit(
                SseEventType.TOOL_CALL,
                {"tool": "skill_read", "billable": False, "input": {"skill_path": "SYSTEM.md"}},
            )
            yield emit(
                SseEventType.TOOL_CALL,
                {"tool": "skill_read", "billable": False, "input": {"skill_path": "SKILLS_INDEX.md"}},
            )
            yield emit(
                SseEventType.TOOL_CALL,
                {
                    "tool": "llm_select_skills",
                    "billable": True,
                    "input": {"provider": llm_config.provider, "model": llm_config.model},
                },
            )
            before_skills_usage = snapshot_llm_usage_session()
            capability_result = await self.capability_context_service.build_context_for_requirement(
                requirement=requirement,
                context=self._build_context(file_path, plan_summary),
                llm_config=llm_config,
                allowed_capabilities=allowed_capabilities,
                capability_catalog_version=capability_catalog_version,
                max_capabilities=3,
            )
            after_skills_usage = snapshot_llm_usage_session()
            skills_context = capability_result.skills_context
            selected_skills = capability_result.selected_capabilities
            capability_disclosure_meta: Dict[str, Any] = capability_result.disclosure_meta
            yield emit(
                SseEventType.TOOL_RESULT,
                {
                    "tool": "llm_select_skills",
                    "billable": True,
                    "output": {"selected_skills": selected_skills},
                    "usage_delta": diff_llm_usage(before_skills_usage, after_skills_usage),
                },
            )

            if "SYSTEM.md" in skills_context:
                yield emit(
                    SseEventType.TOOL_RESULT,
                    {"tool": "skill_read", "billable": False, "output": {"skill_path": "SYSTEM.md"}},
                )
            else:
                yield emit(
                    SseEventType.TOOL_ERROR,
                    {"tool": "skill_read", "billable": False, "error": "SYSTEM.md not found"},
                )

            if "SKILLS_INDEX.md not found" in capability_result.warnings:
                yield emit(
                    SseEventType.TOOL_ERROR,
                    {"tool": "skill_read", "billable": False, "error": "SKILLS_INDEX.md not found"},
                )
            else:
                yield emit(
                    SseEventType.TOOL_RESULT,
                    {"tool": "skill_read", "billable": False, "output": {"skill_path": "SKILLS_INDEX.md"}},
                )

            if selected_skills:
                meta_map = {
                    item.get("id"): item
                    for item in capability_disclosure_meta.get("capabilities", [])
                    if isinstance(item, dict) and item.get("id")
                }
                for capability_name in selected_skills:
                    if capability_name in skills_context:
                        yield emit(
                            SseEventType.TOOL_RESULT,
                            {
                                "tool": "skill_read",
                                "billable": False,
                                "output": {
                                    "skill_path": capability_name,
                                    "selected_sections": meta_map.get(capability_name, {}).get("selected_sections", []),
                                },
                            },
                        )
                    else:
                        yield emit(
                            SseEventType.TOOL_ERROR,
                            {"tool": "skill_read", "billable": False, "error": f"Skill not found: {capability_name}"},
                        )

            for warning in capability_result.warnings:
                if warning in {"SYSTEM.md not found", "SKILLS_INDEX.md not found"} or warning.startswith("Skill not found:"):
                    continue
                yield emit(SseEventType.WARNING, {"message": warning})

            if selected_skills:
                yield emit(SseEventType.LOG, {"message": "skills_selected", "skills": selected_skills})

            yield emit(SseEventType.PROGRESS, {"percent": 0.25, "message": "skills selected"})

            # ---- Select files to read ----
            yield emit(SseEventType.STAGE, {"stage": "select_files"})
            yield emit(
                SseEventType.TOOL_CALL,
                {
                    "tool": "file_list",
                    "billable": False,
                    "input": {
                        "patterns": ["src/**"],
                        "dependencies_count": len(dependencies or []),
                        "file_list_count": len(file_list or []),
                    },
                },
            )
            yield emit(
                SseEventType.TOOL_CALL,
                {
                    "tool": "llm_select_files",
                    "billable": True,
                    "input": {"provider": llm_config.provider, "model": llm_config.model},
                },
            )
            before_file_context_usage = snapshot_llm_usage_session()
            file_context_result = await self.file_context_service.prepare_reference_context(
                app_id=app_id,
                requirement=requirement,
                file_path=file_path,
                plan_summary=plan_summary,
                llm_config=llm_config,
                dependencies=dependencies,
                file_list=file_list,
            )
            after_file_context_usage = snapshot_llm_usage_session()
            candidate_payload = file_context_result.candidate_payload
            candidates = candidate_payload.get("candidates", [])
            read_files = file_context_result.read_files
            snippets = file_context_result.snippets
            yield emit(
                SseEventType.TOOL_RESULT,
                {
                    "tool": "file_list",
                    "billable": False,
                    "output": {
                        "candidate_count": len(candidates),
                        "source_counts": candidate_payload.get("source_counts", {}),
                        "directory_snapshot": candidate_payload.get("directory_snapshot", {}),
                    },
                },
            )
            yield emit(
                SseEventType.TOOL_RESULT,
                {
                    "tool": "llm_select_files",
                    "billable": True,
                    "output": {"selected_files": read_files},
                    "usage_delta": diff_llm_usage(before_file_context_usage, after_file_context_usage),
                },
            )
            for warning in file_context_result.warnings:
                yield emit(SseEventType.WARNING, {"message": warning})

            for snippet in snippets:
                yield emit(
                    SseEventType.TOOL_RESULT,
                    {
                        "tool": "file_read",
                        "billable": False,
                        "output": {
                            "file_path": snippet.get("path"),
                            "total_lines": snippet.get("total_lines"),
                        },
                    },
                )

            if read_files:
                yield emit(SseEventType.LOG, {"message": "files_selected", "files": read_files})

            yield emit(
                SseEventType.TOOL_CALL,
                {
                    "tool": "dependency_analyze",
                    "billable": False,
                    "input": {"target_file": file_path, "selected_files": read_files},
                },
            )
            dependency_graph: Dict[str, Any] = {}
            try:
                dependency_graph = await self.dependency_graph_service.build_dependency_graph(
                    app_id=app_id,
                    target_file=file_path,
                    selected_files=read_files,
                )
                yield emit(
                    SseEventType.TOOL_RESULT,
                    {
                        "tool": "dependency_analyze",
                        "billable": False,
                        "output": {
                            "node_count": dependency_graph.get("node_count", 0),
                            "edge_count": dependency_graph.get("edge_count", 0),
                            "cycle_count": dependency_graph.get("cycle_count", 0),
                            "cycles": dependency_graph.get("cycles", []),
                        },
                    },
                )
            except Exception as e:
                yield emit(
                    SseEventType.TOOL_ERROR,
                    {"tool": "dependency_analyze", "billable": False, "error": str(e)},
                )

            yield emit(SseEventType.PROGRESS, {"percent": 0.45, "message": "references loaded"})

            # ---- Generate code ----
            yield emit(SseEventType.STAGE, {"stage": "generate_code"})
            generation_messages = self.code_generation_service.build_generation_messages(
                skills_context=skills_context,
                file_path=file_path,
                requirement=requirement,
                plan_summary=plan_summary,
                snippets=snippets,
            )
            response_parts: List[str] = []
            buffer = ""
            chunk_index = 0

            yield emit(
                SseEventType.TOOL_CALL,
                {"tool": "llm_generate_code", "billable": True, "input": {"provider": llm_config.provider, "model": llm_config.model}},
            )
            before_generate_usage = snapshot_llm_usage_session()
            try:
                async for token in self.code_generation_service.stream_tokens(
                    llm_config=llm_config,
                    messages=generation_messages,
                ):
                    try:
                        await self._check_cancelled(task_id)
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
                yield emit(SseEventType.ERROR, {"message": f"streaming generation failed: {e}"})
                yield emit(
                    SseEventType.REQUEST_COMPLETE,
                    {"status": "failed", "file_path": file_path, "reason": "stream_generation_failed"},
                )
                return

            if buffer:
                yield emit(
                    SseEventType.CODE_CHUNK,
                    {"index": chunk_index, "content": buffer},
                )

            generation_result = self.code_generation_service.finalize_generation(
                messages=generation_messages,
                response_text="".join(response_parts),
            )
            after_generate_usage = snapshot_llm_usage_session()
            generation_usage_delta = diff_llm_usage(before_generate_usage, after_generate_usage)
            generation_usage_delta = await self._ensure_usage_estimate(
                usage_summary=generation_usage_delta,
                llm_config=llm_config,
                messages=generation_messages,
                generated_text=generation_result.response_text,
            )
            yield emit(
                SseEventType.TOOL_RESULT,
                {
                    "tool": "llm_generate_code",
                    "billable": True,
                    "usage_delta": generation_usage_delta,
                },
            )

            yield emit(SseEventType.PROGRESS, {"percent": 0.8, "message": "code generated"})

            # ---- Write file ----
            try:
                await self._check_cancelled(task_id)
            except AgentCancelledError:
                yield emit(SseEventType.REQUEST_CANCEL, {"task_id": task_id})
                yield emit(SseEventType.REQUEST_COMPLETE, {"status": "cancelled", "file_path": file_path})
                return
            yield emit(SseEventType.STAGE, {"stage": "write_file"})
            write_result = await self.file_write_service.write_src_file(
                app_id=app_id,
                file_path=file_path,
                content=generation_result.code,
            )

            yield emit(
                SseEventType.FILE_UPDATED if write_result.existed else SseEventType.FILE_CREATED,
                {"file_path": file_path},
            )

            usage_final = await self._build_usage_summary(
                llm_config=llm_config,
                generation_messages=generation_messages,
                generated_text=generation_result.response_text,
            )
            yield emit(SseEventType.AGENT_COMPLETE, {"agent": AgentType.CODE.value})
            yield emit(
                SseEventType.REQUEST_COMPLETE,
                {
                    "status": "completed",
                    "file_path": file_path,
                    "skills_used": list(skills_context.keys()),
                    "capability_disclosure": capability_disclosure_meta,
                    "read_files": read_files,
                    "dependency_graph": dependency_graph,
                    "llm_call_count": usage_final.get("call_count", 0),
                    "usage_final": usage_final,
                },
            )
        finally:
            await self._clear_cancel_flag(task_id)
            end_llm_usage_session(session_token)

    async def _build_usage_summary(
        self,
        llm_config: LlmConfig,
        generation_messages: Optional[List[LlmMessage]] = None,
        generated_text: str = "",
    ) -> Dict[str, Any]:
        """
        汇总当前请求内 LLM 调用 usage；若 provider 未返回 usage，则对主生成调用做估算兜底。
        """
        usage_summary = snapshot_llm_usage_session()
        if generation_messages:
            usage_summary = await self._ensure_usage_estimate(
                usage_summary=usage_summary,
                llm_config=llm_config,
                messages=generation_messages,
                generated_text=generated_text,
            )
        usage_summary["billable_calls"] = usage_summary.get("call_count", 0)
        return usage_summary

    async def _ensure_usage_estimate(
        self,
        usage_summary: Dict[str, Any],
        llm_config: LlmConfig,
        messages: List[LlmMessage],
        generated_text: str,
    ) -> Dict[str, Any]:
        """
        当 usage 缺失时，基于 provider 分词规则估算一次调用 token 消耗。
        """
        if usage_summary.get("total_tokens", 0) > 0:
            return usage_summary

        request_messages = [{"role": m.role, "content": m.content} for m in messages]
        prompt_tokens, _ = await TokenCounter.count_messages(
            request_messages,
            llm_config.provider,
            llm_config.model,
        )
        completion_tokens, _ = await TokenCounter.count_tokens(
            generated_text or "",
            llm_config.provider,
            llm_config.model,
        )

        patched = dict(usage_summary)
        patched["prompt_tokens"] = int(prompt_tokens)
        patched["completion_tokens"] = int(completion_tokens)
        patched["total_tokens"] = int(prompt_tokens + completion_tokens)
        patched["estimated"] = True
        patched["estimated_reason"] = "provider_usage_missing"
        return patched

    def _build_context(self, file_path: str, plan_summary: Optional[str]) -> str:
        """
        Agent 阶段方法：用于组织生成流程中的一个步骤。
        输入：当前任务上下文；输出：阶段结果或中间状态。
        """
        if plan_summary:
            return f"file_path: {file_path}\nplan_summary: {plan_summary}"
        return f"file_path: {file_path}"
