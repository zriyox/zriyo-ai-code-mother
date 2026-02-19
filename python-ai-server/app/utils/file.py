"""
文件系统工具
"""

from pathlib import Path
from typing import List
from app.config.settings import settings, get_project_path, resolve_project_path


class ProjectFileSystem:
    """项目文件系统"""

    @staticmethod
    def get_project_path(app_id: int) -> Path:
        """获取项目根目录"""
        return get_project_path(app_id)

    @staticmethod
    def resolve_path(app_id: int, file_path: str) -> Path:
        """解析文件路径，防止路径穿越"""
        return resolve_project_path(app_id, file_path)

    @staticmethod
    async def read_file(app_id: int, file_path: str) -> str:
        """读取文件"""
        path = ProjectFileSystem.resolve_path(app_id, file_path)
        if not path.exists():
            raise FileNotFoundError(f"File not found: {file_path}")
        return path.read_text(encoding="utf-8")

    @staticmethod
    async def write_file(app_id: int, file_path: str, content: str):
        """写入文件"""
        path = ProjectFileSystem.resolve_path(app_id, file_path)
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content, encoding="utf-8")

    @staticmethod
    async def list_files(app_id: int, pattern: str = "**/*") -> List[dict]:
        """列出文件"""
        from pathspec import PathSpec

        project_path = ProjectFileSystem.get_project_path(app_id)
        pathspec = PathSpec.from_lines("gitwildmatch", [pattern])

        files = []
        if project_path.exists():
            for path in project_path.rglob("*"):
                if path.is_file():
                    rel_path = path.relative_to(project_path)
                    if pathspec.match_file(str(rel_path)):
                        files.append({
                            "path": str(rel_path),
                            "size": path.stat().st_size
                        })

        return files

    @staticmethod
    def ensure_project_dir(app_id: int):
        """确保项目目录存在"""
        path = ProjectFileSystem.get_project_path(app_id)
        path.mkdir(parents=True, exist_ok=True)
