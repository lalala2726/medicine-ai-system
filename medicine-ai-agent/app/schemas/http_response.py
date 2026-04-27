from __future__ import annotations

from collections.abc import Mapping
from typing import Any, Optional

import httpx
from pydantic import BaseModel, ConfigDict

from app.core.codes import ResponseCode
from app.core.exception.exceptions import ServiceException


class HttpResponse(BaseModel):
    model_config = ConfigDict(extra="ignore")

    code: int
    message: str
    data: Any = None
    meta: Any = None
    timestamp: Optional[Any] = None

    @classmethod
    def from_response(cls, response: httpx.Response) -> "HttpResponse":
        """
        功能描述：
            从 `httpx.Response` 中解析 JSON 并构建 `HttpResponse` 对象。

        参数说明：
            response (httpx.Response): 原始 HTTP 响应对象，要求响应体为合法 JSON。

        返回值：
            HttpResponse: 解析后的统一响应对象，包含 code/message/data/meta/timestamp。

        异常说明：
            ServiceException: 当响应体不是合法 JSON 时抛出，错误码为 `ResponseCode.OPERATION_FAILED`。
        """
        try:
            payload = response.json()
        except ValueError as exc:
            raise ServiceException(
                code=ResponseCode.OPERATION_FAILED,
                message=f"响应不是合法 JSON，status={response.status_code}",
            ) from exc
        return cls.model_validate(payload)

    def _merge_data_and_meta(self) -> Any:
        """
        功能描述：
            统一合并业务数据与元数据，兼容不同后端结构：
            1. `meta` 在响应顶层（与 `data` 同级）。
            2. `meta` 已经在 `data` 内部。

        参数说明：
            无。

        返回值：
            Any:
                - 当仅有 `data` 时返回原始 `data`；
                - 当存在顶层 `meta` 且 `data` 为映射类型时，返回合并后的映射；
                - 当存在顶层 `meta` 且 `data` 非映射类型时，返回 `{\"data\": ..., \"meta\": ...}`。

        异常说明：
            无。
        """
        if self.meta is None:
            return self.data

        if isinstance(self.data, Mapping):
            if "meta" in self.data:
                return self.data
            merged_payload = dict(self.data)
            merged_payload["meta"] = self.meta
            return merged_payload

        return {
            "data": self.data,
            "meta": self.meta,
        }

    def data_or_raise(self) -> Any:
        """
        功能描述：
            校验业务响应码，成功时返回可直接消费的数据结构，失败时抛出业务异常。

        参数说明：
            无。

        返回值：
            Any: 业务数据；若存在 `meta` 会按兼容规则与 `data` 合并后返回。

        异常说明：
            ServiceException: 当 `code != ResponseCode.SUCCESS.code` 时抛出。
        """
        if self.code != ResponseCode.SUCCESS.code:
            raise ServiceException(code=self.code, message=self.message)
        return self._merge_data_and_meta()

    @classmethod
    def parse_data(cls, response: httpx.Response) -> Any:
        """
        功能描述：
            解析 HTTP 响应并返回业务数据，自动处理业务失败码与 `meta` 合并逻辑。

        参数说明：
            response (httpx.Response): 原始 HTTP 响应对象。

        返回值：
            Any: 成功时返回可直接消费的数据结构（包含 `meta` 时会一并返回）。

        异常说明：
            ServiceException:
                - 响应不是合法 JSON；
                - 业务响应码非成功。
        """
        return cls.from_response(response).data_or_raise()
