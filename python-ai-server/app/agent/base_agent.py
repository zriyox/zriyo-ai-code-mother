"""
Agent 基类（支持取消）
"""

from typing import Optional

from app.utils.cancel import CancelRegistry
from app.utils.errors import AgentCancelledError


class CancelableAgent:
    """可取消 Agent 基类"""

    @staticmethod
    def _check_cancelled(task_id: Optional[str]) -> None:
        if task_id and CancelRegistry.is_cancelled(task_id):
            raise AgentCancelledError()
