"""安全相关能力统一出口。"""

from app.core.security.anonymous_access import (
    allow_anonymous,
    is_anonymous_endpoint,
    is_anonymous_request,
)
from app.core.security.auth_context import (
    get_authorization_header,
    get_current_token,
    get_current_user,
    get_current_user_id,
    get_user,
    get_user_id,
    reset_authorization_header,
    reset_current_user,
    set_authorization_header,
    set_current_user,
)
from app.core.security.cors import load_cors_config
from app.core.security.pre_authorize import has_permission, has_role, pre_authorize
from app.core.security.rate_limit import (
    RateLimitException,
    RateLimitPreset,
    RateLimitRule,
    rate_limit,
)
from app.core.security.role_codes import RoleCode
from app.core.security.system_auth import (
    allow_system,
    is_system_endpoint,
    is_system_request,
    verify_system_request,
)

__all__ = [
    "RoleCode",
    "allow_anonymous",
    "allow_system",
    "is_anonymous_endpoint",
    "is_anonymous_request",
    "is_system_endpoint",
    "is_system_request",
    "verify_system_request",
    "pre_authorize",
    "has_role",
    "has_permission",
    "load_cors_config",
    "set_authorization_header",
    "reset_authorization_header",
    "get_authorization_header",
    "set_current_user",
    "reset_current_user",
    "get_current_user",
    "get_current_user_id",
    "get_current_token",
    "get_user",
    "get_user_id",
    "rate_limit",
    "RateLimitPreset",
    "RateLimitRule",
    "RateLimitException",
]
