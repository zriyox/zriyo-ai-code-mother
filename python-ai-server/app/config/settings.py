"""
配置管理
从环境变量读取配置
"""

import os
from pathlib import Path
from typing import Optional
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    """应用配置"""

    # Java 侧公钥
    JAVA_PUBLIC_KEY: str = ""

    # 项目共享目录
    PROJECT_BASE: str = "/app/projects"

    # 前端脚手架路径
    FRONTEND_SCAFFOLD_PATH: str = ""

    # 服务端口
    PORT: int = 8000

    # 日志级别
    LOG_LEVEL: str = "INFO"

    # Python 服务地址
    PYTHON_BASE_URL: str = "http://localhost:8000"

    class Config:
        env_file = ".env"
        case_sensitive = True


# 创建单例
settings = Settings()


def get_project_path(app_id: int) -> Path:
    """获取项目根目录"""
    base = Path(settings.PROJECT_BASE)
    if not base.is_absolute():
        repo_root = Path(__file__).resolve().parents[3]
        base = (repo_root / base).resolve()
    return base / f"{app_id}"


def resolve_project_path(app_id: int, file_path: str) -> Path:
    """解析项目文件路径，防止路径穿越"""
    project_path = get_project_path(app_id)
    resolved = (project_path / file_path).resolve()

    # 安全检查
    try:
        resolved.relative_to(project_path)
    except ValueError:
        raise PermissionError(f"Path traversal detected: {file_path}")

    return resolved
