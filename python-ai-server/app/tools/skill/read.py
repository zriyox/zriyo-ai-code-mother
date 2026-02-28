"""
Skill read tool (read-only).
"""

import os
from pathlib import Path
from typing import Dict, Any, Optional

from app.config.settings import settings


class SkillReadTool:
    """Read skill document content from skills/codeagent."""

    @staticmethod
    async def run(skill_path: str) -> Dict[str, Any]:
        if not skill_path:
            raise ValueError("skill_path is required")

        skills_root = SkillReadTool._get_skills_root()
        resolved = SkillReadTool._resolve_skill_path(skills_root, skill_path)
        if not resolved or not resolved.exists():
            raise FileNotFoundError(f"Skill not found: {skill_path}")

        content = resolved.read_text(encoding="utf-8")
        return {
            "path": str(resolved),
            "content": SkillReadTool._strip_front_matter(content),
        }

    @staticmethod
    def _get_skills_root() -> Path:
        repo_root = Path(__file__).resolve().parents[4]
        return repo_root / "skills" / "codeagent"

    @staticmethod
    def _resolve_skill_path(root: Path, name: str) -> Optional[Path]:
        clean = name.strip().replace("\\", "/")
        if clean.startswith("codeagent/"):
            clean = clean[len("codeagent/"):]
        # Direct path
        if clean.endswith(".md"):
            candidate = root / clean
            return candidate
        if "/" in clean:
            return root / clean / "SKILL.md"

        # Try by id under code/ and chart/
        for group in ("code", "chart"):
            candidate = root / group / clean / "SKILL.md"
            if candidate.exists():
                return candidate
        return None

    @staticmethod
    def _strip_front_matter(content: str) -> str:
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
