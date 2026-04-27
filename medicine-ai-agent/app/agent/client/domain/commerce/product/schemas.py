from __future__ import annotations

from typing import Annotated

from pydantic import BaseModel, ConfigDict, Field, field_validator, model_validator


class ProductIdsRequest(BaseModel):
    """
    功能描述：
        按商品 ID 列表批量查询的请求参数模型。

    参数说明：
        product_ids (list[int]): 商品 ID 列表。

    返回值：
        无（数据模型定义）。

    异常说明：
        无。
    """

    model_config = ConfigDict(extra="forbid")

    product_ids: list[Annotated[int, Field(ge=1, description="商品 ID")]] = Field(
        ...,
        min_length=1,
        max_length=20,
        description="商品 ID 列表。",
    )


class ProductSearchRequest(BaseModel):
    """
    功能描述：
        商品搜索请求参数模型。

    参数说明：
        keyword (str | None): 搜索关键词。
        category_name (str | None): 商品分类名称。
        usage (str | None): 商品用途或适用场景。
        tag_names (list[str] | None): 商品标签名称列表，辅助筛选。
        page_num (int): 页码。
        page_size (int): 每页数量。

    返回值：
        无（数据模型定义）。

    异常说明：
        ValueError: 当搜索条件全部为空时抛出。
    """

    model_config = ConfigDict(extra="forbid")

    keyword: str | None = Field(default=None, description="搜索关键词")
    category_name: str | None = Field(default=None, description="商品分类名称")
    usage: str | None = Field(default=None, description="商品用途或适用场景")
    tag_names: list[str] | None = Field(default=None, description="商品标签名称列表，辅助筛选")
    page_num: int = Field(default=1, ge=1, description="页码，从 1 开始")
    page_size: int = Field(
        default=10,
        ge=1,
        le=20,
        description="每页数量，范围 1-20",
    )

    @field_validator("keyword", "category_name", "usage")
    @classmethod
    def normalize_optional_text(cls, value: str | None) -> str | None:
        """
        功能描述：
            规范化可选文本查询条件。

        参数说明：
            value (str | None): 原始文本条件。

        返回值：
            str | None: 去空白后的文本；空串归一化为 `None`。

        异常说明：
            无。
        """

        if value is None:
            return None
        normalized = value.strip()
        return normalized or None

    @field_validator("tag_names")
    @classmethod
    def normalize_tag_names(cls, value: list[str] | None) -> list[str] | None:
        """
        功能描述：
            规范化标签名称列表，去除空白项。

        参数说明：
            value (list[str] | None): 原始标签名称列表。

        返回值：
            list[str] | None: 过滤后的非空标签名称列表；全部为空时归一化为 `None`。

        异常说明：
            无。
        """

        if value is None:
            return None
        cleaned = [name.strip() for name in value if name and name.strip()]
        return cleaned or None

    @model_validator(mode="after")
    def validate_query_present(self) -> "ProductSearchRequest":
        """
        功能描述：
            校验至少存在一个有效搜索条件。

        参数说明：
            无。

        返回值：
            ProductSearchRequest: 当前模型实例。

        异常说明：
            ValueError: 当搜索条件全部为空时抛出。
        """

        if not any([self.keyword, self.category_name, self.usage, self.tag_names]):
            raise ValueError("keyword、category_name、usage、tag_names 不能同时为空")
        return self


__all__ = [
    "ProductIdsRequest",
    "ProductSearchRequest",
]
