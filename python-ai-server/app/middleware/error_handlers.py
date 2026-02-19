"""
统一异常处理器

提供 Java 友好的错误响应格式。
"""

from fastapi import Request, status
from fastapi.responses import JSONResponse
from fastapi.exceptions import RequestValidationError
from starlette.exceptions import HTTPException as StarletteHTTPException
from typing import Any, Dict, Union
import traceback

from app.utils.errors import AgentError


class ErrorResponse:
    """统一错误响应格式"""

    def __init__(
        self,
        code: str,
        message: str,
        details: Dict[str, Any] = None,
        trace_id: str = None
    ):
        self.code = code
        self.message = message
        self.details = details or {}
        self.trace_id = trace_id

    def to_dict(self) -> Dict[str, Any]:
        """转换为字典"""
        result = {
            "error": {
                "code": self.code,
                "message": self.message
            }
        }
        if self.details:
            result["error"]["details"] = self.details
        if self.trace_id:
            result["trace_id"] = self.trace_id
        return result


# 错误代码映射
ERROR_CODE_MAP: Dict[type, str] = {
    # ValueError
    ValueError: "INVALID_VALUE",

    # TypeError
    TypeError: "TYPE_ERROR",

    # FileNotFoundError
    FileNotFoundError: "FILE_NOT_FOUND",

    # PermissionError
    PermissionError: "PERMISSION_DENIED",

    # KeyError
    KeyError: "MISSING_KEY",

    # AttributeError
    AttributeError: "ATTRIBUTE_ERROR",

    # AgentError
    AgentError: "AGENT_ERROR",
}


async def agent_error_handler(request: Request, exc: AgentError) -> JSONResponse:
    """Agent 异常处理器"""
    trace_id = getattr(request.state, "trace_id", None)

    return JSONResponse(
        status_code=status.HTTP_400_BAD_REQUEST,
        content=ErrorResponse(
            code=exc.code,
            message=exc.message,
            trace_id=trace_id
        ).to_dict()
    )


async def validation_error_handler(
    request: Request,
    exc: RequestValidationError
) -> JSONResponse:
    """请求验证错误处理器"""
    trace_id = getattr(request.state, "trace_id", None)

    # 格式化验证错误
    errors = []
    for error in exc.errors():
        errors.append({
            "field": ".".join(str(loc) for loc in error["loc"]),
            "message": error["msg"],
            "type": error["type"]
        })

    return JSONResponse(
        status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
        content=ErrorResponse(
            code="VALIDATION_ERROR",
            message="Request validation failed",
            details={"errors": errors},
            trace_id=trace_id
        ).to_dict()
    )


async def http_exception_handler(
    request: Request,
    exc: StarletteHTTPException
) -> JSONResponse:
    """HTTP 异常处理器"""
    trace_id = getattr(request.state, "trace_id", None)

    return JSONResponse(
        status_code=exc.status_code,
        content=ErrorResponse(
            code="HTTP_ERROR",
            message=exc.detail,
            trace_id=trace_id
        ).to_dict()
    )


async def general_exception_handler(request: Request, exc: Exception) -> JSONResponse:
    """通用异常处理器"""
    trace_id = getattr(request.state, "trace_id", None)

    # 获取错误代码
    error_code = ERROR_CODE_MAP.get(type(exc), "INTERNAL_ERROR")

    # 开发环境下返回详细错误信息
    import os
    debug_mode = os.getenv("DEBUG", "false").lower() == "true"

    details = {}
    if debug_mode:
        details["type"] = type(exc).__name__
        details["traceback"] = traceback.format_exc()

    return JSONResponse(
        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
        content=ErrorResponse(
            code=error_code,
            message=str(exc) if debug_mode else "An internal error occurred",
            details=details if details else None,
            trace_id=trace_id
        ).to_dict()
    )


def setup_error_handlers(app):
    """设置所有异常处理器"""
    from fastapi.exceptions import RequestValidationError
    from starlette.exceptions import HTTPException as StarletteHTTPException

    app.add_exception_handler(AgentError, agent_error_handler)
    app.add_exception_handler(RequestValidationError, validation_error_handler)
    app.add_exception_handler(StarletteHTTPException, http_exception_handler)
    app.add_exception_handler(Exception, general_exception_handler)
