from __future__ import annotations

from typing import Any, LiteralString

from app.agent import graph_tool as graph_tools_module


class _FakeNeo4jClient:
    """医学图谱工具测试用 Neo4j 客户端桩。"""

    def __init__(self) -> None:
        """初始化测试桩客户端。

        Args:
            无。

        Returns:
            None: 构造函数无返回值。
        """

        self.query_all_calls: list[dict[str, Any]] = []
        self.query_one_calls: list[dict[str, Any]] = []
        self.query_all_result: list[dict[str, Any]] = []
        self.query_one_result: dict[str, Any] | None = None

    def query_all(
            self,
            query: LiteralString,
            parameters: dict[str, Any] | None = None,
            database: str | None = None,
    ) -> list[dict[str, Any]]:
        """记录批量查询调用并返回预置结果。

        Args:
            query: Cypher 字面量查询语句。
            parameters: 查询参数字典。
            database: 查询使用的数据库名。

        Returns:
            list[dict[str, Any]]: 预置的批量查询结果。
        """

        self.query_all_calls.append(
            {
                "query": query,
                "parameters": parameters,
                "database": database,
            }
        )
        return list(self.query_all_result)

    def query_one(
            self,
            query: LiteralString,
            parameters: dict[str, Any] | None = None,
            database: str | None = None,
    ) -> dict[str, Any] | None:
        """记录单条查询调用并返回预置结果。

        Args:
            query: Cypher 字面量查询语句。
            parameters: 查询参数字典。
            database: 查询使用的数据库名。

        Returns:
            dict[str, Any] | None: 预置的单条查询结果。
        """

        self.query_one_calls.append(
            {
                "query": query,
                "parameters": parameters,
                "database": database,
            }
        )
        return None if self.query_one_result is None else dict(self.query_one_result)


def test_search_symptom_candidates_normalizes_keyword_and_limit(
        monkeypatch,
) -> None:
    """验证口语症状候选工具会规范化关键词并透传 limit。"""

    fake_client = _FakeNeo4jClient()
    fake_client.query_all_result = [{"symptom": "咽痛"}]
    monkeypatch.setattr(graph_tools_module, "get_neo4j_client", lambda: fake_client)

    result = graph_tools_module.search_symptom_candidates.invoke(
        {"keyword": "  喉咙疼  ", "limit": 5}
    )

    assert result == [{"symptom": "咽痛"}]
    assert fake_client.query_all_calls == [
        {
            "query": graph_tools_module.SEARCH_SYMPTOM_CANDIDATES_CYPHER,
            "parameters": {
                "keyword": "喉咙疼",
                "limit": 5,
            },
            "database": graph_tools_module.MEDICAL_GRAPH_DATABASE,
        }
    ]


def test_query_disease_candidates_by_symptoms_normalizes_symptoms_and_database(
        monkeypatch,
) -> None:
    """验证标准症状召回工具会去重症状并固定查询 medicine 数据库。"""

    fake_client = _FakeNeo4jClient()
    fake_client.query_all_result = [
        {
            "disease": "上呼吸道感染",
            "matched_symptoms": ["咽痛", "鼻塞"],
            "score": 2,
        }
    ]
    monkeypatch.setattr(graph_tools_module, "get_neo4j_client", lambda: fake_client)

    result = graph_tools_module.query_disease_candidates_by_symptoms.invoke(
        {
            "symptoms": [" 咽痛 ", "鼻塞", "咽痛"],
            "limit": 8,
        }
    )

    assert result == fake_client.query_all_result
    assert fake_client.query_all_calls == [
        {
            "query": graph_tools_module.QUERY_DISEASE_CANDIDATES_BY_SYMPTOMS_CYPHER,
            "parameters": {
                "symptoms": ["咽痛", "鼻塞"],
                "limit": 8,
            },
            "database": graph_tools_module.MEDICAL_GRAPH_DATABASE,
        }
    ]


def test_query_disease_detail_returns_detail_when_hit(monkeypatch) -> None:
    """验证疾病详情工具命中时返回详情字典。"""

    fake_client = _FakeNeo4jClient()
    fake_client.query_one_result = {
        "disease": "上呼吸道感染",
        "desc": "常见上呼吸道急性感染。",
        "symptoms": ["咽痛", "鼻塞"],
    }
    monkeypatch.setattr(graph_tools_module, "get_neo4j_client", lambda: fake_client)

    result = graph_tools_module.query_disease_detail.invoke(
        {"disease_name": "  上呼吸道感染  "}
    )

    assert result == fake_client.query_one_result
    assert fake_client.query_one_calls == [
        {
            "query": graph_tools_module.QUERY_DISEASE_DETAIL_CYPHER,
            "parameters": {
                "disease_name": "上呼吸道感染",
            },
            "database": graph_tools_module.MEDICAL_GRAPH_DATABASE,
        }
    ]


def test_query_disease_detail_returns_none_when_miss(monkeypatch) -> None:
    """验证疾病详情工具未命中时返回 None。"""

    fake_client = _FakeNeo4jClient()
    fake_client.query_one_result = None
    monkeypatch.setattr(graph_tools_module, "get_neo4j_client", lambda: fake_client)

    result = graph_tools_module.query_disease_detail.invoke(
        {"disease_name": "不存在的疾病"}
    )

    assert result is None
    assert fake_client.query_one_calls == [
        {
            "query": graph_tools_module.QUERY_DISEASE_DETAIL_CYPHER,
            "parameters": {
                "disease_name": "不存在的疾病",
            },
            "database": graph_tools_module.MEDICAL_GRAPH_DATABASE,
        }
    ]


def test_query_followup_symptom_candidates_filters_known_symptoms(
        monkeypatch,
) -> None:
    """验证追问症状候选工具会透传候选疾病、已知症状和排序查询。"""

    fake_client = _FakeNeo4jClient()
    fake_client.query_all_result = [
        {
            "symptom": "打喷嚏",
            "candidate_diseases": ["上呼吸道感染", "病毒性感冒"],
            "disease_count": 2,
        }
    ]
    monkeypatch.setattr(graph_tools_module, "get_neo4j_client", lambda: fake_client)

    result = graph_tools_module.query_followup_symptom_candidates.invoke(
        {
            "candidate_diseases": [" 上呼吸道感染 ", "病毒性感冒", "上呼吸道感染"],
            "known_symptoms": [" 咽痛 ", "鼻塞", "咽痛"],
            "limit": 6,
        }
    )

    assert result == fake_client.query_all_result
    assert fake_client.query_all_calls == [
        {
            "query": graph_tools_module.QUERY_FOLLOWUP_SYMPTOM_CANDIDATES_CYPHER,
            "parameters": {
                "candidate_diseases": ["上呼吸道感染", "病毒性感冒"],
                "known_symptoms": ["咽痛", "鼻塞"],
                "limit": 6,
            },
            "database": graph_tools_module.MEDICAL_GRAPH_DATABASE,
        }
    ]
    assert (
            "ORDER BY balance_distance ASC, disease_count DESC, symptom ASC"
            in graph_tools_module.QUERY_FOLLOWUP_SYMPTOM_CANDIDATES_CYPHER
    )
