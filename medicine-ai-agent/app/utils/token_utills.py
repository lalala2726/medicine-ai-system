from __future__ import annotations

import importlib.util
from functools import lru_cache
from typing import Optional, Sequence

from app.core.exception.exceptions import ServiceException


class TokenUtils:
    """基于 tiktoken 的 token 计数工具类。"""

    @staticmethod
    @lru_cache(maxsize=8)
    def get_encoder(
            encoding_name: Optional[str] = "cl100k_base",
            model_name: Optional[str] = None,
    ):
        """获取 tiktoken 编码器。"""
        if importlib.util.find_spec("tiktoken") is None:
            raise ServiceException("token 计数依赖 tiktoken，请先安装")
        import tiktoken

        if model_name:
            return tiktoken.encoding_for_model(model_name)
        return tiktoken.get_encoding(encoding_name or "cl100k_base")

    @staticmethod
    def count_tokens(
            text: str,
            *,
            encoding_name: Optional[str] = "cl100k_base",
            model_name: Optional[str] = None,
    ) -> int:
        """统计单个文本的 token 数量。"""
        encoder = TokenUtils.get_encoder(encoding_name, model_name)
        return len(encoder.encode(text))

    @staticmethod
    def count_tokens_list(
            texts: Sequence[str],
            *,
            encoding_name: Optional[str] = "cl100k_base",
            model_name: Optional[str] = None,
    ) -> list[int]:
        """统计多个文本的 token 数量，按输入顺序返回列表。"""
        encoder = TokenUtils.get_encoder(encoding_name, model_name)
        return [len(encoder.encode(text)) for text in texts]

    @staticmethod
    def within_limit(
            text: str,
            max_tokens: int,
            *,
            encoding_name: Optional[str] = "cl100k_base",
            model_name: Optional[str] = None,
    ) -> bool:
        """判断文本 token 数量是否不超过最大限制。"""
        token_count = TokenUtils.count_tokens(
            text,
            encoding_name=encoding_name,
            model_name=model_name,
        )
        return token_count <= max_tokens
