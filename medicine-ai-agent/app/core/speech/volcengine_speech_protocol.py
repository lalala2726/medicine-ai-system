from __future__ import annotations

import gzip
import io
import json
import struct
from dataclasses import dataclass
from enum import IntEnum
from typing import Any


class MsgType(IntEnum):
    """火山双向语音协议中的消息类型。"""

    Invalid = 0
    FullClientRequest = 0b0001
    AudioOnlyClient = 0b0010
    FullServerResponse = 0b1001
    AudioOnlyServer = 0b1011
    FrontEndResultServer = 0b1100
    Error = 0b1111


class MsgTypeFlagBits(IntEnum):
    """协议头中的消息标记位。"""

    NoSeq = 0
    PositiveSeq = 0b0001
    LastNoSeq = 0b0010
    NegativeSeq = 0b0011
    WithEvent = 0b0100


class VersionBits(IntEnum):
    """协议版本位定义。"""

    Version1 = 1


class HeaderSizeBits(IntEnum):
    """协议头长度（单位：4字节）。"""

    HeaderSize4 = 1


class SerializationBits(IntEnum):
    """载荷序列化方式。"""

    Raw = 0
    JSON = 0b0001


class CompressionBits(IntEnum):
    """载荷压缩方式。"""

    None_ = 0
    Gzip = 0b0001


class EventType(IntEnum):
    """火山双向语音协议中的事件类型。"""

    None_ = 0

    StartConnection = 1
    FinishConnection = 2

    ConnectionStarted = 50
    ConnectionFailed = 51
    ConnectionFinished = 52

    StartSession = 100
    CancelSession = 101
    FinishSession = 102

    SessionStarted = 150
    SessionCanceled = 151
    SessionFinished = 152
    SessionFailed = 153

    TaskRequest = 200

    # Downstream TTS events
    TTSSentenceStart = 350
    TTSSentenceEnd = 351
    TTSResponse = 352
    TTSEnded = 359

    # Downstream ASR events
    ASRInfo = 450
    ASRResponse = 451
    ASREnded = 459


@dataclass
class Message:
    """
    火山双向语音 WebSocket 二进制帧模型。

    实现当前 STT/TTS 流式场景所需字段与读写逻辑。
    """

    version: VersionBits = VersionBits.Version1
    header_size: HeaderSizeBits = HeaderSizeBits.HeaderSize4
    type: MsgType = MsgType.Invalid
    flag: MsgTypeFlagBits = MsgTypeFlagBits.NoSeq
    serialization: SerializationBits = SerializationBits.JSON
    compression: CompressionBits = CompressionBits.None_

    event: EventType = EventType.None_
    event_code: int = 0
    session_id: str = ""
    connect_id: str = ""
    sequence: int = 0
    error_code: int = 0
    payload: bytes = b""

    @classmethod
    def from_bytes(cls, data: bytes) -> "Message":
        """
        把二进制帧反序列化为 `Message` 对象。

        Args:
            data: 原始协议二进制帧。

        Returns:
            Message: 解析后的消息对象。
        """

        if len(data) < 3:
            raise ValueError(f"Invalid frame length: {len(data)}")
        msg_type = MsgType(data[1] >> 4)
        flag = MsgTypeFlagBits(data[1] & 0b00001111)
        msg = cls(type=msg_type, flag=flag)
        msg.unmarshal(data)
        return msg

    def marshal(self) -> bytes:
        """
        把 `Message` 对象序列化为二进制帧。

        Returns:
            bytes: 可直接发送的协议帧。
        """

        buffer = io.BytesIO()
        header = [
            (self.version << 4) | self.header_size,
            (self.type << 4) | self.flag,
            (self.serialization << 4) | self.compression,
        ]
        header_size = 4 * self.header_size
        if header_size > len(header):
            header.extend([0] * (header_size - len(header)))
        buffer.write(bytes(header))

        if self.flag == MsgTypeFlagBits.WithEvent:
            self._write_event(buffer)
            self._write_session_id(buffer)

        if self.type in (
                MsgType.FullClientRequest,
                MsgType.FullServerResponse,
                MsgType.FrontEndResultServer,
                MsgType.AudioOnlyClient,
                MsgType.AudioOnlyServer,
        ):
            if self.flag in (MsgTypeFlagBits.PositiveSeq, MsgTypeFlagBits.NegativeSeq):
                self._write_sequence(buffer)
        elif self.type == MsgType.Error:
            self._write_error_code(buffer)
        else:
            raise ValueError(f"Unsupported message type: {self.type}")

        self._write_payload(buffer)
        return buffer.getvalue()

    def unmarshal(self, data: bytes) -> None:
        """
        从二进制帧中解析并回填当前对象字段。

        Args:
            data: 原始协议二进制帧。

        Returns:
            None
        """

        buffer = io.BytesIO(data)
        version_and_header_size = buffer.read(1)[0]
        self.version = VersionBits(version_and_header_size >> 4)
        self.header_size = HeaderSizeBits(version_and_header_size & 0b00001111)

        # Skip type+flag, they are parsed in from_bytes.
        buffer.read(1)

        serialization_and_compression = buffer.read(1)[0]
        self.serialization = SerializationBits(serialization_and_compression >> 4)
        self.compression = CompressionBits(serialization_and_compression & 0b00001111)

        header_size = 4 * self.header_size
        if header_size > 3:
            buffer.read(header_size - 3)

        if self.type in (
                MsgType.FullClientRequest,
                MsgType.FullServerResponse,
                MsgType.FrontEndResultServer,
                MsgType.AudioOnlyClient,
                MsgType.AudioOnlyServer,
        ):
            if self.flag in (MsgTypeFlagBits.PositiveSeq, MsgTypeFlagBits.NegativeSeq):
                self._read_sequence(buffer)
        elif self.type == MsgType.Error:
            self._read_error_code(buffer)
        else:
            raise ValueError(f"Unsupported message type: {self.type}")

        if self.flag == MsgTypeFlagBits.WithEvent:
            self._read_event(buffer)
            self._read_session_id(buffer)
            self._read_connect_id(buffer)

        self._read_payload(buffer)

    def _write_event(self, buffer: io.BytesIO) -> None:
        """写入事件类型字段。"""

        buffer.write(struct.pack(">i", self.event))

    def _write_session_id(self, buffer: io.BytesIO) -> None:
        """按协议规则写入会话 ID。"""

        if self.event in (
                EventType.StartConnection,
                EventType.FinishConnection,
                EventType.ConnectionStarted,
                EventType.ConnectionFailed,
        ):
            return
        payload = self.session_id.encode("utf-8")
        buffer.write(struct.pack(">I", len(payload)))
        if payload:
            buffer.write(payload)

    def _write_sequence(self, buffer: io.BytesIO) -> None:
        """写入消息序号字段。"""

        buffer.write(struct.pack(">i", self.sequence))

    def _write_error_code(self, buffer: io.BytesIO) -> None:
        """写入错误码字段。"""

        buffer.write(struct.pack(">I", self.error_code))

    def _write_payload(self, buffer: io.BytesIO) -> None:
        """写入 payload 长度与载荷本体。"""

        buffer.write(struct.pack(">I", len(self.payload)))
        if self.payload:
            buffer.write(self.payload)

    def _read_event(self, buffer: io.BytesIO) -> None:
        """读取事件类型字段。"""

        data = buffer.read(4)
        if data:
            raw_event = struct.unpack(">i", data)[0]
            self.event_code = raw_event
            try:
                self.event = EventType(raw_event)
            except ValueError:
                # 兼容服务端新增事件，避免解析阶段直接崩溃。
                self.event = EventType.None_

    def _read_session_id(self, buffer: io.BytesIO) -> None:
        """读取会话 ID 字段。"""

        if self.event in (
                EventType.StartConnection,
                EventType.FinishConnection,
                EventType.ConnectionStarted,
                EventType.ConnectionFailed,
                EventType.ConnectionFinished,
        ):
            return
        data = buffer.read(4)
        if not data:
            return
        size = struct.unpack(">I", data)[0]
        if size > 0:
            session_id_bytes = buffer.read(size)
            if len(session_id_bytes) == size:
                self.session_id = session_id_bytes.decode("utf-8")

    def _read_connect_id(self, buffer: io.BytesIO) -> None:
        """读取连接 ID 字段。"""

        if self.event not in (
                EventType.ConnectionStarted,
                EventType.ConnectionFailed,
                EventType.ConnectionFinished,
        ):
            return
        data = buffer.read(4)
        if not data:
            return
        size = struct.unpack(">I", data)[0]
        if size > 0:
            self.connect_id = buffer.read(size).decode("utf-8")

    def _read_sequence(self, buffer: io.BytesIO) -> None:
        """读取消息序号字段。"""

        data = buffer.read(4)
        if data:
            self.sequence = struct.unpack(">i", data)[0]

    def _read_error_code(self, buffer: io.BytesIO) -> None:
        """读取错误码字段。"""

        data = buffer.read(4)
        if data:
            self.error_code = struct.unpack(">I", data)[0]

    def _read_payload(self, buffer: io.BytesIO) -> None:
        """读取 payload 长度与载荷本体。"""

        data = buffer.read(4)
        if not data:
            return
        size = struct.unpack(">I", data)[0]
        if size > 0:
            self.payload = buffer.read(size)


@dataclass(frozen=True)
class SttServerMessage:
    """实时 STT 链路中服务端返回帧的解析结果。"""

    message_type: MsgType
    flag: int
    sequence: int | None
    is_last_package: bool
    serialization: SerializationBits
    compression: CompressionBits
    payload: bytes
    payload_json: Any | None
    error_code: int | None = None


def compress_payload(payload: bytes, compression: CompressionBits) -> bytes:
    """
    按协议压缩 payload。

    Args:
        payload: 原始载荷。
        compression: 压缩方式。

    Returns:
        bytes: 压缩后的载荷字节。
    """

    if compression == CompressionBits.None_:
        return payload
    if compression == CompressionBits.Gzip:
        return gzip.compress(payload)
    raise ValueError(f"Unsupported compression type: {compression}")


def decompress_payload(payload: bytes, compression: CompressionBits) -> bytes:
    """
    按协议解压 payload。

    Args:
        payload: 压缩载荷。
        compression: 压缩方式。

    Returns:
        bytes: 解压后的载荷字节。
    """

    if compression == CompressionBits.None_:
        return payload
    if compression == CompressionBits.Gzip:
        return gzip.decompress(payload)
    raise ValueError(f"Unsupported compression type: {compression}")


def deserialize_payload(payload: bytes, serialization: SerializationBits) -> Any | None:
    """
    按协议序列化方式解析 payload。

    Args:
        payload: 解压后的载荷字节。
        serialization: 序列化方式。

    Returns:
        Any | None: JSON 模式返回 Python 对象；Raw 或空载荷返回 `None`。
    """

    if not payload:
        return None
    if serialization == SerializationBits.Raw:
        return None
    if serialization == SerializationBits.JSON:
        return json.loads(payload.decode("utf-8"))
    raise ValueError(f"Unsupported serialization type: {serialization}")


def _read_uint32(frame: bytes, offset: int) -> tuple[int, int]:
    """
    从帧中按大端读取无符号 32 位整数。

    Args:
        frame: 原始协议帧。
        offset: 当前读取偏移量。

    Returns:
        tuple[int, int]: `(读取值, 新偏移量)`。
    """

    if len(frame) < offset + 4:
        raise ValueError("invalid frame: insufficient bytes for uint32")
    return struct.unpack(">I", frame[offset: offset + 4])[0], offset + 4


def _read_int32(frame: bytes, offset: int) -> tuple[int, int]:
    """
    从帧中按大端读取有符号 32 位整数。

    Args:
        frame: 原始协议帧。
        offset: 当前读取偏移量。

    Returns:
        tuple[int, int]: `(读取值, 新偏移量)`。
    """

    if len(frame) < offset + 4:
        raise ValueError("invalid frame: insufficient bytes for int32")
    return struct.unpack(">i", frame[offset: offset + 4])[0], offset + 4


def _read_payload_bytes(frame: bytes, offset: int, size: int) -> tuple[bytes, int]:
    """
    按指定长度从帧中读取 payload。

    Args:
        frame: 原始协议帧。
        offset: 当前读取偏移量。
        size: 目标读取长度。

    Returns:
        tuple[bytes, int]: `(payload 字节, 新偏移量)`。
    """

    if size < 0:
        raise ValueError("invalid frame: payload size must be >= 0")
    end = offset + size
    if len(frame) < end:
        raise ValueError("invalid frame: payload truncated")
    return frame[offset:end], end


def parse_stt_server_message(frame: bytes) -> SttServerMessage:
    """
    解析实时 STT（二进制协议）返回帧。

    该函数用于 sauc(bigmodel/bigmodel_async/bigmodel_nostream) 链路，
    解析规则遵循官方示例：
    - full server response: [sequence?][payload_size][payload]
    - error response: [error_code][payload_size][payload]
    """

    if len(frame) < 4:
        raise ValueError("invalid frame: length < 4")

    message_type = MsgType(frame[1] >> 4)
    flag = frame[1] & 0b00001111
    serialization = SerializationBits(frame[2] >> 4)
    compression = CompressionBits(frame[2] & 0b00001111)

    header_words = frame[0] & 0b00001111
    header_size = 4 * header_words
    if header_size < 4 or len(frame) < header_size:
        raise ValueError("invalid frame: header size")

    cursor = header_size
    sequence: int | None = None
    if flag & 0b0001:
        sequence, cursor = _read_int32(frame, cursor)

    is_last_package = bool(flag & 0b0010)

    if message_type == MsgType.Error:
        error_code, cursor = _read_uint32(frame, cursor)
        payload_size, cursor = _read_uint32(frame, cursor)
        payload, cursor = _read_payload_bytes(frame, cursor, payload_size)
        payload = decompress_payload(payload, compression)
        payload_json = deserialize_payload(payload, serialization)
        return SttServerMessage(
            message_type=message_type,
            flag=flag,
            sequence=sequence,
            is_last_package=is_last_package,
            serialization=serialization,
            compression=compression,
            payload=payload,
            payload_json=payload_json,
            error_code=error_code,
        )

    payload_size, cursor = _read_uint32(frame, cursor)
    payload, cursor = _read_payload_bytes(frame, cursor, payload_size)
    payload = decompress_payload(payload, compression)
    payload_json = deserialize_payload(payload, serialization)
    return SttServerMessage(
        message_type=message_type,
        flag=flag,
        sequence=sequence,
        is_last_package=is_last_package,
        serialization=serialization,
        compression=compression,
        payload=payload,
        payload_json=payload_json,
    )


async def receive_message(websocket: Any) -> Message:
    """
    从 websocket 接收一帧并解析为协议消息对象。

    Args:
        websocket: 已连接的 websocket 客户端对象。

    Returns:
        Message: 解析后的协议消息。
    """

    data = await websocket.recv()
    if isinstance(data, str):
        raise ValueError(f"Unexpected text websocket frame: {data}")
    if not isinstance(data, bytes):
        raise ValueError(f"Unexpected websocket frame type: {type(data)}")
    return Message.from_bytes(data)


async def wait_for_event(
        websocket: Any,
        *,
        msg_type: MsgType,
        event_type: EventType,
) -> Message:
    """
    阻塞等待指定类型与事件的协议消息。

    Args:
        websocket: 已连接的 websocket 客户端对象。
        msg_type: 目标消息类型。
        event_type: 目标事件类型。

    Returns:
        Message: 命中的协议消息。
    """

    while True:
        msg = await receive_message(websocket)
        if msg.type == MsgType.Error:
            payload = msg.payload.decode("utf-8", errors="ignore")
            raise RuntimeError(f"Volcengine protocol error code={msg.error_code} payload={payload}")
        if msg.type == msg_type and msg.event == event_type:
            return msg


async def start_connection(websocket: Any) -> None:
    """
    发送 `StartConnection` 事件。

    Args:
        websocket: 已连接 websocket。

    Returns:
        None
    """

    msg = Message(type=MsgType.FullClientRequest, flag=MsgTypeFlagBits.WithEvent)
    msg.event = EventType.StartConnection
    msg.payload = b"{}"
    await websocket.send(msg.marshal())


async def finish_connection(websocket: Any) -> None:
    """
    发送 `FinishConnection` 事件。

    Args:
        websocket: 已连接 websocket。

    Returns:
        None
    """

    msg = Message(type=MsgType.FullClientRequest, flag=MsgTypeFlagBits.WithEvent)
    msg.event = EventType.FinishConnection
    msg.payload = b"{}"
    await websocket.send(msg.marshal())


async def start_session(websocket: Any, payload: bytes, session_id: str) -> None:
    """
    发送 `StartSession` 事件。

    Args:
        websocket: 已连接 websocket。
        payload: 事件载荷。
        session_id: 会话 ID。

    Returns:
        None
    """

    msg = Message(type=MsgType.FullClientRequest, flag=MsgTypeFlagBits.WithEvent)
    msg.event = EventType.StartSession
    msg.session_id = session_id
    msg.payload = payload
    await websocket.send(msg.marshal())


async def finish_session(websocket: Any, session_id: str) -> None:
    """
    发送 `FinishSession` 事件。

    Args:
        websocket: 已连接 websocket。
        session_id: 会话 ID。

    Returns:
        None
    """

    msg = Message(type=MsgType.FullClientRequest, flag=MsgTypeFlagBits.WithEvent)
    msg.event = EventType.FinishSession
    msg.session_id = session_id
    msg.payload = b"{}"
    await websocket.send(msg.marshal())


async def task_request(websocket: Any, payload: bytes, session_id: str) -> None:
    """
    发送 `TaskRequest` 事件。

    Args:
        websocket: 已连接 websocket。
        payload: 任务请求载荷。
        session_id: 会话 ID。

    Returns:
        None
    """

    msg = Message(type=MsgType.FullClientRequest, flag=MsgTypeFlagBits.WithEvent)
    msg.event = EventType.TaskRequest
    msg.session_id = session_id
    msg.payload = payload
    await websocket.send(msg.marshal())
