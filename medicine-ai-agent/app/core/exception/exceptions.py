from typing import Any, Optional, Union

from app.core.codes import ResponseCode


class ServiceException(Exception):
    """
    统一业务异常类型。

    约定：
    1. `code` 可传入 `ResponseCode` 或 int；
    2. 当 message 为空且 code 为 `ResponseCode` 时，默认使用枚举自带文案；
    3. data 用于附加错误上下文（可选）。
    """

    def __init__(
            self,
            message: Optional[str] = None,
            code: Union[int, ResponseCode] = ResponseCode.OPERATION_FAILED,
            data: Optional[Any] = None,
    ) -> None:
        """
        初始化业务异常。

        Args:
            message: 错误文案；为空时按 code 推导默认文案。
            code: 错误码，可传 int 或 ResponseCode。
            data: 额外错误上下文数据（可选）。
        """

        resolved_message = message
        if resolved_message is None and isinstance(code, ResponseCode):
            resolved_message = code.message
        if resolved_message is None:
            resolved_message = "error"
        super().__init__(resolved_message)
        self.message = resolved_message
        self.code = int(code)
        self.data = data
