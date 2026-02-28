"""
异常定义
"""


class AgentError(Exception):
    """Agent 基础异常"""
    def __init__(self, message: str, code: str = "AGENT_ERROR"):
        self.message = message
        self.code = code
        super().__init__(self.message)


class ToolExecutionError(AgentError):
    """Tool 执行异常"""
    def __init__(self, message: str, tool_name: str = None):
        self.tool_name = tool_name
        super().__init__(message, "TOOL_EXECUTION_ERROR")


class FileOperationError(AgentError):
    """文件操作异常"""
    def __init__(self, message: str, file_path: str = None):
        self.file_path = file_path
        super().__init__(message, "FILE_OPERATION_ERROR")


class LLMError(AgentError):
    """LLM 调用异常"""
    def __init__(self, message: str, provider: str = None):
        self.provider = provider
        super().__init__(message, "LLM_ERROR")


class ConfigurationError(AgentError):
    """配置异常"""
    def __init__(self, message: str):
        super().__init__(message, "CONFIGURATION_ERROR")


class AgentCancelledError(AgentError):
    """Agent 取消异常"""
    def __init__(self, message: str = "Request cancelled"):
        super().__init__(message, "AGENT_CANCELLED")
