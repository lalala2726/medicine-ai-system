from __future__ import annotations

import datetime


def utc_now() -> datetime.datetime:
    """
    功能描述：
        获取带 UTC 时区的当前时间，供 Trace 写入 MongoDB 的 Date 字段使用。

    参数说明：
        无。

    返回值：
        datetime.datetime: 带 UTC 时区信息的当前时间。
    """

    return datetime.datetime.now(datetime.timezone.utc)
