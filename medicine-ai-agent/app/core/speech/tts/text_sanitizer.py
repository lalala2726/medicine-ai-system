from __future__ import annotations

import json
import re
import string


class TtsTextSanitizer:
    """TTS 文本清洗工具，专用于把复杂回答转为可播报文本。"""

    # Markdown 代码块匹配（含多行）。
    _MARKDOWN_CODE_BLOCK_PATTERN = re.compile(r"```[\s\S]*?```")
    # Markdown 行内代码匹配。
    _INLINE_CODE_PATTERN = re.compile(r"`[^`\n]*`")
    # URL 匹配（http/https/www）。
    _URL_PATTERN = re.compile(r"(https?://[^\s)\]}>]+|www\.[^\s)\]}>]+)", re.IGNORECASE)
    # HTML 标签匹配。
    _HTML_TAG_PATTERN = re.compile(r"<[^>]+>")
    # Markdown 图片语法匹配。
    _MARKDOWN_IMAGE_PATTERN = re.compile(r"!\[[^\]]*]\([^)\n]*\)")
    # Markdown 链接语法匹配。
    _MARKDOWN_LINK_PATTERN = re.compile(r"\[[^\]]*]\([^)\n]*\)")
    # 连续空白归一化匹配。
    _WHITESPACE_PATTERN = re.compile(r"\s+")

    # 允许播报的 ASCII 标点（排除 markdown 控制字符）。
    _ASCII_PUNCTUATION = set(string.punctuation) - {"#", "*", "-", "_"}
    # 允许播报的常用中文标点。
    _CHINESE_PUNCTUATION = set("，。！？；：、")

    @classmethod
    def sanitize_text(cls, raw_text: str) -> str:
        """
        清洗原始文本，输出适合 TTS 合成的内容。

        处理顺序：
        1. 删除 Markdown 代码块；
        2. 删除行内代码；
        3. 删除 URL；
        4. 删除 HTML 标签；
        5. 删除 Markdown 图片与链接；
        6. 删除 JSON/结构化片段；
        7. 白名单过滤；
        8. 归一化空白。

        Args:
            raw_text: 原始待播报文本。

        Returns:
            str: 清洗后的可播报文本；为空时返回空字符串。
        """

        return " ".join(cls.sanitize_lines(raw_text)).strip()

    @classmethod
    def sanitize_lines(cls, raw_text: str) -> list[str]:
        """
        清洗原始文本，并尽量保留行边界。

        处理顺序与 `sanitize_text` 一致，但输出为逐行结果，便于上层实现
        “按前 N 行播报”的逻辑。

        Args:
            raw_text: 原始待播报文本。

        Returns:
            list[str]: 清洗后的非空文本行列表；为空时返回空列表。
        """

        if not raw_text:
            return []

        text = raw_text
        text = cls._MARKDOWN_CODE_BLOCK_PATTERN.sub(" ", text)
        text = cls._INLINE_CODE_PATTERN.sub(" ", text)
        text = cls._URL_PATTERN.sub(" ", text)
        text = cls._HTML_TAG_PATTERN.sub(" ", text)
        text = cls._MARKDOWN_IMAGE_PATTERN.sub(" ", text)
        text = cls._MARKDOWN_LINK_PATTERN.sub(" ", text)
        text = cls._remove_structured_segments(text)

        filtered = "".join(ch for ch in text if cls.is_whitelist_char(ch))
        sanitized_lines: list[str] = []
        for line in filtered.splitlines():
            normalized_line = cls._WHITESPACE_PATTERN.sub(" ", line).strip()
            if normalized_line:
                sanitized_lines.append(normalized_line)
        return sanitized_lines

    @classmethod
    def is_whitelist_char(cls, ch: str) -> bool:
        """
        判断字符是否在白名单中。

        Args:
            ch: 待判断的单字符。

        Returns:
            bool: `True` 表示字符可保留。
        """

        if not ch:
            return False
        if ch.isspace():
            return True
        if "A" <= ch <= "Z" or "a" <= ch <= "z" or ch.isdigit():
            return True
        if ch in cls._ASCII_PUNCTUATION or ch in cls._CHINESE_PUNCTUATION:
            return True
        code = ord(ch)
        return 0x4E00 <= code <= 0x9FFF

    @classmethod
    def _remove_structured_segments(cls, text: str) -> str:
        """
        删除可能的 JSON/结构化片段。

        实现方式：
        - 通过 `{}`、`[]` 扫描候选片段；
        - 对候选片段进行结构化判定（JSON 解析 + 启发式规则）；
        - 将命中的区间整段移除。

        Args:
            text: 待处理文本。

        Returns:
            str: 去除结构化片段后的文本。
        """

        intervals: list[tuple[int, int]] = []
        stack: list[tuple[str, int]] = []
        matching = {"}": "{", "]": "["}

        for index, ch in enumerate(text):
            if ch in ("{", "["):
                stack.append((ch, index))
                continue
            if ch not in ("}", "]"):
                continue
            if not stack:
                continue

            opener, start = stack[-1]
            if opener != matching[ch]:
                continue
            stack.pop()

            segment = text[start:index + 1]
            if cls._looks_like_structured_segment(segment):
                intervals.append((start, index + 1))

        if not intervals:
            return text

        merged: list[list[int]] = []
        for start, end in sorted(intervals, key=lambda item: item[0]):
            if not merged or start > merged[-1][1]:
                merged.append([start, end])
            else:
                merged[-1][1] = max(merged[-1][1], end)

        parts: list[str] = []
        cursor = 0
        for start, end in merged:
            if cursor < start:
                parts.append(text[cursor:start])
            cursor = end
        if cursor < len(text):
            parts.append(text[cursor:])
        return "".join(parts)

    @staticmethod
    def _looks_like_structured_segment(segment: str) -> bool:
        """
        判定片段是否疑似 JSON 或结构化数据。

        Args:
            segment: 文本片段。

        Returns:
            bool: `True` 表示应按结构化片段移除。
        """

        s = segment.strip()
        if len(s) < 2:
            return False
        if not ((s[0] == "{" and s[-1] == "}") or (s[0] == "[" and s[-1] == "]")):
            return False

        try:
            loaded = json.loads(s)
            if isinstance(loaded, (dict, list)):
                return True
        except Exception:
            pass

        compact = re.sub(r"\s+", "", s)
        if ":" in compact or "：" in compact:
            return True
        if s.startswith("[") and "," in compact and re.search(r"\d", compact):
            return True
        if s.startswith("[") and ("{" in compact or "}" in compact):
            return True
        return False
