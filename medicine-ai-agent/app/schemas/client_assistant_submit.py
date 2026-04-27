from __future__ import annotations

from typing import Annotated, Literal, Self

from pydantic import BaseModel, ConfigDict, Field, field_validator, model_validator

# 商品咨询卡片类型标识。
CONSULT_PRODUCT_CARD_TYPE = "consult-product-card"
# 就诊人卡片类型标识。
PATIENT_CARD_TYPE = "patient-card"


class ClientAssistantCardActionRequest(BaseModel):
    """客户端卡片点击事件请求体。"""

    model_config = ConfigDict(extra="forbid")

    type: Literal["click"] = Field(..., description="卡片交互类型，固定为 click")
    message_id: str = Field(..., min_length=1, description="被点击卡片所属消息 UUID")
    card_uuid: str = Field(..., min_length=1, description="被点击卡片 UUID")
    card_type: str | None = Field(default=None, description="被点击卡片类型，可选")
    card_scene: str | None = Field(default=None, description="被点击卡片场景标识，可选")
    card_title: str | None = Field(default=None, description="被点击卡片标题，可选")
    action: str | None = Field(default=None, description="被点击按钮的结构化动作编码，可选")

    @field_validator("message_id", "card_uuid")
    @classmethod
    def validate_required_str(cls, value: str) -> str:
        """
        标准化必填字符串字段。

        Args:
            value: 原始字段值。

        Returns:
            str: 去掉首尾空白后的字段值。

        Raises:
            ValueError: 归一化后为空时抛出。
        """

        normalized = value.strip()
        if not normalized:
            raise ValueError("字段不能为空")
        return normalized

    @field_validator("card_type", "card_scene", "card_title", "action")
    @classmethod
    def validate_optional_str(cls, value: str | None) -> str | None:
        """
        标准化可选字符串字段。

        Args:
            value: 原始字段值。

        Returns:
            str | None: 去掉首尾空白后的字段值；空白值返回 `None`。
        """

        if value is None:
            return None
        normalized = value.strip()
        return normalized or None


class ClientAssistantOrderCardPreviewProductRequest(BaseModel):
    """客户端订单卡首个商品预览提交数据。"""

    model_config = ConfigDict(extra="forbid")

    product_id: str = Field(..., min_length=1, description="商品ID")
    product_name: str = Field(..., min_length=1, description="商品名称")
    image_url: str = Field(..., min_length=1, description="商品图片URL")

    @field_validator("product_id", "product_name", "image_url")
    @classmethod
    def validate_preview_required_text(cls, value: str) -> str:
        """
        标准化订单预览商品必填文本字段。

        Args:
            value: 原始字段值。

        Returns:
            str: 去掉首尾空白后的字段值。

        Raises:
            ValueError: 归一化后为空时抛出。
        """

        normalized = value.strip()
        if not normalized:
            raise ValueError("字段不能为空")
        return normalized


class ClientAssistantOrderCardDataRequest(BaseModel):
    """客户端订单卡提交数据。"""

    model_config = ConfigDict(extra="forbid")

    order_no: str = Field(..., min_length=1, description="订单编号")
    order_status: str = Field(..., min_length=1, description="订单状态编码")
    order_status_text: str = Field(..., min_length=1, description="订单状态名称")
    preview_product: ClientAssistantOrderCardPreviewProductRequest = Field(..., description="首个商品预览")
    product_count: int = Field(..., ge=0, description="订单商品总件数")
    pay_amount: str = Field(..., min_length=1, description="实付金额")
    total_amount: str = Field(..., min_length=1, description="订单总金额")
    create_time: str = Field(..., min_length=1, description="下单时间")

    @field_validator(
        "order_no",
        "order_status",
        "order_status_text",
        "pay_amount",
        "total_amount",
        "create_time",
    )
    @classmethod
    def validate_order_required_text(cls, value: str) -> str:
        """
        标准化订单卡必填文本字段。

        Args:
            value: 原始字段值。

        Returns:
            str: 去掉首尾空白后的字段值。

        Raises:
            ValueError: 归一化后为空时抛出。
        """

        normalized = value.strip()
        if not normalized:
            raise ValueError("字段不能为空")
        return normalized


class ClientAssistantAfterSaleCardProductInfoRequest(BaseModel):
    """客户端售后卡商品信息提交数据。"""

    model_config = ConfigDict(extra="forbid")

    product_name: str = Field(..., min_length=1, description="商品名称")
    product_image: str = Field(..., min_length=1, description="商品图片URL")

    @field_validator("product_name", "product_image")
    @classmethod
    def validate_product_info_required_text(cls, value: str) -> str:
        """
        标准化售后卡商品信息必填文本字段。

        Args:
            value: 原始字段值。

        Returns:
            str: 去掉首尾空白后的字段值。

        Raises:
            ValueError: 归一化后为空时抛出。
        """

        normalized = value.strip()
        if not normalized:
            raise ValueError("字段不能为空")
        return normalized


class ClientAssistantAfterSaleCardDataRequest(BaseModel):
    """客户端售后卡提交数据。"""

    model_config = ConfigDict(extra="forbid")

    after_sale_no: str = Field(..., min_length=1, description="售后单号")
    order_no: str = Field(..., min_length=1, description="订单编号")
    after_sale_type: str = Field(..., min_length=1, description="售后类型编码")
    after_sale_type_text: str = Field(..., min_length=1, description="售后类型名称")
    after_sale_status: str = Field(..., min_length=1, description="售后状态编码")
    after_sale_status_text: str = Field(..., min_length=1, description="售后状态名称")
    refund_amount: str = Field(..., min_length=1, description="退款金额")
    apply_reason_name: str = Field(..., min_length=1, description="申请原因名称")
    apply_time: str = Field(..., min_length=1, description="申请时间")
    product_info: ClientAssistantAfterSaleCardProductInfoRequest = Field(..., description="售后商品信息")

    @field_validator(
        "after_sale_no",
        "order_no",
        "after_sale_type",
        "after_sale_type_text",
        "after_sale_status",
        "after_sale_status_text",
        "refund_amount",
        "apply_reason_name",
        "apply_time",
    )
    @classmethod
    def validate_after_sale_text(cls, value: str) -> str:
        """
        标准化售后卡必填文本字段。

        Args:
            value: 原始字段值。

        Returns:
            str: 去掉首尾空白后的字段值。

        Raises:
            ValueError: 归一化后为空时抛出。
        """

        normalized = value.strip()
        if not normalized:
            raise ValueError("字段不能为空")
        return normalized


class ClientAssistantOrderCardRequest(BaseModel):
    """客户端助手 submit 订单卡请求。"""

    model_config = ConfigDict(extra="forbid")

    type: Literal["order-card"] = Field(..., description="卡片类型，当前固定为 order-card")
    data: ClientAssistantOrderCardDataRequest = Field(..., description="订单卡提交数据")


class ClientAssistantAfterSaleCardRequest(BaseModel):
    """客户端助手 submit 售后卡请求。"""

    model_config = ConfigDict(extra="forbid")

    type: Literal["after-sale-card"] = Field(..., description="卡片类型，当前固定为 after-sale-card")
    data: ClientAssistantAfterSaleCardDataRequest = Field(..., description="售后卡提交数据")


class ClientAssistantConsultProductCardDataRequest(BaseModel):
    """客户端商品咨询卡提交数据。"""

    model_config = ConfigDict(extra="forbid")

    product_id: int = Field(..., gt=0, description="商品ID")


class ClientAssistantConsultProductCardRequest(BaseModel):
    """客户端助手 submit 商品咨询卡请求。"""

    model_config = ConfigDict(extra="forbid")

    type: Literal["consult-product-card"] = Field(
        ...,
        description="卡片类型，当前固定为 consult-product-card",
    )
    data: ClientAssistantConsultProductCardDataRequest = Field(..., description="商品咨询卡提交数据")


class ClientAssistantPatientCardDataRequest(BaseModel):
    """客户端就诊人卡提交数据。"""

    model_config = ConfigDict(extra="forbid")

    patient_id: str = Field(..., min_length=1, description="就诊人ID")
    name: str = Field(..., min_length=1, description="就诊人姓名")
    gender: int = Field(..., ge=1, le=2, description="性别编码，1 表示男，2 表示女")
    gender_text: str = Field(..., min_length=1, description="性别展示文案")
    birth_date: str = Field(..., min_length=1, description="出生日期")
    relationship: str = Field(..., min_length=1, description="与当前账号关系")
    is_default: int = Field(..., ge=0, le=1, description="是否为默认就诊人，1 是，0 否")
    allergy: str = Field(default="", description="过敏史")
    past_medical_history: str = Field(default="", description="既往病史")
    chronic_disease: str = Field(default="", description="慢性病信息")
    long_term_medications: str = Field(default="", description="长期用药")

    @field_validator(
        "patient_id",
        "name",
        "gender_text",
        "birth_date",
        "relationship",
    )
    @classmethod
    def validate_patient_required_text(cls, value: str) -> str:
        """
        标准化就诊人卡必填文本字段。

        Args:
            value: 原始字段值。

        Returns:
            str: 去掉首尾空白后的字段值。

        Raises:
            ValueError: 归一化后为空时抛出。
        """

        normalized = value.strip()
        if not normalized:
            raise ValueError("字段不能为空")
        return normalized

    @field_validator(
        "allergy",
        "past_medical_history",
        "chronic_disease",
        "long_term_medications",
    )
    @classmethod
    def validate_patient_optional_text(cls, value: str) -> str:
        """
        标准化就诊人卡可选文本字段。

        Args:
            value: 原始字段值。

        Returns:
            str: 去掉首尾空白后的字段值；未填写时返回空字符串。
        """

        return value.strip()


class ClientAssistantPatientCardRequest(BaseModel):
    """客户端助手 submit 就诊人卡请求。"""

    model_config = ConfigDict(extra="forbid")

    type: Literal[PATIENT_CARD_TYPE] = Field(
        ...,
        description="卡片类型，当前固定为 patient-card",
    )
    data: ClientAssistantPatientCardDataRequest = Field(..., description="就诊人卡提交数据")


ClientAssistantCardRequest = Annotated[
    ClientAssistantOrderCardRequest
    | ClientAssistantAfterSaleCardRequest
    | ClientAssistantConsultProductCardRequest
    | ClientAssistantPatientCardRequest,
    Field(discriminator="type"),
]


class ClientAssistantSubmitRequest(BaseModel):
    """客户端助手 submit 请求体。"""

    model_config = ConfigDict(
        extra="forbid",
        json_schema_extra={
            "examples": [
                {
                    "message_type": "text",
                    "content": "我想申请退款，现在应该怎么操作？",
                    "conversation_uuid": "b7e4f95d-62f6-4a0a-8823-2fbd3d4db0cf",
                },
                {
                    "message_type": "text",
                    "content": "这个药怎么吃？",
                    "conversation_uuid": "b7e4f95d-62f6-4a0a-8823-2fbd3d4db0cf",
                    "card": {
                        "type": "consult-product-card",
                        "data": {
                            "product_id": 21,
                        },
                    },
                },
                {
                    "message_type": "card",
                    "conversation_uuid": "b7e4f95d-62f6-4a0a-8823-2fbd3d4db0cf",
                    "card": {
                        "type": "order-card",
                        "data": {
                            "order_no": "O20260327183759126720",
                            "order_status": "PENDING_PAYMENT",
                            "order_status_text": "待付款",
                            "preview_product": {
                                "product_id": "21",
                                "product_name": "医用口罩",
                                "image_url": "https://example.com/product.png",
                            },
                            "product_count": 1,
                            "pay_amount": "22.00",
                            "total_amount": "22.00",
                            "create_time": "2026-03-27 18:37:59",
                        },
                    },
                },
                {
                    "message_type": "card",
                    "conversation_uuid": "b7e4f95d-62f6-4a0a-8823-2fbd3d4db0cf",
                    "card": {
                        "type": "after-sale-card",
                        "data": {
                            "after_sale_no": "AS20260330065210217435",
                            "order_no": "O20260327183759126720",
                            "after_sale_type": "REFUND_ONLY",
                            "after_sale_type_text": "仅退款",
                            "after_sale_status": "PENDING",
                            "after_sale_status_text": "待审核",
                            "refund_amount": "22.00",
                            "apply_reason_name": "收货地址填错了",
                            "apply_time": "2026-03-30 06:52:10",
                            "product_info": {
                                "product_name": "医用口罩",
                                "product_image": "https://example.com/product.png",
                            },
                        },
                    },
                },
                {
                    "message_type": "card",
                    "conversation_uuid": "b7e4f95d-62f6-4a0a-8823-2fbd3d4db0cf",
                    "card": {
                        "type": "patient-card",
                        "data": {
                            "patient_id": "91",
                            "name": "张三",
                            "gender": 1,
                            "gender_text": "男",
                            "birth_date": "1998-03-15",
                            "relationship": "本人",
                            "is_default": 1,
                            "allergy": "青霉素过敏",
                            "past_medical_history": "无",
                            "chronic_disease": "无",
                            "long_term_medications": "无",
                        },
                    },
                },
            ],
        },
    )

    message_type: Literal["text", "card"] = Field(..., description="消息类型")
    content: str | None = Field(default=None, description="文本消息内容")
    card: ClientAssistantCardRequest | None = Field(default=None, description="卡片消息数据")
    image_urls: list[str] | None = Field(
        default=None,
        min_length=1,
        max_length=5,
        description="图片 URL 列表（最多 5 张）",
    )
    conversation_uuid: str | None = Field(default=None, min_length=1, description="会话UUID")
    reasoning_enabled: bool = Field(default=False, description="当前轮是否开启深度思考")
    card_action: ClientAssistantCardActionRequest | None = Field(
        default=None,
        description="前端交互卡片点击事件字段，用于让后端恢复本轮点击卡片的业务语义。",
    )

    @field_validator("content")
    @classmethod
    def validate_content(cls, value: str | None) -> str | None:
        """
        标准化文本消息内容。

        Args:
            value: 原始文本内容。

        Returns:
            str | None: 去掉首尾空白后的文本内容；空白文本返回 `None`。
        """

        if value is None:
            return None
        normalized = value.strip()
        return normalized or None

    @field_validator("conversation_uuid")
    @classmethod
    def validate_conversation_uuid(cls, value: str | None) -> str | None:
        """
        标准化会话 UUID。

        Args:
            value: 原始会话 UUID。

        Returns:
            str | None: 去掉首尾空白后的会话 UUID；空白值返回 `None`。
        """

        if value is None:
            return None
        normalized = value.strip()
        return normalized or None

    @field_validator("image_urls")
    @classmethod
    def validate_image_urls(cls, value: list[str] | None) -> list[str] | None:
        """
        标准化图片 URL 列表。

        Args:
            value: 原始图片 URL 列表。

        Returns:
            list[str] | None: 去掉每项首尾空白后的 URL 列表；未传时返回 `None`。

        Raises:
            ValueError: 任意 URL 归一化后为空时抛出。
        """

        if value is None:
            return None

        normalized_image_urls: list[str] = []
        for raw_image_url in value:
            normalized_image_url = str(raw_image_url or "").strip()
            if not normalized_image_url:
                raise ValueError("图片地址不能为空")
            normalized_image_urls.append(normalized_image_url)
        return normalized_image_urls

    @model_validator(mode="after")
    def validate_message_payload(self) -> Self:
        """
        校验 submit 请求体与消息类型的组合关系。

        Returns:
            Self: 校验通过后的请求模型自身。

        Raises:
            ValueError:
                - `message_type=text` 但缺少 `content`；
                - `message_type=card` 但缺少 `card`；
                - 任意场景下传入了不匹配当前类型的字段。
        """

        if self.message_type == "text":
            if self.card is not None and self.card.type != CONSULT_PRODUCT_CARD_TYPE:
                raise ValueError("文本消息仅允许附带 consult-product-card")
            if self.card is not None and self.image_urls:
                raise ValueError("文本 + consult-product-card 场景不支持附带图片")
            if not self.content and self.card is None and not self.image_urls:
                raise ValueError("文本消息必须传 content、image_urls，或附带 consult-product-card")
            return self

        if self.card is None:
            raise ValueError("卡片消息必须传 card")
        if self.card.type == CONSULT_PRODUCT_CARD_TYPE:
            raise ValueError("商品咨询必须使用文本消息并附带 consult-product-card")
        if self.content is not None:
            raise ValueError("卡片消息不能传 content")
        if self.image_urls:
            raise ValueError("卡片消息不能传 image_urls")
        return self
