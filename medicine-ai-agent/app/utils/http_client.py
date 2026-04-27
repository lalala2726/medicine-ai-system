from __future__ import annotations

import json as json_lib
import os
import time
import uuid
from pathlib import Path
from typing import Any, Literal, Mapping, Optional

import httpx
from dotenv import load_dotenv
from loguru import logger

from app.core.codes import ResponseCode
from app.core.exception.exceptions import ServiceException
from app.core.security.auth_context import get_authorization_header
from app.core.security.system_auth.canonical import build_canonical_string
from app.core.security.system_auth.config import get_system_auth_settings
from app.core.security.system_auth.constants import (
    HEADER_X_AGENT_KEY,
    HEADER_X_AGENT_NONCE,
    HEADER_X_AGENT_SIGNATURE,
    HEADER_X_AGENT_SIGN_VERSION,
    HEADER_X_AGENT_TIMESTAMP,
)
from app.core.security.system_auth.signer import sign_hmac_sha256_base64url


class HttpClient:
    """
    基于 httpx 的轻量 HTTP 客户端封装。

    - 支持 GET/POST/PUT/DELETE
    - 支持 headers / query params / body
    - 提供统一的超时与基础 headers
    """

    _dotenv_checked = False

    def __init__(
            self,
            *,
            base_url: Optional[str] = None,
            headers: Optional[Mapping[str, str]] = None,
            timeout: Optional[float] = 30.0,
            agent_key: Optional[str] = None,
            default_use_system_signature: Optional[bool] = None,
    ) -> None:
        """初始化 HTTP 客户端。

        Args:
            base_url: 默认基础地址。
            headers: 默认请求头。
            timeout: 默认超时时间（秒）。
            agent_key: 系统签名请求头中的 `X-Agent-Key`。
            default_use_system_signature: 系统签名默认开关。
                - `True`：默认强制签名；
                - `False`：默认不签名；
                - `None`：自动模式（存在用户 Authorization 时不签名；否则若配置了 `X_AGENT_KEY`
                  且存在 `SYSTEM_AUTH_LOCAL_SECRET` 则签名）。
        """
        self._ensure_env_loaded()
        base_url = self._resolve_base_url(base_url)
        self._default_log_enabled = self._parse_bool(
            os.getenv("HTTP_CLIENT_LOG_ENABLED")
        )
        self._agent_key = agent_key or os.getenv("X_AGENT_KEY", "")
        self._default_use_system_signature = default_use_system_signature
        self._default_headers = dict(headers or {})
        self._client = httpx.AsyncClient(
            base_url=base_url or "",
            headers=self._default_headers,
            timeout=timeout,
        )

    @staticmethod
    def _parse_bool(value: Optional[str | bool]) -> bool:
        if isinstance(value, bool):
            return value
        if value is None:
            return False
        return value.strip().lower() in {"1", "true", "yes", "on"}

    @classmethod
    def _ensure_env_loaded(cls) -> None:
        """
        兜底加载项目根目录 .env。
        避免在非 FastAPI 入口（例如脚本、LangGraph 本地运行）下没有提前 load_dotenv 导致配置不生效。
        """
        if cls._dotenv_checked:
            return
        env_path = Path(__file__).resolve().parents[2] / ".env"
        if env_path.exists():
            load_dotenv(dotenv_path=env_path, override=False)
        cls._dotenv_checked = True

    @staticmethod
    def _resolve_base_url(explicit_base_url: Optional[str]) -> str:
        """解析 HTTP 客户端基础地址。

        Args:
            explicit_base_url: 调用方显式传入的基础地址。

        Returns:
            str: 最终用于请求的基础地址。

        Raises:
            ServiceException: 未配置 `HTTP_BASE_URL` 时抛出。
        """

        if explicit_base_url is not None and explicit_base_url.strip() != "":
            return explicit_base_url.strip()
        env_base_url = (os.getenv("HTTP_BASE_URL") or "").strip()
        if env_base_url == "":
            raise ServiceException(
                code=ResponseCode.INTERNAL_ERROR,
                message="缺少 HTTP_BASE_URL 配置",
            )
        return env_base_url

    def _is_log_enabled(self) -> bool:
        value = os.getenv("HTTP_CLIENT_LOG_ENABLED")
        if value is None:
            return self._default_log_enabled
        return self._parse_bool(value)

    @property
    def base_url(self) -> str:
        """返回当前客户端基础地址。

        Returns:
            str: 当前客户端使用的基础 URL，末尾不带 `/`。
        """

        return str(self._client.base_url).rstrip("/")

    @staticmethod
    def _redact_headers(headers: Mapping[str, str]) -> Mapping[str, str]:
        sensitive_keys = {
            "authorization",
            "proxy-authorization",
            "x-api-key",
            "x-agent-key",
            "x-agent-signature",
        }
        return {
            key: "***" if key.lower() in sensitive_keys else value
            for key, value in headers.items()
        }

    @staticmethod
    def _truncate(value: Any, limit: int = 1000) -> str:
        if value is None:
            return ""
        try:
            if isinstance(value, (dict, list, tuple)):
                text = json_lib.dumps(value)
            else:
                text = str(value)
        except Exception:
            text = repr(value)
        if len(text) > limit:
            return f"{text[:limit]}...(truncated)"
        return text

    def _is_system_signing_mode(self) -> bool:
        """判断是否启用系统签名模式。

        Returns:
            bool: 配置了 `X-Agent-Key` 时返回 True。
        """
        return self._agent_key.strip() != ""

    def _resolve_system_signing_secret(self) -> tuple[str, str]:
        """解析当前服务出站签名密钥与签名版本。

        Returns:
            tuple[str, str]: `(secret, sign_version)`。

        Raises:
            ServiceException: 本地签名配置缺失或禁用时抛出。
        """
        settings = get_system_auth_settings()
        if not settings.enabled:
            raise ServiceException(
                code=ResponseCode.SERVICE_UNAVAILABLE,
                message="系统签名认证已禁用，禁止发送系统签名请求",
            )
        if settings.local_secret == "":
            raise ServiceException(
                code=ResponseCode.INTERNAL_ERROR,
                message="系统签名请求缺少 SYSTEM_AUTH_LOCAL_SECRET 配置",
            )
        return settings.local_secret, settings.default_sign_version

    @staticmethod
    def _try_get_authorization_header() -> Optional[str]:
        """尝试读取当前请求上下文中的 Authorization 头。

        Returns:
            Optional[str]: 读取成功返回原始 Authorization；不存在时返回 `None`。

        Raises:
            ServiceException: 非“缺少 Authorization”场景异常会透传。
        """
        try:
            return get_authorization_header()
        except ServiceException as exc:
            if exc.code == ResponseCode.UNAUTHORIZED.code:
                return None
            raise

    def _resolve_use_system_signature(
            self,
            *,
            use_system_signature: Optional[bool],
            has_authorization: bool,
    ) -> bool:
        """解析本次请求是否启用系统签名。

        Args:
            use_system_signature: 本次请求显式开关。
            has_authorization: 当前请求头中是否已有 Authorization。

        Returns:
            bool: 是否启用系统签名。
        """
        if use_system_signature is not None:
            return use_system_signature
        if self._default_use_system_signature is not None:
            return self._default_use_system_signature
        return self._is_system_signing_mode() and not has_authorization

    def _build_headers(
            self,
            headers: Optional[Mapping[str, str]],
            *,
            use_system_signature: Optional[bool] = None,
    ) -> tuple[Mapping[str, str], bool]:
        """
        合并请求头并注入当前请求的 Authorization / X-Agent 基础头。

        Args:
            headers: 显式传入请求头。
            use_system_signature: 本次请求是否启用系统签名。`None` 为自动模式。

        Returns:
            tuple[Mapping[str, str], bool]: `(合并后的请求头, 是否启用系统签名)`。
        """
        merged = dict(self._default_headers)
        if headers:
            merged.update(headers)
        lower_keys = {key.lower() for key in merged}

        if "authorization" not in lower_keys:
            auth = self._try_get_authorization_header()
            if auth:
                merged["Authorization"] = auth
                lower_keys.add("authorization")

        should_use_system_signature = self._resolve_use_system_signature(
            use_system_signature=use_system_signature,
            has_authorization="authorization" in lower_keys,
        )
        if should_use_system_signature and not self._is_system_signing_mode():
            raise ServiceException(
                code=ResponseCode.INTERNAL_ERROR,
                message="系统签名请求缺少 X_AGENT_KEY 配置",
            )
        if not should_use_system_signature and "authorization" not in lower_keys:
            raise ServiceException(
                code=ResponseCode.UNAUTHORIZED,
                message="缺少 Authorization 请求头",
            )

        # 添加 Agent 相关请求头（仅系统签名开启时）
        if should_use_system_signature:
            _, sign_version = self._resolve_system_signing_secret()
            merged[HEADER_X_AGENT_KEY] = self._agent_key
            merged[HEADER_X_AGENT_TIMESTAMP] = str(int(time.time()))
            merged[HEADER_X_AGENT_NONCE] = str(uuid.uuid4())
            merged[HEADER_X_AGENT_SIGN_VERSION] = sign_version

        return merged, should_use_system_signature

    def _attach_system_signature(
            self,
            request: httpx.Request,
            *,
            use_system_signature: bool,
    ) -> None:
        """为请求附加系统签名头。

        Args:
            request: 已构建但尚未发送的 httpx 请求对象。
            use_system_signature: 本次请求是否启用系统签名。

        Returns:
            None

        Raises:
            ServiceException: 系统签名参数缺失或配置非法时抛出。
        """
        if not use_system_signature:
            return
        timestamp_text = (request.headers.get(HEADER_X_AGENT_TIMESTAMP) or "").strip()
        nonce = (request.headers.get(HEADER_X_AGENT_NONCE) or "").strip()
        if timestamp_text == "" or nonce == "":
            raise ServiceException(
                code=ResponseCode.INTERNAL_ERROR,
                message="系统签名头构造失败：缺少时间戳或 nonce",
            )
        try:
            timestamp = int(timestamp_text)
        except ValueError as exc:
            raise ServiceException(
                code=ResponseCode.INTERNAL_ERROR,
                message="系统签名头构造失败：时间戳非法",
            ) from exc

        secret, sign_version = self._resolve_system_signing_secret()
        canonical_string = build_canonical_string(
            method=request.method,
            path=request.url.path,
            query_pairs=request.url.params.multi_items(),
            timestamp=timestamp,
            nonce=nonce,
            body_bytes=request.content or b"",
        )
        signature = sign_hmac_sha256_base64url(
            secret=secret,
            canonical_string=canonical_string,
        )
        request.headers[HEADER_X_AGENT_SIGN_VERSION] = sign_version
        request.headers[HEADER_X_AGENT_SIGNATURE] = signature

    async def close(self) -> None:
        """关闭底层连接池。"""
        await self._client.aclose()

    async def __aenter__(self) -> "HttpClient":
        return self

    async def __aexit__(self, exc_type, exc, tb) -> None:
        await self.close()

    async def request(
            self,
            method: str,
            url: str,
            *,
            headers: Optional[Mapping[str, str]] = None,
            params: Optional[Mapping[str, Any]] = None,
            json: Optional[Any] = None,
            data: Optional[Mapping[str, Any]] = None,
            content: Optional[bytes] = None,
            timeout: Optional[float] = None,
            response_format: Literal["raw", "json"] = "raw",
            include_envelope: bool = False,
            use_system_signature: Optional[bool] = None,
    ) -> Any:
        """
        统一请求入口。

        Args:
            method: HTTP 方法
            url: 请求路径或完整 URL
            headers: 额外请求头（会覆盖同名默认头）
            params: Query 参数
            json: JSON body
            data: 表单 body
            content: 原始字节 body
            timeout: 本次请求超时
            response_format: 返回格式。`raw` 返回 `httpx.Response`，
                `json` 返回 Python 对象。
            include_envelope: 当 `response_format` 为 `json` 时，
                是否保留 `code/message/data/timestamp` 包裹结构。
            use_system_signature: 本次请求系统签名开关。
                - `True`：强制系统签名；
                - `False`：强制不签名（走用户 Authorization）；
                - `None`：自动模式。
        """
        log_enabled = self._is_log_enabled()
        start = time.monotonic()
        try:
            built_headers, should_use_system_signature = self._build_headers(
                headers,
                use_system_signature=use_system_signature,
            )
        except Exception as exc:
            if log_enabled:
                logger.warning(
                    "HTTP request blocked before send: method={} url={} reason={}",
                    method,
                    url,
                    exc,
                )
            raise

        if log_enabled:
            logger.info(
                "HTTP request: method={} url={} headers={} params={} json={} data={} content_length={}",
                method,
                url,
                self._redact_headers(built_headers),
                params,
                self._truncate(json),
                self._truncate(data),
                len(content) if content else 0,
            )
        try:
            request_kwargs: dict[str, Any] = {
                "method": method,
                "url": url,
                "headers": built_headers,
                "params": params,
                "json": json,
                "data": data,
                "content": content,
            }
            if timeout is not None:
                request_kwargs["timeout"] = timeout

            request_obj = self._client.build_request(
                **request_kwargs,
            )
            self._attach_system_signature(
                request_obj,
                use_system_signature=should_use_system_signature,
            )
            response = await self._client.send(request_obj)
        except httpx.HTTPError as exc:
            if log_enabled:
                logger.error(
                    "HTTP request failed: method={} url={} params={} error={}",
                    method,
                    url,
                    params,
                    exc,
                )
            raise

        elapsed_ms = int((time.monotonic() - start) * 1000)
        body = response.text if response.content else ""
        if log_enabled:
            logger.info(
                "HTTP response: method={} url={} status={} elapsed_ms={}",
                method,
                url,
                response.status_code,
                elapsed_ms,
            )
            if body:
                snippet = body[:1000]
                if len(body) > 1000:
                    snippet = f"{snippet}...(truncated)"
                logger.info("HTTP response body: {}", snippet)

        if response.status_code >= 400 and log_enabled:
            logger.warning(
                "HTTP response error: method={} url={} status={} params={}",
                method,
                url,
                response.status_code,
                params,
            )
            if body:
                snippet = body[:500]
                if len(body) > 500:
                    snippet = f"{snippet}...(truncated)"
                logger.warning("HTTP response body: {}", snippet)

        if response_format == "raw":
            return response

        if response_format != "json":
            raise ValueError(f"Unsupported response_format: {response_format}")

        from app.schemas.http_response import HttpResponse

        parsed_response = HttpResponse.from_response(response)
        # 保持现有语义：非成功业务码抛异常。
        parsed_data = parsed_response.data_or_raise()
        payload: Any = parsed_data
        if include_envelope:
            payload = {
                "code": parsed_response.code,
                "message": parsed_response.message,
                "data": parsed_data,
            }
            if parsed_response.timestamp is not None:
                payload["timestamp"] = parsed_response.timestamp

        return payload

    async def get(
            self,
            url: str,
            *,
            headers: Optional[Mapping[str, str]] = None,
            params: Optional[Mapping[str, Any]] = None,
            timeout: Optional[float] = None,
            response_format: Literal["raw", "json"] = "raw",
            include_envelope: bool = False,
            use_system_signature: Optional[bool] = None,
    ) -> Any:
        """发送 GET 请求。"""
        return await self.request(
            "GET",
            url,
            headers=headers,
            params=params,
            timeout=timeout,
            response_format=response_format,
            include_envelope=include_envelope,
            use_system_signature=use_system_signature,
        )

    async def post(
            self,
            url: str,
            *,
            headers: Optional[Mapping[str, str]] = None,
            params: Optional[Mapping[str, Any]] = None,
            json: Optional[Any] = None,
            data: Optional[Mapping[str, Any]] = None,
            content: Optional[bytes] = None,
            timeout: Optional[float] = None,
            response_format: Literal["raw", "json"] = "raw",
            include_envelope: bool = False,
            use_system_signature: Optional[bool] = None,
    ) -> Any:
        """发送 POST 请求。"""
        return await self.request(
            "POST",
            url,
            headers=headers,
            params=params,
            json=json,
            data=data,
            content=content,
            timeout=timeout,
            response_format=response_format,
            include_envelope=include_envelope,
            use_system_signature=use_system_signature,
        )

    async def put(
            self,
            url: str,
            *,
            headers: Optional[Mapping[str, str]] = None,
            params: Optional[Mapping[str, Any]] = None,
            json: Optional[Any] = None,
            data: Optional[Mapping[str, Any]] = None,
            content: Optional[bytes] = None,
            timeout: Optional[float] = None,
            response_format: Literal["raw", "json"] = "raw",
            include_envelope: bool = False,
            use_system_signature: Optional[bool] = None,
    ) -> Any:
        """发送 PUT 请求。"""
        return await self.request(
            "PUT",
            url,
            headers=headers,
            params=params,
            json=json,
            data=data,
            content=content,
            timeout=timeout,
            response_format=response_format,
            include_envelope=include_envelope,
            use_system_signature=use_system_signature,
        )

    async def delete(
            self,
            url: str,
            *,
            headers: Optional[Mapping[str, str]] = None,
            params: Optional[Mapping[str, Any]] = None,
            json: Optional[Any] = None,
            data: Optional[Mapping[str, Any]] = None,
            content: Optional[bytes] = None,
            timeout: Optional[float] = None,
            response_format: Literal["raw", "json"] = "raw",
            include_envelope: bool = False,
            use_system_signature: Optional[bool] = None,
    ) -> Any:
        """发送 DELETE 请求。"""
        return await self.request(
            "DELETE",
            url,
            headers=headers,
            params=params,
            json=json,
            data=data,
            content=content,
            timeout=timeout,
            response_format=response_format,
            include_envelope=include_envelope,
            use_system_signature=use_system_signature,
        )
