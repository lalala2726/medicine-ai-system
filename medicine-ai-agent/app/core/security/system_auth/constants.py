"""系统级签名认证常量定义。"""

# 请求头：系统调用方标识（app_id）
HEADER_X_AGENT_KEY = "X-Agent-Key"
# 请求头：秒级时间戳
HEADER_X_AGENT_TIMESTAMP = "X-Agent-Timestamp"
# 请求头：一次性随机串（防重放）
HEADER_X_AGENT_NONCE = "X-Agent-Nonce"
# 请求头：签名值（Base64URL 编码）
HEADER_X_AGENT_SIGNATURE = "X-Agent-Signature"
# 请求头：签名版本
HEADER_X_AGENT_SIGN_VERSION = "X-Agent-Sign-Version"

# 默认签名版本
DEFAULT_SIGN_VERSION = "v1"
# 系统鉴权路由标记属性
SYSTEM_ENDPOINT_ATTR = "__allow_system__"
