from __future__ import annotations

from typing import Any, LiteralString, TypeVar

from langchain_core.tools import tool
from pydantic import BaseModel, ConfigDict, Field

from app.agent.client.domain.diagnosis.tools.schemas import (
    DiseaseCandidate,
    DiseaseDetail,
    FollowupSymptomCandidate,
    SymptomCandidate,
)
from app.core.agent.middleware import (
    tool_call_status,
    tool_thinking_redaction,
)
from app.core.database.neo4j.client import get_neo4j_client
from app.utils.list_utils import TextListUtils

# 医学图谱工具固定查询的业务数据库名称。
MEDICAL_GRAPH_DATABASE = "medicine"
# 图谱症状关键词规范化阶段固定使用的模型名称。
GRAPH_KEYWORD_MODEL_NAME = "qwen-flash"
# 图谱工具默认返回条数。
DEFAULT_GRAPH_QUERY_LIMIT = 10
# 首轮候选疾病召回默认返回条数。
DEFAULT_DISEASE_CANDIDATE_LIMIT = 5
# 图谱工具允许的最大返回条数。
MAX_GRAPH_QUERY_LIMIT = 50
# 批量查询疾病详情时允许的最大疾病数量。
MAX_BATCH_DISEASE_DETAIL_COUNT = 5

# 症状候选检索的固定 Cypher 查询语句。
SEARCH_SYMPTOM_CANDIDATES_CYPHER: LiteralString = """
    UNWIND $keywords AS keyword
    MATCH (s:Symptom)
    WHERE s.name CONTAINS keyword
    WITH
      s,
      collect(DISTINCT keyword) AS matched_keywords,
      min(
        CASE
          WHEN s.name = keyword THEN 0
          WHEN s.name STARTS WITH keyword THEN 1
          ELSE 2
        END
      ) AS match_priority,
      max(size(keyword)) AS matched_keyword_length
    RETURN s.name AS symptom
    ORDER BY
      match_priority ASC,
      size(matched_keywords) DESC,
      matched_keyword_length DESC,
      size(s.name) ASC,
      s.name ASC
    LIMIT toInteger($limit)
"""

# 标准症状召回候选疾病的固定 Cypher 查询语句。
QUERY_DISEASE_CANDIDATES_BY_SYMPTOMS_CYPHER: LiteralString = """
    MATCH (d:Disease)-[:has_symptom]->(s:Symptom)
    WHERE s.name IN $symptoms
    RETURN
      d.name AS disease,
      collect(DISTINCT s.name) AS matched_symptoms,
      count(DISTINCT s) AS score
    ORDER BY score DESC, disease ASC
    LIMIT toInteger($limit)
"""

# 疾病详情快照查询的固定 Cypher 查询语句。
QUERY_DISEASE_DETAIL_CYPHER: LiteralString = """
    MATCH (d:Disease {name: $disease_name})
    OPTIONAL MATCH (d)-[:has_symptom]->(s:Symptom)
    OPTIONAL MATCH (d)-[:need_check]->(c:Check)
    OPTIONAL MATCH (d)-[:common_drug]->(cd:Drug)
    OPTIONAL MATCH (d)-[:recommand_drug]->(rd:Drug)
    OPTIONAL MATCH (d)-[:do_eat]->(food_ok:Food)
    OPTIONAL MATCH (d)-[:no_eat]->(food_bad:Food)
    OPTIONAL MATCH (d)-[:recommand_eat]->(recipe:Food)
    OPTIONAL MATCH (d)-[:belongs_to]->(dep:Department)
    OPTIONAL MATCH (d)-[:acompany_with]->(comp:Disease)
    RETURN
      d.name AS disease,
      d.desc AS desc,
      d.cause AS cause,
      d.prevent AS prevent,
      d.easy_get AS easy_get,
      d.cure_way AS cure_way,
      d.cure_lasttime AS cure_lasttime,
      d.cured_prob AS cured_prob,
      d.get_prob AS get_prob,
      d.get_way AS get_way,
      d.cost_money AS cost_money,
      d.yibao_status AS yibao_status,
      d.category AS category,
      d.cure_department AS cure_department,
      collect(DISTINCT s.name) AS symptoms,
      collect(DISTINCT c.name) AS checks,
      collect(DISTINCT cd.name) AS common_drugs,
      collect(DISTINCT rd.name) AS recommended_drugs,
      collect(DISTINCT food_ok.name) AS should_eat,
      collect(DISTINCT food_bad.name) AS avoid_eat,
      collect(DISTINCT recipe.name) AS recipes,
      collect(DISTINCT dep.name) AS departments,
      collect(DISTINCT comp.name) AS complications
"""

# 疾病详情批量快照查询的固定 Cypher 查询语句。
QUERY_DISEASE_DETAILS_CYPHER: LiteralString = """
    UNWIND range(0, size($disease_names) - 1) AS idx
    WITH idx, $disease_names[idx] AS disease_name
    MATCH (d:Disease {name: disease_name})
    OPTIONAL MATCH (d)-[:has_symptom]->(s:Symptom)
    OPTIONAL MATCH (d)-[:need_check]->(c:Check)
    OPTIONAL MATCH (d)-[:common_drug]->(cd:Drug)
    OPTIONAL MATCH (d)-[:recommand_drug]->(rd:Drug)
    OPTIONAL MATCH (d)-[:do_eat]->(food_ok:Food)
    OPTIONAL MATCH (d)-[:no_eat]->(food_bad:Food)
    OPTIONAL MATCH (d)-[:recommand_eat]->(recipe:Food)
    OPTIONAL MATCH (d)-[:belongs_to]->(dep:Department)
    OPTIONAL MATCH (d)-[:acompany_with]->(comp:Disease)
    RETURN
      idx AS order_index,
      d.name AS disease,
      d.desc AS desc,
      d.cause AS cause,
      d.prevent AS prevent,
      d.easy_get AS easy_get,
      d.cure_way AS cure_way,
      d.cure_lasttime AS cure_lasttime,
      d.cured_prob AS cured_prob,
      d.get_prob AS get_prob,
      d.get_way AS get_way,
      d.cost_money AS cost_money,
      d.yibao_status AS yibao_status,
      d.category AS category,
      d.cure_department AS cure_department,
      collect(DISTINCT s.name) AS symptoms,
      collect(DISTINCT c.name) AS checks,
      collect(DISTINCT cd.name) AS common_drugs,
      collect(DISTINCT rd.name) AS recommended_drugs,
      collect(DISTINCT food_ok.name) AS should_eat,
      collect(DISTINCT food_bad.name) AS avoid_eat,
      collect(DISTINCT recipe.name) AS recipes,
      collect(DISTINCT dep.name) AS departments,
      collect(DISTINCT comp.name) AS complications
    ORDER BY order_index ASC
"""

# 候选疾病差异症状查询的固定 Cypher 查询语句。
QUERY_FOLLOWUP_SYMPTOM_CANDIDATES_CYPHER: LiteralString = """
    MATCH (d:Disease)-[:has_symptom]->(s:Symptom)
    WHERE d.name IN $candidate_diseases
      AND NOT (s.name IN $known_symptoms)
    WITH
      s.name AS symptom,
      collect(DISTINCT d.name) AS candidate_diseases,
      count(DISTINCT d) AS disease_count,
      size($candidate_diseases) AS total_candidate_count
    WHERE disease_count < total_candidate_count
    WITH
      symptom,
      candidate_diseases,
      disease_count,
      abs((disease_count * 2) - total_candidate_count) AS balance_distance
    RETURN
      symptom,
      candidate_diseases,
      disease_count
    ORDER BY balance_distance ASC, disease_count DESC, symptom ASC
    LIMIT toInteger($limit)
"""

DiagnosisToolSchema = TypeVar("DiagnosisToolSchema", bound=BaseModel)


def _normalize_required_text(value: str, *, field_name: str) -> str:
    """规范化必填字符串字段。

    Args:
        value: 原始字符串值。
        field_name: 字段名称。

    Returns:
        str: 去除首尾空白后的非空字符串。

    Raises:
        ValueError: 归一化后为空时抛出。
    """

    normalized_value = str(value or "").strip()
    if not normalized_value:
        raise ValueError(f"{field_name} 不能为空")
    return normalized_value


def _parse_graph_result_list(
        raw_rows: list[dict[str, Any]],
        *,
        schema_type: type[DiagnosisToolSchema],
) -> list[DiagnosisToolSchema]:
    """将图谱查询结果列表转换为实体列表。

    Args:
        raw_rows: Neo4j 查询返回的原始字典列表。
        schema_type: 目标实体类型。

    Returns:
        list[DiagnosisToolSchema]: 转换后的实体列表。
    """

    return [schema_type.model_validate(raw_row) for raw_row in raw_rows]


def _parse_graph_result_one(
        raw_row: dict[str, Any] | None,
        *,
        schema_type: type[DiagnosisToolSchema],
) -> DiagnosisToolSchema | None:
    """将单条图谱查询结果转换为实体。

    Args:
        raw_row: Neo4j 查询返回的原始字典。
        schema_type: 目标实体类型。

    Returns:
        DiagnosisToolSchema | None: 转换后的实体；未命中时返回 ``None``。
    """

    if raw_row is None:
        return None
    return schema_type.model_validate(raw_row)


class SearchSymptomCandidatesRequest(BaseModel):
    """症状候选检索工具入参。"""

    model_config = ConfigDict(extra="forbid")

    keywords: list[str] = Field(
        ...,
        min_length=1,
        description=(
            "用于检索标准症状的关键词列表。"
            "要求先把用户原始描述拆成多个短词、近义词或标准化候选后再传入，"
        ),
    )
    limit: int = Field(
        default=DEFAULT_GRAPH_QUERY_LIMIT,
        ge=1,
        le=MAX_GRAPH_QUERY_LIMIT,
        description="最多返回的候选症状数量。",
    )


class QueryDiseaseCandidatesBySymptomsRequest(BaseModel):
    """标准症状召回候选疾病工具入参。"""

    model_config = ConfigDict(extra="forbid")

    symptoms: list[str] = Field(
        ...,
        min_length=1,
        description="标准症状列表，例如 ['喉咙痛', '咽痛', '咽喉疼痛']。",
    )
    limit: int = Field(
        default=DEFAULT_DISEASE_CANDIDATE_LIMIT,
        ge=1,
        le=MAX_GRAPH_QUERY_LIMIT,
        description="最多返回的候选疾病数量。",
    )


class QueryDiseaseDetailRequest(BaseModel):
    """疾病详情查询工具入参。"""

    model_config = ConfigDict(extra="forbid")

    disease_name: str = Field(
        ...,
        min_length=1,
        description="疾病名称，例如 '上呼吸道感染'。",
    )


class QueryDiseaseDetailsRequest(BaseModel):
    """疾病详情批量查询工具入参。"""

    model_config = ConfigDict(extra="forbid")

    disease_names: list[str] = Field(
        ...,
        min_length=1,
        max_length=MAX_BATCH_DISEASE_DETAIL_COUNT,
        description="需要批量查询的疾病名称列表，建议传入 2 到 5 个候选疾病。",
    )


class QueryFollowupSymptomCandidatesRequest(BaseModel):
    """追问症状候选查询工具入参。"""

    model_config = ConfigDict(extra="forbid")

    candidate_diseases: list[str] = Field(
        ...,
        min_length=1,
        description="当前候选疾病列表。",
    )
    known_symptoms: list[str] = Field(
        default_factory=list,
        description="当前已知症状列表，用于过滤不需要重复追问的症状。",
    )
    limit: int = Field(
        default=DEFAULT_GRAPH_QUERY_LIMIT,
        ge=1,
        le=MAX_GRAPH_QUERY_LIMIT,
        description="最多返回的追问症状数量。",
    )


@tool(
    args_schema=SearchSymptomCandidatesRequest,
    description=(
            "检索医学图谱中的标准症状候选。"
            "调用前必须先把用户原始症状拆成多个可检索关键词列表，"
            "不要直接把整句口语原样传入。"
            "例如用户说“喉咙疼”，应优先拆成 ['喉', '喉咙', '咽喉', '咽痛', '嗓子疼'] 这类列表。"
            "调用时机：用户给的是口语化症状，且你需要先把自然语言症状映射成图谱标准症状时。"
    ),
)
@tool_thinking_redaction(display_name="症状归类检索")
@tool_call_status(
    tool_name="症状归类检索",
    start_message="正在查询医药数据库，核对症状归类",
    error_message="症状归类检索失败",
    timely_message="症状归类仍在处理中",
)
def search_symptom_candidates(
        keywords: list[str],
        limit: int = DEFAULT_GRAPH_QUERY_LIMIT,
) -> list[SymptomCandidate]:
    """检索标准症状候选。

    Args:
        keywords: 用户口语症状拆解后的关键词列表。
        limit: 最多返回的候选症状数量。

    Returns:
        list[SymptomCandidate]: 标准症状候选实体列表。
    """

    raw_rows = get_neo4j_client().query_all(
        SEARCH_SYMPTOM_CANDIDATES_CYPHER,
        parameters={
            "keywords": keywords,
            "limit": limit,
        },
        database=MEDICAL_GRAPH_DATABASE,
    )
    symptom_candidates = _parse_graph_result_list(
        raw_rows,
        schema_type=SymptomCandidate,
    )
    return symptom_candidates


@tool(
    args_schema=QueryDiseaseCandidatesBySymptomsRequest,
    description=(
            "按标准症状列表召回候选疾病。"
            "调用前要先确认你手里已经是较可靠的标准症状，而不是原始口语。"
            "如果当前信息还太少，优先继续追问，不要急着下结论。"
            "第一轮候选疾病建议直接查询前 5 个。"
            "调用时机：已经把用户口语症状映射成标准症状后，用于生成候选疾病池。"
    ),
)
@tool_thinking_redaction(display_name="候选疾病检索")
@tool_call_status(
    tool_name="候选疾病检索",
    start_message="正在查询医药数据库，缩小可能范围",
    error_message="候选疾病检索失败",
    timely_message="候选疾病检索仍在处理中",
)
def query_disease_candidates_by_symptoms(
        symptoms: list[str],
        limit: int = DEFAULT_DISEASE_CANDIDATE_LIMIT,
) -> list[DiseaseCandidate]:
    """按标准症状查询候选疾病。

    Args:
        symptoms: 需要参与查询的标准症状列表。
        limit: 最多返回的候选疾病数量。

    Returns:
        list[DiseaseCandidate]: 候选疾病实体列表。
    """

    normalized_symptoms = TextListUtils.normalize_required(
        symptoms,
        field_name="symptoms",
    )
    raw_rows = get_neo4j_client().query_all(
        QUERY_DISEASE_CANDIDATES_BY_SYMPTOMS_CYPHER,
        parameters={
            "symptoms": normalized_symptoms,
            "limit": limit,
        },
        database=MEDICAL_GRAPH_DATABASE,
    )
    disease_candidates = _parse_graph_result_list(
        raw_rows,
        schema_type=DiseaseCandidate,
    )
    return disease_candidates


@tool(
    args_schema=QueryDiseaseDetailRequest,
    description=(
            "查询疾病详情快照。"
            "仅适用于用户明确只问单个疾病，或你只需要查询单个疾病详情时。"
            "如果你需要比较多个候选疾病，禁止重复多次调用本工具，必须优先使用“批量查询疾病详情快照”。"
    ),
)
@tool_thinking_redaction(display_name="单个疾病详情查询")
@tool_call_status(
    tool_name="单个疾病详情查询",
    start_message="正在查询相关疾病资料",
    error_message="疾病详情查询失败",
    timely_message="疾病详情仍在处理中",
)
def query_disease_detail(disease_name: str) -> DiseaseDetail | None:
    """查询疾病详情快照。

    Args:
        disease_name: 需要查询的疾病名称。

    Returns:
        DiseaseDetail | None: 疾病详情实体；未命中时返回 `None`。
    """

    normalized_disease_name = _normalize_required_text(
        disease_name,
        field_name="disease_name",
    )
    raw_row = get_neo4j_client().query_one(
        QUERY_DISEASE_DETAIL_CYPHER,
        parameters={"disease_name": normalized_disease_name},
        database=MEDICAL_GRAPH_DATABASE,
    )
    disease_detail = _parse_graph_result_one(raw_row, schema_type=DiseaseDetail)
    return disease_detail


@tool(
    args_schema=QueryDiseaseDetailsRequest,
    description=(
            "批量查询多个候选疾病的详情快照。"
            "调用时机：你已经把候选疾病收敛到 2 到 5 个，需要一次性比较这些疾病的症状、检查、药物、饮食、科室、并发症等信息时。"
            "如果需要比较多个候选疾病，禁止逐个调用单疾病详情工具，必须优先使用本工具一次性查询。"
    ),
)
@tool_thinking_redaction(display_name="候选疾病批量详情查询")
@tool_call_status(
    tool_name="候选疾病批量详情查询",
    start_message="正在批量核对几种可能情况的详细资料",
    error_message="批量疾病详情查询失败",
    timely_message="候选疾病详情仍在处理中",
)
def query_disease_details(disease_names: list[str]) -> list[DiseaseDetail]:
    """批量查询多个候选疾病的详情快照。

    Args:
        disease_names: 需要批量查询的疾病名称列表。

    Returns:
        list[DiseaseDetail]: 按输入顺序返回的疾病详情实体列表。
    """

    normalized_disease_names = TextListUtils.normalize_required(
        disease_names,
        field_name="disease_names",
    )
    raw_rows = get_neo4j_client().query_all(
        QUERY_DISEASE_DETAILS_CYPHER,
        parameters={"disease_names": normalized_disease_names},
        database=MEDICAL_GRAPH_DATABASE,
    )
    disease_details = _parse_graph_result_list(raw_rows, schema_type=DiseaseDetail)
    return disease_details


@tool(
    args_schema=QueryFollowupSymptomCandidatesRequest,
    description=(
            "基于候选疾病列表生成下一轮追问症状候选。"
            "这个工具是为了继续提问缩小范围，不是为了直接给出诊断。"
            "调用时机：已有前 5 个左右候选疾病，但当前还不能稳定判断时，用它找最能区分这些候选疾病的症状。"
    ),
)
@tool_thinking_redaction(display_name="差异症状分析")
@tool_call_status(
    tool_name="差异症状分析",
    start_message="正在分析哪些症状最有区分度",
    error_message="差异症状分析失败",
    timely_message="差异症状分析仍在处理中",
)
def query_followup_symptom_candidates(
        candidate_diseases: list[str],
        known_symptoms: list[str] | None = None,
        limit: int = DEFAULT_GRAPH_QUERY_LIMIT,
) -> list[FollowupSymptomCandidate]:
    """查询下一轮追问症状候选。

    Args:
        candidate_diseases: 当前候选疾病列表。
        known_symptoms: 当前已知症状列表。
        limit: 最多返回的追问症状数量。

    Returns:
        list[FollowupSymptomCandidate]: 追问症状候选实体列表。
    """

    normalized_candidate_diseases = TextListUtils.normalize_required(
        candidate_diseases,
        field_name="candidate_diseases",
    )
    normalized_known_symptoms = TextListUtils.normalize(known_symptoms)
    raw_rows = get_neo4j_client().query_all(
        QUERY_FOLLOWUP_SYMPTOM_CANDIDATES_CYPHER,
        parameters={
            "candidate_diseases": normalized_candidate_diseases,
            "known_symptoms": normalized_known_symptoms,
            "limit": limit,
        },
        database=MEDICAL_GRAPH_DATABASE,
    )
    followup_symptom_candidates = _parse_graph_result_list(
        raw_rows,
        schema_type=FollowupSymptomCandidate,
    )
    return followup_symptom_candidates


__all__ = [
    "DEFAULT_GRAPH_QUERY_LIMIT",
    "DEFAULT_DISEASE_CANDIDATE_LIMIT",
    "MAX_BATCH_DISEASE_DETAIL_COUNT",
    "MAX_GRAPH_QUERY_LIMIT",
    "MEDICAL_GRAPH_DATABASE",
    "QUERY_DISEASE_CANDIDATES_BY_SYMPTOMS_CYPHER",
    "QUERY_DISEASE_DETAIL_CYPHER",
    "QUERY_DISEASE_DETAILS_CYPHER",
    "QUERY_FOLLOWUP_SYMPTOM_CANDIDATES_CYPHER",
    "QueryDiseaseCandidatesBySymptomsRequest",
    "QueryDiseaseDetailRequest",
    "QueryDiseaseDetailsRequest",
    "QueryFollowupSymptomCandidatesRequest",
    "SEARCH_SYMPTOM_CANDIDATES_CYPHER",
    "SearchSymptomCandidatesRequest",
    "DiseaseCandidate",
    "DiseaseDetail",
    "FollowupSymptomCandidate",
    "SymptomCandidate",
    "query_disease_candidates_by_symptoms",
    "query_disease_detail",
    "query_disease_details",
    "query_followup_symptom_candidates",
    "search_symptom_candidates",
]
