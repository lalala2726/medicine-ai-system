import os
from typing import Any

from langchain_core.messages import AIMessageChunk
from langchain_core.outputs import ChatGenerationChunk
from langchain_openai.chat_models.base import BaseChatOpenAI


class ChatQwen(BaseChatOpenAI):
    """阿里云千问聊天模型集成"""

    def _convert_chunk_to_generation_chunk(
            self,
            chunk: dict,
            default_chunk_class: type,
            base_generation_info: dict | None,
    ) -> ChatGenerationChunk | None:
        """处理千问特有的reasoning_content字段"""
        generation_chunk = super()._convert_chunk_to_generation_chunk(
            chunk, default_chunk_class, base_generation_info
        )

        if generation_chunk and (choices := chunk.get("choices")):
            top = choices[0]
            if isinstance(generation_chunk.message, AIMessageChunk):
                # 处理思考内容
                if reasoning_content := top.get("delta", {}).get("reasoning_content"):
                    generation_chunk.message.additional_kwargs["reasoning_content"] = (
                        reasoning_content
                    )

        return generation_chunk

    def _create_chat_result(self, response: dict | Any, generation_info: dict | None = None):
        """处理千问特有的响应字段"""
        result = super()._create_chat_result(response, generation_info)

        if hasattr(response, 'choices') and response.choices:
            if hasattr(response.choices[0].message, 'reasoning_content'):
                result.generations[0].message.additional_kwargs["reasoning_content"] = (
                    response.choices[0].message.reasoning_content
                )

        return result


# 启用思维链
model = ChatQwen(
    model="qwen3.5-plus",
    api_key=os.getenv("DASHSCOPE_API_KEY"),
    base_url="https://dashscope.aliyuncs.com/compatible-mode/v1",
    extra_body={"enable_thinking": True}  # 千问特有参数
)

# 流式响应
for chunk in model.stream(input="你叫什么名字"):
    print(chunk)
