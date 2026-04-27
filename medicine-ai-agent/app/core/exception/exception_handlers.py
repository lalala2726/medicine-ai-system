import time
from collections.abc import Mapping

from fastapi import Request, WebSocket
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from loguru import logger
from pymongo.errors import PyMongoError
from starlette.exceptions import HTTPException as StarletteHTTPException

from app.core.codes import ResponseCode
from app.core.exception.exceptions import ServiceException
from app.schemas.response import ApiResponse


class ExceptionHandlers:
    """
    全局异常处理器集合。

    设计目标：
    1. 统一 API 错误响应结构；
    2. 统一异常日志格式与分类；
    3. 对业务异常、框架异常、数据库异常提供稳定映射。
    """

    @staticmethod
    def _get_request_state_value(
            request: Request | WebSocket | None,
            *,
            key: str,
            default: str,
    ) -> str:
        """读取请求对象中的审计上下文字段。

        Args:
            request: 当前请求对象，可为 `Request`、`WebSocket` 或 `None`。
            key: `request.state` 中的字段名。
            default: 字段不存在时返回的默认值。

        Returns:
            str: 解析后的字段字符串。
        """

        if request is None:
            return default
        state = getattr(request, "state", None)
        if state is None:
            return default
        resolved_value = getattr(state, key, default)
        if resolved_value is None:
            return default
        return str(resolved_value)

    @staticmethod
    def _build_request_context(request: Request | WebSocket | None) -> dict[str, str]:
        """
        构造统一的请求定位信息。

        仅输出 method/path/client 与审计上下文字段，避免泄露 body 与敏感头信息。

        Args:
            request: FastAPI Request/WebSocket 对象；在部分异常场景中可能为 None。

        Returns:
            dict[str, str]: 规范化请求上下文字典。
        """

        if request is None:
            return {
                "method": "unknown",
                "path": "unknown",
                "client_ip": "unknown",
                "user_id": "-",
                "auth_mode": "unknown",
                "request_id": "-",
            }

        method = getattr(request, "method", None) or getattr(request, "scope", {}).get("type", "unknown")
        url = getattr(request, "url", None)
        path = getattr(url, "path", None) or getattr(request, "scope", {}).get("path", "unknown")
        client = getattr(request, "client", None)
        if client is None:
            client_ip = "unknown"
        else:
            client_ip = client.host or "unknown"
        return {
            "method": str(method),
            "path": str(path),
            "client_ip": str(client_ip),
            "user_id": ExceptionHandlers._get_request_state_value(
                request,
                key="user_id",
                default="-",
            ),
            "auth_mode": ExceptionHandlers._get_request_state_value(
                request,
                key="auth_mode",
                default="unknown",
            ),
            "request_id": ExceptionHandlers._get_request_state_value(
                request,
                key="request_id",
                default="-",
            ),
        }

    @staticmethod
    def _is_client_side_failure_code(code: int) -> bool:
        """判断错误码是否属于客户端侧可预期失败。

        Args:
            code: 当前异常对应的业务码或 HTTP 状态码。

        Returns:
            bool: `4xx` 与 `4xxx` 类业务失败返回 `True`。
        """

        if 400 <= code <= 499:
            return True
        code_text = str(code).strip()
        return len(code_text) >= 3 and code_text.startswith("4")

    @staticmethod
    def _resolve_service_exception_level(code: int) -> str:
        """解析业务异常的日志级别。

        Args:
            code: 业务异常错误码。

        Returns:
            str: `WARNING` 或 `ERROR`。
        """

        return "WARNING" if ExceptionHandlers._is_client_side_failure_code(code) else "ERROR"

    @staticmethod
    def _resolve_http_exception_level(status_code: int) -> str:
        """解析 HTTPException 的日志级别。

        Args:
            status_code: HTTP 状态码。

        Returns:
            str: `WARNING` 或 `ERROR`。
        """

        return "WARNING" if 400 <= status_code <= 499 else "ERROR"

    @staticmethod
    def _stringify_exception_message(exc: Exception) -> str:
        """提取异常文案。

        Args:
            exc: 当前异常对象。

        Returns:
            str: 优先使用业务异常 message，其次使用 `str(exc)`。
        """

        if isinstance(exc, ServiceException):
            return exc.message
        message_text = str(exc).strip()
        if message_text != "":
            return message_text
        return exc.__class__.__name__

    @staticmethod
    def _log_exception(
            *,
            request: Request | WebSocket | None,
            exc: Exception,
            category: str,
            level: str,
            include_stack: bool,
    ) -> None:
        """
        记录异常详细日志。

        category 用于标识异常来源：validation/service/http/database/unhandled。
        `WARNING` 级别异常不记录堆栈，`ERROR` 级别异常记录完整堆栈。

        Args:
            request: 触发异常的请求对象，可为 None。
            exc: 捕获到的异常对象。
            category: 异常分类标签，用于日志检索与聚合。
            level: 本次日志级别。
            include_stack: 是否输出完整异常堆栈。

        Returns:
            None
        """

        request_context = ExceptionHandlers._build_request_context(request)
        bound_logger = logger.bind(
            category=category,
            method=request_context["method"],
            path=request_context["path"],
            client_ip=request_context["client_ip"],
            user_id=request_context["user_id"],
            auth_mode=request_context["auth_mode"],
            request_id=request_context["request_id"],
        )
        if include_stack:
            bound_logger.opt(exception=exc).log(level, "API exception")
            return
        bound_logger.log(
            level,
            "API exception message={}",
            ExceptionHandlers._stringify_exception_message(exc),
        )

    @staticmethod
    def _resolve_http_status(code: int) -> int:
        """
        JSONResponse 仅接受合法 HTTP 状态码（100-599）。
        当业务 code 非法（如 4011）时，回落为 400，避免 ASGI 层抛错。

        Args:
            code: 业务异常中的响应码。

        Returns:
            int: 可用于 HTTP 响应的合法状态码。
        """
        return code if 100 <= int(code) <= 599 else ResponseCode.BAD_REQUEST.code

    @staticmethod
    def _format_validation_errors(exc: RequestValidationError) -> list[dict[str, str]]:
        """
        将 FastAPI 参数校验异常格式化为前端可读结构。

        Args:
            exc: FastAPI 抛出的请求校验异常。

        Returns:
            list[dict[str, str]]: 规范化错误列表，包含 field/message/type 字段。
        """

        formatted_errors: list[dict[str, str]] = []
        for item in exc.errors():
            loc = item.get("loc", ())
            field_parts = [
                str(part)
                for part in loc
                if part not in {"body", "query", "path", "header", "cookie"}
            ]
            field = ".".join(field_parts) if field_parts else "body"
            formatted_errors.append(
                {
                    "field": field,
                    "message": item.get("msg", "Validation error"),
                    "type": item.get("type", "validation_error"),
                }
            )
        return formatted_errors

    @staticmethod
    async def request_validation_exception_handler(
            request: Request | WebSocket | None, exc: RequestValidationError
    ) -> JSONResponse:
        """
        处理请求参数校验异常。

        Args:
            request: 当前请求对象。
            exc: 参数校验异常对象。

        Returns:
            JSONResponse: 统一错误响应，状态码为 400。
        """

        ExceptionHandlers._log_exception(
            request=request,
            exc=exc,
            category="validation",
            level="WARNING",
            include_stack=False,
        )
        return JSONResponse(
            status_code=ResponseCode.BAD_REQUEST,
            content={
                "code": ResponseCode.BAD_REQUEST,
                "message": "Validation Failed",
                "errors": ExceptionHandlers._format_validation_errors(exc),
                "timestamp": int(time.time() * 1000),
            },
        )

    @staticmethod
    async def service_exception_handler(request: Request | WebSocket | None, exc: ServiceException) -> JSONResponse:
        """
        处理业务异常（ServiceException）。

        处理逻辑：
        1. 若异常码可映射到 ResponseCode，使用统一 `ApiResponse.error(...)`；
        2. 若异常码为自定义值（非枚举），仍保持原始 code/message 返回；
        3. HTTP 状态码统一通过 `_resolve_http_status` 做合法化兜底。

        Args:
            request: 当前请求对象。
            exc: 业务异常对象。

        Returns:
            JSONResponse: 统一业务错误响应。
        """

        resolved_level = ExceptionHandlers._resolve_service_exception_level(exc.code)
        ExceptionHandlers._log_exception(
            request=request,
            exc=exc,
            category="service",
            level=resolved_level,
            include_stack=resolved_level == "ERROR",
        )
        try:
            response_code = ResponseCode(exc.code)
        except ValueError:
            response_code = None
        if response_code:
            content = ApiResponse.error(response_code, message=exc.message, data=exc.data).model_dump()
        else:
            content = ApiResponse(code=exc.code, message=exc.message, data=exc.data).model_dump()
        headers = getattr(exc, "headers", None)
        resolved_headers: dict[str, str] | None = None
        if isinstance(headers, Mapping):
            resolved_headers = {
                str(key): str(value)
                for key, value in headers.items()
            }
        return JSONResponse(
            status_code=ExceptionHandlers._resolve_http_status(exc.code),
            content=content,
            headers=resolved_headers,
        )

    @staticmethod
    async def http_exception_handler(request: Request | WebSocket | None, exc: StarletteHTTPException) -> JSONResponse:
        """
        处理 Starlette/FastAPI 的 HTTPException。

        Args:
            request: 当前请求对象。
            exc: HTTP 异常对象。

        Returns:
            JSONResponse: 统一 HTTP 错误响应。
        """

        resolved_level = ExceptionHandlers._resolve_http_exception_level(exc.status_code)
        ExceptionHandlers._log_exception(
            request=request,
            exc=exc,
            category="http",
            level=resolved_level,
            include_stack=resolved_level == "ERROR",
        )
        try:
            response_code = ResponseCode(exc.status_code)
        except ValueError:
            response_code = None
        if response_code:
            # 404 错误时包含请求路径信息
            if exc.status_code == 404:
                path = request.url.path if request is not None else "unknown"
                message = f"路由 {path} 不存在"
            else:
                message = None
            content = ApiResponse.error(response_code, message=message).model_dump()
        else:
            content = ApiResponse(code=exc.status_code, message=str(exc.detail)).model_dump()
        return JSONResponse(
            status_code=exc.status_code,
            content=content,
        )

    @staticmethod
    async def pymongo_exception_handler(request: Request | WebSocket | None, exc: PyMongoError) -> JSONResponse:
        """
        处理 MongoDB 驱动异常（PyMongoError）。

        该处理器用于兜底所有请求链路中的数据库异常，不向客户端透出底层错误细节。

        Args:
            request: 当前请求对象。
            exc: PyMongo 相关异常。

        Returns:
            JSONResponse: 统一数据库错误响应（500/数据库错误）。
        """

        ExceptionHandlers._log_exception(
            request=request,
            exc=exc,
            category="database",
            level="ERROR",
            include_stack=True,
        )
        return JSONResponse(
            status_code=ResponseCode.DATABASE_ERROR,
            content=ApiResponse.error(ResponseCode.DATABASE_ERROR).model_dump(),
        )

    @staticmethod
    async def unhandled_exception_handler(request: Request | WebSocket | None, exc: Exception) -> JSONResponse:
        """
        处理未被其他处理器捕获的未知异常。

        Args:
            request: 当前请求对象。
            exc: 未知异常对象。

        Returns:
            JSONResponse: 通用内部错误响应（500）。
        """

        ExceptionHandlers._log_exception(
            request=request,
            exc=exc,
            category="unhandled",
            level="ERROR",
            include_stack=True,
        )
        return JSONResponse(
            status_code=ResponseCode.INTERNAL_ERROR,
            content=ApiResponse.error(ResponseCode.INTERNAL_ERROR).model_dump(),
        )
