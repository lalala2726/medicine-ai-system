import pytest

from app.core.codes import ResponseCode
from app.core.exception.exceptions import ServiceException
from app.core.speech import env_utils as speech_env_utils


def test_resolve_required_env_prefers_dotenv_over_process_env(
        monkeypatch,
        tmp_path,
) -> None:
    dotenv_file = tmp_path / ".env"
    dotenv_file.write_text("VOLCENGINE_APP_ID=from_dotenv\n", encoding="utf-8")
    monkeypatch.setattr(speech_env_utils, "DOTENV_FILE", dotenv_file)
    monkeypatch.setenv("VOLCENGINE_APP_ID", "from_process_env")

    value = speech_env_utils.resolve_required_env("VOLCENGINE_APP_ID")

    assert value == "from_dotenv"


def test_resolve_required_env_falls_back_to_process_env_when_dotenv_empty(
        monkeypatch,
        tmp_path,
) -> None:
    dotenv_file = tmp_path / ".env"
    dotenv_file.write_text("VOLCENGINE_APP_ID=\n", encoding="utf-8")
    monkeypatch.setattr(speech_env_utils, "DOTENV_FILE", dotenv_file)
    monkeypatch.setenv("VOLCENGINE_APP_ID", "from_process_env")

    value = speech_env_utils.resolve_required_env("VOLCENGINE_APP_ID")

    assert value == "from_process_env"


def test_resolve_required_env_raises_when_missing_everywhere(
        monkeypatch,
        tmp_path,
) -> None:
    dotenv_file = tmp_path / ".env"
    dotenv_file.write_text("", encoding="utf-8")
    monkeypatch.setattr(speech_env_utils, "DOTENV_FILE", dotenv_file)
    monkeypatch.delenv("VOLCENGINE_APP_ID", raising=False)

    with pytest.raises(ServiceException) as exc_info:
        speech_env_utils.resolve_required_env("VOLCENGINE_APP_ID")

    assert exc_info.value.code == ResponseCode.INTERNAL_ERROR.code
    assert "VOLCENGINE_APP_ID is not set" in exc_info.value.message


def test_parse_positive_int_uses_default_when_value_empty() -> None:
    resolved = speech_env_utils.parse_positive_int(
        value="",
        name="TEST_VALUE",
        default=42,
    )
    assert resolved == 42


def test_parse_positive_int_raises_when_value_invalid() -> None:
    with pytest.raises(ServiceException) as exc_info:
        speech_env_utils.parse_positive_int(
            value="abc",
            name="TEST_VALUE",
            default=1,
        )
    assert exc_info.value.code == ResponseCode.INTERNAL_ERROR.code
    assert "TEST_VALUE must be an integer" == exc_info.value.message
