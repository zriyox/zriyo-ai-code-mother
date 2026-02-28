"""
前端项目生成工具
复制模板 + 读取 Skill 规范 + LLM 生成代码
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
        requirement: str,
        project_name: str = "ai-generated-app",
    ) -> Dict[str, Any]:
        """
        生成前端项目

        Args:
            app_id: 应用 ID
            requirement: 用户需求描述
            project_name: 项目名称

        Returns:
            生成结果
        """
        project_path = get_project_path(app_id)

        logger.info(f"Generating frontend project: {project_path}")
        logger.info(f"Requirement: {requirement}")

        try:
            # 1. 复制模板
            self._copy_template(project_path, project_name)

            # 2. 创建 node_modules 软链接
            self._link_node_modules(project_path)

            # 3. 读取 Skill 规范
            skills_context = self._load_skills_context(requirement)

            # 4. 生成代码 (返回待生成任务)
            generation_tasks = self._plan_generation(requirement, skills_context)

            return {
                "success": True,
                "project_path": str(project_path),
                "generation_tasks": generation_tasks,
                "skills_used": list(skills_context.keys()),
            }

        except Exception as e:
            logger.error(f"Project generation failed: {e}")
            return {
                "success": False,
                "error": str(e),
            }

    def _copy_template(self, dest_path: Path, project_name: str) -> None:
        """
        复制脚手架模板

        排除:
        - node_modules (使用软链接)
        - dist (构建产物)
        - .git (版本控制)
        """
        exclude = {"node_modules", "dist", ".git", "__pycache__"}

        if not dest_path.exists():
            dest_path.mkdir(parents=True)

        for item in self.scaffold_path.iterdir():
            if item.name in exclude:
                continue
            if item.is_dir():
                dest = dest_path / item.name
                if dest.exists():
                    shutil.rmtree(dest)
                shutil.copytree(item, dest)
            else:
                shutil.copy2(item, dest_path / item.name)

        # 更新 package.json 中的项目名称
        package_json = dest_path / "package.json"
        if package_json.exists():
            self._update_package_json(package_json, project_name)

        logger.info(f"Template copied to {dest_path}")

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

        # 创建相对路径软链接
        relative_src = os.path.relpath(src, project_path)
        dst.symlink_to(relative_src)

        logger.info(f"node_modules linked: {dst} -> {src}")

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

    def _plan_generation(
        self, requirement: str, skills_context: Dict[str, str]
    ) -> List[Dict[str, Any]]:
        """
        规划代码生成任务

        返回需要生成的文件列表和提示词
        """
        # 基础文件（总是生成）
        base_files = [
            "src/main.ts",
            "src/App.vue",
            "src/router/index.ts",
            "src/stores/app.ts",
        ]

        # 根据需求确定需要生成的文件
        files = base_files.copy()

        requirement_lower = requirement.lower()

        # 页面
        if any(kw in requirement_lower for kw in ["页面", "管理", "后台", "dashboard", "admin"]):
            files.extend([
                "src/views/Dashboard.vue",
                "src/layouts/DefaultLayout.vue",
            ])

        # 图表
        if "chart" in requirement_lower or "图表" in requirement_lower:
            files.append("src/components/charts/ChartCard.vue")

        # 表格
        if "table" in requirement_lower or "表格" in requirement_lower:
            files.append("src/views/pages/TablePage.vue")

        # 表单
        if "form" in requirement_lower or "表单" in requirement_lower:
            files.append("src/views/pages/FormPage.vue")

        # 构建生成任务
        tasks = []
        for file_path in files:
            tasks.append({
                "file": file_path,
                "type": self._get_file_type(file_path),
            })

        # 构建系统提示词
        system_prompt = self._build_system_prompt(skills_context)

        return {
            "tasks": tasks,
            "system_prompt": system_prompt,
        }

    def _get_file_type(self, file_path: str) -> str:
        """获取文件类型"""
        if file_path.endswith(".vue"):
            return "vue-component"
        elif file_path.endswith(".ts"):
            return "typescript"
        return "text"

    def _build_system_prompt(self, skills_context: Dict[str, str]) -> str:
        """构建给 LLM 的系统提示词"""
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
## 工作流程

1. 理解用户需求
2. 参考上述文档规范
3. 生成符合规范的代码

## 输出格式

直接输出完整的文件内容，不需要解释。
"""
        return prompt


# 单例
_generator: Optional[ProjectGenerator] = None


def get_generator() -> ProjectGenerator:
    """获取项目生成器单例"""
    global _generator
    if _generator is None:
        _generator = ProjectGenerator()
    return _generator
