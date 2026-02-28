"""
取消请求注册表（进程内）
"""

import threading
import time
from typing import Dict


class CancelRegistry:
    """简单的取消标记存储（进程内）"""

    _lock = threading.Lock()
    _flags: Dict[str, float] = {}

    @classmethod
    def request_cancel(cls, task_id: str) -> None:
        if not task_id:
            return
        with cls._lock:
            cls._flags[task_id] = time.time()

    @classmethod
    def is_cancelled(cls, task_id: str) -> bool:
        if not task_id:
            return False
        with cls._lock:
            return task_id in cls._flags

    @classmethod
    def clear(cls, task_id: str) -> None:
        if not task_id:
            return
        with cls._lock:
            cls._flags.pop(task_id, None)
