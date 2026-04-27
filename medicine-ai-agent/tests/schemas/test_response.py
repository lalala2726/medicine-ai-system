from app.core.codes import ResponseCode
from app.schemas.response import ApiResponse


def test_api_response_success_defaults():
    response = ApiResponse.success()

    assert response.code == ResponseCode.SUCCESS
    assert response.message == ResponseCode.SUCCESS.message
    dumped = response.model_dump()
    assert "data" not in dumped
    assert dumped["code"] == ResponseCode.SUCCESS
    assert "timestamp" in dumped


def test_api_response_error_default_message():
    response = ApiResponse.error(ResponseCode.BAD_REQUEST)

    assert response.code == ResponseCode.BAD_REQUEST
    assert response.message == ResponseCode.BAD_REQUEST.message


def test_api_response_error_custom_message_and_data():
    response = ApiResponse.error(ResponseCode.BAD_REQUEST, message="bad", data={"x": 1})

    assert response.code == ResponseCode.BAD_REQUEST
    assert response.message == "bad"
    assert response.data == {"x": 1}
    dumped = response.model_dump()
    assert dumped["data"] == {"x": 1}
