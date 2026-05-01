from __future__ import annotations

import uuid


def generate_trace_id() -> str:
    """
    功能描述：
        生成一次 Agent Trace 的唯一 ID。

    参数说明：
        无。

    返回值：
        str: trace 唯一标识。
    """

    return f"trace_{uuid.uuid4().hex}"


def generate_span_id() -> str:
    """
    功能描述：
        生成单个 trace span 的唯一 ID。

    参数说明：
        无。

    返回值：
        str: span 唯一标识。
    """

    return f"span_{uuid.uuid4().hex}"


def generate_usage_id() -> str:
    """
    功能描述：
        生成一次模型 Token 用量明细的唯一 ID。

    参数说明：
        无。

    返回值：
        str: Token 用量明细唯一标识。
    """

    return f"usage_{uuid.uuid4().hex}"
