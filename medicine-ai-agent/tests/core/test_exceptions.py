from app.core.codes import ResponseCode
from app.core.exception.exceptions import ServiceException


def test_service_exception_with_response_code_message():
    exc = ServiceException(code=ResponseCode.NOT_FOUND)

    assert exc.message == ResponseCode.NOT_FOUND.message
    assert exc.code == ResponseCode.NOT_FOUND


def test_service_exception_with_missing_message_and_int_code():
    exc = ServiceException(code=422)

    assert exc.message == "error"
    assert exc.code == 422


def test_service_exception_with_explicit_message():
    exc = ServiceException(message="custom", code=ResponseCode.BAD_REQUEST, data={"a": 1})

    assert exc.message == "custom"
    assert exc.code == ResponseCode.BAD_REQUEST
    assert exc.data == {"a": 1}
