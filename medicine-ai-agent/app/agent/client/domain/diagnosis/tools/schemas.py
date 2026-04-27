from __future__ import annotations

from typing import Any

from pydantic import BaseModel, ConfigDict, Field, field_validator


class SymptomCandidate(BaseModel):
    """症状候选实体。"""

    model_config = ConfigDict(extra="forbid")

    symptom: str = Field(description="标准症状名称。")


class DiseaseCandidate(BaseModel):
    """候选疾病实体。"""

    model_config = ConfigDict(extra="forbid")

    disease: str = Field(description="候选疾病名称。")
    matched_symptoms: list[str] = Field(
        default_factory=list,
        description="当前命中的标准症状列表。",
    )
    score: int = Field(description="候选疾病命中得分。")

    @field_validator("matched_symptoms", mode="before")
    @classmethod
    def _normalize_matched_symptoms(cls, value: Any) -> list[str]:
        """归一化候选疾病命中症状列表。"""

        return value


class DiseaseDetail(BaseModel):
    """疾病详情实体。"""

    model_config = ConfigDict(extra="forbid")

    disease: str = Field(description="疾病名称。")
    desc: str | None = Field(default=None, description="疾病简介。")
    cause: str | None = Field(default=None, description="疾病病因。")
    prevent: str | None = Field(default=None, description="疾病预防措施。")
    easy_get: str | None = Field(default=None, description="疾病易感人群。")
    cure_way: list[str] = Field(default_factory=list, description="治疗方式列表。")
    cure_lasttime: str | None = Field(default=None, description="治疗周期说明。")
    cured_prob: str | None = Field(default=None, description="治愈概率说明。")
    get_prob: str | None = Field(default=None, description="发病概率说明。")
    get_way: str | None = Field(default=None, description="传播或感染方式说明。")
    cost_money: str | None = Field(default=None, description="治疗费用说明。")
    yibao_status: str | None = Field(default=None, description="医保状态说明。")
    category: list[str] = Field(default_factory=list, description="疾病分类列表。")
    cure_department: list[str] = Field(default_factory=list, description="原始治疗科室列表。")
    symptoms: list[str] = Field(default_factory=list, description="相关症状列表。")
    checks: list[str] = Field(default_factory=list, description="检查项目列表。")
    common_drugs: list[str] = Field(default_factory=list, description="常用药列表。")
    recommended_drugs: list[str] = Field(default_factory=list, description="推荐药物列表。")
    should_eat: list[str] = Field(default_factory=list, description="宜吃食物列表。")
    avoid_eat: list[str] = Field(default_factory=list, description="忌吃食物列表。")
    recipes: list[str] = Field(default_factory=list, description="推荐食谱列表。")
    departments: list[str] = Field(default_factory=list, description="关联科室列表。")
    complications: list[str] = Field(default_factory=list, description="并发症列表。")
    order_index: int | None = Field(default=None, description="批量查询时的输入顺序。")

    @field_validator(
        "cure_way",
        "category",
        "cure_department",
        "symptoms",
        "checks",
        "common_drugs",
        "recommended_drugs",
        "should_eat",
        "avoid_eat",
        "recipes",
        "departments",
        "complications",
        mode="before",
    )
    @classmethod
    def _normalize_list_fields(cls, value: Any) -> list[str]:
        """归一化疾病详情里的字符串列表字段。"""

        return value


class FollowupSymptomCandidate(BaseModel):
    """追问症状候选实体。"""

    model_config = ConfigDict(extra="forbid")

    symptom: str = Field(description="建议追问的症状名称。")
    candidate_diseases: list[str] = Field(
        default_factory=list,
        description="与该追问症状相关的候选疾病列表。",
    )
    disease_count: int = Field(description="命中该追问症状的候选疾病数量。")

    @field_validator("candidate_diseases", mode="before")
    @classmethod
    def _normalize_candidate_diseases(cls, value: Any) -> list[str]:
        """归一化追问症状关联的候选疾病列表。"""

        return value


__all__ = [
    "DiseaseCandidate",
    "DiseaseDetail",
    "FollowupSymptomCandidate",
    "SymptomCandidate",
]
