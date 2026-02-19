"""
健康检查 API
"""

from fastapi import APIRouter
from pydantic import BaseModel
from typing import Dict
import os
from pathlib import Path

from app.config.settings import settings

router = APIRouter()


class HealthResponse(BaseModel):
    """健康检查响应"""
    status: str
    service: str
    version: str


class ReadinessResponse(BaseModel):
    """就绪检查响应"""
    status: str
    checks: Dict[str, str]


@router.get("/health", response_model=HealthResponse)
async def health():
    """健康检查端点"""
    return HealthResponse(
        status="healthy",
        service="python-ai-server",
        version="0.1.0"
    )


@router.get("/health/live")
async def liveness():
    """K8s 存活探针 - 服务是否存活"""
    return {"status": "alive"}


@router.get("/health/ready")
async def readiness():
    """K8s 就绪探针 - 服务是否就绪"""
    checks = {}

    # 检查项目目录是否可写
    project_path = Path(settings.PROJECT_BASE)
    try:
        project_path.mkdir(parents=True, exist_ok=True)
        checks["filesystem"] = "ok"
    except Exception as e:
        checks["filesystem"] = f"error: {str(e)}"

    # 检查配置
    if settings.JAVA_PUBLIC_KEY:
        checks["jwt_config"] = "ok"
    else:
        checks["jwt_config"] = "warning: no public key configured"

    all_ok = all(v == "ok" or v.startswith("warning") for v in checks.values())

    return {
        "status": "ready" if all_ok else "not_ready",
        "checks": checks
    }


@router.get("/health/deep")
async def deep_health():
    """深度健康检查 - 检查所有依赖"""
    checks = {}
    import sys

    # Python 版本
    checks["python_version"] = sys.version

    # 检查依赖包
    try:
        import fastapi
        checks["fastapi"] = f"ok: {fastapi.__version__}"
    except ImportError:
        checks["fastapi"] = "error: not installed"

    try:
        import langchain
        checks["langchain"] = f"ok: {langchain.__version__}"
    except ImportError:
        checks["langchain"] = "error: not installed"

    try:
        import tiktoken
        checks["tiktoken"] = "ok"
    except ImportError:
        checks["tiktoken"] = "error: not installed"

    # 文件系统检查
    project_path = Path(settings.PROJECT_BASE)
    try:
        project_path.mkdir(parents=True, exist_ok=True)
        test_file = project_path / ".health_check"
        test_file.write_text("test")
        test_file.unlink()
        checks["filesystem_write"] = "ok"
    except Exception as e:
        checks["filesystem_write"] = f"error: {str(e)}"

    return checks
