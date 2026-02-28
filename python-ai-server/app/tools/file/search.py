"""
Search tool using ripgrep.
"""

from typing import Optional, Sequence, Dict, Any, List
import json
import shutil
import subprocess

from app.utils.file import ProjectFileSystem
from app.utils.errors import ToolExecutionError


class FileSearchTool:
    """Search text in project using ripgrep."""

    @staticmethod
    async def run(
        app_id: int,
        query: str,
        patterns: Optional[Sequence[str]] = None,
        allowed_prefixes: Optional[Sequence[str]] = None,
        max_results: int = 200,
        case_sensitive: bool = False,
        context_lines: int = 0,
    ) -> Dict[str, Any]:
        if not query:
            raise ValueError("query is required")

        rg = shutil.which("rg")
        if not rg:
            raise ToolExecutionError("ripgrep (rg) not found", tool_name="search")

        project_path = ProjectFileSystem.get_project_path(app_id)
        if not project_path.exists():
            return {"results": [], "count": 0}

        args = [
            rg,
            "--json",
            "--with-filename",
            "--line-number",
            "--column",
            "--no-messages",
        ]
        if not case_sensitive:
            args.append("-i")
        if context_lines and context_lines > 0:
            args.append(f"-C{context_lines}")
        if patterns:
            for p in patterns:
                args.extend(["--glob", p])
        if allowed_prefixes is None:
            allowed_prefixes = ["src"]
        if allowed_prefixes:
            for p in allowed_prefixes:
                p = p.rstrip("/")
                args.extend(["--glob", f"{p}/**"])
        args.append(query)

        proc = subprocess.run(
            args,
            cwd=str(project_path),
            capture_output=True,
            text=True,
        )

        # rg: 0 = matches, 1 = no matches, >1 = error
        if proc.returncode not in (0, 1):
            raise ToolExecutionError(proc.stderr.strip(), tool_name="search")

        results: List[Dict[str, Any]] = []
        for line in proc.stdout.splitlines():
            try:
                payload = json.loads(line)
            except json.JSONDecodeError:
                continue
            if payload.get("type") != "match":
                continue

            data = payload.get("data", {})
            path_text = data.get("path", {}).get("text", "")
            line_number = data.get("line_number")
            line_text = data.get("lines", {}).get("text", "")
            submatches = data.get("submatches", [])

            match_info = {
                "path": path_text,
                "line": line_number,
                "text": line_text.rstrip("\n"),
            }
            if submatches:
                match_info["matches"] = [
                    {"start": m.get("start"), "end": m.get("end")} for m in submatches
                ]

            results.append(match_info)
            if len(results) >= max_results:
                break

        return {"results": results, "count": len(results)}
