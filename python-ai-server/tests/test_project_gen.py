"""
测试前端项目生成
"""

import sys
import os
from pathlib import Path

# 添加项目路径
sys.path.insert(0, str(Path(__file__).parent.parent))

from app.tools.project import ProjectGenerator


def test_project_generation():
    """测试项目生成"""

    # 配置
    SCAFFOLD_PATH = Path("/Users/zriyo/Desktop/zriyo-ai-code-mother/frontend-scaffold")
    PROJECT_BASE = Path("/Users/zriyo/Desktop/zriyo-ai-code-mother/tmp")
    APP_ID = 1

    # 创建生成器
    generator = ProjectGenerator(scaffold_path=str(SCAFFOLD_PATH))

    # 临时覆盖 PROJECT_BASE
    from app.config import settings
    original_base = settings.settings.PROJECT_BASE
    settings.settings.PROJECT_BASE = str(PROJECT_BASE)

    try:
        # 需求描述
        requirement = "做一个带折线图的数据管理后台，包含登录页面和仪表盘"

        print(f"Testing project generation...")
        print(f"  Scaffold: {SCAFFOLD_PATH}")
        print(f"  Output: {PROJECT_BASE}/app_{APP_ID:06d}")
        print(f"  Requirement: {requirement}")
        print()

        # 生成项目
        result = generator.generate(
            app_id=APP_ID,
            requirement=requirement,
            project_name="test-dashboard"
        )

        print("=" * 60)
        print("GENERATION RESULT")
        print("=" * 60)

        if result["success"]:
            print(f"✅ Success!")
            print(f"  Project Path: {result['project_path']}")
            print(f"  Skills Used: {result['skills_used']}")
            print()
            print(f"  Generation Tasks:")
            tasks = result["generation_tasks"]
            if isinstance(tasks, dict):
                for task in tasks.get("tasks", []):
                    print(f"    - {task['file']} ({task['type']})")

            # 检查生成的文件
            project_path = Path(result["project_path"])
            if project_path.exists():
                print()
                print("  Generated Files:")
                for item in sorted(project_path.rglob("*"))[:20]:
                    if item.is_file() and "node_modules" not in str(item):
                        rel = item.relative_to(project_path)
                        print(f"    ✓ {rel}")
        else:
            print(f"❌ Failed: {result['error']}")

        print("=" * 60)

    finally:
        # 恢复原始配置
        settings.settings.PROJECT_BASE = original_base


if __name__ == "__main__":
    test_project_generation()
