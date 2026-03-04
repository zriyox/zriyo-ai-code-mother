"""
CodeAgent 提示词模板
"""

from typing import Any, Dict, List, Optional, Tuple


def build_file_selector_prompts(
    requirement: str,
    file_path: str,
    plan_summary: Optional[str],
    candidate_text: str,
    max_files: int,
) -> Tuple[str, str]:
    """构建“候选文件选择”提示词"""
    summary_text = f"\n\n## 规划摘要\n{plan_summary}" if plan_summary else ""

    system_prompt = (
        "你是文件选择器。请从候选文件中选择最需要阅读的文件，"
        f"最多 {max_files} 个。只返回 JSON 数组。"
        "如果不需要任何文件，返回空数组 []。"
        "你不能访问候选列表之外的任何文件。"
    )
    user_prompt = (
        f"## 目标文件\n{file_path}\n\n"
        f"## 用户需求\n{requirement}"
        f"{summary_text}\n\n"
        "## 候选文件\n"
        f"{candidate_text}"
    )

    return system_prompt, user_prompt


def build_single_file_user_prompt(
    file_path: str,
    requirement: str,
    plan_summary: Optional[str],
    snippets: List[Dict[str, Any]],
) -> str:
    """构建单文件生成 user prompt"""
    summary_text = f"\n\n## 规划摘要\n{plan_summary}" if plan_summary else ""
    snippets_text = ""
    if snippets:
        parts = []
        for item in snippets:
            parts.append(f"### {item['path']}\n{item['content']}")
        snippets_text = "\n\n## 参考文件\n" + "\n\n".join(parts)

    return (
        f"## 目标文件\n{file_path}\n\n"
        f"## 用户需求\n{requirement}"
        f"{summary_text}"
        f"{snippets_text}\n\n"
        "## 约束\n"
        "- 本阶段不支持你主动调用工具；可用上下文仅限本提示词已提供内容。\n"
        "- 当前项目是最小模板，默认不存在业务页面/路由/store，除非在“参考文件”中已给出。\n"
        "- 不要假设额外本地文件已存在；若必须引用本地模块，请使用可后续生成的标准路径并保持一致。\n"
        "- 仅使用项目技术栈内能力，不要引入 package.json 之外的新第三方依赖。\n"
        "- 只输出目标文件完整内容；不要输出解释、不要输出 Markdown 代码块。\n"
    )
