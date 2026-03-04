"""
文件上下文服务。

职责：
- 收集可读文件候选（dependencies/file_list/src 扫描）
- 让 LLM 从候选中选择要读取的文件
- 产出目录快照用于可观测
"""

import asyncio
import json
from dataclasses import dataclass, field
from pathlib import PurePosixPath
from typing import Any, Dict, List, Optional, Set

from loguru import logger

from app.llm.client import LlmClient
from app.models.llm import LlmConfig, LlmMessage
from app.prompts.code_agent_prompts import build_file_selector_prompts
from app.tools.file.read import FileReadTool
from app.tools.file.listing import FileListTool
from app.utils.file import ProjectFileSystem


@dataclass
class FileContextResult:
    """文件上下文节点执行结果。"""

    candidate_payload: Dict[str, Any] = field(default_factory=dict)
    read_files: List[str] = field(default_factory=list)
    snippets: List[Dict[str, Any]] = field(default_factory=list)
    warnings: List[str] = field(default_factory=list)
    selection_usage_delta: Dict[str, Any] = field(default_factory=dict)


class FileContextService:
    """代码生成前的文件上下文收集服务。"""

    async def collect_candidates(
        self,
        app_id: int,
        dependencies: Optional[List[str]],
        file_list: Optional[List[str]],
        max_files: int = 200,
    ) -> Dict[str, Any]:
        from_dependencies = self._sanitize_src_candidate_paths(app_id, dependencies or [], max_files=max_files)
        from_file_list = self._sanitize_src_candidate_paths(app_id, file_list or [], max_files=max_files)

        scanned_paths: List[str] = []
        try:
            result = await FileListTool.run(app_id, patterns=["src/**"], limit=max_files)
            files = [f.get("path") for f in result.get("files", []) if f.get("path")]
            scanned_paths = self._sanitize_src_candidate_paths(app_id, files, max_files=max_files)
        except Exception:
            scanned_paths = []

        merged = self._merge_unique_paths(
            [from_dependencies, from_file_list, scanned_paths],
            max_files=max_files,
        )
        return {
            "candidates": merged,
            "source_counts": {
                "dependencies": len(from_dependencies),
                "file_list": len(from_file_list),
                "scan": len(scanned_paths),
            },
            "directory_snapshot": self._build_directory_snapshot(merged),
        }

    async def select_files_to_read(
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
        return await asyncio.to_thread(
            self._select_files_to_read_sync,
            requirement,
            file_path,
            plan_summary,
            candidates,
            llm_config,
            max_files,
        )

    async def prepare_reference_context(
        self,
        app_id: int,
        requirement: str,
        file_path: str,
        plan_summary: Optional[str],
        llm_config: LlmConfig,
        dependencies: Optional[List[str]] = None,
        file_list: Optional[List[str]] = None,
        max_read_files: int = 5,
        max_read_chars: int = 4000,
    ) -> FileContextResult:
        """
        文件上下文节点聚合方法：
        1) 收集候选文件
        2) LLM 选择需要读取的文件
        3) 读取选中文件内容
        """
        result = FileContextResult()

        candidate_payload = await self.collect_candidates(
            app_id=app_id,
            dependencies=dependencies,
            file_list=file_list,
        )
        result.candidate_payload = candidate_payload
        candidates = candidate_payload.get("candidates", [])
        if not candidates:
            return result

        try:
            selected_files = await self.select_files_to_read(
                requirement=requirement,
                file_path=file_path,
                plan_summary=plan_summary,
                candidates=candidates,
                llm_config=llm_config,
                max_files=max_read_files,
            )
        except Exception as exc:
            result.warnings.append(f"file selection failed: {exc}")
            return result

        result.read_files = selected_files
        if not selected_files:
            return result

        for selected_file in selected_files:
            try:
                read_res = await FileReadTool.run(app_id, selected_file, max_chars=max_read_chars)
                result.snippets.append(
                    {
                        "path": selected_file,
                        "content": read_res.get("content", ""),
                        "total_lines": read_res.get("total_lines"),
                    }
                )
            except Exception as exc:
                result.warnings.append(f"read file failed: {selected_file}, {exc}")

        return result

    def _select_files_to_read_sync(
        self,
        requirement: str,
        file_path: str,
        plan_summary: Optional[str],
        candidates: List[str],
        llm_config: LlmConfig,
        max_files: int,
    ) -> List[str]:
        candidate_text = "\n".join(f"- {p}" for p in candidates[:200])
        system_prompt, user_prompt = build_file_selector_prompts(
            requirement=requirement,
            file_path=file_path,
            plan_summary=plan_summary,
            candidate_text=candidate_text,
            max_files=max_files,
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
        for item in selected:
            if item in candidate_set:
                filtered.append(item)
        return filtered[:max_files]

    def _merge_unique_paths(self, groups: List[List[str]], max_files: int) -> List[str]:
        result: List[str] = []
        seen: Set[str] = set()
        for group in groups:
            for item in group:
                if item in seen:
                    continue
                seen.add(item)
                result.append(item)
                if len(result) >= max_files:
                    return result
        return result

    def _build_directory_snapshot(self, paths: List[str]) -> Dict[str, Any]:
        directories = sorted({str(PurePosixPath(path).parent) for path in paths if "/" in path and path != "src"})
        top_level: Dict[str, int] = {}
        for path in paths:
            parts = path.split("/")
            key = "/".join(parts[:2]) if len(parts) >= 2 else path
            top_level[key] = top_level.get(key, 0) + 1
        top_dirs = sorted(top_level.items(), key=lambda item: item[1], reverse=True)[:20]
        return {
            "directories": directories[:80],
            "top_directories": [{"path": key, "file_count": value} for key, value in top_dirs],
        }

    def _sanitize_src_candidate_paths(
        self,
        app_id: int,
        paths: List[str],
        max_files: int,
    ) -> List[str]:
        project_root = ProjectFileSystem.get_project_path(app_id)
        result: List[str] = []
        seen = set()

        for raw in paths:
            try:
                resolved = ProjectFileSystem.resolve_path(
                    app_id,
                    str(raw),
                    allowed_prefixes=["src"],
                )
                rel_path = resolved.relative_to(project_root).as_posix()
            except Exception:
                continue

            if rel_path in seen:
                continue
            seen.add(rel_path)
            result.append(rel_path)
            if len(result) >= max_files:
                break
        return result

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
                return [str(item) for item in data]
        except Exception:
            logger.warning(f"Failed to parse JSON list: {text[:200]}")
        return []
