import importlib.util

import pytest

from app.core.exception.exceptions import ServiceException
from app.utils.token_utills import TokenUtils


class DummyEncoder:
    def encode(self, text: str) -> list[int]:
        return list(range(len(text)))


def test_get_encoder_requires_tiktoken(monkeypatch):
    TokenUtils.get_encoder.cache_clear()
    monkeypatch.setattr(importlib.util, "find_spec", lambda name: None)

    with pytest.raises(ServiceException):
        TokenUtils.get_encoder()


def test_count_tokens_and_list(monkeypatch):
    TokenUtils.get_encoder.cache_clear()
    monkeypatch.setattr(TokenUtils, "_get_encoder", lambda *args, **kwargs: DummyEncoder())

    assert TokenUtils.count_tokens("abc") == 3
    assert TokenUtils.count_tokens_list(["a", "abcd", ""]) == [1, 4, 0]


def test_within_limit(monkeypatch):
    TokenUtils.get_encoder.cache_clear()
    monkeypatch.setattr(TokenUtils, "_get_encoder", lambda *args, **kwargs: DummyEncoder())

    assert TokenUtils.within_limit("abcd", 4) is True
    assert TokenUtils.within_limit("abcd", 3) is False
