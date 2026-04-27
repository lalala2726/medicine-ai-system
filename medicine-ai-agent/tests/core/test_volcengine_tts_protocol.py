import struct

from app.core.speech.volcengine_speech_protocol import EventType, Message, MsgType


def _build_server_event_frame(event: int, payload: bytes = b"{}") -> bytes:
    """
    构造最小可解析的 FullServerResponse + WithEvent 二进制帧。

    帧结构：
    - 3字节基础头 + 1字节padding（HeaderSize4）
    - event(int32)
    - session_id_len(uint32) + session_id
    - payload_len(uint32) + payload
    """

    header = bytes(
        [
            0x11,  # version=1, header_size=1
            0x94,  # msg_type=0x9(FullServerResponse), flag=0x4(WithEvent)
            0x10,  # serialization=json, compression=none
            0x00,  # padding
        ]
    )
    event_bytes = struct.pack(">i", event)
    session_id_bytes = b""
    session_id_len = struct.pack(">I", len(session_id_bytes))
    payload_len = struct.pack(">I", len(payload))
    return header + event_bytes + session_id_len + session_id_bytes + payload_len + payload


def test_message_from_bytes_accepts_known_tts_event_350():
    frame = _build_server_event_frame(350, payload=b'{"text":"hi"}')
    message = Message.from_bytes(frame)

    assert message.type == MsgType.FullServerResponse
    assert message.event == EventType.TTSSentenceStart
    assert message.event_code == 350


def test_message_from_bytes_tolerates_unknown_event():
    frame = _build_server_event_frame(9999, payload=b"{}")
    message = Message.from_bytes(frame)

    assert message.type == MsgType.FullServerResponse
    assert message.event == EventType.None_
    assert message.event_code == 9999
