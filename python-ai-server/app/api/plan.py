"""
模块职责：规划接口，接收需求后调用 Planner 生成执行计划。
Java 对照：可类比“先规划再执行”的编排入口 Controller。
"""

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field
from typing import Optional, Dict, Any, List

from app.agent.planner import get_planner
from app.models.plan import ProjectPlan
from app.models.llm import LlmConfig
from loguru import logger

router = APIRouter(prefix="/api/v1/plan", tags=["plan"])


# ==================== 请求模型 ====================

class CreatePlanRequest(BaseModel):
    """
    CreatePlanRequest 请求 DTO：定义接口入参结构与校验约束。
    Java 对照：可类比 Controller 入参对象（Request DTO）。
    """
    requirement: str = Field(
        ...,
        description="用户需求描述",
        min_length=1,
        examples=["做一个带折线图的数据管理后台，包含登录和仪表盘"]
    )
    project_name: str = Field(
        default="ai-generated-app",
        description="项目名称"
    )
    app_id: Optional[int] = Field(
        default=None,
        description="应用 ID（可选）"
    )
    save_path: Optional[str] = Field(
        default=None,
        description="规划文件保存路径（可选），如不保存则仅返回规划内容"
    )
    # LLM 配置（由 Java 侧传入）
    llm_config: LlmConfig = Field(
        ...,
        description="LLM 配置"
    )
    task_id: Optional[str] = Field(default=None, description="任务 ID（可选，用于取消）")
    allowed_capabilities: Optional[List[str]] = Field(default=None, description="编排侧下发的 capability 白名单（可选）")
    capability_catalog_version: Optional[str] = Field(default=None, description="capability 枚举版本（可选）")

    model_config = {
        "json_schema_extra": {
            "examples": [
                {
                    "requirement": "做一个带折线图的数据管理后台，包含登录页面和仪表盘",
                    "project_name": "data-dashboard",
                    "app_id": 1,
                    "llm_config": {
                        "provider": "openai",
                        "model": "gpt-4o",
                        "api_key": "sk-...",
                        "temperature": 0.3,
                        "max_tokens": 6000
                    }
                }
            ]
        }
    }


# ==================== API 端点 ====================

@router.post(
    "/create",
    response_model=ProjectPlan,
    summary="创建项目规划（LLM 驱动）",
    description="""
使用 LLM 分析用户需求，生成项目执行计划。

**功能：**
1. LLM 分析项目类型（admin/dashboard/landing/h5等）
2. LLM 识别功能特征（登录/表格/图表/拖拽等）
3. LLM 规划文件列表（含依赖关系）
4. LLM 生成执行顺序

**LLM 配置：**
由 Java 侧传入 provider、model、api_key、base_url 等参数

**返回：** 完整的项目规划（ProjectPlan），包含：
- project: 项目信息
- files: 需要生成的文件列表
- execution_order: 执行步骤顺序

**使用流程：**
1. Java 侧调用此接口获取规划
2. 展示给用户确认
3. 调用执行接口按规划生成项目
"""
)
async def create_plan(request: CreatePlanRequest) -> ProjectPlan:
    """
    创建项目规划（LLM 驱动）

    此接口使用 LLM 分析需求并生成规划，不执行任何文件操作。
    """
    planner = get_planner()

    try:
        plan = await planner.plan(
            requirement=request.requirement,
            llm_config=request.llm_config,
            project_name=request.project_name,
            app_id=request.app_id,
            allowed_capabilities=request.allowed_capabilities,
            capability_catalog_version=request.capability_catalog_version,
            task_id=request.task_id,
        )

        # 如果指定了保存路径，保存规划文件
        if request.save_path:
            import json
            from pathlib import Path

            save_file = Path(request.save_path)
            save_file.parent.mkdir(parents=True, exist_ok=True)

            with open(save_file, "w", encoding="utf-8") as f:
                json.dump(plan.model_dump(), f, ensure_ascii=False, indent=2)

        return plan

    except Exception as e:
        logger.exception("PLAN_CREATE failed")
        raise HTTPException(
            status_code=500,
            detail={
                "error": "PLAN_CREATION_FAILED",
                "message": str(e)
            }
        )


@router.post(
    "/analyze",
    summary="分析需求（快速预览）",
    description="""
使用 LLM 快速分析用户需求，返回项目概览。

**适用于：** 需求确认、预览阶段，比完整规划更快速
"""
)
async def analyze_requirement(request: CreatePlanRequest) -> Dict[str, Any]:
    """
    路由处理函数：接收请求参数并调用下游能力。
    输入：Pydantic 模型或 query 参数；输出：JSON 或流式响应。
    说明：包含异步/流式处理逻辑，需关注事件边界与错误兜底。
    """
    planner = get_planner()

    try:
        # 获取完整规划
        plan = await planner.plan(
            requirement=request.requirement,
            llm_config=request.llm_config,
            project_name=request.project_name,
            app_id=request.app_id,
            allowed_capabilities=request.allowed_capabilities,
            capability_catalog_version=request.capability_catalog_version,
            task_id=request.task_id,
        )

        # 返回简化的分析结果
        return {
            "plan_id": plan.plan_id,
            "project_type": plan.project.type,
            "project": {
                "name": plan.project.name,
                "type": plan.project.type,
                "description": plan.project.description,
                "features": plan.project.features,
                "tech_stack": plan.project.tech_stack,
            },
            "estimation": {
                "file_count": plan.estimated_files,
                "complexity": plan.estimated_complexity,
            },
            "files_preview": [
                {
                    "path": f.path,
                    "type": f.type,
                    "description": f.description
                }
                for f in plan.files[:10]  # 只返回前 10 个文件预览
            ],
            "total_files": len(plan.files),
        }

    except Exception as e:
        logger.exception("PLAN_ANALYZE failed")
        raise HTTPException(
            status_code=500,
            detail={
                "error": "ANALYSIS_FAILED",
                "message": str(e)
            }
        )
