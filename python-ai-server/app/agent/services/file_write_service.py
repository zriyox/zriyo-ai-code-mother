"""
文件写入服务。

职责：
- 限定 src/** 写入
- 返回“创建 or 更新”结果，便于编排层回传事件
"""

from dataclasses import dataclass

from app.utils.file import ProjectFileSystem


@dataclass
class FileWriteResult:
    """文件写入节点执行结果。"""

    file_path: str
    existed: bool


class FileWriteService:
    """代码写入节点服务。"""

    async def write_src_file(self, app_id: int, file_path: str, content: str) -> FileWriteResult:
        resolved = ProjectFileSystem.resolve_path(app_id, file_path, allowed_prefixes=["src"])
        existed = resolved.exists()
        await ProjectFileSystem.write_file(app_id, file_path, content, allowed_prefixes=["src"])
        return FileWriteResult(file_path=file_path, existed=existed)
