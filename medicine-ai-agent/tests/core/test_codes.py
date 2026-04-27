from app.core.codes import ResponseCode


def test_response_code_values_and_message():
    assert int(ResponseCode.SUCCESS) == 200
    assert ResponseCode.SUCCESS.message == "操作成功"
    assert ResponseCode.SUCCESS.code == 200
