"""
结果模型定义
"""

from pydantic import BaseModel
from typing import List, Dict, Optional


class AgentResult(BaseModel):
    """Agent 执行结果"""
    request_id: str
    trace_id: str
    status: str                    # "success" | "failed"

    # 生成/修改的文件（通知 Java 记录）
    files: List[Dict] = []         # [{"path": "...", "action": "created|modified"}]

    # 其他结果
    data: Dict = {}                # 根据任务类型不同而不同
    error: Optional[str] = None

    class Config:
        # 即使字段为 None 也要序列化
        serialize_when_none = False
