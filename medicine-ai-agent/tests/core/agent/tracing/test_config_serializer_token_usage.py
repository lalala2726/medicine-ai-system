"""Agent Trace 配置、序列化和 token 提取单元测试。"""

from __future__ import annotations

import dataclasses
import datetime
import enum
from types import SimpleNamespace

from bson import ObjectId
from pydantic import BaseModel

from app.core.agent.tracing import config as config_module
from app.core.agent.tracing.serializer import serialize_exception, serialize_value
from app.core.agent.tracing.token_usage import (
    extract_finish_reason,
    extract_model_token_usage_from_message,
    extract_raw_usage_from_message,
    extract_response_model_token_usage,
    extract_response_token_usage,
    extract_tool_calls,
)


class _TracePayloadModel(BaseModel):
    """用于验证 Pydantic 对象序列化的测试模型。"""

    name: str
    count: int


@dataclasses.dataclass
class _TracePayloadDataclass:
    """用于验证 dataclass 对象序列化的测试模型。"""

    enabled: bool
    label: str


class _TracePayloadEnum(str, enum.Enum):
    """用于验证枚举对象序列化的测试枚举。"""

    ACTIVE = "active"


def test_load_agent_trace_settings_reads_env_and_ignores_invalid_values(
        monkeypatch,
) -> None:
    """验证 trace 配置能读取合法环境变量，并对非法正整数回退默认值。"""
    monkeypatch.setenv(config_module.AGENT_TRACE_ENABLED_ENV_NAME, "off")
    monkeypatch.setenv(config_module.AGENT_TRACE_QUEUE_MAX_SIZE_ENV_NAME, "invalid")
    monkeypatch.setenv(config_module.AGENT_TRACE_BATCH_SIZE_ENV_NAME, "25")
    monkeypatch.setenv(config_module.AGENT_TRACE_FLUSH_INTERVAL_MS_ENV_NAME, "0")
    monkeypatch.setenv(config_module.AGENT_TRACE_PAYLOAD_MAX_CHARS_ENV_NAME, "128")

    settings = config_module.load_agent_trace_settings()

    assert settings.enabled is False
    assert settings.queue_max_size == config_module.DEFAULT_AGENT_TRACE_QUEUE_MAX_SIZE
    assert settings.batch_size == 25
    assert settings.flush_interval_ms == config_module.DEFAULT_AGENT_TRACE_FLUSH_INTERVAL_MS
    assert settings.payload_max_chars == 128


def test_serialize_value_normalizes_common_payload_types() -> None:
    """验证 Mongo payload 序列化会处理常见对象并截断超长字符串。"""
    object_id = ObjectId()
    occurred_at = datetime.datetime(2026, 4, 28, 10, 30, 0)

    payload = serialize_value(
        {
            "model": _TracePayloadModel(name="abc", count=2),
            "dataclass": _TracePayloadDataclass(enabled=True, label="node"),
            "enum": _TracePayloadEnum.ACTIVE,
            "object_id": object_id,
            "occurred_at": occurred_at,
            "text": "abcdef",
        },
        max_chars=3,
    )

    assert payload["model"] == {"name": "abc", "count": 2}
    assert payload["dataclass"] == {"enabled": True, "label": "nod...(已截断，原始长度=4)"}
    assert payload["enum"] == "active"
    assert payload["object_id"] == str(object_id)
    assert payload["occurred_at"] == "2026-04-28T10:30:00"
    assert payload["text"] == "abc...(已截断，原始长度=6)"


def test_serialize_exception_outputs_stable_error_payload() -> None:
    """验证异常序列化字段稳定，便于 Java 端按固定结构读取。"""
    error_payload = serialize_exception(ValueError("参数错误"))

    assert error_payload == {
        "error_type": "ValueError",
        "error_message": "参数错误",
    }


def test_extract_response_token_usage_supports_multiple_provider_shapes() -> None:
    """验证 token 汇总支持 usage_metadata、token_usage 和 usage 三种常见结构。"""
    messages = [
        SimpleNamespace(
            usage_metadata={
                "input_tokens": "10",
                "output_tokens": 5,
                "total_tokens": "20",
            },
        ),
        SimpleNamespace(
            response_metadata={
                "token_usage": {
                    "prompt_tokens": 3,
                    "completion_tokens": 4,
                },
            },
        ),
        SimpleNamespace(
            response_metadata={
                "usage": {
                    "prompt_token_count": 2,
                    "completion_token_count": 6,
                    "total_token_count": 8,
                },
                "finish_reason": "tool_calls",
            },
            tool_calls=[{"name": "order_list"}],
        ),
    ]

    usage = extract_response_token_usage(messages)

    assert usage == {
        "input_tokens": 15,
        "output_tokens": 15,
        "total_tokens": 35,
    }
    assert extract_finish_reason(messages) == "tool_calls"
    assert extract_tool_calls(messages) == [{"name": "order_list"}]


def test_extract_model_token_usage_supports_official_cache_usage_shapes() -> None:
    """验证模型监控能读取官方文档中的 OpenAI、DashScope、Anthropic 与 LangChain 缓存字段。"""
    openai_compatible_message = SimpleNamespace(
        response_metadata={
            "token_usage": {
                "prompt_tokens": 3019,
                "completion_tokens": 104,
                "total_tokens": 3123,
                "prompt_tokens_details": {
                    "cached_tokens": 2048,
                    "cache_creation_input_tokens": 0,
                },
            }
        }
    )
    dashscope_top_level_message = SimpleNamespace(
        response_metadata={
            "token_usage": {
                "input_tokens": 1292,
                "output_tokens": 87,
                "total_tokens": 1379,
                "cached_tokens": 1152,
            }
        }
    )
    anthropic_compatible_message = SimpleNamespace(
        response_metadata={
            "usage": {
                "input_tokens": 369,
                "output_tokens": 28,
                "cache_creation_input_tokens": 17,
                "cache_read_input_tokens": 896,
            }
        }
    )
    langchain_standard_message = SimpleNamespace(
        usage_metadata={
            "input_tokens": 2174,
            "output_tokens": 88,
            "total_tokens": 2262,
            "input_token_details": {
                "cache_read": 0,
                "cache_creation": 2156,
            },
        }
    )

    assert extract_model_token_usage_from_message(openai_compatible_message) == {
        "input_tokens": 3019,
        "output_tokens": 104,
        "total_tokens": 3123,
        "cache_read_tokens": 2048,
        "cache_write_tokens": 0,
        "cache_total_tokens": 2048,
    }
    assert extract_model_token_usage_from_message(dashscope_top_level_message) == {
        "input_tokens": 1292,
        "output_tokens": 87,
        "total_tokens": 1379,
        "cache_read_tokens": 1152,
        "cache_write_tokens": 0,
        "cache_total_tokens": 1152,
    }
    assert extract_model_token_usage_from_message(anthropic_compatible_message) == {
        "input_tokens": 369,
        "output_tokens": 28,
        "total_tokens": 397,
        "cache_read_tokens": 896,
        "cache_write_tokens": 17,
        "cache_total_tokens": 913,
    }
    assert extract_model_token_usage_from_message(langchain_standard_message) == {
        "input_tokens": 2174,
        "output_tokens": 88,
        "total_tokens": 2262,
        "cache_read_tokens": 0,
        "cache_write_tokens": 2156,
        "cache_total_tokens": 2156,
    }
    assert extract_response_model_token_usage(
        [
            openai_compatible_message,
            dashscope_top_level_message,
            anthropic_compatible_message,
            langchain_standard_message,
        ]
    ) == {
               "input_tokens": 6854,
               "output_tokens": 307,
               "total_tokens": 7161,
               "cache_read_tokens": 4096,
               "cache_write_tokens": 2173,
               "cache_total_tokens": 6269,
           }
    assert extract_raw_usage_from_message(openai_compatible_message)["prompt_tokens_details"] == {
        "cached_tokens": 2048,
        "cache_creation_input_tokens": 0,
    }
