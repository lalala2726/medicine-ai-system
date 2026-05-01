import asyncio
from contextlib import asynccontextmanager

from dotenv import load_dotenv
from fastapi import FastAPI
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from pymongo.errors import PyMongoError
from starlette.exceptions import HTTPException as StarletteHTTPException

from app.api.main import api_router
from app.core.config_sync import initialize_agent_config_snapshot
from app.core.agent.tracing import start_agent_tracing, stop_agent_tracing
from app.core.database import clear_neo4j_connection_cache, verify_neo4j_connection
from app.core.database.neo4j.config import is_neo4j_startup_ping_enabled
from app.core.exception.exception_handlers import ExceptionHandlers
from app.core.exception.exceptions import ServiceException
from app.core.logging import initialize_logging
from app.core.logging.http_audit import authorization_header_middleware
from app.core.mq.broker import get_broker, is_mq_configured
from app.core.prompt_sync import initialize_agent_prompt_snapshot
from app.core.security.cors import load_cors_config
from app.core.speech import (
    verify_volcengine_stt_connection_on_startup,
    verify_volcengine_tts_connection_on_startup,
)

# 加载 .env 配置，确保本地开发环境变量生效
load_dotenv()
initialize_logging()

OPENAPI_DESCRIPTION = """
    ## 项目简介
    
    本项目提供药品相关的 AI 能力接口，包含管理助手对话、药品图片解析、知识库导入与检索等功能。
    
    ## 认证说明
    
    除 `/docs`、`/redoc`、`/openapi.json`、显式标注 `allow_anonymous`（匿名）与
    `allow_system`（系统签名）的接口外，其他接口均需要用户认证。
    
    请由药品服务端提供访问令牌，并在请求头中携带：
    
    - `Authorization: Bearer <token>`
"""

_speech_startup_probe_done = False  # 进程级语音探活状态，避免重复执行重型启动检查。


async def _run_speech_startup_probes() -> None:
    """在配置快照就绪后执行语音启动探活。"""

    probe_results = await asyncio.gather(
        verify_volcengine_stt_connection_on_startup(),
        verify_volcengine_tts_connection_on_startup(),
        return_exceptions=True,
    )
    for result in probe_results:
        if isinstance(result, Exception):
            raise result


async def _prepare_runtime_before_serving() -> None:
    """显式固化启动顺序：先加载配置快照，再探活语音连接。"""

    initialize_agent_config_snapshot()
    initialize_agent_prompt_snapshot()
    if is_neo4j_startup_ping_enabled():
        verify_neo4j_connection()
    await _run_speech_startup_probes()


@asynccontextmanager
async def lifespan(_app: FastAPI):
    """管理服务启动与关闭期间的资源生命周期。

    Args:
        _app: FastAPI 应用实例；当前实现中仅用于满足生命周期签名约束。

    Returns:
        AsyncIterator[None]: 应用生命周期上下文管理器。
    """

    global _speech_startup_probe_done
    if not _speech_startup_probe_done:
        await _prepare_runtime_before_serving()
        _speech_startup_probe_done = True
    else:
        initialize_agent_config_snapshot()
        initialize_agent_prompt_snapshot()
    start_agent_tracing()

    # 启动 MQ broker（有配置时才启动）
    _broker = None
    if is_mq_configured():
        import app.core.mq.handlers  # noqa: F401 — 触发 subscriber 注册
        _broker = get_broker()
        await _broker.start()

    try:
        yield
    finally:
        try:
            if _broker is not None:
                await _broker.close()
        finally:
            try:
                stop_agent_tracing()
            finally:
                clear_neo4j_connection_cache()


app = FastAPI(
    title="Medicine AI Agent API",
    description=OPENAPI_DESCRIPTION,
    version="0.1.0",
    lifespan=lifespan,
)
app.add_middleware(CORSMiddleware, **load_cors_config())
app.include_router(api_router)

app.add_exception_handler(
    RequestValidationError,
    ExceptionHandlers.request_validation_exception_handler,
)
app.add_exception_handler(ServiceException, ExceptionHandlers.service_exception_handler)
app.add_exception_handler(StarletteHTTPException, ExceptionHandlers.http_exception_handler)
app.add_exception_handler(PyMongoError, ExceptionHandlers.pymongo_exception_handler)
app.add_exception_handler(Exception, ExceptionHandlers.unhandled_exception_handler)
app.middleware("http")(authorization_header_middleware)
