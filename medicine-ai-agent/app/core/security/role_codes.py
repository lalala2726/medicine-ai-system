from enum import StrEnum


class RoleCode(StrEnum):
    """系统内置角色编码枚举。"""

    SUPER_ADMIN = "super_admin"
    ADMIN = "admin"
    USER = "user"
