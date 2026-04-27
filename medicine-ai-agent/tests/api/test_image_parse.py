from fastapi.testclient import TestClient

import app.main as main_module
from app.api.routes import image_parse as image_parse_module
from app.main import app
from app.schemas.auth import AuthUser


def _auth_headers() -> dict[str, str]:
    return {"Authorization": "Bearer test-token"}


def _mock_auth(monkeypatch) -> None:
    async def _fake_fetch_current_user() -> AuthUser:
        return AuthUser(id=1, username="tester")

    monkeypatch.setattr(
        main_module,
        "verify_authorization",
        _fake_fetch_current_user,
    )


def test_image_parse_success(monkeypatch):
    _mock_auth(monkeypatch)
    monkeypatch.setattr(image_parse_module, "parse_drug_images", lambda images: {"ok": True})
    client = TestClient(app)

    response = client.post(
        "/image_parse/drug",
        headers=_auth_headers(),
        json={"images": ["abc"]},
    )

    assert response.status_code == 200
    body = response.json()
    assert body["code"] == 200
    assert body["message"] == "解析成功"
    assert body["data"] == {"ok": True}


def test_image_parse_requires_images(monkeypatch):
    _mock_auth(monkeypatch)
    client = TestClient(app)

    response = client.post(
        "/image_parse/drug",
        headers=_auth_headers(),
        json={"images": []},
    )

    assert response.status_code == 400
    body = response.json()
    assert body["code"] == 400
    assert body["message"] == "Validation Failed"
    assert body["errors"][0]["field"] == "images"
    assert body["errors"][0]["type"] == "too_short"


def test_image_parse_validation_error_is_wrapped(monkeypatch):
    _mock_auth(monkeypatch)
    client = TestClient(app)

    response = client.post(
        "/image_parse/drug",
        headers=_auth_headers(),
        json={"imageUrl": ["abc"]},
    )

    assert response.status_code == 400
    body = response.json()
    assert body["code"] == 400
    assert body["message"] == "Validation Failed"
    assert "detail" not in body
    assert body["errors"][0]["field"] == "image_urls"
    assert body["errors"][0]["type"] == "missing"
