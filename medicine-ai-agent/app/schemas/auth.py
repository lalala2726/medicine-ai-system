from __future__ import annotations

from datetime import datetime, timezone
from typing import Any

from pydantic import AliasChoices, BaseModel, ConfigDict, Field, field_validator


def _parse_datetime_value(value: Any) -> datetime | None:
    """容错解析 Spring 可能返回的日期格式（秒/毫秒时间戳或字符串）。"""
    if value is None:
        return None
    if isinstance(value, datetime):
        return value
    if isinstance(value, (int, float)):
        timestamp = float(value)
        # 13 位毫秒时间戳转换为秒；10 位秒级时间戳直接使用。
        if abs(timestamp) > 1e11:
            timestamp /= 1000.0
        return datetime.fromtimestamp(timestamp, tz=timezone.utc)
    if isinstance(value, str):
        text = value.strip()
        if not text:
            return None
        if text.lstrip("-").isdigit():
            return _parse_datetime_value(int(text))
        try:
            return datetime.fromisoformat(text.replace("Z", "+00:00"))
        except ValueError:
            return None
    return None


class AuthUser(BaseModel):
    """认证后的当前用户上下文模型（对齐 Spring AuthUserDto）。"""

    model_config = ConfigDict(
        populate_by_name=True,
        extra="ignore",
    )

    id: int
    username: str | None = None
    nickname: str | None = None
    avatar: str | None = None
    email: str | None = None
    phone_number: str | None = Field(
        default=None,
        validation_alias=AliasChoices("phone_number", "phoneNumber"),
    )
    gender: int | None = None
    birthday: datetime | None = None
    real_name: str | None = Field(
        default=None,
        validation_alias=AliasChoices("real_name", "realName"),
    )
    id_card: str | None = Field(
        default=None,
        validation_alias=AliasChoices("id_card", "idCard"),
    )
    last_login_time: datetime | None = Field(
        default=None,
        validation_alias=AliasChoices("last_login_time", "lastLoginTime"),
    )
    last_login_ip: str | None = Field(
        default=None,
        validation_alias=AliasChoices("last_login_ip", "lastLoginIp"),
    )
    last_login_location: str | None = Field(
        default=None,
        validation_alias=AliasChoices("last_login_location", "lastLoginLocation"),
    )
    status: int | None = None
    create_time: datetime | None = Field(
        default=None,
        validation_alias=AliasChoices("create_time", "createTime"),
    )
    update_time: datetime | None = Field(
        default=None,
        validation_alias=AliasChoices("update_time", "updateTime"),
    )
    create_by: str | None = Field(
        default=None,
        validation_alias=AliasChoices("create_by", "createBy"),
    )
    update_by: str | None = Field(
        default=None,
        validation_alias=AliasChoices("update_by", "updateBy"),
    )
    delete_time: datetime | None = Field(
        default=None,
        validation_alias=AliasChoices("delete_time", "deleteTime"),
    )
    is_delete: int | None = Field(
        default=None,
        validation_alias=AliasChoices("is_delete", "isDelete"),
    )
    roles: list[str] = Field(default_factory=list)
    permissions: list[str] = Field(default_factory=list)

    @field_validator(
        "birthday",
        "last_login_time",
        "create_time",
        "update_time",
        "delete_time",
        mode="before",
    )
    @classmethod
    def _parse_datetime_fields(cls, value: Any) -> datetime | None:
        return _parse_datetime_value(value)


class AuthorizationContext(BaseModel):
    """
    `/agent/authorization` 返回的用户上下文结构。

    约束：
    - user 必填且不可为空
    - roles / permissions 可为空（null）
    """

    model_config = ConfigDict(extra="ignore")

    user: AuthUser
    roles: list[str] | None = None
    permissions: list[str] | None = None

    def to_auth_user(self) -> AuthUser:
        return self.user.model_copy(
            update={
                "roles": self.roles or [],
                "permissions": self.permissions or [],
            }
        )
