"""
File read tool (supports line range).
"""

from typing import Optional, Dict, Any, List

from app.utils.file import ProjectFileSystem


class FileReadTool:
    """Read file content with optional line range."""

    @staticmethod
    async def run(
        app_id: int,
        file_path: str,
        allowed_prefixes: Optional[List[str]] = None,
        start_line: Optional[int] = None,
        end_line: Optional[int] = None,
        max_chars: Optional[int] = None,
    ) -> Dict[str, Any]:
        if allowed_prefixes is None:
            allowed_prefixes = ["src"]
        path = ProjectFileSystem.resolve_path(app_id, file_path, allowed_prefixes=allowed_prefixes)
        if not path.exists():
            raise FileNotFoundError(f"File not found: {file_path}")

        text = path.read_text(encoding="utf-8")
        lines = text.splitlines(keepends=True)
        total_lines = len(lines)

        if start_line is None and end_line is None:
            start = 1
            end = total_lines
        else:
            start = max(1, start_line or 1)
            end = min(total_lines, end_line or total_lines)
            if end < start:
                end = start

        content = "".join(lines[start - 1 : end]) if lines else ""
        if max_chars is not None and max_chars >= 0:
            content = content[:max_chars]

        return {
            "path": str(path),
            "content": content,
            "total_lines": total_lines,
            "start_line": start,
            "end_line": end,
        }
