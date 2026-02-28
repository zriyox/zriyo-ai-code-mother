"""
Python AI Server - FastAPI 入口
Agent 执行层（无状态）
"""

from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.responses import JSONResponse
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import Optional
import os

from app.api.health import router as health_router
from app.api.internal import router as internal_router
from app.api.agent import router as agent_router
from app.api.project import router as project_router
from app.api.plan import router as plan_router
from app.config.settings import settings
from app.middleware.error_handlers import setup_error_handlers


# API 元数据
API_DESCRIPTION = """
## Python AI Server API

Agent 执行层服务，为 Java 侧提供 AI 编码能力。

### 功能
- 代码生成与分析
- 文档解析与摘要
- 图表生成与数据分析
- Token 计算服务

### 调用说明
- 所有 API 返回统一的 JSON 格式
- 错误响应包含 `error.code` 和 `error.message` 字段
- SSE 流式响应使用 `text/event-stream` 格式

### 认证
内部 API (`/api/internal/*`) 供 Java 服务调用，通过网络隔离保护。
外部 API (`/api/v1/*`) 需要通过 Java 侧的认证层。
"""

TAGS_METADATA = [
    {
        "name": "agent",
        "description": "Agent 执行相关接口，支持代码生成、文档分析等任务",
    },
    {
        "name": "internal",
        "description": "内部接口，供 Java 服务调用",
    },
    {
        "name": "health",
        "description": "健康检查接口，用于 K8s 探针",
    },
    {
        "name": "project",
        "description": "前端项目生成接口，支持脚手架创建和代码生成",
    },
    {
        "name": "plan",
        "description": "项目规划接口，分析需求生成执行计划",
    },
]


@asynccontextmanager
async def lifespan(app: FastAPI):
    """应用生命周期管理"""
    from loguru import logger

    # 启动时执行
    logger.info(f"Python AI Server starting on port {settings.PORT}")
    logger.info(f"Project base path: {settings.PROJECT_BASE}")
    logger.info(f"Debug mode: {os.getenv('DEBUG', 'false')}")

    yield

    # 关闭时执行
    logger.info("Python AI Server shutting down")


# 创建 FastAPI 应用
app = FastAPI(
    title="Python AI Server",
    description=API_DESCRIPTION,
    version="0.1.0",
    docs_url="/docs",
    redoc_url="/redoc",
    openapi_tags=TAGS_METADATA,
    lifespan=lifespan,
    # Java 友好的 JSON 配置
    default_response_class=JSONResponse,
)


# 配置 CORS（如果需要）
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 生产环境应限制具体来源
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 注册异常处理器
setup_error_handlers(app)


# 注册路由
app.include_router(health_router, tags=["health"])
app.include_router(internal_router)  # 内部路由已包含 prefix
app.include_router(agent_router)
app.include_router(project_router)
app.include_router(plan_router)


@app.get("/")
async def root():
    """根路径"""
    return {
        "service": "Python AI Server",
        "version": "0.1.0",
        "status": "running"
    }


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        "app.main:app",
        host="0.0.0.0",
        port=settings.PORT,
        reload=True,
        log_level=settings.LOG_LEVEL.lower()
    )
