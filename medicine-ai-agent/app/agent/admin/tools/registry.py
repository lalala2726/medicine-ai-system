"""
管理端工具注册中心。
"""

from __future__ import annotations

from app.agent.admin.tools.after_sale import after_sale_context, after_sale_detail, after_sale_list
from app.agent.admin.tools.analytics import (
    analytics_after_sale_efficiency_summary,
    analytics_after_sale_reason_distribution,
    analytics_after_sale_status_distribution,
    analytics_after_sale_trend,
    analytics_conversion_summary,
    analytics_fulfillment_summary,
    analytics_range_summary,
    analytics_realtime_overview,
    analytics_return_refund_risk_products,
    analytics_sales_trend,
    analytics_top_selling_products,
)
from app.agent.admin.tools.base import get_safe_user_info
from app.agent.admin.tools.order import (
    order_context,
    order_detail,
    order_list,
)
from app.agent.tools import get_current_time
from app.agent.tools.rag_query import search_knowledge_context
from app.agent.admin.tools.product import drug_detail, product_detail, product_list
from app.agent.admin.tools.user import (
    user_consume_info,
    user_context,
    user_detail,
    user_list,
    user_wallet,
    user_wallet_flow,
)
from app.core.agent.middleware import (
    DynamicToolingTextConfig,
    ManagedDynamicToolRegistry,
)

# admin 动态工具协议文案配置。
_ADMIN_DYNAMIC_TOOLING_TEXT_CONFIG = DynamicToolingTextConfig(
    list_description=(
        "查看当前可加载的业务工具精确名称目录。"
        "当你不确定工具名、需要确认多个工具该怎么写，或准备一次加载多个工具时，先调用本工具。"
    ),
    list_tool_name="查看可加载工具目录",
    list_start_message="正在查看可加载工具目录",
    list_error_message="查看可加载工具目录失败",
    list_timely_message="可加载工具目录正在持续整理中",
    list_usage_tip=(
        "调用 load_tools 时，tool_keys 必须使用 exact_tool_names 中的精确值；"
        "支持一次传入多个工具名同时加载。"
    ),
    load_description=(
        "加载当前任务所需的业务工具。"
        "当你需要调用当前不可见的订单、商品、售后、用户或分析工具时，必须先调用本工具。"
        "这是工具加载步骤，不需要等待用户确认。"
        "参数只传 tool_keys 数组。"
        "tool_keys 支持一次传入多个精确工具名同时加载。"
    ),
    load_tool_name="加载业务工具",
    load_start_message="正在加载业务工具",
    load_error_message="加载业务工具失败",
    load_timely_message="业务工具正在持续加载中",
    load_success_prefix="已加载以下业务工具，可继续直接调用：",
)


class AdminToolRegistry(ManagedDynamicToolRegistry):
    """
    功能描述：
        统一维护 admin 单 Agent 的基础工具、业务工具和工具索引。

    参数说明：
        无。

    返回值：
        无（注册中心对象）。

    异常说明：
        ValueError: 当工具 key 重复或缺失时抛出。
    """

    def __init__(self) -> None:
        """
        功能描述：
            初始化工具注册中心并构建工具索引。

        参数说明：
            无。

        返回值：
            None。

        异常说明：
            ValueError: 当工具 key 重复或非法时抛出。
        """

        business_tools_by_domain = {
            "order": (
                order_list,
                order_context,
                order_detail,
            ),
            "product": (
                product_list,
                product_detail,
                drug_detail,
            ),
            "after_sale": (
                after_sale_list,
                after_sale_context,
                after_sale_detail,
            ),
            "user": (
                user_list,
                user_context,
                user_detail,
                user_wallet,
                user_wallet_flow,
                user_consume_info,
            ),
            "analytics": (
                analytics_realtime_overview,
                analytics_range_summary,
                analytics_conversion_summary,
                analytics_fulfillment_summary,
                analytics_after_sale_efficiency_summary,
                analytics_after_sale_status_distribution,
                analytics_after_sale_reason_distribution,
                analytics_top_selling_products,
                analytics_return_refund_risk_products,
                analytics_sales_trend,
                analytics_after_sale_trend,
            ),
        }
        super().__init__(
            business_tools_by_domain=business_tools_by_domain,
            extra_base_tools=(
                search_knowledge_context,
                get_safe_user_info,
                get_current_time,
            ),
            text_config=_ADMIN_DYNAMIC_TOOLING_TEXT_CONFIG,
        )


# 默认管理端工具注册中心。
ADMIN_TOOL_REGISTRY = AdminToolRegistry()

__all__ = [
    "ADMIN_TOOL_REGISTRY",
    "AdminToolRegistry",
]
