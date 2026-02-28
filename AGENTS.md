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
