"""
模块职责：Agent 公共基类，提供协作式取消能力。
Java 对照：可类比抽象基类 + 中断检查工具方法。
"""

from typing import Optional

from app.utils.cancel import CancelService
from app.utils.errors import AgentCancelledError


class CancelableAgent:
    """可取消 Agent 基类（协作式取消，不是强制 kill 线程）。"""

    @staticmethod
    async def _check_cancelled(task_id: Optional[str]) -> None:
        # 在关键阶段主动检查一次；若已取消则抛异常，由上层统一转成取消事件
        """
        协作式取消检查：在关键步骤主动检测任务是否被标记取消。
        若已取消则抛出 AgentCancelledError，由上层统一转为取消事件。
        """
        if task_id and await CancelService.is_cancelled(task_id):
            raise AgentCancelledError()

    @staticmethod
    async def _clear_cancel_flag(task_id: Optional[str]) -> None:
        if task_id:
            await CancelService.clear(task_id)
