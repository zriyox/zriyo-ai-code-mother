# Python AI Server 说明

## 角色定位
Python 侧是 **无状态执行层**，负责工具执行、文件读写、规划/生成等能力，通过 HTTP/SSE 被 Java 侧调用。配置与权限由 Java 统一管理。

## 目录结构
```
python-ai-server/
├── app/
│   ├── main.py            # FastAPI 入口
│   ├── api/               # HTTP 路由（agent/project/plan/internal/health）
│   ├── agent/             # 规划与 Agent 逻辑
│   ├── llm/               # LLM 客户端
│   ├── tools/             # Tool 实现（file/…）
│   ├── models/            # Pydantic 模型
│   └── utils/             # 工具与通用函数
├── tests/                 # pytest
├── requirements.txt
├── pyproject.toml
└── .env.example
```

## 本地运行
```bash
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --reload
```

## 配置说明
参考 `.env.example`，主要变量：
- `PROJECT_BASE`: 项目共享目录（默认 `./projects`）
- `PORT`: 服务端口（默认 `8000`）
- `LOG_LEVEL`: 日志级别

## 代码结构与语法风格（简要）

### 1) FastAPI 路由
```python
from fastapi import APIRouter

router = APIRouter(prefix="/api/v1")

@router.post("/example")
async def example(req: RequestModel) -> ResponseModel:
    return ResponseModel(...)
```

### 2) Pydantic 模型
```python
from pydantic import BaseModel

class Req(BaseModel):
    app_id: int
    name: str

class Resp(BaseModel):
    success: bool
```

### 3) 异步函数
I/O 操作使用 `async def` + `await`：
```python
async def read_something():
    ...
```

### 4) 类型标注
```python
from typing import Optional, List

def f(x: Optional[str]) -> List[str]:
    return []
```

## Tool 约定（新增工具时）
1. 放到 `app/tools/<domain>/` 下（如 `app/tools/file/`）。
2. 使用 `class ToolName` + `@staticmethod async def run(...)` 结构。
3. 返回 `dict`，包含必要的结果字段。
4. 依赖文件操作必须通过 `ProjectFileSystem.resolve_path` 防止路径穿越。
5. 若需要对外暴露，更新 `/api/internal/tools` 的工具列表描述。

示例：
```python
class FileReadTool:
    @staticmethod
    async def run(app_id: int, file_path: str) -> dict:
        ...
```

## 测试
```bash
cd python-ai-server
pytest
```

## 常见问题
- “import 未解析”：确保 IDE 使用 `python-ai-server/venv/bin/python`，并把 `python-ai-server` 设为 Source Root。
- `rg` 未找到：安装 `ripgrep`（搜索工具依赖）。
