# Repository Guidelines

## Project Structure & Module Organization
- Java multi-module Maven project at repo root (`pom.xml`).
  - `zriyo-app/`, `zriyo-service/`, `zriyo-model/`, `zriyo-common/` contain Java source under `src/main/java`.
- Python AI server in `python-ai-server/` with FastAPI code in `app/` and tests in `tests/`.
- Frontend scaffold in `frontend-scaffold/` (Vite + Vue) with `src/` and `package.json`.
- Documentation lives in `docs/`; CodeAgent skill docs live in `skills/codeagent/`.
- Skill docs follow the structure `skills/codeagent/code/<skill>/SKILL.md`.

## Build, Test, and Development Commands
- Java: `mvn -q -pl zriyo-app -am package`, `mvn -pl zriyo-app spring-boot:run`
- Python: `pip install -r python-ai-server/requirements.txt`, `cd python-ai-server && uvicorn app.main:app --reload`, `pytest`
- Frontend: `cd frontend-scaffold && npm run dev`, `npm run build`

## Coding Style & Naming Conventions
- Java: 4-space indentation; clear service/controller naming.
- Python: `black` (line length 100), `ruff`, optional `mypy`.
- Vue/TS: `PascalCase.vue`, utilities `kebab-case.ts`.
- Skill files: `skills/codeagent/code/<skill>/SKILL.md` with YAML front-matter.

## Testing Guidelines
- Python tests use `pytest` with coverage configured in `python-ai-server/pyproject.toml`.
- Test files: `python-ai-server/tests/test_*.py`.
- Java tests follow Maven defaults (if present): `mvn -pl zriyo-app -am test`.

## Commit & Pull Request Guidelines
- Conventional Commits (e.g., `feat: ...`, `fix: ...`, `refactor: ...`).
- PRs: summary, linked issue/requirement, tests run, UI screenshots if applicable.

## Architecture & References
- High-level architecture and agent design: `docs/ARCHITECTURE_V2.md`.
- Project summary and session-memory format: `docs/PROJECT_SUMMARY.md`.
- When context remaining is ~30%, record key actions/decisions/todos in `docs/SESSION_MEMORY.md`.

## Architecture Summary
- Java orchestrates intent, routing, context, tracing, and quotas.
- Python is stateless execution (tools/services) and streams results via SSE.
- Pipeline: plan → generate → check → preview → feedback/repair.
- Skills are agent-scoped and loaded on demand.
- First write is full, subsequent updates use diff patches.
- Model routing, version rollback, and BaaS are planned extensions.

## ARCHITECTURE_V2 落地要点（来源：`docs/ARCHITECTURE_V2.md`）
- 总体目标：构建通用 Agent 系统，覆盖代码生成/修复、文档分析、图表生成、部署、BaaS 数据能力，并预留视频笔记与网页实时总结问答能力。
- 架构分工：遵循 **Java 75% 编排 + Python 25% 工具服务**。
  - Java：意图识别、任务路由、Agent 循环、上下文管理、追踪、配置/密钥管理、业务编排。
  - Python：无状态执行层（FastAPI + Tool 服务），专注执行能力与流式结果返回。
- 关键原则：
  - Python 侧不持久化 API Key/Token/配置。
  - Java 侧统一管理配置，并通过 JWT 透传必要上下文。
  - 全链路必须具备 traceId，支持问题追溯。
  - 新能力优先通过新增 Agent/Tool 扩展，避免破坏主架构。
- 通信协议：Java ↔ Python 使用 HTTP + SSE（流式），并结合 JWT 校验。
- 计划与执行：
  - 支持 PlanGate（小改动可跳过规划，大改动先规划）。
  - 规划采用双模型：
    - Plan A：给用户看的 Markdown 计划（可流式输出）。
    - Plan B：系统执行 JSON 计划（files/dependencies/execution_order/checks）。
  - Java 维护 step_status（pending/in_progress/completed）并通过 SSE 同步。
- 可靠性要求：幂等、重试（指数退避）、SSE 断线恢复、应用级限流隔离（默认万级）。
- 路由与 Agent 协作：
  - 先识别意图（CODE/DOC/CHART/DEPLOY/DEBUG/TEST 等）再路由到专项 Agent。
  - 支持顺序执行与并行执行，由 TeamLead/编排层负责聚合与冲突处理。

## Python 注释规范（面向 Java 开发者）
- 目标：让 Java 同学快速理解 Python 侧职责边界与调用链，不改变业务逻辑。
- 必加位置：
  - 模块头（模块职责 + Java 对照）
  - 路由入口（FastAPI handler）
  - 核心类与公开函数
  - SSE / 异步流程关键方法
- 注释内容要求：
  - 说明职责、输入输出、关键阶段（如 select/read/generate/write）
  - 使用 Java 可理解术语（Controller/Service/Adapter/Facade 等）
  - 避免“翻译代码式”空洞注释
- 禁止项：
  - 不写无意义注释（如“初始化变量”）
  - 不改业务逻辑
  - 不覆盖已有高质量 docstring（详细且结构化的注释）
- 自动化执行脚本：
  - 文件：`python-ai-server/scripts/comment_enhancer.py`
  - 范围：固定核心清单（`--scope core`）
  - 语言与受众：`--lang zh --audience java`
- 推荐执行方式（先预演后落盘）：
  - Dry run：
    `./python-ai-server/venv/bin/python ./python-ai-server/scripts/comment_enhancer.py --scope core --dry-run --report ./tmp/comment-enhancer-dry-run.json --lang zh --audience java`
  - Apply：
    `./python-ai-server/venv/bin/python ./python-ai-server/scripts/comment_enhancer.py --scope core --apply --report ./tmp/comment-enhancer-report.json --lang zh --audience java`
  - 一次性后台执行（产生日志 + 报告）：
    `nohup ./python-ai-server/venv/bin/python ./python-ai-server/scripts/comment_enhancer.py --scope core --apply --report ./tmp/comment-enhancer-report.json --lang zh --audience java > ./logs/comment_enhancer.log 2>&1 &`
