from __future__ import annotations

from typing import Any

from langchain_core.messages import AIMessageChunk
from langchain_core.outputs import ChatGenerationChunk
from langchain_openai import ChatOpenAI, OpenAIEmbeddings
from pydantic import SecretStr

from app.core.llms.common import prepare_chat_client_kwargs, resolve_llm_value

DEFAULT_DASHSCOPE_BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1"


class ChatQwen(ChatOpenAI):
    """
    功能描述:
        千问聊天模型客户端，基于 OpenAI 兼容协议封装，
        额外补齐 `reasoning_content` 在流式/非流式场景下的统一承载。

    参数说明:
        继承 `ChatOpenAI` 构造参数，主要包括 `model/api_key/base_url/extra_body`。

    返回值:
        无（类定义）。

    异常说明:
        无（异常由父类模型请求链路抛出）。
    """

    def _convert_chunk_to_generation_chunk(
            self,
            chunk: dict,
            default_chunk_class: type,
            base_generation_info: dict | None,
    ) -> ChatGenerationChunk | None:
        """
        功能描述:
            将千问流式响应片段转换为 `ChatGenerationChunk`，并把
            `delta.reasoning_content` 注入到 `message.additional_kwargs`。

        参数说明:
            chunk (dict): 单次流式返回片段原始数据，默认值: 无。
            default_chunk_class (type): 默认消息片段类型，默认值: 无。
            base_generation_info (dict | None): 基础生成信息，默认值: None。

        返回值:
            ChatGenerationChunk | None: 转换后的片段；无法转换时返回 None。

        异常说明:
            无显式抛出；父类实现抛出的异常将继续向上抛出。
        """

        generation_chunk = super()._convert_chunk_to_generation_chunk(
            chunk, default_chunk_class, base_generation_info
        )

        if generation_chunk and (choices := chunk.get("choices")):
            top = choices[0]
            if isinstance(generation_chunk.message, AIMessageChunk):
                reasoning_content = top.get("delta", {}).get("reasoning_content")
                if reasoning_content:
                    generation_chunk.message.additional_kwargs["reasoning_content"] = reasoning_content

        return generation_chunk

    def _create_chat_result(self, response: dict | Any, generation_info: dict | None = None) -> Any:
        """
        功能描述:
            在非流式响应中补齐 `reasoning_content` 到消息 `additional_kwargs`，
            与流式行为保持一致，方便上层统一读取。

        参数说明:
            response (dict | Any): 模型原始响应对象，默认值: 无。
            generation_info (dict | None): 生成附加元信息，默认值: None。

        返回值:
            Any: 父类构造后的聊天结果对象。

        异常说明:
            无显式抛出；父类实现抛出的异常将继续向上抛出。
        """

        result = super()._create_chat_result(response, generation_info)

        if hasattr(response, "choices") and response.choices:
            if hasattr(response.choices[0].message, "reasoning_content"):
                result.generations[0].message.additional_kwargs["reasoning_content"] = (
                    response.choices[0].message.reasoning_content
                )

        return result


def create_aliyun_chat_model(
        *,
        model: str | None = None,
        api_key: str | None = None,
        base_url: str | None = None,
        extra_body: dict[str, Any] | None = None,
        **kwargs: Any,
) -> ChatQwen:
    """
    功能描述:
        创建阿里云（DashScope）聊天模型客户端，返回增强版 `ChatQwen`。

    参数说明:
        model (str | None): 模型名称；默认值 `None`。
            为 `None` 时读取 `DASHSCOPE_CHAT_MODEL`，若仍为空则报错。
        api_key (str | None): API 密钥；默认值 `None`。
            为 `None` 时读取 `load_dotenv` 生效后的环境变量 `DASHSCOPE_API_KEY`。
        base_url (str | None): API 基础地址；默认值 `None`。
            为 `None` 时读取 `DASHSCOPE_BASE_URL`，若未配置则使用默认 DashScope 地址。
        extra_body (dict[str, Any] | None): 透传扩展参数；默认值 `None`。
        **kwargs (Any): 其余透传 `ChatQwen` 构造参数。

    返回值:
        ChatQwen: 千问聊天模型客户端实例。

    异常说明:
        RuntimeError:
            - 未提供 `api_key` 且环境变量 `DASHSCOPE_API_KEY` 未设置；
            - 未传 `model` 且环境变量 `DASHSCOPE_CHAT_MODEL` 未设置。
    """

    key = resolve_llm_value(name="DASHSCOPE_API_KEY", explicit=api_key)
    if not key:
        raise RuntimeError("DASHSCOPE_API_KEY is not set")

    resolved_model = resolve_llm_value(name="DASHSCOPE_CHAT_MODEL", explicit=model)
    if not resolved_model:
        raise RuntimeError("DASHSCOPE_CHAT_MODEL is not set")

    resolved_base_url = resolve_llm_value(
        name="DASHSCOPE_BASE_URL",
        explicit=base_url,
        default=DEFAULT_DASHSCOPE_BASE_URL,
    )

    prepared_kwargs = prepare_chat_client_kwargs(extra_body=extra_body, **kwargs)

    return ChatQwen(
        model=resolved_model,
        api_key=SecretStr(key),
        base_url=resolved_base_url,
        **prepared_kwargs,
    )


def create_aliyun_image_model(
        *,
        model: str | None = None,
        api_key: str | None = None,
        base_url: str | None = None,
        extra_body: dict[str, Any] | None = None,
        **kwargs: Any,
) -> ChatQwen:
    """
    功能描述:
        创建阿里云（DashScope）图像理解模型客户端。

    参数说明:
        model (str | None): 图像模型名称；默认值 `None`。
            为 `None` 时读取 `DASHSCOPE_IMAGE_MODEL`，若仍为空则报错。
        api_key (str | None): API 密钥；默认值 `None`。
            为 `None` 时读取 `load_dotenv` 生效后的环境变量 `DASHSCOPE_API_KEY`。
        base_url (str | None): API 基础地址；默认值 `None`。
            为 `None` 时读取 `DASHSCOPE_BASE_URL`，若未配置则使用默认 DashScope 地址。
        extra_body (dict[str, Any] | None): 透传扩展参数；默认值 `None`。
        **kwargs (Any): 其余透传模型参数。

    返回值:
        ChatQwen: 图像理解可用的千问模型实例。

    异常说明:
        RuntimeError:
            - 未提供 `api_key` 且环境变量 `DASHSCOPE_API_KEY` 未设置；
            - 未传 `model` 且环境变量 `DASHSCOPE_IMAGE_MODEL` 未设置。
    """

    resolved_model = resolve_llm_value(name="DASHSCOPE_IMAGE_MODEL", explicit=model)
    if not resolved_model:
        raise RuntimeError("DASHSCOPE_IMAGE_MODEL is not set")

    return create_aliyun_chat_model(
        model=resolved_model,
        api_key=api_key,
        base_url=base_url,
        extra_body=extra_body,
        **kwargs,
    )


def create_aliyun_embedding_model(
        *,
        model: str | None = None,
        api_key: str | None = None,
        base_url: str | None = None,
        dimensions: int | None = 1024,
        **kwargs: Any,
) -> OpenAIEmbeddings:
    """
    功能描述:
        创建阿里云（DashScope）向量嵌入模型客户端。

    参数说明:
        model (str | None): 嵌入模型名称；默认值 `None`。
            为 `None` 时读取 `DASHSCOPE_EMBEDDING_MODEL`，若仍为空则报错。
        api_key (str | None): API 密钥；默认值 `None`。
            为 `None` 时读取 `load_dotenv` 生效后的环境变量 `DASHSCOPE_API_KEY`。
        base_url (str | None): API 基础地址；默认值 `None`。
            为 `None` 时读取 `DASHSCOPE_BASE_URL`，若未配置则使用默认 DashScope 地址。
        dimensions (int | None): 向量维度；默认值 `1024`。
            传 `None` 时不向底层客户端设置该参数。
        **kwargs (Any): 其余透传 `OpenAIEmbeddings` 构造参数。

    返回值:
        OpenAIEmbeddings: 可直接用于文本向量化的嵌入模型实例。

    异常说明:
        RuntimeError:
            - 未提供 `api_key` 且环境变量 `DASHSCOPE_API_KEY` 未设置；
            - 未传 `model` 且环境变量 `DASHSCOPE_EMBEDDING_MODEL` 未设置。
    """

    key = resolve_llm_value(name="DASHSCOPE_API_KEY", explicit=api_key)
    if not key:
        raise RuntimeError("DASHSCOPE_API_KEY is not set")

    resolved_model = resolve_llm_value(name="DASHSCOPE_EMBEDDING_MODEL", explicit=model)
    if not resolved_model:
        raise RuntimeError("DASHSCOPE_EMBEDDING_MODEL is not set")

    resolved_base_url = resolve_llm_value(
        name="DASHSCOPE_BASE_URL",
        explicit=base_url,
        default=DEFAULT_DASHSCOPE_BASE_URL,
    )

    embedding_kwargs = dict(kwargs)
    if dimensions is not None:
        embedding_kwargs["dimensions"] = dimensions

    return OpenAIEmbeddings(
        model=resolved_model,
        check_embedding_ctx_length=False,
        api_key=SecretStr(key),
        base_url=resolved_base_url,
        **embedding_kwargs,
    )
