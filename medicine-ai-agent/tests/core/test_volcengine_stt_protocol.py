import gzip
import json
import struct

from app.core.speech.volcengine_speech_protocol import (
    CompressionBits,
    MsgType,
    SerializationBits,
    compress_payload,
    decompress_payload,
    parse_stt_server_message,
)


def _build_stt_server_frame(
        *,
        message_type: int,
        flag: int,
        serialization: int,
        compression: int,
        sequence: int | None,
        payload: bytes,
        error_code: int | None = None,
) -> bytes:
    header = bytes(
        [
            0x11,  # version=1, header_size=1
            ((message_type & 0x0F) << 4) | (flag & 0x0F),
            ((serialization & 0x0F) << 4) | (compression & 0x0F),
            0x00,
        ]
    )
    frame = bytearray(header)
    if sequence is not None:
        frame.extend(struct.pack(">i", sequence))
    if message_type == MsgType.Error:
        assert error_code is not None
        frame.extend(struct.pack(">I", error_code))
    frame.extend(struct.pack(">I", len(payload)))
    frame.extend(payload)
    return bytes(frame)


def test_parse_stt_full_server_response_with_gzip_json_payload() -> None:
    raw_payload = json.dumps(
        {"result": {"text": "你好世界", "utterances": []}},
        ensure_ascii=False,
    ).encode("utf-8")
    compressed = gzip.compress(raw_payload)
    frame = _build_stt_server_frame(
        message_type=MsgType.FullServerResponse,
        flag=0b0001,  # has sequence
        serialization=SerializationBits.JSON,
        compression=CompressionBits.Gzip,
        sequence=2,
        payload=compressed,
    )

    parsed = parse_stt_server_message(frame)

    assert parsed.message_type == MsgType.FullServerResponse
    assert parsed.sequence == 2
    assert parsed.is_last_package is False
    assert parsed.payload_json == {"result": {"text": "你好世界", "utterances": []}}


def test_parse_stt_full_server_response_marks_last_package_when_flag_contains_last_bit() -> None:
    payload = gzip.compress(b'{"result":{"text":"final"}}')
    frame = _build_stt_server_frame(
        message_type=MsgType.FullServerResponse,
        flag=0b0011,  # has sequence + last package
        serialization=SerializationBits.JSON,
        compression=CompressionBits.Gzip,
        sequence=7,
        payload=payload,
    )

    parsed = parse_stt_server_message(frame)

    assert parsed.sequence == 7
    assert parsed.is_last_package is True
    assert parsed.payload_json == {"result": {"text": "final"}}


def test_parse_stt_error_frame() -> None:
    payload = b'{"message":"bad request"}'
    frame = _build_stt_server_frame(
        message_type=MsgType.Error,
        flag=0,
        serialization=SerializationBits.JSON,
        compression=CompressionBits.None_,
        sequence=None,
        payload=payload,
        error_code=45000001,
    )

    parsed = parse_stt_server_message(frame)

    assert parsed.message_type == MsgType.Error
    assert parsed.error_code == 45000001
    assert parsed.payload_json == {"message": "bad request"}


def test_compress_and_decompress_payload_with_gzip() -> None:
    raw = b"hello-stt-protocol"

    compressed = compress_payload(raw, CompressionBits.Gzip)
    assert compressed != raw

    decompressed = decompress_payload(compressed, CompressionBits.Gzip)
    assert decompressed == raw
