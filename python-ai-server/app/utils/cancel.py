"""
任务取消服务（支持 memory / redis 后端）。

目标：
- 对 Agent 暴露统一异步接口（request_cancel/is_cancelled/clear）
- 默认 memory 兼容本地开发
- 生产可切换 redis，实现多副本共享取消信号
"""

from __future__ import annotations

import asyncio
import time
from typing import Dict, Optional, Protocol

from loguru import logger

from app.config.settings import settings


class CancelStore(Protocol):
    async def request_cancel(self, task_id: str) -> None:
        ...

    async def is_cancelled(self, task_id: str) -> bool:
        ...

    async def clear(self, task_id: str) -> None:
        ...

    async def close(self) -> None:
        ...


class InMemoryCancelStore:
    """进程内取消标记存储（单实例可用）。"""

    def __init__(self):
        self._lock = asyncio.Lock()
        self._flags: Dict[str, float] = {}

    async def request_cancel(self, task_id: str) -> None:
        if not task_id:
            return
        async with self._lock:
            self._flags[task_id] = time.time()

    async def is_cancelled(self, task_id: str) -> bool:
        if not task_id:
            return False
        async with self._lock:
            return task_id in self._flags

    async def clear(self, task_id: str) -> None:
        if not task_id:
            return
        async with self._lock:
            self._flags.pop(task_id, None)

    async def close(self) -> None:
        return


class RedisCancelStore:
    """Redis 取消标记存储（多副本共享，生产推荐）。"""

    def __init__(self, redis_url: str, key_prefix: str, ttl_seconds: int):
        self._redis_url = redis_url
        self._key_prefix = key_prefix
        self._ttl_seconds = max(1, int(ttl_seconds))
        self._client = None
        self._lock = asyncio.Lock()

    def _key(self, task_id: str) -> str:
        return f"{self._key_prefix}{task_id}"

    async def _get_client(self):
        if self._client is not None:
            return self._client

        async with self._lock:
            if self._client is not None:
                return self._client
            try:
                from redis.asyncio import Redis  # type: ignore
            except Exception as e:
                raise RuntimeError(
                    "redis 依赖缺失，请安装 `redis>=5.0.0` 或切换 CANCEL_BACKEND=memory"
                ) from e

            self._client = Redis.from_url(self._redis_url, decode_responses=True)
            return self._client

    async def request_cancel(self, task_id: str) -> None:
        if not task_id:
            return
        client = await self._get_client()
        await client.set(self._key(task_id), "1", ex=self._ttl_seconds)

    async def is_cancelled(self, task_id: str) -> bool:
        if not task_id:
            return False
        client = await self._get_client()
        return bool(await client.exists(self._key(task_id)))

    async def clear(self, task_id: str) -> None:
        if not task_id:
            return
        client = await self._get_client()
        await client.delete(self._key(task_id))

    async def close(self) -> None:
        if self._client is not None:
            await self._client.aclose()
            self._client = None


class CancelService:
    """
    取消能力门面（Facade）。

    对上层隐藏具体后端类型，后续其他 Agent 直接复用。
    """

    _store: Optional[CancelStore] = None
    _backend_name: str = "memory"
    _init_lock = asyncio.Lock()

    @classmethod
    async def _init_store(cls) -> None:
        backend = (settings.CANCEL_BACKEND or "memory").strip().lower()
        if backend == "redis":
            redis_url = (settings.REDIS_URL or "").strip()
            if not redis_url:
                logger.warning("CANCEL_BACKEND=redis 但 REDIS_URL 为空，回退 memory")
                cls._store = InMemoryCancelStore()
                cls._backend_name = "memory"
                return
            try:
                store = RedisCancelStore(
                    redis_url=redis_url,
                    key_prefix=settings.CANCEL_KEY_PREFIX,
                    ttl_seconds=settings.CANCEL_TTL_SECONDS,
                )
                await store._get_client()
                cls._store = store
                cls._backend_name = "redis"
                logger.info("CancelService initialized with redis backend")
                return
            except Exception as e:
                logger.warning(f"初始化 redis cancel backend 失败，回退 memory: {e}")

        cls._store = InMemoryCancelStore()
        cls._backend_name = "memory"
        logger.info("CancelService initialized with memory backend")

    @classmethod
    async def ensure_ready(cls) -> None:
        if cls._store is not None:
            return
        async with cls._init_lock:
            if cls._store is None:
                await cls._init_store()

    @classmethod
    async def request_cancel(cls, task_id: str) -> None:
        await cls.ensure_ready()
        await cls._store.request_cancel(task_id)  # type: ignore[union-attr]

    @classmethod
    async def is_cancelled(cls, task_id: str) -> bool:
        await cls.ensure_ready()
        return await cls._store.is_cancelled(task_id)  # type: ignore[union-attr]

    @classmethod
    async def clear(cls, task_id: str) -> None:
        await cls.ensure_ready()
        await cls._store.clear(task_id)  # type: ignore[union-attr]

    @classmethod
    async def shutdown(cls) -> None:
        if cls._store is not None:
            await cls._store.close()
            cls._store = None

    @classmethod
    def backend_name(cls) -> str:
        return cls._backend_name
