"""消费者 handler 注册入口。

导入此包即可触发所有 ``@broker.subscriber`` 向 broker 注册。
在 ``app/main.py`` 的 lifespan 中 ``import app.core.mq.handlers`` 完成注册。
"""

from app.core.mq.handlers import agent_config_refresh_handler as agent_config_refresh_handler  # noqa: F401
from app.core.mq.handlers import agent_prompt_refresh_handler as agent_prompt_refresh_handler  # noqa: F401
from app.core.mq.handlers import chunk_add_handler as chunk_add_handler  # noqa: F401
from app.core.mq.handlers import chunk_rebuild_handler as chunk_rebuild_handler  # noqa: F401
from app.core.mq.handlers import import_handler as import_handler  # noqa: F401
