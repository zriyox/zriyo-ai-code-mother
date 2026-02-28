"""
规划 Agent (Planner) - 简化版
只做文件规划，不涉及具体代码实现
"""

import json
import textwrap
from datetime import datetime
from pathlib import Path
from typing import List, Optional
from loguru import logger

from app.models.plan import ProjectPlan, ProjectInfo, FileInfo, ExecutionStep
from app.models.llm import LlmConfig, LlmMessage
from app.llm.client import LlmClient


FIXED_TECH_STACK = {
    "framework": "Vue 3.5.13",
    "language": "TypeScript 5.8.0",
    "build": "Vite 6.3.5",
    "router": "Vue Router 4.5.0",
    "state": "Pinia 2.2.8",
    "ui": "Element Plus 2.10.4",
    "css": "Tailwind CSS 3.4.17",
}

REQUIRED_STYLE_FILES = [
    {
        "path": "src/assets/styles/tokens.css",
        "type": "css",
        "description": "设计系统变量（颜色/字体/圆角/阴影/间距）",
    },
    {
        "path": "src/assets/styles/global.css",
        "type": "css",
        "description": "全局基础样式与重置",
    },
]


class PlannerAgent:
    """规划 Agent - 使用 LLM 分析需求，生成文件结构规划"""

    def __init__(self, scaffold_path: str):
        """
        初始化规划 Agent

        Args:
            scaffold_path: 前端脚手架路径（用于获取目录结构参考）
        """
        self.scaffold_path = Path(scaffold_path)

    def plan(
        self,
        requirement: str,
        llm_config: LlmConfig,
        project_name: str = "ai-generated-app",
        app_id: Optional[int] = None,
    ) -> ProjectPlan:
        """
        使用 LLM 分析需求，生成项目文件规划

        Args:
            requirement: 用户需求描述
            llm_config: LLM 配置（由 Java 侧传入）
            project_name: 项目名称
            app_id: 应用 ID

        Returns:
            项目规划
        """
        logger.info(f"Planning project: {project_name}")
        logger.info(f"Requirement: {requirement}")
        logger.info(f"LLM: {llm_config.provider}/{llm_config.model}")

        # 构建 LLM 消息（不包含 Skill 文档）
        messages = self._build_messages(requirement, project_name)

        # 调用 LLM
        client = LlmClient(llm_config)
        response = client.call(
            messages,
            temperature=0.3,
            max_tokens=4000,
        )

        # 解析响应
        plan = self._parse_response(response, requirement, project_name)

        logger.info(f"Plan created: {plan.plan_id}")
        logger.info(f"  Files: {len(plan.files)}, Complexity: {plan.estimated_complexity}")

        return plan

    def _build_messages(
        self,
        requirement: str,
        project_name: str,
    ) -> List[LlmMessage]:
        """构建发送给 LLM 的消息"""

        tech_stack_lines = "\n".join([f"- **{k}**: {v}" for k, v in FIXED_TECH_STACK.items()])

        system_prompt = textwrap.dedent("""你是前端架构专家。你的任务是分析用户需求，规划 Vue 3 项目的文件结构。

## 技术栈约束

__TECH_STACK__

## 可用组件库

项目配置了以下功能组件，根据需求选择使用：
- **图表**: ECharts 5.6.0 (折线图、柱状图、饼图等)
- **3D**: Three.js 0.173.0 (3D 场景、模型展示)
- **拖拽**: vuedraggable 4.1.0 (列表排序、看板)
- **代码编辑**: CodeMirror 6.0.1 (代码高亮、编辑器)
- **国际化**: vue-i18n 10.0.4 (多语言)
- **图标**: @iconify/vue 4.3.0 (200k+ 图标)
- **日期**: Day.js 1.11.13
- **HTTP**: Axios 1.7.9
- **工具**: Lodash-es, NanoID, Markdown-it, Highlight.js, NProgress, Screenfull

## 标准目录结构

```
src/
├── api/               # API 接口
├── assets/           # 静态资源
│   ├── images/       # 图片
│   └── styles/       # 全局样式
├── components/       # 公共组件
│   ├── common/       # 通用组件
│   ├── business/     # 业务组件
│   ├── charts/       # 图表组件
│   └── 3d/           # 3D 组件
├── composables/      # 组合式函数
├── layouts/          # 布局组件
├── router/           # 路由配置
├── stores/           # Pinia 状态
├── types/            # TypeScript 类型
├── utils/            # 工具函数
└── views/            # 页面组件
    └── pages/        # 具体页面
```

## 项目类型

- **admin**: 后台管理系统（侧边栏 + 顶部导航）
- **dashboard**: 数据仪表盘（图表为主）
- **landing**: 落地页（营销展示）
- **h5**: 移动端 H5 页面

## 输出格式

返回 JSON 格式，不要有其他文字：

```json
{
  "human_plan": [
    {
      "step_id": "S1",
      "title": "基础入口与配置",
      "description": "创建入口、路由与全局样式等基础文件",
      "files": ["src/main.ts", "src/App.vue", "src/router/index.ts"]
    }
  ],
    "project": {
    "name": "项目名称",
    "type": "项目类型(admin/dashboard/landing/h5)",
    "description": "项目描述",
    "features": ["功能1", "功能2"],
      "tech_stack": {
        "framework": "Vue 3.5.13",
        "language": "TypeScript 5.8.0",
        "build": "Vite 6.3.5",
        "router": "Vue Router 4.5.0",
        "state": "Pinia 2.2.8",
        "ui": "Element Plus 2.10.4",
        "css": "Tailwind CSS 3.4.17"
      }
  },
  "files": [
    {
      "path": "src/main.ts",
      "type": "typescript",
      "description": "文件作用说明",
      "step_id": "S1",
      "dependencies": []
    },
    {
      "path": "src/views/pages/Dashboard.vue",
      "type": "vue-component",
      "description": "页面作用说明",
      "step_id": "S3",
      "dependencies": ["src/components/charts/ChartCard.vue"]
    }
  ],
  "execution_order": [
    {
      "order": 1,
      "action": "copy_template",
      "target": ".",
      "description": "复制脚手架模板"
    },
    {
      "order": 2,
      "action": "link_node_modules",
      "target": "node_modules",
      "description": "创建 node_modules 软链接"
    },
    {
      "order": 3,
      "action": "generate_file",
      "target": "src/main.ts",
      "description": "生成文件"
    }
  ]
}
```

## 规划要求

1. **类型**: `typescript` 或 `vue-component`
2. **依赖**: 确保被依赖的文件在执行顺序中排在前面
3. **顺序**: 设计系统/样式 → 配置/基础 → 状态/路由 → 组件 → 页面
4. **human_plan**: 必须输出，且覆盖所有文件
5. **step_id**: 每个文件必须关联一个 human_plan.step_id
6. **description**: 清晰说明文件的作用
7. **全局样式**: 必须包含 `src/assets/styles/tokens.css` 与 `src/assets/styles/global.css`，并在入口中引入""")
        system_prompt = system_prompt.replace("__TECH_STACK__", tech_stack_lines)

        user_prompt = f"""请为以下需求规划文件结构：

## 用户需求
{requirement}

请输出 JSON 格式的文件规划。"""

        return [
            LlmMessage(role="system", content=system_prompt),
            LlmMessage(role="user", content=user_prompt),
        ]

    def _parse_response(
        self,
        response: str,
        requirement: str,
        project_name: str,
    ) -> ProjectPlan:
        """解析 LLM 响应"""
        # 提取 JSON
        json_str = response.strip()

        if json_str.startswith("```json"):
            json_str = json_str[7:]
        elif json_str.startswith("```"):
            json_str = json_str[3:]

        if json_str.endswith("```"):
            json_str = json_str[:-3]

        json_str = json_str.strip()

        # 解析 JSON
        try:
            data = json.loads(json_str)
        except json.JSONDecodeError as e:
            logger.error(f"Failed to parse LLM response: {e}")
            logger.error(f"Response: {response[:500]}")
            raise ValueError(f"LLM 返回的不是有效的 JSON 格式")

        # 补充缺失字段
        if "plan_id" not in data:
            data["plan_id"] = f"plan_{datetime.now().strftime('%Y%m%d_%H%M%S')}"

        if "created_at" not in data:
            data["created_at"] = datetime.now().isoformat()

        if "estimated_files" not in data:
            data["estimated_files"] = len(data.get("files", []))

        if "estimated_complexity" not in data:
            data["estimated_complexity"] = self._calculate_complexity(data.get("files", []))

        if "project" in data and "description" not in data["project"]:
            data["project"]["description"] = requirement

        # human_plan 必须由 LLM 输出
        if "human_plan" not in data or not data["human_plan"]:
            raise ValueError("LLM 返回缺少 human_plan")

        # 确保全局样式文件存在（若缺失则补全到第一个步骤）
        self._ensure_global_style_files(data)

        # 校验 human_plan 与 files 的映射关系
        plan_steps = {p.get("step_id") for p in data.get("human_plan", [])}
        if not plan_steps:
            raise ValueError("human_plan 缺少 step_id")
        for f in data.get("files", []):
            step_id = f.get("step_id")
            if not step_id:
                raise ValueError(f"文件缺少 step_id: {f.get('path')}")
            if step_id not in plan_steps:
                raise ValueError(f"文件 step_id 无效: {step_id}")

        # 强制使用系统固定技术栈（白名单）
        if "project" in data:
            if data.get("project", {}).get("tech_stack") != FIXED_TECH_STACK:
                logger.warning("LLM 返回的 tech_stack 与系统固定值不一致，已覆盖为固定白名单")
            data["project"]["tech_stack"] = FIXED_TECH_STACK

        # 创建 ProjectPlan
        try:
            plan = ProjectPlan(**data)
        except Exception as e:
            logger.error(f"Failed to create ProjectPlan: {e}")
            logger.error(f"Data: {data}")
            raise

        return plan

    def _ensure_global_style_files(self, data: Dict) -> None:
        """确保规划包含全局样式文件与执行步骤"""
        human_plan = data.get("human_plan") or []
        if not human_plan:
            return
        first_step = human_plan[0]
        step_id = first_step.get("step_id")
        if not step_id:
            return

        files = data.get("files") or []
        existing_paths = {f.get("path") for f in files}

        step_files = first_step.get("files")
        if not isinstance(step_files, list):
            step_files = []
        first_step["files"] = step_files

        for item in REQUIRED_STYLE_FILES:
            if item["path"] not in existing_paths:
                files.append({
                    "path": item["path"],
                    "type": item["type"],
                    "description": item["description"],
                    "step_id": step_id,
                    "dependencies": [],
                    "template": None,
                    "content_hint": None,
                    "metadata": {},
                })
            if item["path"] not in step_files:
                step_files.append(item["path"])

        data["files"] = files

        # 确保执行顺序包含样式文件生成
        execution_order = data.get("execution_order") or []
        existing_targets = {s.get("target") for s in execution_order}

        insert_index = 0
        for i, step in enumerate(execution_order):
            if step.get("action") in ("copy_template", "link_node_modules"):
                insert_index = i + 1

        new_steps = []
        for item in REQUIRED_STYLE_FILES:
            if item["path"] not in existing_targets:
                new_steps.append({
                    "order": 0,
                    "action": "generate_file",
                    "target": item["path"],
                    "description": item["description"],
                    "dependencies": [],
                })

        if new_steps:
            execution_order = execution_order[:insert_index] + new_steps + execution_order[insert_index:]
            for idx, step in enumerate(execution_order, start=1):
                step["order"] = idx
            data["execution_order"] = execution_order


    def _calculate_complexity(self, files: List[Dict]) -> str:
        """计算复杂度"""
        vue_count = sum(1 for f in files if f.get("type") == "vue-component")
        ts_count = sum(1 for f in files if f.get("type") == "typescript")

        if vue_count <= 5 and ts_count <= 3:
            return "simple"
        elif vue_count <= 15 and ts_count <= 10:
            return "medium"
        else:
            return "complex"


# 单例
_planner: Optional[PlannerAgent] = None


def get_planner(scaffold_path: Optional[str] = None) -> PlannerAgent:
    """获取规划 Agent 单例"""
    global _planner
    if _planner is None:
        default_path = "/Users/zriyo/Desktop/zriyo-ai-code-mother/frontend-scaffold"
        _planner = PlannerAgent(scaffold_path or default_path)
    return _planner
