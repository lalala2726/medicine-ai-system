from __future__ import annotations

from typing import Any, LiteralString

import pytest
from neo4j.exceptions import DriverError

from app.core.database.neo4j.client import Neo4jClient
from app.core.exception.exceptions import ServiceException


class _FakeRecord:
    def __init__(self, payload: dict[str, Any]) -> None:
        self._payload = payload

    def data(self) -> dict[str, Any]:
        return dict(self._payload)


class _FakeResult:
    def __init__(self, rows: list[dict[str, Any]]) -> None:
        self._rows = [_FakeRecord(row) for row in rows]

    def __iter__(self):
        return iter(self._rows)


class _FakeTransaction:
    def __init__(self, *, rows: list[dict[str, Any]] | None = None, error: Exception | None = None) -> None:
        self._rows = rows or []
        self._error = error
        self.run_calls: list[dict[str, Any]] = []

    def run(self, query: LiteralString, parameters: dict[str, Any]) -> _FakeResult:
        self.run_calls.append(
            {
                "query": query,
                "parameters": parameters,
            }
        )
        if self._error is not None:
            raise self._error
        return _FakeResult(self._rows)


class _FakeSession:
    def __init__(self, database: str | None, transaction: _FakeTransaction) -> None:
        self.database = database
        self.transaction = transaction
        self.closed = False
        self.read_calls = 0
        self.write_calls = 0

    def __enter__(self) -> "_FakeSession":
        return self

    def __exit__(self, exc_type, exc, tb) -> None:
        self.closed = True

    def execute_read(self, work):
        self.read_calls += 1
        return work(self.transaction)

    def execute_write(self, work):
        self.write_calls += 1
        return work(self.transaction)


class _FakeDriver:
    def __init__(self, transaction: _FakeTransaction) -> None:
        self._transaction = transaction
        self.sessions: list[_FakeSession] = []
        self.verify_kwargs: dict[str, Any] | None = None
        self.verify_error: Exception | None = None

    def session(self, **config) -> _FakeSession:
        session = _FakeSession(config.get("database"), self._transaction)
        self.sessions.append(session)
        return session

    def verify_connectivity(self, **kwargs) -> None:
        self.verify_kwargs = kwargs
        if self.verify_error is not None:
            raise self.verify_error


def test_query_all_passes_query_parameters_and_database() -> None:
    """验证 query_all 会透传 Cypher、参数与数据库名。"""
    rows = [{"name": "黄连"}, {"name": "金银花"}]
    transaction = _FakeTransaction(rows=rows)
    driver = _FakeDriver(transaction)
    client = Neo4jClient(driver=driver, default_database="medicine_graph")

    result = client.query_all(
        "MATCH (n:Herb) WHERE n.name CONTAINS $keyword RETURN n.name AS name",
        parameters={"keyword": "花"},
    )

    assert result == rows
    assert driver.sessions[0].database == "medicine_graph"
    assert driver.sessions[0].read_calls == 1
    assert transaction.run_calls == [
        {
            "query": "MATCH (n:Herb) WHERE n.name CONTAINS $keyword RETURN n.name AS name",
            "parameters": {"keyword": "花"},
        }
    ]


def test_query_one_returns_none_when_result_empty() -> None:
    """验证 query_one 在无结果时返回 None。"""
    transaction = _FakeTransaction(rows=[])
    driver = _FakeDriver(transaction)
    client = Neo4jClient(driver=driver, default_database="medicine_graph")

    result = client.query_one("MATCH (n:Herb) RETURN n LIMIT 1")

    assert result is None
    assert driver.sessions[0].read_calls == 1


def test_execute_read_uses_transaction_callback_and_custom_database() -> None:
    """验证 execute_read 会使用读事务并支持显式数据库覆盖。"""
    transaction = _FakeTransaction()
    driver = _FakeDriver(transaction)
    client = Neo4jClient(driver=driver, default_database="medicine_graph")

    result = client.execute_read(
        lambda tx: tx.run("RETURN $value AS value", {"value": 42}),
        database="analytics_graph",
    )

    assert isinstance(result, _FakeResult)
    assert driver.sessions[0].database == "analytics_graph"
    assert driver.sessions[0].read_calls == 1
    assert transaction.run_calls == [
        {
            "query": "RETURN $value AS value",
            "parameters": {"value": 42},
        }
    ]


def test_execute_write_uses_transaction_callback() -> None:
    """验证 execute_write 会使用写事务执行回调。"""
    transaction = _FakeTransaction()
    driver = _FakeDriver(transaction)
    client = Neo4jClient(driver=driver, default_database="medicine_graph")

    result = client.execute_write(
        lambda tx: tx.run("CREATE (n:Herb {name: $name}) RETURN 1", {"name": "黄芪"}),
    )

    assert isinstance(result, _FakeResult)
    assert driver.sessions[0].write_calls == 1
    assert transaction.run_calls == [
        {
            "query": "CREATE (n:Herb {name: $name}) RETURN 1",
            "parameters": {"name": "黄芪"},
        }
    ]


def test_query_all_wraps_neo4j_driver_error() -> None:
    """验证 query_all 遇到驱动异常时会转换为业务异常。"""
    transaction = _FakeTransaction(error=DriverError("boom"))
    driver = _FakeDriver(transaction)
    client = Neo4jClient(driver=driver, default_database="medicine_graph")

    with pytest.raises(ServiceException, match="Neo4j 查询失败"):
        client.query_all("MATCH (n) RETURN n")


def test_execute_write_wraps_neo4j_driver_error() -> None:
    """验证 execute_write 遇到驱动异常时会转换为业务异常。"""
    transaction = _FakeTransaction(error=DriverError("boom"))
    driver = _FakeDriver(transaction)
    client = Neo4jClient(driver=driver, default_database="medicine_graph")

    with pytest.raises(ServiceException, match="Neo4j 写事务执行失败"):
        client.execute_write(lambda tx: tx.run("CREATE (n) RETURN n", {}))


def test_verify_connectivity_uses_default_database() -> None:
    """验证客户端连通性校验会使用默认数据库。"""
    transaction = _FakeTransaction()
    driver = _FakeDriver(transaction)
    client = Neo4jClient(driver=driver, default_database="medicine_graph")

    client.verify_connectivity()

    assert driver.verify_kwargs == {"database": "medicine_graph"}
