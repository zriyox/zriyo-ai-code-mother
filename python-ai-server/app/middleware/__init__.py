"""
中间件导出
"""

from app.middleware.error_handlers import setup_error_handlers, ErrorResponse

__all__ = [
    "setup_error_handlers",
    "ErrorResponse",
]
