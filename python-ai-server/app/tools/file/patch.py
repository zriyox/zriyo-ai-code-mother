"""
Patch tool (apply unified diff).
"""

from typing import Dict, Any, List, Optional
from pathlib import Path

from unidiff import PatchSet

from app.utils.file import ProjectFileSystem


class FilePatchTool:
    """Apply unified diff patches to project files."""

    @staticmethod
    async def run(app_id: int, patch_text: str, allowed_prefixes: Optional[list[str]] = None) -> Dict[str, Any]:
        if not patch_text:
            raise ValueError("patch_text is required")
        if allowed_prefixes is None:
            allowed_prefixes = ["src"]

        patch_set = PatchSet(patch_text)
        if not patch_set:
            raise ValueError("No valid patch found")

        changes: List[Dict[str, Any]] = []

        for patched_file in patch_set:
            target_path = FilePatchTool._normalize_patch_path(
                patched_file.path or patched_file.target_file
            )
            if not target_path:
                raise ValueError("Invalid patch path")

            resolved = ProjectFileSystem.resolve_path(app_id, target_path, allowed_prefixes=allowed_prefixes)

            if patched_file.is_removed_file:
                if resolved.exists():
                    resolved.unlink()
                changes.append({"path": str(resolved), "action": "deleted"})
                continue

            if resolved.exists():
                original_text = resolved.read_text(encoding="utf-8")
                original_lines = original_text.splitlines(keepends=True)
            else:
                original_lines = []

            new_lines = FilePatchTool._apply_hunks(original_lines, patched_file)
            resolved.parent.mkdir(parents=True, exist_ok=True)
            resolved.write_text("".join(new_lines), encoding="utf-8")
            changes.append({"path": str(resolved), "action": "updated"})

        return {"changed": changes, "count": len(changes)}

    @staticmethod
    def _normalize_patch_path(path_text: str) -> str:
        if not path_text:
            return ""
        clean = path_text.strip()
        if clean.startswith("a/") or clean.startswith("b/"):
            clean = clean[2:]
        if clean in ("/dev/null", "dev/null"):
            return ""
        # Prevent absolute paths
        return Path(clean).as_posix().lstrip("/")

    @staticmethod
    def _apply_hunks(original_lines: List[str], patched_file) -> List[str]:
        new_lines: List[str] = []
        src_index = 0

        for hunk in patched_file:
            hunk_start = max(0, hunk.source_start - 1)
            if hunk_start > len(original_lines):
                raise ValueError("Hunk start beyond end of file")

            new_lines.extend(original_lines[src_index:hunk_start])
            src_index = hunk_start

            for line in hunk:
                if line.is_context:
                    if src_index >= len(original_lines) or original_lines[src_index] != line.value:
                        raise ValueError("Patch context mismatch")
                    new_lines.append(original_lines[src_index])
                    src_index += 1
                elif line.is_removed:
                    if src_index >= len(original_lines) or original_lines[src_index] != line.value:
                        raise ValueError("Patch removal mismatch")
                    src_index += 1
                elif line.is_added:
                    new_lines.append(line.value)

        new_lines.extend(original_lines[src_index:])
        return new_lines
