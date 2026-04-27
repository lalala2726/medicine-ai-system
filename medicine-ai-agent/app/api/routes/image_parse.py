"""
图像解析 API 路由

接收图片 URL 并调用大模型进行结构化解析。
"""

from typing import List

from fastapi import APIRouter
from pydantic import AliasChoices, BaseModel, Field

from app.schemas.response import ApiResponse
from app.services.image_parse_service import parse_drug_images, parse_product_tag_images

router = APIRouter(prefix="/image_parse", tags=["图像解析"])


class ImageParseRequest(BaseModel):
    """图像解析请求参数"""

    image_urls: List[str] = Field(
        ...,
        min_length=1,
        max_length=5,
        validation_alias=AliasChoices("image_urls", "images"),
        description="图片 URL 列表，最少1张，最多5张",
    )


class TagGroupItemRequest(BaseModel):
    """标签项请求参数"""

    id: str = Field(..., description="标签 ID")
    name: str = Field(..., description="标签名称")


class TagGroupRequest(BaseModel):
    """标签分组请求参数"""

    typeName: str = Field(..., description="标签类型名称")
    tags: List[TagGroupItemRequest] = Field(..., description="当前类型下的标签列表")


class ProductTagParseRequest(BaseModel):
    """商品标签图片识别请求参数"""

    image_urls: List[str] = Field(
        ...,
        min_length=1,
        max_length=5,
        validation_alias=AliasChoices("image_urls", "images"),
        description="图片 URL 列表，最少1张，最多5张",
    )
    tag_groups: List[TagGroupRequest] = Field(
        ...,
        min_length=1,
        validation_alias=AliasChoices("tag_groups", "tagGroups"),
        description="按标签类型分组的所有可用标签列表",
    )


@router.post("/drug", summary="解析药品图片")
def parse_image(request: ImageParseRequest) -> ApiResponse[dict]:
    """
    接收图片 URL 并使用大模型进行解析

    Args:
        request: 图像解析请求参数

    Returns:
        ApiResponse[dict]: 解析结果

    Raises:
        ServiceException: 图片列表为空时抛出异常
    """

    data = parse_drug_images(request.image_urls)
    return ApiResponse.success(data=data, message="解析成功")


@router.post("/product_tag", summary="识别商品标签")
def parse_product_tags(request: ProductTagParseRequest) -> ApiResponse[dict]:
    """
    接收药品图片与标签列表，使用大模型识别匹配的商品标签。

    Args:
        request: 商品标签图片识别请求参数

    Returns:
        ApiResponse[dict]: 匹配结果，包含 matchedTagIds、confidence、reasoning

    Raises:
        ServiceException: 图片或标签列表为空时抛出异常
    """

    tag_groups_dicts = [group.model_dump() for group in request.tag_groups]
    data = parse_product_tag_images(request.image_urls, tag_groups_dicts)
    return ApiResponse.success(data=data, message="标签识别成功")
