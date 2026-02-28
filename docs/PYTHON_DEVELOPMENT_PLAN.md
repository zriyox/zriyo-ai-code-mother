# Python AI Server 开发规划文档

## 文档版本

| 版本 | 日期 | 作者 | 说明 |
|--------|--------|-------|------|
| v1.0 | 2025-02-15 | Claude | Python 侧开发规划（混合模式）|
| v1.1 | 2025-02-16 | Claude | 添加 LangChain 集成方案 |
| v1.2 | 2025-02-16 | Claude | 添加 Token 计算与上下文管理 |

---

## 一、架构定位

### 1.1 职责划分

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        Java 侧 vs Python 侧                                │
│                                                                             │
│  ┌─────────────────────────────────────┐         ┌─────────────────────────┐│
│  │         Java 侧                     │         │      Python 侧          ││
│  │                                     │         │                         ││
│  │  管理层（状态）                     │         │  执行层（无状态）        ││
│  │                                     │         │                         ││
│  │  ✅ 权限验证（JWT）                 │         │  ✅ 代码生成             ││
│  │  ✅ 配额管理                         │         │  ✅ 代码分析             ││
│  │  ✅ 变更记录/追溯                   │         │  ✅ 文件搜索             ││
│  │  ✅ Key 轮换                         │         │  ✅ 文件读写             ││
│  │  ✅ 限流                             │         │  ✅ 文档解析             ││
│  │  ✅ 会话状态管理                     │         │  ✅ 图表生成             ││
│  │                                     │         │  ✅ 数据分析             ││
│  └─────────────────────────────────────┘         └─────────────────────────┘│
│                                                                             │
│  共享文件系统:                                                            │
│  /app/projects/                                                            │
│    ├── app_001/               ← Python 直接读写                              │
│    └── app_002/                                                            │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 1.2 核心原则

| 原则 | Python | Java |
|------|--------|------|
| **计算 vs 管理** | Python 做计算 | Java 做管理 |
| **执行 vs 记录** | Python 执行任务 | Java 记录结果 |
| **快速失败** | 出错直接抛异常 | Java 决定是否重试 |

### 1.3 功能分工表

| 功能 | 负责方 | 说明 |
|------|--------|------|
| **文件读取** | Python | 直接读共享目录 |
| **文件写入** | Python | 直接写共享目录 |
| **代码生成** | Python | LLM 生成 |
| **代码解析** | Python | tree-sitter |
| **代码搜索** | Python | ripgrep |
| **文档解析** | Python | PyPDF2, python-docx |
| **图表生成** | Python | matplotlib, plotly |
| **权限验证** | Java | JWT 验证 |
| **配额管理** | Java | 限制使用量 |
| **变更记录** | Java | Python 写入后通知 Java |
| **Key 轮换** | Java | 多 Key 管理 |
| **限流** | Java | 频率控制 |

---

## 二、技术栈

### 2.1 核心依赖

```txt
# Web 框架
fastapi==0.104.1
uvicorn[standard]==0.24.0
pydantic==2.5.0

# === LangChain 框架 ===
langchain==0.1.20
langchain-openai==0.0.5
langchain-anthropic==0.1.11
langchain-mcp-adapters==0.0.3    # MCP Server 支持
langchain-community==0.0.38      # 社区扩展

# Token 计算
tiktoken==0.5.2                  # OpenAI token 计数

# LLM SDK
openai==1.12.0
anthropic==0.18.1

# JWT 验证
python-jose[cryptography]==3.3.1

# 文件操作
pathspec==0.12.0              # 模式匹配

# 代码分析
tree-sitter==0.21.0           # 多语言代码解析

# 文档处理
pandas==2.2.0
numpy==1.26.0
openpyxl==3.1.2
python-docx==1.1.0
PyPDF2==3.0.1
pdfplumber==0.10.3

# 图表生成
matplotlib==3.8.0
plotly==5.18.0
kaleido==0.2.1

# 日志
loguru==0.7.2
```

### 2.2 系统依赖

```bash
# ripgrep - 超快的文本搜索
apt-get install ripgrep
```

---

## 三、项目结构

```
python-ai-server/
├── app/
│   ├── main.py                        # FastAPI 入口
│   │
│   ├── config/
│   │   └── settings.py                # 配置
│   │
│   ├── middleware/
│   │   └── jwt_validator.py           # JWT 验证
│   │
│   ├── models/
│   │   ├── request.py                 # 请求模型
│   │   ├── event.py                   # SSE 事件模型
│   │   └── result.py                  # 结果模型
│   │
│   ├── api/
│   │   ├── agent.py                   # /api/v1/agent-run
│   │   └── internal.py                # /api/internal/* (供 Java 调用)
│   │
│   ├── agent/                         # Agent 系统
│   │   ├── base.py                    # Agent 基类（基于 LangChain）
│   │   ├── code_agent.py
│   │   ├── doc_agent.py
│   │   └── chart_agent.py
│   │
│   ├── langchain/                     # LangChain 集成
│   │   ├── tools.py                   # LangChain Tools 定义
│   │   ├── mcp.py                      # MCP Server 集成
│   │   ├── llm_factory.py             # LangChain LLM 工厂
│   │   └── agents.py                  # LangChain Agent 包装
│   │
│   ├── tools/                         # Tool 系统（保留自定义实现）
│   │   ├── base.py                    # Tool 基类
│   │   │
│   │   ├── file/                      # 文件操作
│   │   │   ├── read.py                # 文件读取
│   │   │   ├── write.py               # 文件写入
│   │   │   └── search.py              # 搜索（ripgrep）
│   │   │
│   │   ├── code/                      # 代码相关
│   │   │   ├── parse.py               # 代码解析（tree-sitter）
│   │   │   ├── analyze.py             # 代码分析
│   │   │   └── generate.py            # 代码生成
│   │   │
│   │   ├── doc/                       # 文档处理
│   │   │   ├── docx.py
│   │   │   ├── pdf.py
│   │   │   └── excel.py
│   │   │
│   │   └── chart/                     # 图表生成
│   │       ├── matplotlib.py
│   │       ├── plotly.py
│   │       └── pandas.py
│   │
│   ├── llm/
│   │   ├── factory.py                 # LLM 客户端工厂
│   │   ├── openai.py
│   │   └── anthropic.py
│   │
│   ├── utils/
│   │   ├── sse.py                     # SSE 事件推送
│   │   ├── errors.py                  # 异常定义
│   │   ├── file.py                    # 文件系统工具
│   │   └── token.py                   # Token 计算工具
│   │
│   └── prompts/
│       ├── code.txt
│       ├── doc.txt
│       └── chart.txt
│
├── projects/                          # 共享目录（挂载点）
│   ├── app_001/
│   └── app_002/
│
├── requirements.txt
├── pyproject.toml
└── README.md
```

---

## 四、通信协议

### 4.1 Java → Python 请求

```python
from pydantic import BaseModel
from typing import Optional


class LlmConfig(BaseModel):
    """LLM 配置"""
    provider: str
    model: str
    api_key: str
    temperature: float = 0.7
    max_tokens: int = 4000


class AgentRequest(BaseModel):
    """Agent 执行请求"""
    request_id: str
    trace_id: str
    user_id: int
    app_id: int

    task_type: str                 # "CODE_GEN" | "DOC_PARSE" | "CHART_DRAW" | "CODE_ANALYZE"
    message: str

    # 可选：指定操作的文件
    target_files: Optional[list[str]] = None

    # 项目上下文
    project_context: Optional[dict] = None
    conversation: Optional[list[dict]] = None

    llm_config: LlmConfig
```

### 4.2 Python → Java SSE 事件

```python
from enum import Enum


class EventType(str, Enum):
    # 生命周期
    START = "start"
    PROGRESS = "progress"
    COMPLETE = "complete"
    ERROR = "error"

    # Agent 事件
    AGENT_START = "agent_start"
    AGENT_THINKING = "agent_thinking"

    # Tool 事件
    TOOL_CALL = "tool_call"
    TOOL_RESULT = "tool_result"

    # 文件操作事件（通知 Java 记录）
    FILE_READ = "file_read"
    FILE_WRITTEN = "file_written"


class AgentEvent(BaseModel):
    """统一事件格式"""
    trace_id: str
    event_type: EventType
    timestamp: int
    data: dict
```

### 4.3 完成响应

```python
class AgentResult(BaseModel):
    """Agent 执行结果"""
    request_id: str
    trace_id: str
    status: str                    # "success" | "failed"

    # 生成/修改的文件（通知 Java 记录）
    files: list[dict] = []         # [{"path": "...", "action": "created|modified"}]

    # 其他结果
    data: dict = {}                # 根据任务类型不同而不同
    error: str = None
```

---

### 4.4 前端生成与预览接口补充（规划）

为支持“规划 → 生成 → 预览”闭环，新增前端生成相关接口，详细示例见：

- `docs/API_SPEC_PLAN.md`

核心接口（规划）：
1. `POST /api/v1/plan/create`：生成项目规划
2. `POST /api/v1/project/generate`：生成脚手架 + 自动按需读取 skills
3. `POST /api/v1/project/code/generate`：生成单文件代码（或返回 system_prompt）
4. `POST /api/v1/project/code/write`：写入文件
5. `POST /api/v1/preview/run`：预览构建 + 运行时异常采集

扩展字段：
1. `skills`: 不再由 Java 传入，Python 侧自动按需匹配
2. `baas`: 通用后端能力注入（首期 Supabase）


## 五、文件系统工具

### 5.1 项目文件系统

```python
# app/utils/file.py

from pathlib import Path


class ProjectFileSystem:
    """项目文件系统"""

    # 共享目录基础路径
    BASE_PATH = Path("/app/projects")

    @classmethod
    def get_project_path(cls, app_id: int) -> Path:
        """获取项目根目录"""
        return cls.BASE_PATH / f"app_{app_id:06d}"

    @classmethod
    def resolve_path(cls, app_id: int, file_path: str) -> Path:
        """解析文件路径，防止路径穿越"""
        project_path = cls.get_project_path(app_id)
        resolved = (project_path / file_path).resolve()

        # 安全检查
        try:
            resolved.relative_to(project_path)
        except ValueError:
            raise PermissionError(f"Path traversal detected: {file_path}")

        return resolved

    @classmethod
    async def read_file(cls, app_id: int, file_path: str) -> str:
        """读取文件"""
        path = cls.resolve_path(app_id, file_path)
        if not path.exists():
            raise FileNotFoundError(f"File not found: {file_path}")
        return path.read_text(encoding="utf-8")

    @classmethod
    async def write_file(cls, app_id: int, file_path: str, content: str):
        """写入文件"""
        path = cls.resolve_path(app_id, file_path)
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content, encoding="utf-8")

    @classmethod
    async def list_files(cls, app_id: int, pattern: str = "**/*") -> list[dict]:
        """列出文件"""
        from pathspec import PathSpec

        project_path = cls.get_project_path(app_id)
        pathspec = PathSpec.from_lines("gitwildmatch", [pattern])

        files = []
        for path in project_path.rglob("*"):
            if path.is_file():
                rel_path = path.relative_to(project_path)
                if pathspec.match_file(str(rel_path)):
                    files.append({
                        "path": str(rel_path),
                        "size": path.stat().st_size
                    })

        return files
```

---

## 六、核心 Tool 设计

### 6.1 文件读取 Tool

```python
# app/tools/file/read.py

from app.tools.base import BaseTool
from app.utils.file import ProjectFileSystem


class FileReadTool(BaseTool):
    """文件读取工具"""

    name = "file_read"
    description = "读取项目文件内容"

    async def execute(
        self,
        app_id: int,
        file_path: str
    ) -> dict:
        """
        读取文件

        Args:
            app_id: 应用 ID
            file_path: 文件路径

        Returns:
            文件内容
        """
        content = await ProjectFileSystem.read_file(app_id, file_path)

        return {
            "path": file_path,
            "content": content,
            "size": len(content)
        }
```

### 6.2 文件写入 Tool

```python
# app/tools/file/write.py

from app.tools.base import BaseTool
from app.utils.file import ProjectFileSystem


class FileWriteTool(BaseTool):
    """文件写入工具"""

    name = "file_write"
    description = "写入文件内容"

    async def execute(
        self,
        app_id: int,
        file_path: str,
        content: str
    ) -> dict:
        """
        写入文件

        Args:
            app_id: 应用 ID
            file_path: 文件路径
            content: 文件内容

        Returns:
            写入结果
        """
        path = ProjectFileSystem.resolve_path(app_id, file_path)
        existed = path.exists()

        await ProjectFileSystem.write_file(app_id, file_path, content)

        return {
            "path": file_path,
            "action": "modified" if existed else "created",
            "size": len(content)
        }
```

### 6.3 文件搜索 Tool（ripgrep）

```python
# app/tools/file/search.py

import subprocess
from app.tools.base import BaseTool
from app.utils.file import ProjectFileSystem


class FileSearchTool(BaseTool):
    """文件搜索工具 - 使用 ripgrep"""

    name = "file_search"
    description = "在项目中搜索文件内容，支持正则表达式"

    async def execute(
        self,
        app_id: int,
        pattern: str,
        file_pattern: str = "*",
        case_sensitive: bool = False,
        context_lines: int = 2
    ) -> list[dict]:
        """使用 ripgrep 搜索"""
        project_path = ProjectFileSystem.get_project_path(app_id)

        cmd = [
            "rg", pattern, str(project_path),
            "-g", file_pattern,
            "-C", str(context_lines),
            "--json"
        ]

        if not case_sensitive:
            cmd.append("-i")

        result = subprocess.run(cmd, capture_output=True, text=True, timeout=30)

        if result.returncode != 0:
            return []

        matches = []
        for line in result.stdout.split("\n"):
            if line.strip():
                import json
                data = json.loads(line)
                if data.get("type") == "match":
                    matches.append({
                        "path": _relative_path(data["data"]["path"]["text"], project_path),
                        "line": data["data"]["line_number"],
                        "content": data["data"]["lines"]["text"]
                    })

        return matches
```

### 6.4 代码解析 Tool（tree-sitter）

```python
# app/tools/code/parse.py

from tree_sitter import Language, Parser
from app.tools.base import BaseTool


class CodeParseTool(BaseTool):
    """代码解析工具"""

    name = "code_parse"
    description = "解析代码结构，提取函数、类、导入等"

    _parsers: dict[str, Parser] = {}

    def __init__(self):
        self._init_parsers()

    def _init_parsers(self):
        """初始化语言解析器"""
        PY_LANG = Language("/build/languages.so", "python")
        self._parsers["python"] = Parser(PY_LANG)
        self._parsers["py"] = Parser(PY_LANG)

        JS_LANG = Language("/build/languages.so", "javascript")
        self._parsers["javascript"] = Parser(JS_LANG)
        self._parsers["typescript"] = Parser(JS_LANG)
        self._parsers["js"] = Parser(JS_LANG)
        self._parsers["tsx"] = Parser(JS_LANG)

        JAVA_LANG = Language("/build/languages.so", "java")
        self._parsers["java"] = Parser(JAVA_LANG)

    async def execute(
        self,
        app_id: int,
        file_path: str
    ) -> dict:
        """解析代码文件"""
        from app.utils.file import ProjectFileSystem

        path = ProjectFileSystem.resolve_path(app_id, file_path)
        content = path.read_text()

        ext = self._get_extension(file_path)
        parser = self._parsers.get(ext)

        if not parser:
            return {"error": f"Unsupported language: {ext}"}

        tree = parser.parse(bytes(content, "utf8"))

        return {
            "path": file_path,
            "language": ext,
            "functions": self._extract_functions(tree),
            "classes": self._extract_classes(tree),
            "imports": self._extract_imports(tree)
        }
```

### 6.5 代码生成 Tool

```python
# app/tools/code/generate.py

from app.tools.base import BaseTool
from app.llm.factory import create_client


class CodeGenerateTool(BaseTool):
    """代码生成工具"""

    name = "code_generate"
    description = "根据需求生成代码"

    async def execute(
        self,
        requirement: str,
        context_files: list[dict] = None,
        llm_config: "LlmConfig" = None
    ) -> dict:
        """生成代码"""
        llm = create_client(llm_config)

        # 构建 prompt
        prompt = self._build_prompt(requirement, context_files)

        # 调用 LLM
        response = await llm.chat(prompt, stream=False)

        # 解析生成的代码
        return self._parse_response(response)
```

### 6.6 文档解析 Tool

```python
# app/tools/doc/pdf.py

import PyPDF2
from app.tools.base import BaseTool


class PdfParseTool(BaseTool):
    """PDF 解析工具"""

    name = "pdf_parse"
    description = "解析 PDF 文档，提取文本和结构"

    async def execute(
        self,
        app_id: int,
        file_path: str
    ) -> dict:
        """解析 PDF"""
        from app.utils.file import ProjectFileSystem

        path = ProjectFileSystem.resolve_path(app_id, file_path)

        with open(path, "rb") as f:
            reader = PyPDF2.PdfReader(f)

            pages = []
            for i, page in enumerate(reader.pages):
                text = page.extract_text()
                if text.strip():
                    pages.append({
                        "page": i + 1,
                        "text": text
                    })

            metadata = reader.metadata or {}

            return {
                "path": file_path,
                "pages": len(pages),
                "content": pages,
                "metadata": {
                    "title": metadata.get("/Title", ""),
                    "author": metadata.get("/Author", "")
                }
            }
```

### 6.7 图表生成 Tool

```python
# app/tools/chart/plotly.py

import plotly.graph_objects as go
from app.tools.base import BaseTool


class PlotlyChartTool(BaseTool):
    """Plotly 图表生成工具"""

    name = "plotly_chart"
    description = "生成交互式图表"

    async def execute(
        self,
        app_id: int,
        chart_type: str,
        data: dict,
        config: dict = None,
        output_path: str = None
    ) -> dict:
        """生成图表"""
        chart_type = chart_type.lower()

        if chart_type == "line":
            fig = go.Figure(data=go.Scatter(**data))
        elif chart_type == "bar":
            fig = go.Figure(data=go.Bar(**data))
        elif chart_type == "pie":
            fig = go.Figure(data=go.Pie(**data))
        else:
            raise ValueError(f"Unknown chart type: {chart_type}")

        if config:
            fig.update_layout(**config)

        # 如果指定了输出路径，保存图片
        if output_path:
            from app.utils.file import ProjectFileSystem
            path = ProjectFileSystem.resolve_path(app_id, output_path)
            fig.write_image(str(path))

        return {
            "type": chart_type,
            "data": fig.to_dict(),
            "output_path": output_path
        }
```

---

## 七、Agent 编排架构（重要）

### 7.1 单 Agent vs 多 Agent 职责

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        Agent 编排分工                                        │
│                                                                             │
│  ┌─────────────────────────────────────┐         ┌─────────────────────────┐│
│  │         Java 侧（编排层）           │         │      Python 侧          ││
│  │                                     │         │      （执行层）          ││
│  │                                     │         │                         ││
│  │  ┌─────────────────────────────┐   │         │  ┌───────────────────┐ ││
│  │  │  AgentOrchestrator         │   │         │  │   CodeAgent       │ ││
│  │  │  - 意图识别                 │   │         │  │   DocAgent        │ ││
│  │  │  - 任务路由                 │   │  调用  │  │   ChartAgent      │ ││
│  │  │  - Agent 切换决策           │   │  ────→ │  │                   │ ││
│  │  │  - 执行计划                 │   │         │  │  单 Agent 执行      │ ││
│  │  │  - 结果聚合                 │   │         │  │  返回结果          │ ││
│  │  └─────────────────────────────┘   │         │  └───────────────────┘ ││
│  │                                     │         │                         ││
│  │  决定：调用哪个 Agent？                                               ││
│  │  决定：什么时候切换 Agent？                                           ││
│  │  决定：Agent 之间的依赖关系                                           ││
│  │  记录：Agent 执行历史和状态                                           ││
│  │                                     │         │                         ││
│  └─────────────────────────────────────┘         └─────────────────────────┘│
└─────────────────────────────────────────────────────────────────────────────┘
```

### 7.2 编排流程示例

```
用户: "分析这个项目的代码，然后生成统计图表"
         ↓
┌─────────────────────────────────────────────────────────────────────────────┐
│  Java 侧 - AgentOrchestrator                                              │
│                                                                             │
│  1. 意图识别：用户需求包含两个任务                                          │
│     - 分析代码 → CODE_ANALYZE → CodeAgent                                │
│     - 生成图表 → CHART_GEN → ChartAgent                                  │
│                                                                             │
│  2. 生成执行计划：                                                       │
│     Step 1: 调用 CodeAgent 分析代码                                        │
│     Step 2: 调用 ChartAgent 生成图表（依赖 Step1 结果）                      │
│                                                                             │
│  3. 执行 Step 1 → POST /api/v1/agent-run                                │
│     {"task_type": "CODE_ANALYZE", "app_id": 1, ...}                       │
└─────────────────────────────────────────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────────────────────────────────────────┐
│  Python 侧 - CodeAgent                                                     │
│                                                                             │
│  - 读取项目文件                                                          │
│  - 使用 tree-sitter 解析代码                                             │
│  - 分析代码结构                                                          │
│  - 返回分析结果（SSE 事件流）                                           │
└─────────────────────────────────────────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────────────────────────────────────────┐
│  Java 侧 - AgentOrchestrator                                              │
│                                                                             │
│  4. 收到 CodeAgent 结果                                                    │
│  5. 提取数据分析传递给下一步                                              │
│  6. 执行 Step 2 → POST /api/v1/agent-run                                │
│     {"task_type": "CHART_GEN", "app_id": 1, "data": {...}}                   │
└─────────────────────────────────────────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────────────────────────────────────────┐
│  Python 侧 - ChartAgent                                                    │
│                                                                             │
│  - 接收分析结果数据                                                       │
│  - 使用 pandas/matplotlib 生成图表                                       │
│  - 保存图表文件到共享目录                                                │
│  - 返回图表路径（SSE 事件流）                                            │
└─────────────────────────────────────────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────────────────────────────────────────┐
│  Java 侧 - AgentOrchestrator                                              │
│                                                                             │
│  7. 聚合最终结果返回给用户                                                  │
│  8. 记录 Agent 执行历史                                                    │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 7.3 职责总结

| 职责 | 位置 | 说明 |
|------|------|------|
| **单 Agent 执行逻辑** | Python | CodeAgent/DocAgent/ChartAgent 具体实现 |
| **多 Agent 编排** | Java | 意图识别、任务路由、Agent 切换、结果聚合 |
| **Agent 状态管理** | Java | 记录执行历史、状态切换、依赖关系 |
| **Agent 工具调用** | Python | Agent 内部调用 Tool 完成任务 |

### 7.4 Python 侧不做什么

| ❌ 不做 | 原因 |
|--------|------|
| **Agent 之间的切换决策** | 由 Java 的 AgentOrchestrator 决定 |
| **执行计划生成** | 由 Java 的 PlannerAgent 生成 |
| **Agent 结果聚合** | 由 Java 的 AgentOrchestrator 聚合 |
| **Agent 状态管理** | 由 Java 维护，Python 只负责执行 |
| **任务依赖管理** | 由 Java 处理，Python 按指令执行 |

### 7.5 Python Agent 的设计原则

```
Python Agent 是"无状态执行器"：

1. 接收明确的任务指令（task_type + message）
2. 执行任务，通过 SSE 流式推送进度
3. 返回执行结果
4. 不关心"下一步做什么"（由 Java 决定）
5. 不关心"为什么执行"（由 Java 决定）
```

---

## 八、LangChain 集成

### 8.1 为什么使用 LangChain

Python 侧采用 **LangChain** 作为 Agent 执行框架，主要优势：

| 功能 | LangChain 能力 | 说明 |
|---|---|---|
| **Tool 管理** | `@tool` 装饰器 | 简化 Tool 定义和注册 |
| **Agent 执行** | `create_tool_calling_agent` | 开箱即用的 Tool 调用 Agent |
| **MCP 集成** | `langchain-mcp-adapters` | 原生支持 MCP Server |
| **SSE 流式** | `astream_events()` | 完美支持事件流推送 |
| **多 LLM** | 统一接口 | OpenAI / Anthropic / 其他 |

### 8.2 架构分工

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        Java 侧 vs Python 侧 (LangChain)                       │
│                                                                             │
│  ┌─────────────────────────────────────┐         ┌─────────────────────────┐│
│  │         Java 侧                     │         │      Python 侧          ││
│  │                                     │         │      (LangChain)        ││
│  │  编排层                              │         │      执行层              ││
│  │  ┌─────────────────────────────┐   │         │  ┌───────────────────┐ ││
│  │  │  AgentOrchestrator         │   │         │  │   LangChain       │ ││
│  │  │  - 意图识别                 │   │  调用  │  │   - Tools         │ ││
│  │  │  - 任务路由                 │   │  ────→ │  │   - Agents        │ ││
│  │  │  - Agent 切换决策           │   │         │  │   - MCP Adapters  │ ││
│  │  └─────────────────────────────┘   │         │  └───────────────────┘ ││
│  │                                     │         │                         ││
│  │  决定：调用哪个 Agent？                                               ││
│  │  决定：Agent 之间的依赖关系                                           ││
│  │  记录：Agent 执行历史和状态                                           ││
│  └─────────────────────────────────────┘         └─────────────────────────┘│
└─────────────────────────────────────────────────────────────────────────────┘
```

### 8.3 Python 侧不做什么

| ❌ 不做 | 原因 |
|--------|------|
| **多 Agent 编排** | 由 Java 的 AgentOrchestrator 负责 |
| **Agent 之间切换** | 由 Java 决定调用哪个 Agent |
| **执行计划生成** | 由 Java 的 PlannerAgent 生成 |
| **Agent 状态管理** | 由 Java 维护，Python 保持无状态 |

### 8.4 LLM 工厂（LangChain）

```python
# app/langchain/llm_factory.py

from langchain_openai import ChatOpenAI
from langchain_anthropic import ChatAnthropic
from app.models.request import LlmConfig

def create_langchain_llm(config: LlmConfig):
    """创建 LangChain LLM 实例"""

    if config.provider == "openai":
        return ChatOpenAI(
            model=config.model,
            api_key=config.api_key,
            temperature=config.temperature,
            max_tokens=config.max_tokens,
        )

    elif config.provider == "anthropic":
        return ChatAnthropic(
            model=config.model,
            api_key=config.api_key,
            temperature=config.temperature,
            max_tokens=config.max_tokens,
        )

    elif config.provider == "codex":
        # 使用 OpenAI 的代码生成能力
        return ChatOpenAI(
            model="gpt-4-turbo",  # 或 gpt-4-codex
            api_key=config.api_key,
            temperature=0.2,  # 代码生成用较低温度
        )

    else:
        raise ValueError(f"Unsupported provider: {config.provider}")
```

### 8.5 LangChain Tools 定义

```python
# app/langchain/tools.py

from langchain.tools import tool
from app.utils.file import ProjectFileSystem
import subprocess
from pathlib import Path

@tool
def file_read(file_path: str, app_id: int) -> str:
    """读取项目文件内容

    Args:
        file_path: 文件相对路径
        app_id: 应用 ID

    Returns:
        文件内容
    """
    return ProjectFileSystem.read_file(app_id, file_path)

@tool
def file_write(file_path: str, content: str, app_id: int) -> dict:
    """写入文件内容

    Args:
        file_path: 文件相对路径
        content: 文件内容
        app_id: 应用 ID

    Returns:
        写入结果 {path, action, size}
    """
    path = ProjectFileSystem.resolve_path(app_id, file_path)
    existed = path.exists()

    ProjectFileSystem.write_file_sync(app_id, file_path, content)

    return {
        "path": file_path,
        "action": "modified" if existed else "created",
        "size": len(content)
    }

@tool
def file_search(pattern: str, app_id: int, case_sensitive: bool = False) -> list[dict]:
    """在项目中搜索文件内容（使用 ripgrep）

    Args:
        pattern: 搜索模式（支持正则）
        app_id: 应用 ID
        case_sensitive: 是否区分大小写

    Returns:
        匹配结果列表
    """
    project_path = ProjectFileSystem.get_project_path(app_id)

    cmd = ["rg", pattern, str(project_path), "-C", "2", "--json"]
    if not case_sensitive:
        cmd.append("-i")

    result = subprocess.run(cmd, capture_output=True, text=True, timeout=30)

    if result.returncode != 0:
        return []

    matches = []
    for line in result.stdout.split("\n"):
        if line.strip():
            import json
            data = json.loads(line)
            if data.get("type") == "match":
                matches.append({
                    "path": str(Path(data["data"]["path"]["text"]).relative_to(project_path)),
                    "line": data["data"]["line_number"],
                    "content": data["data"]["lines"]["text"]
                })

    return matches
```

### 8.6 LangChain Agent 执行

```python
# app/langchain/agents.py

from langchain.agents import create_tool_calling_agent
from langchain_core.messages import HumanMessage
from langchain_openai import ChatOpenAI
from app.models.request import AgentRequest
from app.langchain.tools import file_read, file_write, file_search
from app.langchain.llm_factory import create_langchain_llm

class LangChainAgentExecutor:
    """LangChain Agent 执行器"""

    def __init__(self, request: AgentRequest):
        self.request = request
        self.llm = create_langchain_llm(request.llm_config)
        self.tools = [file_read, file_write, file_search]

    async def execute(self):
        """执行 Agent，返回 SSE 事件流"""
        # 创建 Agent
        agent = create_tool_calling_agent(self.llm, self.tools)

        # 执行并流式返回
        async for event in self._stream_events(agent):
            yield event

    async def _stream_events(self, agent):
        """将 Agent 执行转换为 SSE 事件"""
        trace_id = self.request.trace_id

        # 启动事件
        yield self._event("start", {"agent_type": "langchain"})

        try:
            # 使用 astream_events 获取详细事件
            async for chunk in agent.astream_events(
                {"messages": [HumanMessage(content=self.request.message)]},
                version="v1"
            ):
                event_type = chunk.event
                data = chunk.data

                # 转换事件类型
                if event_type == "on_chat_start":
                    yield self._event("agent_start", data)

                elif event_type == "on_tool_start":
                    yield self._event("tool_call", {
                        "tool": data.get("input", {}).get("name"),
                        "input": data.get("input", {})
                    })

                elif event_type == "on_tool_end":
                    yield self._event("tool_result", {
                        "tool": data.get("input", {}).get("name"),
                        "output": data.get("output")
                    })

                elif event_type == "on_chat_end":
                    yield self._event("complete", {
                        "output": data.get("output", {})
                    })

        except Exception as e:
            yield self._event("error", {"error": str(e)})

    def _event(self, event_type: str, data: dict) -> dict:
        """构造 SSE 事件"""
        import time
        return {
            "trace_id": self.request.trace_id,
            "event_type": event_type,
            "timestamp": int(time.time() * 1000),
            "data": data
        }
```

### 8.7 MCP Server 集成

```python
# app/langchain/mcp.py

from langchain_mcp_adapters.tools import load_mcp_tools
from langchain_core.tools import StructuredTool

class MCPManager:
    """MCP Server 管理器"""

    _loaded_tools: dict[str, list[StructuredTool]] = {}

    @classmethod
    async def load_server(cls, server_name: str, server_path: str) -> list[StructuredTool]:
        """加载 MCP Server 的 Tools"""
        if server_name in cls._loaded_tools:
            return cls._loaded_tools[server_name]

        tools = await load_mcp_tools(server_path)
        cls._loaded_tools[server_name] = tools

        return tools

    @classmethod
    def get_available_servers(cls) -> list[str]:
        """获取可用的 MCP Server 列表"""
        return list(cls._loaded_tools.keys())


# 使用示例
# async def agent_with_mcp():
#     # 加载 MCP Server Tools
#     mcp_tools = await MCPManager.load_server(
#         "filesystem",
#         "npx -y @modelcontextprotocol/server-filesystem /path/to/allow"
#     )
#
#     # 组合所有 Tools
#     all_tools = [file_read, file_write, file_search] + mcp_tools
#
#     # 创建 Agent
#     agent = create_tool_calling_agent(llm, all_tools)
```

### 8.8 Agent 基类（更新）

```python
# app/agent/base.py

from abc import ABC, abstractmethod
from app.models.request import AgentRequest
from app.langchain.agents import LangChainAgentExecutor

class BaseAgent(ABC):
    """Agent 基类（基于 LangChain）"""

    def __init__(self, request: AgentRequest):
        self.request = request
        # 使用 LangChain 执行器
        self.executor = LangChainAgentExecutor(request)

    async def run(self):
        """执行 Agent，yield 事件"""
        async for event in self.executor.execute():
            yield event

    @property
    @abstractmethod
    def agent_type(self) -> str:
        """Agent 类型"""
        pass
```

---

## 九、Token 计算与上下文管理

### 9.1 为什么需要 Token 管理

不同 LLM 的上下文窗口限制不同：

| LLM | 上下文窗口 | 建议使用上限 |
|-----|----------|------------|
| GPT-4-turbo | 128K | 100K |
| GPT-4o | 128K | 100K |
| Claude 3 Opus | 200K | 150K |
| Claude 3.5 Sonnet | 200K | 150K |
| Gemini Pro | 1M | 500K |
| 通义千问 | 30K | 25K |

**关键问题**：
- 不同 LLM 的 Tokenizer 不同，计算方式不一致
- 上下文过长需要智能总结（不能简单裁剪）
- 需要在 Java 侧统一管理上下文，Python 侧只负责执行

### 9.2 各 LLM Tokenizer 差异

| LLM 提供商 | Tokenizer | 特点 |
|-----------|-----------|------|
| OpenAI (GPT-4/GPT-3.5) | `cl100k_base` (tiktoken) | 1 token ≈ 0.75 英文单词，中文约 2-3 字符/token |
| Anthropic (Claude) | 自定义 tokenizer | 与 OpenAI 类似但不完全相同 |
| Google (Gemini) | 自定义 | 独立分词方式 |
| 国产模型 (通义千问/文心等) | 各自实现 | 差异较大 |

**结论**：不能用同一套计数，需要针对不同 provider 使用不同方式。

### 9.3 架构设计

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        Token 管理架构                                        │
│                                                                             │
│  ┌─────────────────────────────────────┐         ┌─────────────────────────┐│
│  │         Java 侧                     │         │      Python 侧          ││
│  │                                     │         │                         ││
│  │  ConversationContext                │         │  TokenCounter           ││
│  │  - 维护完整对话历史                 │  查询  │  - tiktoken (OpenAI)    ││
│  │  - Token 计数                       │  ────→ │  - 各 LLM 计数接口      ││
│  │  - 自动触发总结                     │         │  - /api/internal/count  ││
│  │  - 保留关键上下文                   │         │                         ││
│  │                                     │         │                         ││
│  │  调用 Python 时只传递：             │         │                         ││
│  │  - 当前可见消息（在限制内）         │         │                         ││
│  │  - 总结的上下文（如有）             │         │                         ││
│  └─────────────────────────────────────┘         └─────────────────────────┘│
└─────────────────────────────────────────────────────────────────────────────┘
```

### 9.4 Python 侧 Token 计数服务

```python
# app/api/internal.py
from fastapi import APIRouter
from pydantic import BaseModel
import tiktoken

router = APIRouter(prefix="/api/internal", tags=["internal"])


class TokenCountRequest(BaseModel):
    provider: str
    text: str


@router.post("/count-tokens")
async def count_tokens(request: TokenCountRequest):
    """计算文本的 token 数量（供 Java 侧调用）"""
    provider = request.provider.lower()
    text = request.text

    if provider in ["openai", "codex"]:
        # OpenAI 使用 tiktoken 精确计算
        encoding = tiktoken.encoding_for_model("gpt-4")
        token_count = len(encoding.encode(text))

    elif provider in ["anthropic", "claude"]:
        # Claude 近似使用 cl100k_base
        encoding = tiktoken.get_encoding("cl100k_base")
        token_count = len(encoding.encode(text))

    else:
        # 其他模型粗略估算：中英文混合 1.5 字符 ≈ 1 token
        chinese_chars = sum(1 for c in text if '\u4e00' <= c <= '\u9fff')
        other_chars = len(text) - chinese_chars
        token_count = (chinese_chars // 2) + (other_chars // 4)

    return {"token_count": token_count}


@router.post("/count-messages")
async def count_messages(request: dict):
    """计算消息列表的 token 数量"""
    provider = request.get("provider", "openai")
    messages = request.get("messages", [])

    if provider in ["openai", "codex"]:
        encoding = tiktoken.encoding_for_model("gpt-4")

        # 每条消息的开销（role + 格式）
        total = len(messages) * 4

        for msg in messages:
            total += encoding.encode(msg.get("content", ""))
            total += encoding.encode(msg.get("role", ""))

        # reply overhead
        total += 3
        return {"token_count": total}

    # 其他 provider 简化处理
    total_text = sum(len(m.get("content", "")) for m in messages)
    return {"token_count": total_text // 2}
```

### 9.5 Java 侧 Token 计数服务

```java
// TokenCounterService.java
package com.zriyo.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import java.util.List;
import java.util.Map;

@Service
public class TokenCounterService {

    @Value("${python.base-url}")
    private String pythonBaseUrl;

    private final RestTemplate restTemplate;

    public TokenCounterService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 计算消息列表的 token 数量
     */
    public int countTokens(String provider, List<ChatMessage> messages) {
        try {
            Map<String, Object> request = Map.of(
                "provider", provider,
                "messages", messages
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                pythonBaseUrl + "/api/internal/count-messages",
                request,
                Map.class
            );

            return (Integer) response.get("token_count");

        } catch (Exception e) {
            // 降级：使用粗略估算
            return estimateTokens(messages);
        }
    }

    /**
     * 粗略估算（降级方案）
     * 中英文混合：平均 1.5 字符 ≈ 1 token
     */
    private int estimateTokens(List<ChatMessage> messages) {
        int charCount = messages.stream()
            .mapToInt(m -> m.getContent().length())
            .sum();
        return (int) (charCount / 1.5);
    }

    /**
     * 计算单段文本的 token 数量
     */
    public int countTokens(String provider, String text) {
        try {
            Map<String, Object> request = Map.of(
                "provider", provider,
                "text", text
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                pythonBaseUrl + "/api/internal/count-tokens",
                request,
                Map.class
            );

            return (Integer) response.get("token_count");

        } catch (Exception e) {
            return (int) (text.length() / 1.5);
        }
    }
}
```

### 9.6 Java 侧本地 Token 计数（可选）

对于 OpenAI，Java 侧也可以直接计算：

```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.knuddels</groupId>
    <artifactId>jtokkit</artifactId>
    <version>1.0.0</version>
</dependency>
```

```java
// OpenAITokenCounter.java
package com.zriyo.util;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;

public class OpenAITokenCounter {

    private static final EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
    private static final Encoding encoding = registry.getEncoding(EncodingType.CL100K_BASE);

    public static int countTokens(String text) {
        return encoding.countTokens(text);
    }

    public static int countTokens(List<ChatMessage> messages) {
        int total = 0;
        // 每条消息的开销（role + 格式）
        total += messages.size() * 4;

        for (ChatMessage msg : messages) {
            total += encoding.countTokens(msg.getContent());
            total += encoding.countTokens(msg.getRole());
        }
        // reply overhead
        total += 3;
        return total;
    }
}
```

### 9.7 Java 侧统一 Token 计数器

```java
// UnifiedTokenCounter.java
package com.zriyo.service;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class UnifiedTokenCounter {

    private final TokenCounterService pythonTokenCounter;

    public UnifiedTokenCounter(TokenCounterService pythonTokenCounter) {
        this.pythonTokenCounter = pythonTokenCounter;
    }

    /**
     * 根据不同 provider 选择最佳计数方式
     */
    public int countTokens(String provider, List<ChatMessage> messages) {
        return switch (provider.toLowerCase()) {
            case "openai", "codex" -> {
                // OpenAI 可以用本地 jtokkit（精确）
                yield OpenAITokenCounter.countTokens(messages);
            }
            case "anthropic", "claude" -> {
                // Claude 调用 Python 服务
                yield pythonTokenCounter.countTokens("anthropic", messages);
            }
            default -> {
                // 其他用 Python 服务
                yield pythonTokenCounter.countTokens(provider, messages);
            }
        };
    }
}
```

### 9.8 上下文管理策略

```java
// ConversationContext.java
package com.zriyo.service;

import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class ConversationContext {

    private final UnifiedTokenCounter tokenCounter;
    private final LlmSummarizationService summarizationService;

    // 所有 Agent 共享的对话上下文
    private final List<ChatMessage> messages = new ArrayList<>();

    // 总结的上下文（压缩后的历史）
    private String summarizedContext = "";

    // Token 阈值配置
    private static final int WARNING_THRESHOLD = 70000;  // 触发警告
    private static final int SUMMARIZE_THRESHOLD = 80000; // 触发总结
    private static final int HARD_LIMIT = 100000;         // 硬限制

    public ConversationContext(
            UnifiedTokenCounter tokenCounter,
            LlmSummarizationService summarizationService) {
        this.tokenCounter = tokenCounter;
        this.summarizationService = summarizationService;
    }

    /**
     * 添加消息到上下文
     */
    public synchronized void addMessage(ChatMessage message) {
        messages.add(message);

        int currentTokens = tokenCounter.countTokens("openai", messages);

        if (currentTokens > HARD_LIMIT) {
            // 强制总结
            summarize(true);
        } else if (currentTokens > SUMMARIZE_THRESHOLD) {
            // 触发总结
            summarize(false);
        }
    }

    /**
     * 总结历史消息
     */
    private void summarize(boolean force) {
        if (messages.size() < 4) {
            return; // 消息太少，不总结
        }

        // 保留最近 4 条消息
        int keepCount = 4;
        int summarizeIndex = messages.size() - keepCount;

        if (summarizeIndex <= 0) {
            return;
        }

        List<ChatMessage> toSummarize = messages.subList(0, summarizeIndex);

        // 调用 LLM 总结
        String summary = summarizationService.summarize(toSummarize);

        // 更新总结上下文
        if (summarizedContext != null && !summarizedContext.isEmpty()) {
            summarizedContext += "\n\n" + summary;
        } else {
            summarizedContext = summary;
        }

        // 移除已总结的消息
        messages.subList(0, summarizeIndex).clear();
    }

    /**
     * 获取当前可见消息（用于发送给 Python）
     */
    public List<ChatMessage> getVisibleMessages() {
        List<ChatMessage> result = new ArrayList<>();

        // 添加总结的上下文（如果有）
        if (summarizedContext != null && !summarizedContext.isEmpty()) {
            result.add(new ChatMessage("system",
                "以下是之前对话的总结：\n" + summarizedContext));
        }

        // 添加当前消息
        result.addAll(messages);

        return result;
    }

    /**
     * 获取当前 token 估算
     */
    public int getCurrentTokenCount() {
        return tokenCounter.countTokens("openai", getVisibleMessages());
    }

    /**
     * 清空上下文
     */
    public synchronized void clear() {
        messages.clear();
        summarizedContext = "";
    }
}
```

### 9.9 Python 侧使用总结后的上下文

```python
# app/api/agent.py
from fastapi import APIRouter
from app.models.request import AgentRequest
from app.agent.base import BaseAgent

router = APIRouter(prefix="/api/v1", tags=["agent"])


@router.post("/agent-run")
async def run_agent(request: AgentRequest):
    """执行 Agent 任务"""
    # Java 侧已经处理好了上下文
    # 这里直接使用 request.conversation（已经在限制内）

    agent = BaseAgent.create(request)

    # 返回 SSE 流
    from fastapi.responses import StreamingResponse
    return StreamingResponse(
        agent.run(),
        media_type="text/event-stream"
    )
```

### 9.10 总结

| 职责 | 位置 | 说明 |
|------|------|------|
| **Token 精确计算** | Python | 使用 tiktoken 等库 |
| **Token 估算** | Java | 使用 jtokkit 或调用 Python |
| **上下文管理** | Java | ConversationContext 统一管理 |
| **上下文总结** | Java | 调用 LLM 总结历史消息 |
| **发送给 Python** | Java | 只发送当前可见消息 |

**关键设计**：
- 所有 Agent 共享一个对话上下文
- 超过阈值自动触发总结（不简单裁剪）
- Python 侧无状态，只接收已处理的上下文

---

## 十、开发阶段（更新）

### Phase 1: 基础框架 + LangChain（3 天）

| 任务 | 文件 |
|------|------|
| 项目初始化 | `requirements.txt`, `pyproject.toml` |
| FastAPI 入口 | `app/main.py` |
| JWT 验证 | `app/middleware/jwt_validator.py` |
| 数据模型 | `app/models/*.py` |
| 文件系统工具 | `app/utils/file.py` |
| SSE 工具 | `app/utils/sse.py` |
| **LangChain 集成** | **`app/langchain/*.py`** |
| 健康检查 | `app/api/health.py` |

### Phase 2: LLM 层 + Token 计算（1 天）

| 任务 | 文件 |
|------|------|
| LLM 工厂（LangChain） | `app/langchain/llm_factory.py` |
| 多 Provider 支持 | OpenAI / Anthropic / Codex |
| **Token 计算工具** | **`app/utils/token.py`** |
| **内部 Token API** | **`app/api/internal.py`** |

### Phase 3: LangChain Tools（2 天）

| 任务 | 文件 |
|------|------|
| LangChain Tools 定义 | `app/langchain/tools.py` |
| 文件操作 Tools | file_read, file_write, file_search |
| MCP 集成 | `app/langchain/mcp.py` |

### Phase 4: Agent 实现（2 天）

| 任务 | 文件 |
|------|------|
| Agent 基类（更新） | `app/agent/base.py` |
| CodeAgent | `app/agent/code_agent.py` |
| DocAgent | `app/agent/doc_agent.py` |
| ChartAgent | `app/agent/chart_agent.py` |

### Phase 5: 文档/图表 Tools（2 天）

| 任务 | 文件 |
|------|------|
| 文档解析 | `app/tools/doc/*.py` |
| 图表生成 | `app/tools/chart/*.py` |

### Phase 6: 测试与优化（2 天）

| 任务 | 描述 |
|------|------|
| 单元测试 | Tool 测试 |
| 联调测试 | Java ↔ Python 通信 |
| MCP 测试 | MCP Server 连接测试 |

---

## 十一、部署配置

### 环境变量

```bash
# .env
JAVA_PUBLIC_KEY="..."     # Java 侧公钥
PROJECT_BASE="/app/projects"
PORT=8000
LOG_LEVEL=INFO
```

### Docker Compose

```yaml
version: '3.8'

services:
  java:
    image: zriyo/java-app
    volumes:
      - ./projects:/app/projects
    environment:
      - JAVA_PUBLIC_KEY=...
      - PROJECT_BASE=/app/projects

  python:
    image: zriyo/python-ai
    volumes:
      - ./projects:/app/projects    # 同一个共享目录
    environment:
      - JAVA_PUBLIC_KEY=...
      - PROJECT_BASE=/app/projects
```

### Dockerfile

```dockerfile
FROM python:3.11-slim

# 安装系统依赖
RUN apt-get update && apt-get install -y \
    ripgrep \
    && rm -rf /var/lib/apt/lists/*

# 安装 tree-sitter（可选，如需代码解析）
RUN pip install tree-sitter tree-sitter-python tree-sitter-javascript tree-sitter-java

WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app ./app

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
```

### 10.1 MCP Server 配置（可选）

如果需要连接外部 MCP Server，可以在 Docker Compose 中添加配置：

```yaml
services:
  python:
    environment:
      # MCP Server 配置
      - MCP_FILESYSTEM_PATH=/app/projects
      - MCP_GITHUB_PATH=/app/projects
    # 如需访问外部 MCP Server，可添加额外配置
    # networks:
    #   - mcp-network
```

---

## 十二、Java 侧需要实现的功能

| 功能 | 说明 |
|------|------|
| **JWT 验证** | 验证 Token，提取配置 |
| **配额管理** | 限制文件数量/存储大小 |
| **变更记录** | Python 写入文件后，记录变更历史 |
| **权限控制** | 验证用户对项目的访问权限 |
| **Key 轮换** | 多 API Key 轮换使用 |
| **限流** | 请求频率限制 |
| **追溯** | 记录操作日志 |
| **Token 计数** | 调用 Python `/api/internal/count-tokens` |
| **上下文管理** | ConversationContext 统一管理 |
| **上下文总结** | 超过阈值自动总结 |

---

**文档结束**
