from __future__ import annotations

from typing import Any, Mapping


def _to_int(value: Any) -> int:
    """
    功能描述：
        将 token 字段转换成非负整数。

    参数说明：
        value (Any): 原始 token 字段值。

    返回值：
        int: 非负整数；无法转换时返回 0。
    """

    try:
        resolved_value = int(value)
    except (TypeError, ValueError):
        return 0
    return max(resolved_value, 0)


def _first_int(*values: Any) -> int:
    """
    功能描述：
        从多个候选 token 字段中读取第一个有效数值。

    参数说明：
        *values (Any): 按优先级排列的候选字段值。

    返回值：
        int: 第一个有效的非负整数；全部为空或不可转换时返回 0。
    """

    for value in values:
        if value is None:
            continue
        return _to_int(value)
    return 0


def _extract_response_usage_mapping(message: Any) -> Mapping[str, Any]:
    """
    功能描述：
        从模型响应元数据中提取供应商原始 usage 映射。

    参数说明：
        message (Any): 模型返回消息。

    返回值：
        Mapping[str, Any]: response_metadata 中的 usage 映射；不存在时返回空映射。
    """

    response_metadata = getattr(message, "response_metadata", None)
    if not isinstance(response_metadata, Mapping):
        return {}
    token_usage = response_metadata.get("token_usage")
    if isinstance(token_usage, Mapping):
        return token_usage
    usage = response_metadata.get("usage")
    if isinstance(usage, Mapping):
        return usage
    return {}


def _extract_usage_mapping(message: Any) -> Mapping[str, Any]:
    """
    功能描述：
        从模型消息中提取 usage 映射。

    参数说明：
        message (Any): 模型返回消息。

    返回值：
        Mapping[str, Any]: usage 映射；不存在时返回空映射。
    """

    usage_metadata = getattr(message, "usage_metadata", None)
    if isinstance(usage_metadata, Mapping):
        return usage_metadata
    response_usage = _extract_response_usage_mapping(message)
    if response_usage:
        return response_usage
    return {}


def extract_raw_usage_from_message(message: Any) -> dict[str, Any]:
    """
    功能描述：
        从模型消息中提取原始 usage 结构，保留供应商特有字段。

    参数说明：
        message (Any): 模型返回消息。

    返回值：
        dict[str, Any]: 原始 usage 字段；不存在时返回空字典。
    """

    response_usage = _extract_response_usage_mapping(message)
    usage = response_usage or _extract_usage_mapping(message)
    return dict(usage) if isinstance(usage, Mapping) else {}


def _extract_prompt_token_details(usage: Mapping[str, Any]) -> Mapping[str, Any]:
    """
    功能描述：
        从 usage 中提取输入 prompt token 明细。

    参数说明：
        usage (Mapping[str, Any]): 原始 usage 映射。

    返回值：
        Mapping[str, Any]: prompt token 明细；不存在时返回空映射。
    """

    for key in ("prompt_tokens_details", "input_tokens_details", "input_token_details"):
        value = usage.get(key)
        if isinstance(value, Mapping):
            return value
    return {}


def _extract_cache_read_tokens(*usages: Mapping[str, Any]) -> int:
    """
    功能描述：
        按官方协议字段提取缓存命中 Token 数。

    参数说明：
        *usages (Mapping[str, Any]): 标准化 usage 与供应商原始 usage 映射。

    返回值：
        int: 缓存命中 Token 数。
    """

    candidates: list[Any] = []
    for usage in usages:
        if not isinstance(usage, Mapping):
            continue
        prompt_token_details = _extract_prompt_token_details(usage)
        candidates.extend(
            [
                prompt_token_details.get("cached_tokens"),
                prompt_token_details.get("cache_read_input_tokens"),
                prompt_token_details.get("cache_read"),
                usage.get("cached_tokens"),
                usage.get("cache_read_input_tokens"),
                usage.get("cache_read"),
            ]
        )
    return _first_int(*candidates)


def _extract_cache_write_tokens(*usages: Mapping[str, Any]) -> int:
    """
    功能描述：
        按官方协议字段提取缓存创建 Token 数。

    参数说明：
        *usages (Mapping[str, Any]): 标准化 usage 与供应商原始 usage 映射。

    返回值：
        int: 缓存创建 Token 数。
    """

    candidates: list[Any] = []
    for usage in usages:
        if not isinstance(usage, Mapping):
            continue
        prompt_token_details = _extract_prompt_token_details(usage)
        candidates.extend(
            [
                prompt_token_details.get("cache_creation_input_tokens"),
                prompt_token_details.get("cache_write_input_tokens"),
                prompt_token_details.get("cache_creation"),
                usage.get("cache_creation_input_tokens"),
                usage.get("cache_write_input_tokens"),
                usage.get("cache_creation"),
            ]
        )
    return _first_int(*candidates)


def extract_token_usage_from_message(message: Any) -> dict[str, int]:
    """
    功能描述：
        从单条 AI 消息中提取 token 用量。

    参数说明：
        message (Any): 模型返回消息。

    返回值：
        dict[str, int]: 标准化 token 字段。
    """

    usage = _extract_usage_mapping(message)
    input_tokens = _to_int(
        usage.get("input_tokens")
        or usage.get("prompt_tokens")
        or usage.get("prompt_token_count")
    )
    output_tokens = _to_int(
        usage.get("output_tokens")
        or usage.get("completion_tokens")
        or usage.get("completion_token_count")
    )
    total_tokens = _to_int(
        usage.get("total_tokens")
        or usage.get("total_token_count")
        or (input_tokens + output_tokens)
    )
    return {
        "input_tokens": input_tokens,
        "output_tokens": output_tokens,
        "total_tokens": total_tokens,
    }


def extract_model_token_usage_from_message(message: Any) -> dict[str, int]:
    """
    功能描述：
        从单条 AI 消息中提取模型监控使用的完整 token 用量。

    参数说明：
        message (Any): 模型返回消息。

    返回值：
        dict[str, int]: 包含基础 token 与缓存 token 的标准化字段。
    """

    usage = _extract_usage_mapping(message)
    response_usage = _extract_response_usage_mapping(message)
    token_usage = extract_token_usage_from_message(message)
    cache_read_tokens = _extract_cache_read_tokens(usage, response_usage)
    cache_write_tokens = _extract_cache_write_tokens(usage, response_usage)
    return {
        **token_usage,
        "cache_read_tokens": cache_read_tokens,
        "cache_write_tokens": cache_write_tokens,
        "cache_total_tokens": cache_read_tokens + cache_write_tokens,
    }


def extract_response_token_usage(messages: list[Any] | tuple[Any, ...] | None) -> dict[str, int]:
    """
    功能描述：
        从模型响应消息列表中汇总 token 用量。

    参数说明：
        messages (list[Any] | tuple[Any, ...] | None): 模型响应消息列表。

    返回值：
        dict[str, int]: token 汇总。
    """

    totals = {
        "input_tokens": 0,
        "output_tokens": 0,
        "total_tokens": 0,
    }
    for message in list(messages or []):
        usage = extract_token_usage_from_message(message)
        totals["input_tokens"] += usage["input_tokens"]
        totals["output_tokens"] += usage["output_tokens"]
        totals["total_tokens"] += usage["total_tokens"]
    return totals


def extract_response_model_token_usage(messages: list[Any] | tuple[Any, ...] | None) -> dict[str, int]:
    """
    功能描述：
        从模型响应消息列表中汇总模型监控使用的完整 token 用量。

    参数说明：
        messages (list[Any] | tuple[Any, ...] | None): 模型响应消息列表。

    返回值：
        dict[str, int]: 包含基础 token 与缓存 token 的汇总字段。
    """

    totals = {
        "input_tokens": 0,
        "output_tokens": 0,
        "total_tokens": 0,
        "cache_read_tokens": 0,
        "cache_write_tokens": 0,
        "cache_total_tokens": 0,
    }
    for message in list(messages or []):
        usage = extract_model_token_usage_from_message(message)
        totals["input_tokens"] += usage["input_tokens"]
        totals["output_tokens"] += usage["output_tokens"]
        totals["total_tokens"] += usage["total_tokens"]
        totals["cache_read_tokens"] += usage["cache_read_tokens"]
        totals["cache_write_tokens"] += usage["cache_write_tokens"]
        totals["cache_total_tokens"] += usage["cache_total_tokens"]
    return totals


def extract_response_raw_usage(messages: list[Any] | tuple[Any, ...] | None) -> dict[str, Any]:
    """
    功能描述：
        从模型响应消息列表中提取最后一条可用的原始 usage 结构。

    参数说明：
        messages (list[Any] | tuple[Any, ...] | None): 模型响应消息列表。

    返回值：
        dict[str, Any]: 原始 usage 结构；不存在时返回空字典。
    """

    for message in reversed(list(messages or [])):
        raw_usage = extract_raw_usage_from_message(message)
        if raw_usage:
            return raw_usage
    return {}


def extract_finish_reason(messages: list[Any] | tuple[Any, ...] | None) -> str | None:
    """
    功能描述：
        从模型响应中解析结束原因。

    参数说明：
        messages (list[Any] | tuple[Any, ...] | None): 模型响应消息列表。

    返回值：
        str | None: finish reason；不存在时返回 None。
    """

    for message in reversed(list(messages or [])):
        response_metadata = getattr(message, "response_metadata", None)
        if not isinstance(response_metadata, Mapping):
            continue
        for key in ("finish_reason", "stop_reason"):
            value = response_metadata.get(key)
            if isinstance(value, str) and value.strip():
                return value.strip()
    return None


def extract_tool_calls(messages: list[Any] | tuple[Any, ...] | None) -> list[Any]:
    """
    功能描述：
        从模型响应中提取 tool_calls。

    参数说明：
        messages (list[Any] | tuple[Any, ...] | None): 模型响应消息列表。

    返回值：
        list[Any]: tool_calls 列表。
    """

    tool_calls: list[Any] = []
    for message in list(messages or []):
        message_tool_calls = getattr(message, "tool_calls", None)
        if isinstance(message_tool_calls, list):
            tool_calls.extend(message_tool_calls)
    return tool_calls
