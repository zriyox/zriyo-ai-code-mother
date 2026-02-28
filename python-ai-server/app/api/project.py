"""
前端项目生成 API
"""

from fastapi import APIRouter, HTTPException, BackgroundTasks, Response
from fastapi.responses import StreamingResponse, JSONResponse
from pydantic import BaseModel, Field
from typing import Optional, List, Dict, Any

from app.tools.project import get_generator
from app.agent.code_agent import CodeAgent
from app.models.llm import LlmConfig
from app.models.event import SseEvent
from app.models.enums import SseEventType
from app.utils.sse import sse_stream
import time
import uuid

router = APIRouter(prefix="/api/v1/project", tags=["project"])


# ==================== 请求模型 ====================

class GenerateProjectRequest(BaseModel):
    """生成项目请求"""
    app_id: int = Field(..., description="应用 ID", ge=1)
    requirement: str = Field(..., description="用户需求描述", min_length=1)
    project_name: str = Field(default="ai-generated-app", description="项目名称")
    auto_generate: bool = Field(default=False, description="是否自动生成代码（需要 LLM）")

    model_config = {
        "json_schema_extra": {
            "examples": [
                {
                    "app_id": 1,
                    "requirement": "做一个带折线图的数据管理后台，包含登录页面和仪表盘",
                    "project_name": "data-dashboard",
                    "auto_generate": False
                }
            ]
        }
    }


class GenerateCodeRequest(BaseModel):
    """生成代码请求"""
    app_id: int = Field(..., description="应用 ID")
    file_path: str = Field(..., description="要生成的文件路径，如 src/views/Dashboard.vue")
    requirement: str = Field(..., description="代码需求描述")
    llm_config: LlmConfig = Field(..., description="LLM 配置")


class GenerateAndWriteRequest(BaseModel):
    """生成并写入代码请求"""
    app_id: int = Field(..., description="应用 ID")
    task_id: Optional[str] = Field(default=None, description="任务 ID（可选，用于取消）")
    file_path: str = Field(..., description="要生成的文件路径，如 src/views/Dashboard.vue")
    requirement: str = Field(..., description="代码需求描述")
    llm_config: LlmConfig = Field(..., description="LLM 配置")
    plan_summary: Optional[str] = Field(default=None, description="规划摘要（可选）")
    dependencies: Optional[List[str]] = Field(default=None, description="依赖文件路径列表（可选）")
    file_list: Optional[List[str]] = Field(default=None, description="可读文件列表（可选）")


class WriteCodeRequest(BaseModel):
    """写入代码请求"""
    app_id: int = Field(..., description="应用 ID")
    file_path: str = Field(..., description="文件路径")
    content: str = Field(..., description="文件内容")


# ==================== 响应模型 ====================

class GenerateProjectResponse(BaseModel):
    """生成项目响应"""
    success: bool
    project_path: Optional[str] = None
    generation_tasks: Optional[Dict[str, Any]] = None
    skills_used: Optional[List[str]] = None
    error: Optional[str] = None


class FileWriteResponse(BaseModel):
    """文件写入响应"""
    success: bool
    file_path: str
    error: Optional[str] = None


class GenerateAndWriteResponse(BaseModel):
    """生成并写入代码响应"""
    file_path: str
    code: str
    skills_used: List[str]
    read_files: List[str]


# ==================== API 端点 ====================

@router.post(
    "/generate",
    response_model=GenerateProjectResponse,
    summary="生成前端项目",
    description="""
生成前端项目脚手架，包括：
1. 复制模板文件
2. 创建 node_modules 软链接
3. 加载相关 Skill 文档
4. 返回代码生成计划

**注意：** 此接口仅创建项目结构，不生成具体代码。
使用 `/generate-code` 接口生成具体文件。
"""
)
async def generate_project(request: GenerateProjectRequest) -> GenerateProjectResponse:
    """生成前端项目"""
    generator = get_generator()

    result = generator.generate(
        app_id=request.app_id,
        requirement=request.requirement,
        project_name=request.project_name,
    )

    if result["success"]:
        return GenerateProjectResponse(**result)
    else:
        raise HTTPException(status_code=500, detail=result)


@router.post(
    "/code/generate",
    summary="生成代码",
    description="""
根据需求生成指定文件的代码。

**返回：** 生成的代码内容（不直接写入文件）

使用 `/code/write` 接口将代码写入文件。
"""
)
async def generate_code(request: GenerateCodeRequest) -> Dict[str, Any]:
    """生成代码"""
    # TODO: 调用 LLM 生成代码
    generator = get_generator()

    # 读取相关 Skill 文档
    skills_context = generator._load_skills_context(
        request.requirement,
        llm_config=request.llm_config,
        context=f"file_path: {request.file_path}",
    )

    # 构建系统提示词
    system_prompt = generator._build_system_prompt(skills_context)

    # 这里应该调用 LLM，暂时返回提示词供测试
    return {
        "file_path": request.file_path,
        "system_prompt": system_prompt[:1000] + "...",  # 截断用于展示
        "skills_used": list(skills_context.keys()),
        "code": None,  # TODO: 调用 LLM 生成
        "message": "LLM integration pending"
    }


@router.post(
    "/code/generate-and-write",
    responses={
        200: {"description": "生成并写入完成（非流式）", "model": GenerateAndWriteResponse},
        202: {"description": "生成中（SSE 流式）"},
    },
    summary="生成并写入代码",
    description="""
根据需求生成指定文件的代码，并直接写入文件。
"""
)
async def generate_and_write_code(
    request: GenerateAndWriteRequest,
    stream: bool = True,
) -> Response:
    """生成并写入代码（支持 SSE）"""
    agent = CodeAgent()
    trace_id = uuid.uuid4().hex

    if stream:
        async def event_stream():
            try:
                async for event in agent.generate_and_write_stream(
                    app_id=request.app_id,
                    file_path=request.file_path,
                    requirement=request.requirement,
                    llm_config=request.llm_config,
                    plan_summary=request.plan_summary,
                    dependencies=request.dependencies,
                    file_list=request.file_list,
                    task_id=request.task_id,
                    trace_id=trace_id,
                ):
                    yield event
            except Exception as e:
                error_event = SseEvent(
                    trace_id=trace_id,
                    event_type=SseEventType.ERROR,
                    timestamp=int(time.time() * 1000),
                    data={"error": str(e)},
                )
                complete_event = SseEvent(
                    trace_id=trace_id,
                    event_type=SseEventType.REQUEST_COMPLETE,
                    timestamp=int(time.time() * 1000),
                    data={"status": "failed", "file_path": request.file_path},
                )
                yield error_event
                yield complete_event

        return StreamingResponse(
            sse_stream(event_stream()),
            media_type="text/event-stream",
            headers={
                "Cache-Control": "no-cache",
                "Connection": "keep-alive",
                "X-Accel-Buffering": "no",
            },
            status_code=202,
        )

    result = await agent.generate_and_write(
        app_id=request.app_id,
        file_path=request.file_path,
        requirement=request.requirement,
        llm_config=request.llm_config,
        plan_summary=request.plan_summary,
        dependencies=request.dependencies,
        file_list=request.file_list,
        task_id=request.task_id,
    )
    return JSONResponse(content=GenerateAndWriteResponse(**result).model_dump())


@router.post(
    "/code/write",
    response_model=FileWriteResponse,
    summary="写入代码",
    description="""
将代码写入指定文件。

**注意：** 文件路径相对于项目根目录。
"""
)
async def write_code(request: WriteCodeRequest) -> FileWriteResponse:
    """写入代码到文件"""
    from app.config.settings import resolve_project_path

    try:
        file_path = resolve_project_path(request.app_id, request.file_path)
        # 确保目录存在
        file_path.parent.mkdir(parents=True, exist_ok=True)

        # 写入文件
        with open(file_path, "w", encoding="utf-8") as f:
            f.write(request.content)

        return FileWriteResponse(
            success=True,
            file_path=str(file_path)
        )
    except PermissionError as e:
        return FileWriteResponse(
            success=False,
            file_path=request.file_path,
            error=str(e)
        )
    except Exception as e:
        return FileWriteResponse(
            success=False,
            file_path=request.file_path,
            error=str(e)
        )


@router.get(
    "/{app_id}/files",
    summary="列出项目文件",
    description="列出指定项目的所有文件"
)
async def list_files(app_id: int) -> Dict[str, Any]:
    """列出项目文件"""
    from pathlib import Path
    from app.config.settings import get_project_path

    project_path = get_project_path(app_id)

    if not project_path.exists():
        raise HTTPException(status_code=404, detail=f"Project {app_id} not found")

    files = []
    for file_path in project_path.rglob("*"):
        if file_path.is_file():
            # 排除 node_modules
            if "node_modules" not in str(file_path):
                rel_path = file_path.relative_to(project_path)
                files.append(str(rel_path))

    return {
        "app_id": app_id,
        "project_path": str(project_path),
        "files": sorted(files)
    }


@router.get(
    "/{app_id}/files/{file_path:path}",
    summary="读取文件内容",
    description="读取指定文件的内容"
)
async def read_file(app_id: int, file_path: str) -> Dict[str, Any]:
    """读取文件内容"""
    from pathlib import Path
    from app.config.settings import get_project_path, resolve_project_path

    resolved_path = resolve_project_path(app_id, file_path)

    if not resolved_path.exists() or not resolved_path.is_file():
        raise HTTPException(status_code=404, detail=f"File not found: {file_path}")

    with open(resolved_path, "r", encoding="utf-8") as f:
        content = f.read()

    return {
        "file_path": file_path,
        "content": content
    }
