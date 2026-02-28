#!/usr/bin/env python3
"""
Python 核心链路注释增强脚本（面向 Java 开发者）。

用途：
- 扫描固定核心文件列表（--scope core）
- 为缺失或过短的模块/类/函数 docstring 自动补全
- 输出 JSON 报告（dry-run / apply）
"""

from __future__ import annotations

import argparse
import ast
import json
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Tuple


CORE_RELATIVE_FILES = [
    "python-ai-server/app/main.py",
    "python-ai-server/app/api/agent.py",
    "python-ai-server/app/api/project.py",
    "python-ai-server/app/api/plan.py",
    "python-ai-server/app/api/internal.py",
    "python-ai-server/app/agent/base_agent.py",
    "python-ai-server/app/agent/code_agent.py",
    "python-ai-server/app/agent/planner.py",
    "python-ai-server/app/llm/client.py",
]

PRIVATE_WHITELIST = {
    "_check_cancelled",
    "_collect_candidates",
    "_select_files_to_read",
    "_build_context",
    "_build_user_prompt",
    "_strip_code_fence",
    "_parse_json_list",
    "_build_model",
    "_resolve_base_url",
    "_to_langchain_messages",
    "_parse_response",
    "_ensure_global_style_files",
    "_calculate_complexity",
}

MODULE_DOC_OVERRIDES: Dict[str, List[str]] = {
    "python-ai-server/app/main.py": [
        "模块职责：Python AI Server 的 FastAPI 入口，负责应用初始化、路由装配与生命周期管理。",
        "Java 对照：可类比 Spring Boot Application + WebMvc 配置入口。",
    ],
    "python-ai-server/app/api/agent.py": [
        "模块职责：Agent 执行相关 API 路由层，负责把请求转交给执行链路。",
        "Java 对照：可类比 Spring MVC Controller（参数校验 + 响应封装）。",
    ],
    "python-ai-server/app/api/project.py": [
        "模块职责：项目初始化与单文件生成写入 API。",
        "Java 对照：可类比项目脚手架与代码生成 Controller 聚合层。",
    ],
    "python-ai-server/app/api/plan.py": [
        "模块职责：规划接口，接收需求后调用 Planner 生成执行计划。",
        "Java 对照：可类比“先规划再执行”的编排入口 Controller。",
    ],
    "python-ai-server/app/api/internal.py": [
        "模块职责：内部 API（Java 服务调用），提供 token 计数、工具元数据、健康检查等能力。",
        "Java 对照：可类比内部服务适配层（仅内网或受信调用）。",
    ],
    "python-ai-server/app/agent/base_agent.py": [
        "模块职责：Agent 公共基类，提供协作式取消能力。",
        "Java 对照：可类比抽象基类 + 中断检查工具方法。",
    ],
    "python-ai-server/app/agent/code_agent.py": [
        "模块职责：单文件代码生成 Agent，覆盖技能选择、上下文读取、LLM 生成与落盘。",
        "Java 对照：可类比 Service 编排链路（select -> read -> generate -> write）。",
    ],
    "python-ai-server/app/agent/planner.py": [
        "模块职责：需求规划 Agent，负责让 LLM 输出可执行文件计划。",
        "Java 对照：可类比“需求解析 + 计划模型构建”服务层。",
    ],
    "python-ai-server/app/llm/client.py": [
        "模块职责：统一 LLM 访问适配层，屏蔽不同提供商 SDK 差异。",
        "Java 对照：可类比多实现 Client 的 Facade/Adapter。",
    ],
}

SYMBOL_DOC_OVERRIDES: Dict[Tuple[str, str], List[str]] = {
    ("python-ai-server/app/main.py", "lifespan"): [
        "应用生命周期钩子：启动时记录环境信息，关闭时输出收尾日志。",
        "Java 对照：类似 @PostConstruct / @PreDestroy 的组合。",
    ],
    ("python-ai-server/app/main.py", "root"): [
        "根路径健康返回，用于最轻量连通性检测。",
        "输出：服务名、版本号、运行状态。",
    ],
    ("python-ai-server/app/agent/base_agent.py", "CancelableAgent._check_cancelled"): [
        "协作式取消检查：在关键步骤主动检测任务是否被标记取消。",
        "若已取消则抛出 AgentCancelledError，由上层统一转为取消事件。",
    ],
    ("python-ai-server/app/agent/code_agent.py", "CodeAgent.generate_and_write_stream"): [
        "流式生成主流程：按阶段发出 SSE 事件并最终落盘目标文件。",
        "阶段链路：select_skills -> select_files -> generate_code -> write_file。",
    ],
    ("python-ai-server/app/agent/code_agent.py", "CodeAgent._collect_candidates"): [
        "收集可读上下文候选文件。",
        "优先级：dependencies > file_list > 自动扫描 src/**。",
    ],
    ("python-ai-server/app/agent/code_agent.py", "CodeAgent._select_files_to_read"): [
        "让 LLM 在候选文件中挑选最有价值的少量上下文。",
        "返回值会再次按候选白名单过滤，避免越界读取。",
    ],
    ("python-ai-server/app/agent/planner.py", "PlannerAgent.plan"): [
        "规划主入口：调用 LLM 生成项目计划并做结构化校验。",
        "输出：ProjectPlan（供 Java 编排层后续执行）。",
    ],
    ("python-ai-server/app/llm/client.py", "LlmClient._build_model"): [
        "根据 provider 构建对应 ChatModel 实例。",
        "策略：anthropic/gemini 走专用实现，其余走 OpenAI 兼容协议。",
    ],
}


@dataclass
class Edit:
    start: int
    end: int
    new_lines: List[str]
    action: str
    symbol: str
    preview: str


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Python 注释增强脚本")
    parser.add_argument("--scope", choices=["core"], required=True, help="目标范围，仅支持 core")
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument("--apply", action="store_true", help="应用修改并写回文件")
    mode.add_argument("--dry-run", action="store_true", help="仅预览，不落盘")
    parser.add_argument("--report", required=True, help="JSON 报告输出路径")
    parser.add_argument("--lang", default="zh", choices=["zh"], help="注释语言")
    parser.add_argument("--audience", default="java", choices=["java"], help="目标读者")
    return parser.parse_args()


def resolve_repo_root() -> Path:
    # .../python-ai-server/scripts/comment_enhancer.py -> repo root
    return Path(__file__).resolve().parents[2]


def is_high_quality_doc(doc: str) -> bool:
    s = (doc or "").strip()
    if not s:
        return False
    if len(s) >= 60:
        return True
    if "\n" in s and len(s) >= 35:
        return True
    if any(token in s for token in ["Args", "Returns", "Java", "SSE", "流程"]) and len(s) >= 24:
        return True
    return False


def render_doc_block(indent: str, lines: List[str]) -> List[str]:
    content = [f'{indent}"""']
    content.extend(f"{indent}{line}" for line in lines)
    content.append(f'{indent}"""')
    return [line + "\n" for line in content]


def should_handle_symbol(name: str) -> bool:
    if not name.startswith("_"):
        return True
    return name in PRIVATE_WHITELIST


def default_module_doc(rel_path: str) -> List[str]:
    if "/api/" in rel_path:
        return [
            "模块职责：提供 FastAPI 路由入口，负责请求参数校验与响应组织。",
            "Java 对照：可类比 Spring MVC Controller。",
        ]
    if "/agent/" in rel_path:
        return [
            "模块职责：承载 Agent 编排核心逻辑。",
            "Java 对照：可类比 Service 层中的流程编排实现。",
        ]
    if "/llm/" in rel_path:
        return [
            "模块职责：封装 LLM 访问细节。",
            "Java 对照：可类比外部服务 Client 适配层。",
        ]
    return ["模块职责：核心实现模块。", "Java 对照：可类比后端服务实现文件。"]


def default_symbol_doc(rel_path: str, symbol: str, node_kind: str, is_async: bool) -> List[str]:
    name = symbol.split(".")[-1]
    lines = [f"{name} 的职责说明。"]

    if node_kind == "class":
        if "/api/" in rel_path:
            if name.endswith("Request"):
                return [
                    f"{name} 请求 DTO：定义接口入参结构与校验约束。",
                    "Java 对照：可类比 Controller 入参对象（Request DTO）。",
                ]
            if name.endswith("Response"):
                return [
                    f"{name} 响应 DTO：定义接口出参结构。",
                    "Java 对照：可类比 Controller 返回对象（Response DTO）。",
                ]
            return [
                f"{name} 数据模型定义。",
                "Java 对照：可类比 POJO/record，用于序列化与反序列化。",
            ]

        return [
            f"{name} 类型定义。",
            "Java 对照：可类比领域模型或配置载体类型。",
        ]

    if "/api/" in rel_path:
        lines = [
            "路由处理函数：接收请求参数并调用下游能力。",
            "输入：Pydantic 模型或 query 参数；输出：JSON 或流式响应。",
        ]
    elif "/agent/" in rel_path:
        lines = [
            "Agent 阶段方法：用于组织生成流程中的一个步骤。",
            "输入：当前任务上下文；输出：阶段结果或中间状态。",
        ]
    elif "/llm/" in rel_path:
        lines = [
            "LLM 访问方法：对上层暴露统一调用接口。",
            "输入：消息列表与参数；输出：模型文本或增量片段。",
        ]

    if "stream" in name.lower() or is_async:
        lines.append("说明：包含异步/流式处理逻辑，需关注事件边界与错误兜底。")
    if name.startswith("get_"):
        lines.append("返回：目标对象实例或查询结果。")

    return lines


def choose_symbol_doc(rel_path: str, symbol: str, node_kind: str, is_async: bool) -> List[str]:
    if (rel_path, symbol) in SYMBOL_DOC_OVERRIDES:
        return SYMBOL_DOC_OVERRIDES[(rel_path, symbol)]
    return default_symbol_doc(rel_path, symbol, node_kind, is_async)


def choose_module_doc(rel_path: str) -> List[str]:
    return MODULE_DOC_OVERRIDES.get(rel_path, default_module_doc(rel_path))


def build_symbol_name(class_stack: List[str], name: str) -> str:
    if class_stack:
        return f"{'.'.join(class_stack)}.{name}"
    return name


def collect_symbol_edits(
    tree: ast.Module,
    rel_path: str,
    min_quality: int = 24,
) -> List[Edit]:
    edits: List[Edit] = []

    def walk(body: List[ast.stmt], class_stack: List[str]) -> None:
        for stmt in body:
            if isinstance(stmt, ast.ClassDef):
                symbol = build_symbol_name(class_stack, stmt.name)
                if should_handle_symbol(stmt.name):
                    maybe_add_edit(stmt, symbol, class_stack)
                walk(stmt.body, class_stack + [stmt.name])
                continue

            if isinstance(stmt, (ast.FunctionDef, ast.AsyncFunctionDef)):
                symbol = build_symbol_name(class_stack, stmt.name)
                if should_handle_symbol(stmt.name):
                    maybe_add_edit(stmt, symbol, class_stack)
                continue

    def maybe_add_edit(node: ast.AST, symbol: str, class_stack: List[str]) -> None:
        assert isinstance(node, (ast.ClassDef, ast.FunctionDef, ast.AsyncFunctionDef))
        existing = ast.get_docstring(node, clean=False)
        node_kind = "class" if isinstance(node, ast.ClassDef) else "function"
        has_bad_generated_doc = bool(
            existing
            and "路由处理函数：接收请求参数并调用下游能力。" in existing
            and "输入：Pydantic 模型或 query 参数；输出：JSON 或流式响应。" in existing
        )
        should_fix = (
            (not existing)
            or has_bad_generated_doc
            or (len(existing.strip()) < min_quality and not is_high_quality_doc(existing))
        )
        if not should_fix:
            return

        indent = " " * (node.col_offset + 4)
        doc_lines = choose_symbol_doc(rel_path, symbol, node_kind, isinstance(node, ast.AsyncFunctionDef))
        new_block = render_doc_block(indent, doc_lines)

        first_stmt = node.body[0] if node.body else None
        if (
            first_stmt
            and isinstance(first_stmt, ast.Expr)
            and isinstance(getattr(first_stmt, "value", None), ast.Constant)
            and isinstance(first_stmt.value.value, str)
        ):
            edits.append(
                Edit(
                    start=first_stmt.lineno,
                    end=first_stmt.end_lineno,
                    new_lines=new_block,
                    action="replace",
                    symbol=symbol,
                    preview=doc_lines[0],
                )
            )
            return

        insert_line = node.body[0].lineno if node.body else node.lineno + 1
        edits.append(
            Edit(
                start=insert_line,
                end=insert_line - 1,
                new_lines=new_block,
                action="insert",
                symbol=symbol,
                preview=doc_lines[0],
            )
        )

    walk(tree.body, [])
    return edits


def collect_module_edit(tree: ast.Module, lines: List[str], rel_path: str) -> List[Edit]:
    existing = ast.get_docstring(tree, clean=False)
    if existing and is_high_quality_doc(existing):
        return []

    doc_lines = choose_module_doc(rel_path)
    new_block = render_doc_block("", doc_lines)

    if tree.body:
        first_stmt = tree.body[0]
        if (
            isinstance(first_stmt, ast.Expr)
            and isinstance(getattr(first_stmt, "value", None), ast.Constant)
            and isinstance(first_stmt.value.value, str)
        ):
            return [
                Edit(
                    start=first_stmt.lineno,
                    end=first_stmt.end_lineno,
                    new_lines=new_block,
                    action="replace",
                    symbol="module",
                    preview=doc_lines[0],
                )
            ]

    insert_at = 1
    if lines and lines[0].startswith("#!"):
        insert_at = 2
        if len(lines) >= 2 and "coding" in lines[1]:
            insert_at = 3
    return [
        Edit(
            start=insert_at,
            end=insert_at - 1,
            new_lines=new_block + ["\n"],
            action="insert",
            symbol="module",
            preview=doc_lines[0],
        )
    ]


def apply_edits(source_lines: List[str], edits: List[Edit]) -> List[str]:
    if not edits:
        return source_lines[:]

    new_lines = source_lines[:]
    for edit in sorted(edits, key=lambda x: (x.start, x.end), reverse=True):
        if edit.start <= edit.end:
            new_lines[edit.start - 1 : edit.end] = edit.new_lines
        else:
            idx = max(edit.start - 1, 0)
            new_lines[idx:idx] = edit.new_lines
    return new_lines


def process_file(path: Path, repo_root: Path, apply_changes: bool) -> Dict:
    rel_path = path.relative_to(repo_root).as_posix()
    source = path.read_text(encoding="utf-8")
    source_lines = source.splitlines(keepends=True)

    try:
        tree = ast.parse(source)
    except SyntaxError as exc:
        return {
            "file": rel_path,
            "changed": False,
            "change_count": 0,
            "changes": [],
            "error": f"SyntaxError: {exc}",
        }

    edits = []
    edits.extend(collect_module_edit(tree, source_lines, rel_path))
    edits.extend(collect_symbol_edits(tree, rel_path))

    # 去重：同一位置仅保留最后一个（优先 symbol 覆盖）
    dedup: Dict[Tuple[int, int], Edit] = {}
    for e in edits:
        dedup[(e.start, e.end)] = e
    final_edits = list(dedup.values())

    if not final_edits:
        return {
            "file": rel_path,
            "changed": False,
            "change_count": 0,
            "changes": [],
        }

    new_lines = apply_edits(source_lines, final_edits)
    changed = new_lines != source_lines

    if apply_changes and changed:
        path.write_text("".join(new_lines), encoding="utf-8")

    return {
        "file": rel_path,
        "changed": changed,
        "change_count": len(final_edits) if changed else 0,
        "changes": [
            {
                "symbol": e.symbol,
                "action": e.action,
                "line": e.start,
                "preview": e.preview,
            }
            for e in sorted(final_edits, key=lambda x: x.start)
        ],
    }


def main() -> int:
    args = parse_args()
    repo_root = resolve_repo_root()
    apply_changes = args.apply

    targets = [repo_root / rel for rel in CORE_RELATIVE_FILES]
    missing = [str(p) for p in targets if not p.exists()]
    if missing:
        raise FileNotFoundError(f"目标文件不存在: {missing}")

    results = [process_file(p, repo_root, apply_changes) for p in targets]

    changed_files = sum(1 for r in results if r.get("changed"))
    changes = sum(r.get("change_count", 0) for r in results)

    report = {
        "generated_at": datetime.now().isoformat(timespec="seconds"),
        "scope": args.scope,
        "mode": "apply" if args.apply else "dry-run",
        "lang": args.lang,
        "audience": args.audience,
        "repo_root": str(repo_root),
        "totals": {
            "files": len(targets),
            "changed_files": changed_files,
            "changes": changes,
        },
        "files": results,
    }

    report_path = Path(args.report)
    if not report_path.is_absolute():
        report_path = repo_root / report_path
    report_path.parent.mkdir(parents=True, exist_ok=True)
    report_path.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")

    print(
        f"[comment-enhancer] mode={report['mode']} files={len(targets)} "
        f"changed_files={changed_files} changes={changes} report={report_path}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
