from __future__ import annotations


class TextListUtils:
    """字符串列表处理工具类。"""

    @staticmethod
    def normalize(values: list[str] | None) -> list[str]:
        """规范化字符串列表并保持原顺序去重。

        Args:
            values (list[str] | None): 原始字符串列表。

        Returns:
            list[str]: 去空白、去重后的字符串列表。
        """

        normalized_values: list[str] = []
        seen: set[str] = set()
        for raw_value in values or []:
            normalized_value = str(raw_value or "").strip()
            if not normalized_value or normalized_value in seen:
                continue
            seen.add(normalized_value)
            normalized_values.append(normalized_value)
        return normalized_values

    @staticmethod
    def normalize_required(values: list[str] | None, *, field_name: str) -> list[str]:
        """规范化必填字符串列表。

        Args:
            values (list[str] | None): 原始字符串列表。
            field_name (str): 当前字段名称。

        Returns:
            list[str]: 去空白、去重后的字符串列表。

        Raises:
            ValueError: 归一化后为空时抛出。
        """

        normalized_values = TextListUtils.normalize(values)
        if not normalized_values:
            raise ValueError(f"{field_name} 不能为空")
        return normalized_values

    @staticmethod
    def normalize_unique_required(values: list[str] | None, *, field_name: str) -> list[str]:
        """规范化必填字符串列表，并拒绝重复项。

        Args:
            values (list[str] | None): 原始字符串列表。
            field_name (str): 当前字段名称。

        Returns:
            list[str]: 去空白后的字符串列表。

        Raises:
            ValueError: 归一化后为空或包含重复项时抛出。
        """

        normalized_values: list[str] = []
        seen: set[str] = set()
        for raw_value in values or []:
            normalized_value = str(raw_value or "").strip()
            if not normalized_value:
                continue
            if normalized_value in seen:
                raise ValueError(f"{field_name} 不能包含重复项")
            seen.add(normalized_value)
            normalized_values.append(normalized_value)

        if not normalized_values:
            raise ValueError(f"{field_name} 不能为空")
        return normalized_values


__all__ = [
    "TextListUtils",
]
