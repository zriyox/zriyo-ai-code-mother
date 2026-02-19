"""
Python AI Server - FastAPI 入口
Agent 执行层（无状态）
"""

from fastapi import FastAPI
from fastapi.responses import JSONResponse
from pydantic import BaseModel
from typing import Optional
import os

from app.api.health import router as health_router
from app.api.internal import router as internal_router
from app.api.agent import router as agent_router
from app.config.settings import settings


# 创建 FastAPI 应用
app = FastAPI(
    title="Python AI Server",
    description="Agent 执行层 - 基于 LangChain",
    version="0.1.0",
    docs_url="/docs",
    redoc_url="/redoc"
)


# 注册路由
app.include_router(health_router, tags=["health"])
app.include_router(internal_router)  # 内部路由已包含 prefix
app.include_router(agent_router, prefix="/api/v1")


@app.get("/")
async def root():
    """根路径"""
    return {
        "service": "Python AI Server",
        "version": "0.1.0",
        "status": "running"
    }


@app.get("/health/live")
async def liveness():
    """K8s 存活探针"""
    return {"status": "alive"}


@app.get("/health/ready")
async def readiness():
    """K8s 就绪探针"""
    return {
        "status": "ready",
        "checks": {
            "database": "ok",
            "filesystem": "ok"
        }
    }


# 启动事件
@app.on_event("startup")
async def startup_event():
    """服务启动时执行"""
    import time
    from loguru import logger

    logger.info(f"Python AI Server starting on port {settings.PORT}")
    logger.info(f"Project base path: {settings.PROJECT_BASE}")


# 关闭事件
@app.on_event("shutdown")
async def shutdown_event():
    """服务关闭时执行"""
    from loguru import logger
    logger.info("Python AI Server shutting down")


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        "app.main:app",
        host="0.0.0.0",
        port=settings.PORT,
        reload=True,
        log_level=settings.LOG_LEVEL.lower()
    )
