import json
from typing import Any, List

from langchain_core.messages import HumanMessage
from pydantic import BaseModel, Field, ValidationError

from app.core.codes import ResponseCode
from app.core.config_sync import create_agent_image_llm
from app.core.exception.exceptions import ServiceException
from app.utils.prompt_utils import load_managed_prompt

# 药品图片解析提示词业务键。
_DRUG_PARSER_PROMPT_KEY = "image_parser_drug_prompt"
# 药品图片解析提示词本地回退路径。
_DRUG_PARSER_PROMPT_LOCAL_PATH = "image_parser/drug_prompt.md"

# 商品标签图片识别提示词业务键。
_TAG_PARSER_PROMPT_KEY = "image_parser_product_tag_prompt"
# 商品标签图片识别提示词本地回退路径。
_TAG_PARSER_PROMPT_LOCAL_PATH = "image_parser/product_tag_prompt.md"

# 药品图片解析结构化输出配置。
_JSON_OBJECT_RESPONSE_FORMAT: dict[str, Any] = {
    "response_format": {"type": "json_object"},
}


class ImageCompletionParseError(ValueError):
    """
    功能描述：
        标识图片模型响应内容无法提取为有效文本的解析异常。

    参数说明：
        无。

    返回值：
        无。

    异常说明：
        无。
    """


class DrugImageSchema(BaseModel):
    """
    功能描述：
        药品图片结构化识别结果 Schema，约束图片解析返回字段与类型。

    参数说明：
        commonName (str | None): 药品通用名。
        brand (str | None): 品牌名称。
        composition (str | None): 成分信息。
        characteristics (str | None): 性状描述。
        packaging (str | None): 包装规格。
        validityPeriod (str | None): 有效期。
        storageConditions (str | None): 贮藏条件。
        productionUnit (str | None): 生产单位。
        approvalNumber (str | None): 批准文号。
        executiveStandard (str | None): 执行标准。
        originType (str | None): 产地类型（国产/进口）。
        isOutpatientMedicine (bool | None): 是否外用药。
        prescription (bool | None): 是否处方药。
        efficacy (str | None): 功能主治。
        usageMethod (str | None): 用法用量。
        adverseReactions (str | None): 不良反应。
        precautions (str | None): 注意事项。
        taboo (str | None): 禁忌。
        warmTips (str | None): 温馨提示。
        instruction (str | None): 说明书文本。

    返回值：
        无（数据模型定义）。

    异常说明：
        pydantic.ValidationError: 当字段类型不符合约束时抛出。
    """

    commonName: str | None = Field(default=None)
    brand: str | None = Field(default=None)
    composition: str | None = Field(default=None)
    characteristics: str | None = Field(default=None)
    packaging: str | None = Field(default=None)
    validityPeriod: str | None = Field(default=None)
    storageConditions: str | None = Field(default=None)
    productionUnit: str | None = Field(default=None)
    approvalNumber: str | None = Field(default=None)
    executiveStandard: str | None = Field(default=None)
    originType: str | None = Field(default=None)
    isOutpatientMedicine: bool | None = Field(default=None)
    prescription: bool | None = Field(default=None)
    efficacy: str | None = Field(default=None)
    usageMethod: str | None = Field(default=None)
    adverseReactions: str | None = Field(default=None)
    precautions: str | None = Field(default=None)
    taboo: str | None = Field(default=None)
    warmTips: str | None = Field(default=None)
    instruction: str | None = Field(default=None)


def _extract_completion_text(raw_content: Any) -> str:
    """
    功能描述：
        从图片模型响应的 `content` 字段提取文本。

    参数说明：
        raw_content (Any): 模型返回对象中的 `content` 原始值。

    返回值：
        str: 归一化后的文本内容。

    异常说明：
        ImageCompletionParseError: 当 `content` 不包含有效文本时抛出。
    """

    if isinstance(raw_content, str):
        normalized_text = raw_content.strip()
        if normalized_text:
            return normalized_text
        raise ImageCompletionParseError("completion content is empty")

    if isinstance(raw_content, list):
        text_parts: list[str] = []
        for part in raw_content:
            if isinstance(part, str):
                normalized_text = part.strip()
                if normalized_text:
                    text_parts.append(normalized_text)
                continue
            if isinstance(part, dict):
                normalized_text = str(part.get("text") or "").strip()
                if normalized_text:
                    text_parts.append(normalized_text)
                continue
            normalized_text = str(getattr(part, "text", "") or "").strip()
            if normalized_text:
                text_parts.append(normalized_text)
        merged_text = "\n".join(text_parts).strip()
        if merged_text:
            return merged_text
        raise ImageCompletionParseError("completion content is empty")

    normalized_text = str(raw_content or "").strip()
    if normalized_text:
        return normalized_text
    raise ImageCompletionParseError("completion content is empty")


def parse_drug_images(images: List[str]) -> dict:
    """
    功能描述：
        解析药品图片并返回结构化字段结果。

    参数说明：
        images (List[str]): 图片 URL 列表，仅支持 `http/https` 协议。

    返回值：
        dict: 药品图片结构化识别结果字典，字段由 `DrugImageSchema` 定义。

    异常说明：
        ServiceException: 当输入存在非 `http/https` URL 时抛出 `BAD_REQUEST`。
        ServiceException: 当模型结构化输出不合法时抛出 `INTERNAL_ERROR`。
    """

    def normalize_image_url(image_url: str) -> str:
        """
        功能描述：
            规范化单张图片输入，仅接受可被多模态模型直接访问的公网 URL。

        参数说明：
            image_url (str): 原始图片 URL 字符串。

        返回值：
            str: 去除首尾空白后的图片 URL 字符串。

        异常说明：
            ServiceException: 当输入不是 `http/https` URL 时抛出 `BAD_REQUEST`。
        """

        trimmed = image_url.strip()
        lower = trimmed.lower()
        if lower.startswith("http://") or lower.startswith("https://"):
            return trimmed
        raise ServiceException(
            message="图片仅支持 http/https URL",
            code=ResponseCode.BAD_REQUEST,
        )

    normalized_images = [normalize_image_url(img) for img in images]

    try:
        llm = create_agent_image_llm(
            think=False,
            extra_body=_JSON_OBJECT_RESPONSE_FORMAT,
        )
        completion = llm.invoke(
            [
                HumanMessage(
                    content=[
                        *[
                            {
                                "type": "image_url",
                                "image_url": {"url": image},
                            }
                            for image in normalized_images
                        ],
                        {
                            "type": "text",
                            "text": load_managed_prompt(
                                _DRUG_PARSER_PROMPT_KEY,
                                local_prompt_path=_DRUG_PARSER_PROMPT_LOCAL_PATH,
                            ),
                        },
                    ],
                )
            ],
        )
        content = _extract_completion_text(getattr(completion, "content", None))

        return DrugImageSchema.model_validate(json.loads(content)).model_dump()
    except (ValidationError, json.JSONDecodeError, ImageCompletionParseError) as exc:
        raise ServiceException(
            message="模型返回非 JSON 内容",
            code=ResponseCode.INTERNAL_ERROR,
        ) from exc


class ProductTagImageSchema(BaseModel):
    """
    功能描述：
        商品标签图片识别结果 Schema，约束图片标签匹配返回字段与类型。

    参数说明：
        matchedTagIds (list[str]): 匹配的标签 ID 列表。
        confidence (str | None): 整体匹配置信度。
        reasoning (str | None): 匹配理由简述。

    返回值：
        无（数据模型定义）。

    异常说明：
        pydantic.ValidationError: 当字段类型不符合约束时抛出。
    """

    matchedTagIds: list[str] = Field(default_factory=list)
    confidence: str | None = Field(default=None)
    reasoning: str | None = Field(default=None)


class TagGroupItem(BaseModel):
    """
    功能描述：
        单个标签信息，用于构造提示词中的标签列表。

    参数说明：
        id (str): 标签 ID。
        name (str): 标签名称。
    """

    id: str
    name: str


class TagGroup(BaseModel):
    """
    功能描述：
        标签分组信息，按标签类型分组。

    参数说明：
        typeName (str): 标签类型名称。
        tags (list[TagGroupItem]): 当前类型下的标签列表。
    """

    typeName: str
    tags: list[TagGroupItem]


def _build_tag_list_text(tag_groups: list[dict]) -> str:
    """
    功能描述：
        将标签分组数据构造为提示词中可嵌入的文本块。

    参数说明：
        tag_groups (list[dict]): 标签分组列表，每组包含 typeName 和 tags。

    返回值：
        str: 格式化后的标签列表文本。
    """

    lines: list[str] = []
    for group_data in tag_groups:
        group = TagGroup.model_validate(group_data)
        lines.append(f"【{group.typeName}】")
        for tag in group.tags:
            lines.append(f"  - ID: {tag.id}，名称: {tag.name}")
        lines.append("")
    return "\n".join(lines)


def parse_product_tag_images(images: list[str], tag_groups: list[dict]) -> dict:
    """
    功能描述：
        解析药品图片并根据给定的标签体系匹配适合的商品标签。

    参数说明：
        images (list[str]): 图片 URL 列表，仅支持 `http/https` 协议。
        tag_groups (list[dict]): 标签分组列表，包含所有可用标签。

    返回值：
        dict: 标签匹配结果字典，包含 matchedTagIds、confidence、reasoning。

    异常说明：
        ServiceException: 当输入存在非 `http/https` URL 时抛出 `BAD_REQUEST`。
        ServiceException: 当标签列表为空时抛出 `BAD_REQUEST`。
        ServiceException: 当模型结构化输出不合法时抛出 `INTERNAL_ERROR`。
    """

    if not tag_groups:
        raise ServiceException(
            message="标签列表不能为空",
            code=ResponseCode.BAD_REQUEST,
        )

    def normalize_image_url(image_url: str) -> str:
        """
        功能描述：
            规范化单张图片输入，仅接受可被多模态模型直接访问的公网 URL。

        参数说明：
            image_url (str): 原始图片 URL 字符串。

        返回值：
            str: 去除首尾空白后的图片 URL 字符串。

        异常说明：
            ServiceException: 当输入不是 `http/https` URL 时抛出 `BAD_REQUEST`。
        """

        trimmed = image_url.strip()
        lower = trimmed.lower()
        if lower.startswith("http://") or lower.startswith("https://"):
            return trimmed
        raise ServiceException(
            message="图片仅支持 http/https URL",
            code=ResponseCode.BAD_REQUEST,
        )

    normalized_images = [normalize_image_url(img) for img in images]

    # 构造标签列表文本并嵌入提示词
    tag_list_text = _build_tag_list_text(tag_groups)
    prompt_template = load_managed_prompt(
        _TAG_PARSER_PROMPT_KEY,
        local_prompt_path=_TAG_PARSER_PROMPT_LOCAL_PATH,
    )
    prompt_text = prompt_template.replace("{{TAG_LIST}}", tag_list_text)

    try:
        llm = create_agent_image_llm(
            think=False,
            extra_body=_JSON_OBJECT_RESPONSE_FORMAT,
        )
        completion = llm.invoke(
            [
                HumanMessage(
                    content=[
                        *[
                            {
                                "type": "image_url",
                                "image_url": {"url": image},
                            }
                            for image in normalized_images
                        ],
                        {
                            "type": "text",
                            "text": prompt_text,
                        },
                    ],
                )
            ],
        )
        content = _extract_completion_text(getattr(completion, "content", None))

        return ProductTagImageSchema.model_validate(json.loads(content)).model_dump()
    except (ValidationError, json.JSONDecodeError, ImageCompletionParseError) as exc:
        raise ServiceException(
            message="模型返回非 JSON 内容",
            code=ResponseCode.INTERNAL_ERROR,
        ) from exc
