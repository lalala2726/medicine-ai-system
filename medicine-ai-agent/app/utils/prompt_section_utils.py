from __future__ import annotations


def normalize_text(text: str) -> str:
    """规范化提示词文本。

    功能：
        将不同平台产生的换行符统一为 `\n`，并移除文本末尾空白，
        让后续匹配逻辑不受 CRLF/LF 差异影响。

    工作原理：
        1. `\r\n` -> `\n`
        2. `\r` -> `\n`
        3. `rstrip()` 去掉末尾空白字符

    说明：
        该函数不修改中间内容（例如段内空格、段间空行），只做最小归一化。
    """

    return text.replace("\r\n", "\n").replace("\r", "\n").rstrip()


def contains_block(text: str, block: str) -> bool:
    """按“块边界”判断文本中是否已包含目标块。

    功能：
        判断 `text` 是否已经完整包含 `block`，避免重复注入。
        与简单的 `block in text` 不同，此处要求命中位置是一个独立段落块。

    工作原理：
        1. 先对 `text` / `block` 做 `normalize_text`。
        2. 在文本中逐个查找 `block` 的出现位置。
        3. 对每个命中检查左右边界：
           - 左边界：命中在文本开头，或前面是一个空行（`\\n\\n`）。
           - 右边界：命中在文本结尾，或后面是一个空行（`\\n\\n`）。
        4. 只要有一次命中满足左右边界，即返回 `True`。

    为什么这样做：
        防止把“某段中的子句”误判成“已存在完整块”，降低误判率。
    """

    normalized_text = normalize_text(text)
    normalized_block = normalize_text(block)
    if not normalized_text or not normalized_block:
        return False

    search_from = 0
    while True:
        index = normalized_text.find(normalized_block, search_from)
        if index < 0:
            return False

        end = index + len(normalized_block)
        before_ok = index == 0 or normalized_text[:index].endswith("\n\n")
        after_ok = end == len(normalized_text) or normalized_text[end:].startswith("\n\n")
        if before_ok and after_ok:
            return True

        search_from = index + 1


def split_template(template: str, placeholder: str = "{skills_list}") -> tuple[str, str] | list[str]:
    """按占位符拆分模板前后缀。

    功能：
        将带占位符的模板拆成 `(prefix, suffix)`，用于后续定位
        `prefix ... suffix` 包裹的完整段落。

    工作原理：
        1. 对模板做 `normalize_text`。
        2. 若模板中存在占位符，按第一次出现位置拆分并返回前后缀。
        3. 若不存在占位符，返回 `("", "")` 作为降级信号。

    典型用途：
        技能模板可写为 `前缀 + {skills_list} + 后缀`，注入时通过前后缀定位
        已存在的技能段，实现替换而不是重复追加。
    """

    normalized_template = normalize_text(template)
    if placeholder not in normalized_template:
        return "", ""

    return normalized_template.split(placeholder, 1)


def find_section_span(text: str, prefix: str, suffix: str) -> tuple[int, int] | None:
    """定位 `prefix ... suffix` 在文本中的区间。

    功能：
        在 `text` 中找到由 `prefix` 开始、以 `suffix` 结束的连续区间，
        返回 `(start, end)` 下标（`end` 为切片右边界）。

    工作原理：
        1. 先规范化 `text/prefix/suffix`。
        2. 若前后缀都存在：
           - 从左到右查找 `prefix`。
           - 在该 `prefix` 之后查找最近的 `suffix`。
           - 找到后返回完整区间。
        3. 若仅 `prefix` 存在：返回 `prefix` 到文本末尾的区间。
        4. 若仅 `suffix` 存在：返回文本开头到 `suffix` 末尾的区间。
        5. 若都不存在或无法匹配，返回 `None`。

    说明：
        该函数用于“定位并替换已注入段落”，保证幂等更新。
    """

    normalized_text = normalize_text(text)
    normalized_prefix = normalize_text(prefix)
    normalized_suffix = normalize_text(suffix)
    if not normalized_text:
        return None
    if not normalized_prefix and not normalized_suffix:
        return None

    if normalized_prefix and normalized_suffix:
        search_from = 0
        while True:
            start = normalized_text.find(normalized_prefix, search_from)
            if start < 0:
                return None
            end = normalized_text.find(
                normalized_suffix,
                start + len(normalized_prefix),
            )
            if end >= 0:
                return start, end + len(normalized_suffix)
            search_from = start + 1

    if normalized_prefix:
        start = normalized_text.find(normalized_prefix)
        if start >= 0:
            return start, len(normalized_text)
        return None

    end = normalized_text.find(normalized_suffix)
    if end >= 0:
        return 0, end + len(normalized_suffix)
    return None
