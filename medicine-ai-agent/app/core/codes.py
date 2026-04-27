from enum import IntEnum


class ResponseCode(IntEnum):
    SUCCESS = (200, "操作成功")
    BAD_REQUEST = (400, "请求错误")
    OPERATION_FAILED = (400, "操作失败")
    RESULT_EMPTY = (400, "结果为空")
    UNAUTHORIZED = (401, "未认证或登录已失效")
    FORBIDDEN = (403, "无权限访问")
    CONFLICT = (409, "资源冲突")
    NOT_FOUND = (404, "资源不存在")
    TOO_MANY_REQUESTS = (429, "请求过于频繁")
    ERROR = (500, "服务器内部异常")
    INTERNAL_ERROR = (500, "服务器内部异常")
    DATABASE_ERROR = (500, "数据库错误")
    SERVICE_UNAVAILABLE = (503, "服务暂不可用")

    def __new__(cls, code: int, message: str):
        obj = int.__new__(cls, code)
        obj._value_ = code
        obj.message = message
        return obj

    @property
    def code(self) -> int:
        return int(self)
