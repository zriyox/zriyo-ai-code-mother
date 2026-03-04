"""
ProjectGenerator 提示词模板
"""

from typing import Dict, Tuple, Optional


def build_skill_selector_prompts(
    requirement: str,
    context: Optional[str],
    available_text: str,
    skills_index: str,
    max_skills: int,
) -> Tuple[str, str]:
    """构建技能选择提示词"""
    context_text = f"\n\n## 额外上下文\n{context}" if context else ""

    system_prompt = (
        "你是 Skill Selector。请从给定的技能列表中选择与需求最相关的技能。"
        f"最多选择 {max_skills} 个，只能返回列表中的 ID。"
        "如果不需要任何技能，返回空数组 []。输出必须是 JSON 数组，不要附加其他文字。"
    )

    user_prompt = (
        f"## 用户需求\n{requirement}"
        f"{context_text}\n\n"
        "## 可用技能列表\n"
        f"{available_text}\n\n"
        "## 技能索引\n"
        f"{skills_index}"
    )

    return system_prompt, user_prompt


def build_codegen_system_prompt(skills_context: Dict[str, str]) -> str:
    """构建代码生成系统提示词"""
    prompt = """你是 Vue 3 前端开发专家，正在帮用户生成代码。

## 技术栈

Vue 3.5.13 + TypeScript 5.8.0 + Vite 6.3.5
Vue Router 4.5.0 + Pinia 2.2.8
Element Plus 2.10.4 + Tailwind CSS 3.4.17
@vueuse/core 11.3.0 + Day.js + Lodash-es

## 代码规范

"""

    # 添加 SYSTEM.md 内容
    if "SYSTEM.md" in skills_context:
        prompt += skills_context["SYSTEM.md"] + "\n\n"

    # 添加相关 Skill 文档
    prompt += "## 参考文档\n\n"
    for skill_name, content in skills_context.items():
        if skill_name == "SYSTEM.md":
            continue
        prompt += f"### {skill_name}\n{content[:500]}...\n\n"

    prompt += """
## 生成约束（重要）

- 当前项目初始化后只保证存在“最小模板”与配置文件，不要假设已有完整业务代码。
- 默认只可靠存在：`src/main.ts`、`src/App.vue`、`src/vite-env.d.ts`、`src/types/env.d.ts`（以及根目录配置文件）。
- 生成代码时禁止虚构本地文件现状；若需要依赖其他本地模块，请使用清晰且可后续生成的标准路径。
- 优先保证当前目标文件可读、可维护、可被后续步骤衔接。

## 工具边界（重要）

- 你当前阶段不能直接调用工具；工具调用由编排层（Agent）在外部完成。
- 你可使用的信息仅包括：用户需求、规划摘要、已注入的参考文档、已注入的参考文件片段。
- 如缺少上下文，请在输出代码中采取保守实现，不要编造“已存在但未提供”的本地模块。

## 工作流程

1. 理解用户需求
2. 参考上述文档规范
3. 在最小模板前提下生成可落地代码

## 输出格式

直接输出完整的文件内容，不需要解释，不要输出 Markdown 代码块。
"""
    return prompt
