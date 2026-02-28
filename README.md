<div align="center">

# Zriyo AI Code Mother

**通用智能体平台（重构中）**

从“前端代码生成器”升级为：

**Java 编排 + Python 无状态执行 + 多技能（Skills）驱动** 的通用 Agent 系统。

</div>

---

## 1. 项目定位

> 这是一个正在进行大重构的项目。当前目标是构建“可扩展的通用智能体平台”，而不只是单一代码生成工具。

核心思路：

- **Java 侧负责编排与状态**：任务路由、流程状态、SSE 事件、持久化
- **Python 侧负责执行能力**：规划、文件工具、代码生成、流式输出
- **Skills 负责知识/规范注入**：按需加载、最小上下文

---

## 2. 当前架构（重构版）

### 2.1 模块结构

```text
zriyo-ai-code-mother/
├── zriyo-app/            # Java 启动模块（Controller/Config/Python 集成）
├── zriyo-service/        # Java 服务层（Mapper/Workflow Action）
├── zriyo-model/          # Java DTO/Entity
├── zriyo-common/         # Java 通用组件（SSE/Result/工具）
├── python-ai-server/     # Python FastAPI 执行层
├── frontend-scaffold/    # 前端脚手架模板（Vite + Vue3）
├── skills/               # Skill 文档体系
├── sql/                  # 数据库脚本（含 workflow_core）
└── docs/                 # 架构与重构设计文档
```

### 2.2 设计原则

1. **职责分离**：编排（Java）与执行（Python）解耦
2. **无状态执行**：Python 尽量无状态，方便扩展和重试
3. **事件驱动**：关键阶段通过 SSE 透出
4. **安全边界**：文件操作限定项目目录，防路径穿越
5. **增量演进**：先可用，再补齐完整 Agent 协作能力

---

## 3. 已落地能力（以当前代码为准）

### 3.1 Java 侧

- Python 进程自动拉起与健康探测
- Java → Python 的内部 HTTP 调用封装
- 任务取消接口（转发到 Python）
- Workflow 基础实体与 Mapper（`workflow_def` / `project_session` 等）

### 3.2 Python 侧

- FastAPI 基础服务与健康检查
- 规划接口（`/api/v1/plan/create`）
- 项目接口（`/api/v1/project/*`）
- 文件工具：读取 / 列表 / 搜索 / patch
- CodeAgent：单文件生成 + SSE 流式返回 + 协作式取消
- Token 计数能力（多 provider 兼容）

### 3.3 Skills

- `skills/codeagent` 下已建立技能索引与多类技能文档
- 支持按需求动态选择技能注入上下文

---

## 4. 当前状态说明（重要）

项目处于**重构进行中**，并非全部完成：

- 部分 API 仍是占位实现（例如通用 `agent-run`/`chat`）
- 前端页面为脚手架基础能力，后续会重构 UI 与交互
- 测试体系在补齐中（已有 Python 测试，但尚未全绿）

如果你希望一个“稳定生产版本”，请关注后续发布分支与里程碑。

---

## 5. 快速开始

## 5.1 环境要求

- Java 21
- Maven 3.9+
- Python 3.11+
- Node.js 18+
- MySQL 8+

### 5.2 Java 后端

```bash
mvn -q -pl zriyo-app -am package
mvn -pl zriyo-app spring-boot:run
```

### 5.3 Python 执行层

```bash
pip install -r python-ai-server/requirements.txt
cd python-ai-server
uvicorn app.main:app --reload
```

### 5.4 前端脚手架

```bash
cd frontend-scaffold
npm install
npm run dev
```

---

## 6. 数据库脚本

推荐先看：

- `sql/workflow_core.sql`（新工作流核心表）
- `sql/zriyo_ai_code_mother.sql`（综合脚本）
- `sql/drop_deprecated_tables.sql`（清理历史表）

---

## 7. 关键文档

- 架构总览：`docs/ARCHITECTURE_V2.md`
- 项目摘要：`docs/PROJECT_SUMMARY.md`
- 计划执行机制：`docs/PLAN_EXECUTION.md`
- 工作流核心：`docs/WORKFLOW_CORE.md`

---

## 8. 开发约定（简版）

- 提交信息使用 Conventional Commits（`feat:` / `fix:` / `refactor:`）
- Java / Python / 前端代码按各模块既定风格
- 新增 Tool/Skill 时请补充对应文档与示例

---

## 9. Roadmap（重构方向）

1. 完整通用 Agent 编排（多 Agent 协作）
2. 统一计划-执行-回放链路
3. 更完整的 Tool 白名单与权限控制
4. 前端工作台重构（计划视图、执行态、预览态）
5. 可观测性与测试覆盖提升

---

## 10. License

MIT
