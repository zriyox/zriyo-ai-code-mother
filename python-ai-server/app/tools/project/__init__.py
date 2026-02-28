"""
前端项目初始化工具
负责确定性初始化：复制模板 + 软链接依赖。
"""

import os
import json
import shutil
from pathlib import Path
from typing import Optional, Dict, Any, List
from loguru import logger

from app.config.settings import settings, get_project_path
from app.llm.client import LlmClient
from app.models.llm import LlmConfig, LlmMessage
from app.prompts.project_prompts import (
    build_codegen_system_prompt,
    build_skill_selector_prompts,
)


class ProjectGenerator:
    """前端项目生成器"""

    def __init__(self, scaffold_path: Optional[str] = None):
        """
        初始化生成器

        Args:
            scaffold_path: 前端脚手架路径，默认从配置读取
        """
        self.scaffold_path = Path(scaffold_path or self._get_scaffold_path())
        self.docs_path = self._get_skills_root()
        self.skills_path = self.docs_path

    def _get_scaffold_path(self) -> Path:
        """获取脚手架路径"""
        # 从环境变量或默认路径获取
        default_path = Path(__file__).resolve().parents[4] / "frontend-scaffold"
        configured = settings.FRONTEND_SCAFFOLD_PATH or os.getenv("FRONTEND_SCAFFOLD_PATH")
        return Path(configured) if configured else default_path

    def _get_skills_root(self) -> Path:
        """获取技能根目录（repo/skills/codeagent）"""
        return Path(__file__).resolve().parents[4] / "skills" / "codeagent"

    def generate(
        self,
        app_id: int,
        requirement: Optional[str] = None,
        project_name: str = "ai-generated-app",
    ) -> Dict[str, Any]:
        """
        生成前端项目

        Args:
            app_id: 应用 ID
            requirement: 用户需求描述（可选，初始化阶段不参与 LLM）
            project_name: 项目名称

        Returns:
            生成结果
        """
        project_path = get_project_path(app_id)

        logger.info(f"Generating frontend project: {project_path}")
        if requirement:
            logger.info(f"Requirement (ignored in init stage): {requirement}")

        try:
            # 1. 复制模板
            self._copy_template(project_path, project_name)

            # 2. 创建 node_modules 软链接
            self._link_node_modules(project_path)

            return {
                "success": True,
                "project_path": str(project_path),
            }

        except Exception as e:
            logger.error(f"Project generation failed: {e}")
            return {
                "success": False,
                "error": str(e),
            }

    def _copy_template(self, dest_path: Path, project_name: str) -> None:
        """
        复制最小脚手架模板（显式白名单）。

        设计目标：
        1. 仅复制“通用配置 + 最小入口”文件，避免把业务示例页带入新项目。
        2. 行为可读、可审计：白名单都写在代码里，后续维护只需改这里。

        会复制：
        - 根目录配置：package/vite/ts/eslint/tailwind 等
        - 最小入口：src/main.ts、src/App.vue
        - 类型声明：src/vite-env.d.ts、src/types/env.d.ts、types/*.d.ts
        """
        allow_root_files = {
            ".env.example",
            ".eslintrc-auto-import.json",
            ".gitignore",
            ".npmrc",
            ".prettierrc.json",
            "README.md",
            "eslint.config.mjs",
            "index.html",
            "package-lock.json",
            "package.json",
            "postcss.config.js",
            "tailwind.config.js",
            "tsconfig.json",
            "tsconfig.node.json",
            "vite.config.ts",
        }
        allow_relative_files = {
            "src/main.ts",
            "src/App.vue",
            "src/vite-env.d.ts",
            "src/types/env.d.ts",
            "src/assets/styles/variables.scss",
            "types/auto-imports.d.ts",
            "types/components.d.ts",
        }

        if not dest_path.exists():
            dest_path.mkdir(parents=True)

        # 复制根目录白名单文件
        for filename in allow_root_files:
            src = self.scaffold_path / filename
            dst = dest_path / filename
            if src.exists() and src.is_file():
                shutil.copy2(src, dst)
            else:
                logger.warning(f"Template root file missing, skipped: {src}")

        # 复制相对路径白名单文件
        for rel_path in allow_relative_files:
            src = self.scaffold_path / rel_path
            dst = dest_path / rel_path
            if src.exists() and src.is_file():
                dst.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(src, dst)
            else:
                logger.warning(f"Template file missing, skipped: {src}")

        # 更新 package.json 中的项目名称
        package_json = dest_path / "package.json"
        if package_json.exists():
            self._update_package_json(package_json, project_name)

        logger.info(
            f"Template copied to {dest_path} "
            f"(root_files={len(allow_root_files)}, relative_files={len(allow_relative_files)})"
        )

    def _update_package_json(self, package_path: Path, project_name: str) -> None:
        """更新 package.json"""
        import json

        with open(package_path, "r", encoding="utf-8") as f:
            content = json.load(f)

        content["name"] = project_name

        with open(package_path, "w", encoding="utf-8") as f:
            json.dump(content, f, indent=2, ensure_ascii=False)

    def _link_node_modules(self, project_path: Path) -> None:
        """创建 node_modules 软链接"""
        src = self.scaffold_path / "node_modules"
        dst = project_path / "node_modules"

        if dst.exists():
            shutil.rmtree(dst)

        # 创建相对路径软链接；跨盘符（Windows）时降级为绝对路径软链接
        try:
            link_target = os.path.relpath(src, project_path)
        except ValueError:
            link_target = str(src)

        dst.symlink_to(link_target)
        logger.info(f"node_modules linked: {dst} -> {link_target}")

    def _load_skills_context(
        self,
        requirement: str,
        llm_config: Optional[LlmConfig] = None,
        context: Optional[str] = None,
    ) -> Dict[str, str]:
        """
        根据需求加载相关的 Skill 文档

        Returns:
            {skill_name: skill_content}
        """
        skills = {}

        # 基础规范（默认需要）
        system_content = self._read_skill("SYSTEM.md")
        if system_content:
            skills["SYSTEM.md"] = system_content

        skills_index = self._read_skill("SKILLS_INDEX.md")

        # LLM 自动选择技能
        selected: List[str] = []
        if llm_config and skills_index:
            try:
                selected = self._select_skills_by_llm(
                    requirement=requirement,
                    context=context,
                    skills_index=skills_index,
                    llm_config=llm_config,
                )
            except Exception as e:
                logger.warning(f"Skill selection failed, fallback to base only: {e}")

        for skill_name in selected:
            content = self._read_skill(skill_name)
            if content:
                skills[skill_name] = content
            else:
                logger.warning(f"Skill not found: {skill_name}")

        logger.info(f"Loaded {len(skills)} skill documents")
        return skills

    def _list_available_skills(self) -> List[str]:
        """列出可用技能 ID（code/<skill>）"""
        skills_dir = self.skills_path / "code"
        if not skills_dir.exists():
            return []
        items = []
        for p in skills_dir.iterdir():
            if p.is_dir():
                items.append(f"code/{p.name}")
        return sorted(items)

    def _select_skills_by_llm(
        self,
        requirement: str,
        context: Optional[str],
        skills_index: str,
        llm_config: LlmConfig,
        max_skills: int = 3,
    ) -> List[str]:
        """使用 LLM 选择需要加载的技能列表"""
        available = self._list_available_skills()
        if not available:
            return []

        available_text = "\n".join(f"- {s}" for s in available)
        system_prompt, user_prompt = build_skill_selector_prompts(
            requirement=requirement,
            context=context,
            available_text=available_text,
            skills_index=skills_index,
            max_skills=max_skills,
        )

        client = LlmClient(llm_config)
        response = client.call(
            [
                LlmMessage(role="system", content=system_prompt),
                LlmMessage(role="user", content=user_prompt),
            ],
            temperature=0.1,
            max_tokens=300,
        )

        selected = self._parse_skill_list(response)
        if not selected:
            return []

        available_set = set(available)
        alias_map = {s.split("/", 1)[1]: s for s in available}

        filtered: List[str] = []
        for raw in selected:
            name = raw.strip()
            if name in available_set:
                filtered.append(name)
            elif name in alias_map:
                filtered.append(alias_map[name])

        # 去重 + 截断
        result: List[str] = []
        for s in filtered:
            if s not in result:
                result.append(s)
            if len(result) >= max_skills:
                break
        return result

    def _parse_skill_list(self, response: str) -> List[str]:
        """解析 LLM 返回的技能列表 JSON"""
        text = response.strip()
        if text.startswith("```json"):
            text = text[7:]
        elif text.startswith("```"):
            text = text[3:]
        if text.endswith("```"):
            text = text[:-3]
        text = text.strip()
        try:
            data = json.loads(text)
            if isinstance(data, list):
                return [str(x) for x in data]
        except Exception:
            logger.warning(f"Failed to parse skill list JSON: {response[:200]}")
        return []

    def _read_skill(self, filename: str) -> Optional[str]:
        """读取 Skill 文档内容（支持 SKILL.md 目录结构）"""
        search_roots = [self.skills_path, self.docs_path]

        for root in search_roots:
            for candidate in self._resolve_skill_candidates(root, filename):
                if candidate.exists() and candidate.is_file():
                    with open(candidate, "r", encoding="utf-8") as f:
                        return self._strip_front_matter(f.read())
        return None

    def _resolve_skill_candidates(self, root: Path, name: str) -> list[Path]:
        """解析技能路径候选（支持旧文件名与新目录结构）"""
        candidates: list[Path] = []
        clean = name.strip().replace("\\", "/")

        # 直接路径（文件或子路径）
        candidates.append(root / clean)
        if clean and not clean.endswith(".md"):
            candidates.append(root / clean / "SKILL.md")

        # 解析为 skill id
        if clean.endswith(".md"):
            stem = Path(clean).stem  # e.g. axios-skill / SKILL
            skill_id = stem[:-6] if stem.endswith("-skill") else stem
        else:
            skill_id = clean[:-6] if clean.endswith("-skill") else clean

        if skill_id and skill_id.lower() != "skill":
            candidates.append(root / skill_id / "SKILL.md")
            candidates.append(root / f"{skill_id}-skill" / "SKILL.md")

            # 支持按 Agent 分组目录：skills/<agent>/<skill>/SKILL.md
            if root.exists():
                for agent_dir in root.iterdir():
                    if agent_dir.is_dir():
                        candidates.append(agent_dir / skill_id / "SKILL.md")
                        candidates.append(agent_dir / f"{skill_id}-skill" / "SKILL.md")

        return candidates

    def _strip_front_matter(self, content: str) -> str:
        """去除 YAML front matter，仅返回正文"""
        if not content.startswith("---"):
            return content

        lines = content.splitlines()
        if not lines or lines[0].strip() != "---":
            return content

        end_index = None
        for i in range(1, len(lines)):
            if lines[i].strip() == "---":
                end_index = i
                break

        if end_index is None:
            return content

        body = "\n".join(lines[end_index + 1 :])
        return body.lstrip("\n")

    def _normalize_skill_name(self, name: str) -> Optional[str]:
        """规范化技能文件名，防止路径穿越"""
        if not name:
            return None
        clean = name.strip().replace("\\", "/")
        if clean.startswith("/"):
            return None
        parts = [p for p in clean.split("/") if p]
        if any(p == ".." for p in parts):
            return None
        if not clean:
            return None
        return clean

    def _build_system_prompt(self, skills_context: Dict[str, str]) -> str:
        """构建给 LLM 的系统提示词"""
        return build_codegen_system_prompt(skills_context)


# 单例
_generator: Optional[ProjectGenerator] = None


def get_generator() -> ProjectGenerator:
    """获取项目生成器单例"""
    global _generator
    if _generator is None:
        _generator = ProjectGenerator()
    return _generator
