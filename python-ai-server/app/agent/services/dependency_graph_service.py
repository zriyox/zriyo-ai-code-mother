"""
依赖图分析服务。

职责：
- 基于本地 import 语句建立文件依赖图
- 输出边、未解析依赖与循环依赖
"""

import posixpath
import re
from pathlib import PurePosixPath
from typing import Any, Dict, List, Optional, Set, Tuple

from app.utils.file import ProjectFileSystem


class DependencyGraphService:
    """本地文件依赖关系分析服务。"""

    async def build_dependency_graph(
        self,
        app_id: int,
        target_file: str,
        selected_files: List[str],
        max_nodes: int = 200,
    ) -> Dict[str, Any]:
        nodes = self._merge_unique_paths([[target_file], selected_files], max_files=max_nodes)
        adjacency: Dict[str, List[str]] = {node: [] for node in nodes}
        unresolved: List[Dict[str, str]] = []
        edge_set: Set[Tuple[str, str]] = set()

        for src_file in nodes:
            try:
                content = await ProjectFileSystem.read_file(app_id, src_file, allowed_prefixes=["src"])
            except Exception:
                continue
            imports = self._extract_local_import_specs(content)
            for spec in imports:
                resolved = self._resolve_local_import_path(app_id, src_file, spec)
                if not resolved:
                    unresolved.append({"from": src_file, "import": spec})
                    continue
                edge = (src_file, resolved)
                if edge in edge_set:
                    continue
                edge_set.add(edge)
                adjacency.setdefault(src_file, []).append(resolved)
                adjacency.setdefault(resolved, [])

        cycles = self._detect_cycles(adjacency)
        edges = [{"from": src, "to": dst} for src, dst in sorted(edge_set)]
        return {
            "nodes": sorted(adjacency.keys()),
            "edges": edges,
            "node_count": len(adjacency),
            "edge_count": len(edges),
            "cycles": cycles,
            "cycle_count": len(cycles),
            "has_cycle": bool(cycles),
            "unresolved_imports": unresolved[:100],
        }

    def _extract_local_import_specs(self, content: str) -> List[str]:
        patterns = [
            re.compile(r"(?:import|export)\s+[^'\"\\n]*from\s*['\"]([^'\"]+)['\"]"),
            re.compile(r"import\s*['\"]([^'\"]+)['\"]"),
            re.compile(r"import\(\s*['\"]([^'\"]+)['\"]\s*\)"),
            re.compile(r"require\(\s*['\"]([^'\"]+)['\"]\s*\)"),
        ]
        specs: List[str] = []
        for pattern in patterns:
            specs.extend(pattern.findall(content or ""))

        local_specs: List[str] = []
        for spec in specs:
            text = str(spec).strip()
            if text.startswith("./") or text.startswith("../") or text.startswith("@/") or text.startswith("src/"):
                local_specs.append(text)
        dedup: List[str] = []
        for item in local_specs:
            if item not in dedup:
                dedup.append(item)
        return dedup

    def _resolve_local_import_path(self, app_id: int, source_file: str, import_spec: str) -> Optional[str]:
        source_dir = PurePosixPath(source_file).parent.as_posix()
        if import_spec.startswith("@/"):
            base = f"src/{import_spec[2:]}"
        elif import_spec.startswith("src/"):
            base = import_spec
        else:
            joined = posixpath.join(source_dir, import_spec)
            base = posixpath.normpath(joined)

        if not base.startswith("src/"):
            return None

        candidates = [base]
        if "." not in PurePosixPath(base).name:
            extensions = [".ts", ".tsx", ".js", ".jsx", ".vue", ".json", ".css", ".scss", ".less"]
            candidates.extend([base + ext for ext in extensions])
            candidates.extend([f"{base}/index{ext}" for ext in extensions])

        for candidate in candidates:
            try:
                resolved = ProjectFileSystem.resolve_path(app_id, candidate, allowed_prefixes=["src"])
                if resolved.exists():
                    return resolved.relative_to(ProjectFileSystem.get_project_path(app_id)).as_posix()
            except Exception:
                continue
        return None

    def _detect_cycles(self, adjacency: Dict[str, List[str]]) -> List[List[str]]:
        visited: Set[str] = set()
        on_stack: Set[str] = set()
        stack: List[str] = []
        cycles: Set[Tuple[str, ...]] = set()

        def dfs(node: str) -> None:
            visited.add(node)
            on_stack.add(node)
            stack.append(node)

            for neighbor in adjacency.get(node, []):
                if neighbor not in visited:
                    dfs(neighbor)
                    continue
                if neighbor in on_stack:
                    try:
                        start_index = stack.index(neighbor)
                    except ValueError:
                        continue
                    cycle = stack[start_index:] + [neighbor]
                    normalized = self._normalize_cycle(cycle)
                    cycles.add(tuple(normalized))

            stack.pop()
            on_stack.remove(node)

        for node in adjacency.keys():
            if node not in visited:
                dfs(node)

        return [list(item) for item in sorted(cycles)]

    def _normalize_cycle(self, cycle: List[str]) -> List[str]:
        if len(cycle) <= 2:
            return cycle
        body = cycle[:-1]
        rotations: List[List[str]] = []
        for i in range(len(body)):
            rotations.append(body[i:] + body[:i])
        reversed_body = list(reversed(body))
        for i in range(len(reversed_body)):
            rotations.append(reversed_body[i:] + reversed_body[:i])
        canonical = min(rotations)
        return canonical + [canonical[0]]

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
