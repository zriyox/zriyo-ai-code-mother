"""
内部 API - 供 Java 侧调用

所有接口返回统一的 JSON 格式，便于 Java 反序列化。
"""

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field
from typing import List, Dict, Any, Optional

from app.utils.token_counter import TokenCounter, Provider

router = APIRouter(
    prefix="/api/internal",
    tags=["internal"],
    responses={
        400: {"description": "请求参数错误"},
        500: {"description": "服务器内部错误"},
    }
)


# ==================== 请求模型 ====================

class TokenCountRequest(BaseModel):
    """
    TokenCountRequest 请求 DTO：定义接口入参结构与校验约束。
    Java 对照：可类比 Controller 入参对象（Request DTO）。
    """
    provider: str = Field(
        ...,
        description="LLM 提供商",
        examples=["openai", "claude", "gemini", "deepseek", "qwen", "glm", "ernie", "kimi"]
    )
    text: str = Field(..., description="要计算 token 数量的文本", min_length=1)
    model: Optional[str] = Field(None, description="可选的模型名称，用于更精确的计算")

    model_config = {
        "json_schema_extra": {
            "examples": [
                {"provider": "openai", "text": "Hello, world!"},
                {"provider": "claude", "text": "这是一段中文文本"},
                {"provider": "deepseek", "text": "混合文本 with English 和 中文"}
            ]
        }
    }


class MessagesCountRequest(BaseModel):
    """
    MessagesCountRequest 请求 DTO：定义接口入参结构与校验约束。
    Java 对照：可类比 Controller 入参对象（Request DTO）。
    """
    provider: str = Field(..., description="LLM 提供商")
    messages: List[Dict[str, str]] = Field(
        ...,
        description="消息列表，每条消息包含 role 和 content",
        min_length=1
    )
    model: Optional[str] = Field(None, description="可选的模型名称")

    model_config = {
        "json_schema_extra": {
            "examples": [[
                {"role": "system", "content": "You are a helpful assistant."},
                {"role": "user", "content": "Hello!"}
            ]]
        }
    }


class HealthCheckRequest(BaseModel):
    """
    HealthCheckRequest 请求 DTO：定义接口入参结构与校验约束。
    Java 对照：可类比 Controller 入参对象（Request DTO）。
    """
    services: Optional[List[str]] = Field(
        None,
        description="要检查的服务列表，如 ['llm', 'database', 'filesystem']"
    )


class TaskCancelRequest(BaseModel):
    """
    TaskCancelRequest 请求 DTO：定义接口入参结构与校验约束。
    Java 对照：可类比 Controller 入参对象（Request DTO）。
    """
    task_id: str = Field(..., description="任务 ID", min_length=1)


# ==================== 响应模型 ====================

class TokenCountResponse(BaseModel):
    """
    TokenCountResponse 响应 DTO：定义接口出参结构。
    Java 对照：可类比 Controller 返回对象（Response DTO）。
    """
    token_count: int = Field(..., description="计算得到的 token 数量")
    provider: str = Field(..., description="请求的提供商")
    estimated: bool = Field(False, description="是否为估算值（非精确计算）")
    chars_per_token: Optional[float] = Field(None, description="每个 token 平均字符数")


class MessagesCountResponse(BaseModel):
    """
    MessagesCountResponse 响应 DTO：定义接口出参结构。
    Java 对照：可类比 Controller 返回对象（Response DTO）。
    """
    token_count: int = Field(..., description="计算得到的总 token 数量")
    message_count: int = Field(..., description="消息数量")
    provider: str = Field(..., description="请求的提供商")
    estimated: bool = Field(False, description="是否为估算值")


class PingResponse(BaseModel):
    """
    PingResponse 响应 DTO：定义接口出参结构。
    Java 对照：可类比 Controller 返回对象（Response DTO）。
    """
    pong: bool = Field(True, description="服务是否正常")
    timestamp: int = Field(..., description="当前时间戳（毫秒）")
    version: str = Field(..., description="服务版本")


class HealthCheckResponse(BaseModel):
    """
    HealthCheckResponse 响应 DTO：定义接口出参结构。
    Java 对照：可类比 Controller 返回对象（Response DTO）。
    """
    status: str = Field(..., description="overall 状态: healthy, degraded, unhealthy")
    checks: Dict[str, Dict[str, Any]] = Field(
        default_factory=dict,
        description="各服务的检查结果"
    )


class TaskCancelResponse(BaseModel):
    """
    TaskCancelResponse 响应 DTO：定义接口出参结构。
    Java 对照：可类比 Controller 返回对象（Response DTO）。
    """
    task_id: str = Field(..., description="任务 ID")
    cancelled: bool = Field(True, description="是否已标记取消")


class ProvidersResponse(BaseModel):
    """
    ProvidersResponse 响应 DTO：定义接口出参结构。
    Java 对照：可类比 Controller 返回对象（Response DTO）。
    """
    providers: List[str] = Field(..., description="支持的提供商列表")
    count: int = Field(..., description="提供商数量")


class ProviderInfoResponse(BaseModel):
    """
    ProviderInfoResponse 响应 DTO：定义接口出参结构。
    Java 对照：可类比 Controller 返回对象（Response DTO）。
    """
    provider: str = Field(..., description="提供商名称")
    precision: str = Field(..., description="计算精度: exact, approximate, unknown")
    tokenizer: str = Field(..., description="使用的 tokenizer")
    chars_per_token: str = Field(..., description="每 token 平均字符数说明")


class ToolParam(BaseModel):
    """
    ToolParam 数据模型定义。
    Java 对照：可类比 POJO/record，用于序列化与反序列化。
    """
    name: str = Field(..., description="参数名")
    type: str = Field(..., description="类型")
    required: bool = Field(default=True, description="是否必填")
    description: str = Field(..., description="参数说明")
    default: Optional[str] = Field(default=None, description="默认值")


class ToolInfo(BaseModel):
    """
    ToolInfo 数据模型定义。
    Java 对照：可类比 POJO/record，用于序列化与反序列化。
    """
    name: str = Field(..., description="工具名称")
    description: str = Field(..., description="工具描述")
    params: List[ToolParam] = Field(default_factory=list, description="参数列表")
    requires: Dict[str, Any] = Field(default_factory=dict, description="依赖要求")
    version: str = Field(default="v1", description="工具版本")


class ToolsResponse(BaseModel):
    """
    ToolsResponse 响应 DTO：定义接口出参结构。
    Java 对照：可类比 Controller 返回对象（Response DTO）。
    """
    tools: List[ToolInfo] = Field(..., description="工具列表")
    count: int = Field(..., description="工具数量")


# ==================== API 端点 ====================

@router.post(
    "/count-tokens",
    response_model=TokenCountResponse,
    summary="计算文本 Token 数量",
    description="""
计算文本的 token 数量，供 Java 侧调用进行上下文管理。

**支持的提供商：**
- `openai`, `codex` - OpenAI 系列（精确计算）
- `anthropic`, `claude` - Claude 系列（近似计算）
- `gemini`, `google` - Google Gemini（近似计算）
- `deepseek` - DeepSeek（精确计算，需 transformers）
- `qwen`, `tongyi` - 通义千问（精确计算，需 transformers）
- `glm`, `zhipu` - 智谱（精确计算，需 transformers）
- `ernie`, `baidu` - 文心一言（精确计算，需 transformers）
- `kimi`, `moonshot` - 月之暗面（精确计算，需 transformers）

**计算方式：**
- OpenAI/Codex: 使用 tiktoken 精确计算（cl100k_base）
- Anthropic/Claude: 使用 cl100k_base 近似计算
- Gemini: 基于字符数的近似计算
- 其他: 使用 HuggingFace tokenizer 或字符估算

**注意：** 不同 LLM 的 tokenizer 不同，计算结果可能有差异。
""")
async def count_tokens(request: TokenCountRequest) -> TokenCountResponse:
    """
    路由处理函数：接收请求参数并调用下游能力。
    输入：Pydantic 模型或 query 参数；输出：JSON 或流式响应。
    说明：包含异步/流式处理逻辑，需关注事件边界与错误兜底。
    """
    try:
        token_count, estimated = await TokenCounter.count_tokens(
            request.text,
            request.provider,
            request.model
        )

        # 计算每个 token 的平均字符数
        chars_per_token = len(request.text) / token_count if token_count > 0 else 0

        return TokenCountResponse(
            token_count=token_count,
            provider=request.provider,
            estimated=estimated,
            chars_per_token=round(chars_per_token, 2)
        )
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail={
                "error": "TOKEN_COUNT_FAILED",
                "message": str(e)
            }
        )


@router.post(
    "/count-messages",
    response_model=MessagesCountResponse,
    summary="计算消息列表 Token 数量",
    description="""
计算消息列表的 token 数量，考虑消息格式的开销。

**计算规则：**
- OpenAI 系列: 每条消息有固定的格式开销（约 4 tokens），role 和 content 分别计算
- Anthropic 系列: 同 OpenAI
- 其他提供商: 计算所有文本 token 后加上估算的格式开销（约 5 tokens/消息）
""")
async def count_messages(request: MessagesCountRequest) -> MessagesCountResponse:
    """
    路由处理函数：接收请求参数并调用下游能力。
    输入：Pydantic 模型或 query 参数；输出：JSON 或流式响应。
    说明：包含异步/流式处理逻辑，需关注事件边界与错误兜底。
    """
    try:
        token_count, estimated = await TokenCounter.count_messages(
            request.messages,
            request.provider,
            request.model
        )

        return MessagesCountResponse(
            token_count=token_count,
            message_count=len(request.messages),
            provider=request.provider,
            estimated=estimated
        )
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail={
                "error": "MESSAGE_COUNT_FAILED",
                "message": str(e)
            }
        )


@router.get(
    "/ping",
    response_model=PingResponse,
    summary="服务连通性测试",
    description="简单的 ping 端点，用于测试服务是否可用。"
)
async def ping() -> PingResponse:
    """
    路由处理函数：接收请求参数并调用下游能力。
    输入：Pydantic 模型或 query 参数；输出：JSON 或流式响应。
    说明：包含异步/流式处理逻辑，需关注事件边界与错误兜底。
    """
    import time
    return PingResponse(
        pong=True,
        timestamp=int(time.time() * 1000),
        version="0.1.0"
    )


@router.get(
    "/providers",
    response_model=ProvidersResponse,
    summary="获取支持的提供商列表",
    description="返回所有支持的 LLM 提供商列表。"
)
async def get_providers() -> ProvidersResponse:
    """
    路由处理函数：接收请求参数并调用下游能力。
    输入：Pydantic 模型或 query 参数；输出：JSON 或流式响应。
    说明：包含异步/流式处理逻辑，需关注事件边界与错误兜底。
    返回：目标对象实例或查询结果。
    """
    providers = TokenCounter.get_supported_providers()
    return ProvidersResponse(
        providers=providers,
        count=len(providers)
    )


@router.get(
    "/providers/{provider}",
    response_model=ProviderInfoResponse,
    summary="获取提供商信息",
    description="""
获取指定提供商的详细信息，包括 tokenizer 类型和计算精度。

**返回信息：**
- `precision`: 计算精度（exact=精确, approximate=近似）
- `tokenizer`: 使用的分词器
- `chars_per_token`: 每个 token 大约对应多少字符
""")
async def get_provider_info(provider: str) -> ProviderInfoResponse:
    """
    路由处理函数：接收请求参数并调用下游能力。
    输入：Pydantic 模型或 query 参数；输出：JSON 或流式响应。
    说明：包含异步/流式处理逻辑，需关注事件边界与错误兜底。
    返回：目标对象实例或查询结果。
    """
    info = TokenCounter.get_provider_info(provider)
    return ProviderInfoResponse(**info)


@router.get(
    "/tools",
    response_model=ToolsResponse,
    summary="获取工具列表",
    description="返回 Python 侧可用工具列表及参数说明，供 Java 侧做选择与授权。"
)
async def get_tools() -> ToolsResponse:
    """
    路由处理函数：接收请求参数并调用下游能力。
    输入：Pydantic 模型或 query 参数；输出：JSON 或流式响应。
    说明：包含异步/流式处理逻辑，需关注事件边界与错误兜底。
    返回：目标对象实例或查询结果。
    """
    tools = [
        ToolInfo(
            name="file_read",
            description="读取文件内容（支持行范围）",
            params=[
                ToolParam(name="app_id", type="int", description="应用 ID"),
                ToolParam(name="file_path", type="string", description="文件路径"),
                ToolParam(
                    name="allowed_prefixes",
                    type="array[string]",
                    required=False,
                    description="允许访问的子目录前缀（默认 [\"src\"]）",
                ),
                ToolParam(name="start_line", type="int", required=False, description="起始行（1-based）"),
                ToolParam(name="end_line", type="int", required=False, description="结束行（1-based）"),
                ToolParam(name="max_chars", type="int", required=False, description="最大字符数"),
            ],
        ),
        ToolInfo(
            name="file_list",
            description="列出项目文件（支持 glob/pathspec）",
            params=[
                ToolParam(name="app_id", type="int", description="应用 ID"),
                ToolParam(
                    name="patterns",
                    type="array[string]",
                    required=False,
                    description="路径匹配模式列表，默认为 **/*",
                ),
                ToolParam(
                    name="allowed_prefixes",
                    type="array[string]",
                    required=False,
                    description="允许访问的子目录前缀（默认 [\"src\"]）",
                ),
                ToolParam(name="limit", type="int", required=False, description="最大返回数量"),
            ],
        ),
        ToolInfo(
            name="file_search",
            description="全文搜索（基于 ripgrep）",
            params=[
                ToolParam(name="app_id", type="int", description="应用 ID"),
                ToolParam(name="query", type="string", description="搜索关键词"),
                ToolParam(
                    name="patterns",
                    type="array[string]",
                    required=False,
                    description="glob 过滤规则",
                ),
                ToolParam(
                    name="allowed_prefixes",
                    type="array[string]",
                    required=False,
                    description="允许访问的子目录前缀（默认 [\"src\"]）",
                ),
                ToolParam(name="max_results", type="int", required=False, description="最大返回条数"),
                ToolParam(name="case_sensitive", type="bool", required=False, description="是否区分大小写"),
                ToolParam(name="context_lines", type="int", required=False, description="上下文行数"),
            ],
            requires={"external_binaries": ["rg"]},
        ),
        ToolInfo(
            name="file_patch",
            description="增量写入（应用 unified diff）",
            params=[
                ToolParam(name="app_id", type="int", description="应用 ID"),
                ToolParam(name="patch_text", type="string", description="diff 内容"),
                ToolParam(
                    name="allowed_prefixes",
                    type="array[string]",
                    required=False,
                    description="允许访问的子目录前缀（默认 [\"src\"]）",
                ),
            ],
            requires={"python_packages": ["unidiff"]},
        ),
        ToolInfo(
            name="skill_read",
            description="读取 Skill 文档（仅限 skills/codeagent/）",
            params=[
                ToolParam(
                    name="skill_path",
                    type="string",
                    description="技能路径或名称（相对 skills/codeagent/，如 code/element-plus 或 code/element-plus/SKILL.md）",
                )
            ],
        ),
    ]

    return ToolsResponse(tools=tools, count=len(tools))


@router.post(
    "/health",
    response_model=HealthCheckResponse,
    summary="健康检查",
    description="""
检查服务及其依赖的健康状态。

**可检查的服务：**
- `llm`: LLM 服务连通性
- `filesystem`: 项目文件系统可访问性
- `database`: 数据库连接（如有）
""")
async def health_check(request: HealthCheckRequest = HealthCheckRequest()) -> HealthCheckResponse:
    """
    路由处理函数：接收请求参数并调用下游能力。
    输入：Pydantic 模型或 query 参数；输出：JSON 或流式响应。
    说明：包含异步/流式处理逻辑，需关注事件边界与错误兜底。
    """
    from pathlib import Path
    from app.config.settings import settings

    checks = {}
    overall_status = "healthy"

    # 检查文件系统
    if request.services is None or "filesystem" in request.services:
        project_path = Path(settings.PROJECT_BASE)
        fs_accessible = project_path.exists() and project_path.is_dir()
        checks["filesystem"] = {
            "status": "ok" if fs_accessible else "error",
            "path": str(settings.PROJECT_BASE),
            "accessible": fs_accessible
        }
        if not fs_accessible:
            overall_status = "unhealthy"

    # 检查 LLM（如果提供）
    if request.services and "llm" in request.services:
        # TODO: 实现 LLM 连通性检查
        checks["llm"] = {
            "status": "unknown",
            "message": "LLM health check not implemented"
        }
        overall_status = "degraded" if overall_status == "healthy" else overall_status

    return HealthCheckResponse(
        status=overall_status,
        checks=checks
    )


@router.post(
    "/task/cancel",
    response_model=TaskCancelResponse,
    summary="取消任务（内部）",
    description="标记任务取消，供执行端协作式中断。"
)
async def cancel_task(request: TaskCancelRequest) -> TaskCancelResponse:
    """
    路由处理函数：接收请求参数并调用下游能力。
    输入：Pydantic 模型或 query 参数；输出：JSON 或流式响应。
    说明：包含异步/流式处理逻辑，需关注事件边界与错误兜底。
    """
    from app.utils.cancel import CancelService

    await CancelService.request_cancel(request.task_id)
    return TaskCancelResponse(task_id=request.task_id, cancelled=True)
