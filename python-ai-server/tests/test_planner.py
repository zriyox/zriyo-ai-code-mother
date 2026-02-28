"""
测试规划 Agent
"""

import sys
import json
from pathlib import Path

# 添加项目路径
sys.path.insert(0, str(Path(__file__).parent.parent))

from app.agent.planner import PlannerAgent


def test_planner():
    """测试规划 Agent"""

    # 配置
    SCAFFOLD_PATH = "/Users/zriyo/Desktop/zriyo-ai-code-mother/frontend-scaffold"

    # 创建规划器
    planner = PlannerAgent(scaffold_path=SCAFFOLD_PATH)

    # 测试用例
    test_cases = [
        {
            "name": "数据仪表盘",
            "requirement": "做一个带折线图的数据管理后台，包含登录页面和仪表盘",
            "project_name": "data-dashboard"
        },
        {
            "name": "3D 展示页面",
            "requirement": "做一个 3D 模型展示页面，支持鼠标旋转和缩放",
            "project_name": "3d-showcase"
        },
        {
            "name": "后台管理系统",
            "requirement": "做一个后台管理系统，包含用户管理、角色管理、权限管理，需要表格和表单",
            "project_name": "admin-system"
        },
        {
            "name": "落地页",
            "requirement": "做一个产品介绍落地页，包含导航栏、英雄区、特性介绍",
            "project_name": "landing-page"
        },
    ]

    for i, test_case in enumerate(test_cases, 1):
        print("=" * 70)
        print(f"测试用例 {i}: {test_case['name']}")
        print("=" * 70)
        print(f"需求: {test_case['requirement']}")
        print()

        # 生成规划
        plan = planner.plan(
            requirement=test_case['requirement'],
            project_name=test_case['project_name']
        )

        # 输出规划结果
        print(f"📋 规划 ID: {plan.plan_id}")
        print(f"📁 项目类型: {plan.project.type} ({planner.PROJECT_TYPES.get(plan.project.type, '未知')})")
        print(f"🔧 技术栈: {', '.join(plan.project.tech_stack.values())}")
        print(f"📊 复杂度: {plan.estimated_complexity}")
        print(f"📄 预估文件数: {plan.estimated_files}")
        print()

        print("🎯 识别的功能:")
        for feature in plan.project.features:
            print(f"   - {feature}")
        print()

        print("📝 生成的文件:")
        for file_info in plan.files:
            deps = f" (依赖: {', '.join(file_info.dependencies)})" if file_info.dependencies else ""
            print(f"   [{file_info.type}] {file_info.path}{deps}")
        print()

        print("⚡ 执行步骤:")
        for step in plan.execution_order:
            print(f"   {step.order}. [{step.action}] {step.target}")
            print(f"      {step.description}")
        print()

        # 保存规划文件
        output_dir = Path("/Users/zriyo/Desktop/zriyo-ai-code-mother/tmp/plans")
        output_dir.mkdir(parents=True, exist_ok=True)
        output_file = output_dir / f"{plan.project.name}_plan.json"

        with open(output_file, "w", encoding="utf-8") as f:
            json.dump(plan.model_dump(), f, ensure_ascii=False, indent=2)

        print(f"✅ 规划已保存到: {output_file}")
        print()


if __name__ == "__main__":
    test_planner()
