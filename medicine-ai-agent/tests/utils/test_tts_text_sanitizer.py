from __future__ import annotations

from app.core.speech.tts.text_sanitizer import TtsTextSanitizer

RAW_TEXT_BLOCK = """
### 最新订单购买者信息

最新一个订单的买家是用户ID为1的用户。以下是该用户的个人信息：

- **用户ID**：1
- **用户名**：未提供（系统未返回）
- **昵称**：啦啦啦
- **真实姓名**：张一闯
- **手机号**：13609199105
- **邮箱**：chuang@zhangchuangla.cn
- **性别**：未知
- **身份证号**：610122100101021201
- **头像**：![](http://192.168.10.110:9000/medicine/2025/11/db0a93be-4cb1-4dc6-baad-9543becf3c74.jpg)
- **注册时间**：2025-09-07
- **最后登录时间**：2026-01-30
- **最后登录IP**：0:0:0:0:0:0:0:1（IPv6不支持查询位置）
- **用户状态**：正常
- **钱包余额**：9,546.12 元
- **总订单数**：180 笔
- **总消费金额**：31,663.68 元

以上信息来源于系统后台数据，无额外编造。
"""


def test_sanitize_text_preview_print_to_console():
    """打印原文与清洗结果，便于人工确认清洗效果。"""

    sanitized = TtsTextSanitizer.sanitize_text(RAW_TEXT_BLOCK)

    print("\n========== 原始文本 ==========")
    print(RAW_TEXT_BLOCK)
    print("\n========== 清洗后文本 ==========")
    print(sanitized)
    print("================================\n")

    assert sanitized
    assert "https://" not in sanitized.lower()
    assert "```" not in sanitized
    assert "`" not in sanitized
    assert "#" not in sanitized
    assert "*" not in sanitized
    assert '"name":"alice"' not in sanitized


def test_sanitize_text_only_contains_whitelist_chars():
    """清洗结果应仅包含白名单字符。"""

    sanitized = TtsTextSanitizer.sanitize_text(RAW_TEXT_BLOCK)

    for ch in sanitized:
        assert TtsTextSanitizer.is_whitelist_char(ch), f"发现非白名单字符: {repr(ch)}"


def test_sanitize_text_returns_empty_for_structured_only_input():
    """仅包含结构化噪声时，清洗结果应为空。"""

    structured_only = """
```json
{"a": 1, "b": [1, 2, 3]}
```
{"k":"v","arr":[1,2]}
[1,2,3,4]
https://example.com/path?a=1
www.example.com/demo
[文档](https://example.com/doc)
![图](https://example.com/a.png)
<div>{"inner":"json"}</div>
`code snippet`
"""

    sanitized = TtsTextSanitizer.sanitize_text(structured_only)

    assert sanitized == ""
