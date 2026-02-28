"""
File listing tool (supports glob/pathspec).
"""

from typing import Optional, List, Dict, Any, Sequence
from pathlib import Path

from pathspec import PathSpec

from app.utils.file import ProjectFileSystem


class FileListTool:
    """List project files with pathspec/glob patterns."""

    @staticmethod
    async def run(
        app_id: int,
        patterns: Optional[Sequence[str]] = None,
        allowed_prefixes: Optional[Sequence[str]] = None,
        limit: int = 2000,
    ) -> Dict[str, Any]:
        project_path = ProjectFileSystem.get_project_path(app_id)
        if not project_path.exists():
            return {"files": [], "count": 0}

        use_patterns = list(patterns) if patterns else ["**/*"]
        spec = PathSpec.from_lines("gitwildmatch", use_patterns)

        if allowed_prefixes is None:
            allowed_prefixes = ["src"]

        files: List[Dict[str, Any]] = []
        for path in project_path.rglob("*"):
            if not path.is_file():
                continue
            rel_path = path.relative_to(project_path)
            rel_posix = rel_path.as_posix()
            if allowed_prefixes:
                allowed = any(
                    rel_posix == p.rstrip("/") or rel_posix.startswith(p.rstrip("/") + "/")
                    for p in allowed_prefixes
                )
                if not allowed:
                    continue
            if spec.match_file(rel_posix):
                files.append(
                    {
                        "path": rel_posix,
                        "size": path.stat().st_size,
                    }
                )
            if len(files) >= limit:
                break

        return {"files": files, "count": len(files)}
