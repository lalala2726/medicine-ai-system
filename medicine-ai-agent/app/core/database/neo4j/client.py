from __future__ import annotations

from functools import lru_cache
from typing import Any, Callable, LiteralString, TypeVar

from neo4j import Driver, ManagedTransaction, Session
from neo4j.exceptions import DriverError, Neo4jError

from app.core.codes import ResponseCode
from app.core.database.neo4j.config import (
    _clear_neo4j_driver_cache,
    get_neo4j_driver,
    get_neo4j_settings,
)
from app.core.exception.exceptions import ServiceException

T = TypeVar("T")


class Neo4jClient:
    """Neo4j 通用查询客户端。"""

    def __init__(self, driver: Driver, default_database: str) -> None:
        """初始化 Neo4j 通用客户端。

        Args:
            driver: 已初始化完成的 Neo4j Driver。
            default_database: 未显式传入数据库名时使用的默认数据库。

        Returns:
            None: 构造函数无返回值。
        """
        self._driver = driver
        self._default_database = default_database

    def _resolve_database(self, database: str | None) -> str:
        """解析当前调用使用的数据库名。

        Args:
            database: 调用方显式传入的数据库名；为空时回退默认库。

        Returns:
            str: 最终生效的数据库名。
        """
        resolved_database = (database or self._default_database).strip()
        if not resolved_database:
            raise ServiceException(
                code=ResponseCode.INTERNAL_ERROR,
                message="Neo4j 数据库名不能为空",
            )
        return resolved_database

    def _run_with_session(
            self,
            *,
            database: str | None,
            error_message: str,
            runner: Callable[[Session], T],
    ) -> T:
        """在托管 Session 生命周期下执行回调。

        Args:
            database: 当前调用使用的数据库名；为空时回退默认库。
            error_message: 底层驱动异常时转换后的统一错误文案。
            runner: 持有 `Session` 的业务回调。

        Returns:
            T: 回调执行返回值。

        Raises:
            ServiceException: Neo4j 驱动相关异常时抛出统一业务异常。
        """
        resolved_database = self._resolve_database(database)
        try:
            with self._driver.session(database=resolved_database) as session:
                return runner(session)
        except (DriverError, Neo4jError) as exc:
            raise ServiceException(
                code=ResponseCode.DATABASE_ERROR,
                message=error_message,
            ) from exc

    def query_all(
            self,
            query: LiteralString,
            parameters: dict[str, Any] | None = None,
            database: str | None = None,
    ) -> list[dict[str, Any]]:
        """执行查询并返回全部结果行。

        Args:
            query: 待执行的 Cypher 字面量查询语句。
            parameters: Cypher 参数字典；为空时使用空字典。
            database: 当前查询使用的数据库名；为空时回退默认库。

        Returns:
            list[dict[str, Any]]: 查询结果列表，每一行会被规范化为字典。

        Raises:
            ServiceException: Neo4j 查询失败时抛出。
        """

        def _execute(session: Session) -> list[dict[str, Any]]:
            def _work(tx: ManagedTransaction) -> list[dict[str, Any]]:
                result = tx.run(query, parameters or {})
                return [record.data() for record in result]

            return session.execute_read(_work)

        return self._run_with_session(
            database=database,
            error_message="Neo4j 查询失败",
            runner=_execute,
        )

    def query_one(
            self,
            query: LiteralString,
            parameters: dict[str, Any] | None = None,
            database: str | None = None,
    ) -> dict[str, Any] | None:
        """执行查询并返回首条结果。

        Args:
            query: 待执行的 Cypher 字面量查询语句。
            parameters: Cypher 参数字典；为空时使用空字典。
            database: 当前查询使用的数据库名；为空时回退默认库。

        Returns:
            dict[str, Any] | None: 首条查询结果；未命中时返回 `None`。

        Raises:
            ServiceException: Neo4j 查询失败时抛出。
        """

        def _execute(session: Session) -> dict[str, Any] | None:
            def _work(tx: ManagedTransaction) -> dict[str, Any] | None:
                result = tx.run(query, parameters or {})
                for record in result:
                    return record.data()
                return None

            return session.execute_read(_work)

        return self._run_with_session(
            database=database,
            error_message="Neo4j 查询失败",
            runner=_execute,
        )

    def execute_read(
            self,
            work: Callable[[ManagedTransaction], T],
            *,
            database: str | None = None,
    ) -> T:
        """执行读事务回调。

        Args:
            work: 读事务回调，参数为 Neo4j `ManagedTransaction`。
            database: 当前事务使用的数据库名；为空时回退默认库。

        Returns:
            T: 事务回调的返回值。

        Raises:
            ServiceException: Neo4j 读事务执行失败时抛出。
        """
        return self._run_with_session(
            database=database,
            error_message="Neo4j 读事务执行失败",
            runner=lambda session: session.execute_read(work),
        )

    def execute_write(
            self,
            work: Callable[[ManagedTransaction], T],
            *,
            database: str | None = None,
    ) -> T:
        """执行写事务回调。

        Args:
            work: 写事务回调，参数为 Neo4j `ManagedTransaction`。
            database: 当前事务使用的数据库名；为空时回退默认库。

        Returns:
            T: 事务回调的返回值。

        Raises:
            ServiceException: Neo4j 写事务执行失败时抛出。
        """
        return self._run_with_session(
            database=database,
            error_message="Neo4j 写事务执行失败",
            runner=lambda session: session.execute_write(work),
        )

    def verify_connectivity(self) -> None:
        """校验当前客户端绑定的 Neo4j 连通性。

        Args:
            无额外参数；默认校验当前客户端默认数据库。

        Returns:
            None: 校验成功时无返回值。

        Raises:
            ServiceException: Neo4j 连通性校验失败时抛出。
        """
        try:
            self._driver.verify_connectivity(database=self._default_database)
        except (DriverError, Neo4jError) as exc:
            raise ServiceException(
                code=ResponseCode.DATABASE_ERROR,
                message="Neo4j 连接校验失败",
            ) from exc


@lru_cache(maxsize=1)
def get_neo4j_client() -> Neo4jClient:
    """创建并缓存 Neo4j 通用客户端。

    Args:
        无额外参数；客户端会复用全局 Neo4j Driver 与默认数据库配置。

    Returns:
        Neo4jClient: 进程级复用的 Neo4j 通用客户端实例。
    """
    settings = get_neo4j_settings()
    return Neo4jClient(
        driver=get_neo4j_driver(),
        default_database=settings.database,
    )


def clear_neo4j_connection_cache() -> None:
    """清理 Neo4j 客户端、Driver 与配置缓存。

    Args:
        无额外参数。

    Returns:
        None: 无返回值。
    """
    get_neo4j_client.cache_clear()
    _clear_neo4j_driver_cache()
