"""
规划文件模型定义
"""

from pydantic import BaseModel, Field
from typing import List, Dict, Any, Optional


class ProjectInfo(BaseModel):
    """项目信息"""
    name: str = Field(..., description="项目名称")
    type: str = Field(..., description="项目类型: admin, dashboard, landing, h5, portal")
    description: str = Field(..., description="项目描述")
    tech_stack: Dict[str, str] = Field(default_factory=dict, description="技术栈")
    features: List[str] = Field(default_factory=list, description="功能列表")


class FileInfo(BaseModel):
    """文件信息"""
    path: str = Field(..., description="文件相对路径，如 src/views/Dashboard.vue")
    type: str = Field(..., description="文件类型: vue-component, typescript, scss, etc.")
    description: str = Field(..., description="文件描述")
    step_id: str = Field(..., description="对应的人类可读计划步骤 ID（human_plan.step_id）")
    dependencies: List[str] = Field(default_factory=list, description="依赖的其他文件")
    template: Optional[str] = Field(None, description="使用的模板名称")
    content_hint: Optional[str] = Field(None, description="内容提示，用于生成")
    metadata: Dict[str, Any] = Field(default_factory=dict, description="额外元数据")


class ExecutionStep(BaseModel):
    """执行步骤"""
    order: int = Field(..., description="执行顺序")
    action: str = Field(..., description="动作类型: copy_template, link_node_modules, generate_file")
    target: str = Field(..., description="目标路径或文件")
    description: str = Field(..., description="步骤描述")
    dependencies: List[str] = Field(default_factory=list, description="依赖的前置步骤")


class HumanPlanStep(BaseModel):
    """人类可读计划步骤"""
    step_id: str = Field(..., description="步骤 ID，如 S1")
    title: str = Field(..., description="步骤标题")
    description: str = Field(..., description="步骤描述")
    files: List[str] = Field(default_factory=list, description="涉及的文件列表")


class ProjectPlan(BaseModel):
    """项目规划"""
    project: ProjectInfo
    files: List[FileInfo]
    execution_order: List[ExecutionStep]
    human_plan: List[HumanPlanStep] = Field(default_factory=list, description="人类可读计划")

    # 规划元数据
    plan_id: str = Field(..., description="规划 ID")
    created_at: str = Field(..., description="创建时间")
    estimated_files: int = Field(..., description="预估文件数量")
    estimated_complexity: str = Field(..., description="复杂度: simple, medium, complex")

    class Config:
        json_schema_extra = {
            "example": {
                "plan_id": "plan_20250219_001",
                "created_at": "2025-02-19T12:00:00Z",
                "project": {
                    "name": "data-dashboard",
                    "type": "dashboard",
                    "description": "数据可视化仪表盘",
                    "tech_stack": {
                        "framework": "Vue 3.5.13",
                        "ui": "Element Plus 2.10.4"
                    },
                    "features": ["登录", "仪表盘", "数据图表"]
                },
                "files": [
                    {
                        "path": "src/main.ts",
                        "type": "typescript",
                        "description": "应用入口",
                        "step_id": "S1",
                        "dependencies": [],
                        "template": "main"
                    }
                ],
                "execution_order": [
                    {
                        "order": 1,
                        "action": "copy_template",
                        "target": ".",
                        "description": "复制模板文件"
                    },
                    {
                        "order": 2,
                        "action": "link_node_modules",
                        "target": "node_modules",
                        "description": "创建软链接"
                    }
                ],
                "estimated_files": 15,
                "estimated_complexity": "medium"
            }
        }
