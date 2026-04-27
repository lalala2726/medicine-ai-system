import httpx
import pytest

from app.core.codes import ResponseCode
from app.core.exception.exceptions import ServiceException
from app.schemas.http_response import HttpResponse


def _build_response(payload: dict) -> httpx.Response:
    """
    功能描述：
        构造用于单元测试的 `httpx.Response` 对象。

    参数说明：
        payload (dict): 要写入响应体的 JSON 对象。

    返回值：
        httpx.Response: status_code=200 且携带指定 JSON 的响应对象。

    异常说明：
        无。
    """
    request = httpx.Request("GET", "http://test.local/mock")
    return httpx.Response(status_code=200, request=request, json=payload)


def test_parse_data_includes_top_level_meta_into_data_mapping() -> None:
    """
    测试目的：
        验证当后端把 `meta` 放在顶层（与 `data` 同级）时，`parse_data` 会把 `meta` 合并进返回数据。

    预期结果：
        返回值为原 `data` 内容，并额外包含 `meta` 字段。
    """
    response = _build_response(
        {
            "code": 200,
            "message": "ok",
            "data": {
                "rows": [{"id": "6", "username": "ikun"}],
                "total": "1",
            },
            "meta": {
                "entityDescription": "管理端智能体用户列表视图",
            },
        }
    )

    result = HttpResponse.parse_data(response)

    assert result == {
        "rows": [{"id": "6", "username": "ikun"}],
        "total": "1",
        "meta": {"entityDescription": "管理端智能体用户列表视图"},
    }


def test_parse_data_keeps_data_meta_when_meta_already_in_data() -> None:
    """
    测试目的：
        验证当 `meta` 已经在 `data` 内部时，`parse_data` 不会重复覆盖或改写该字段。

    预期结果：
        返回值与原始 `data` 保持一致。
    """
    expected_data = {
        "rows": [{"id": "6", "username": "ikun"}],
        "meta": {
            "fieldDescriptions": {"username": "用户名"},
        },
    }
    response = _build_response(
        {
            "code": 200,
            "message": "ok",
            "data": expected_data,
        }
    )

    result = HttpResponse.parse_data(response)

    assert result == expected_data


def test_parse_data_raises_service_exception_for_non_success_code() -> None:
    """
    测试目的：
        验证当业务状态码非成功时，`parse_data` 会抛出 `ServiceException`。

    预期结果：
        抛出的异常 code/message 与响应中的业务码与消息一致。
    """
    response = _build_response(
        {
            "code": 500,
            "message": "biz error",
            "data": None,
        }
    )

    with pytest.raises(ServiceException) as exc_info:
        HttpResponse.parse_data(response)

    assert exc_info.value.code == 500
    assert exc_info.value.message == "biz error"


def test_from_response_raises_when_body_is_not_json() -> None:
    """
    测试目的：
        验证当响应体不是合法 JSON 时，`from_response` 会抛出 `ServiceException`。

    预期结果：
        异常码为 `ResponseCode.OPERATION_FAILED.code`。
    """
    request = httpx.Request("GET", "http://test.local/mock")
    response = httpx.Response(status_code=200, request=request, text="not-json")

    with pytest.raises(ServiceException) as exc_info:
        HttpResponse.from_response(response)

    assert exc_info.value.code == ResponseCode.OPERATION_FAILED.code
