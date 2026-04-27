package com.zhangyichuang.medicine.agent.mapping;

import com.zhangyichuang.medicine.model.enums.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Agent 编码中文映射注册表。
 */
public final class AgentCodeLabelRegistry {

    /**
     * 订单状态编码映射。
     */
    public static final String AGENT_ORDER_STATUS = "agent.order.status";

    /**
     * 支付方式编码映射。
     */
    public static final String AGENT_ORDER_PAY_TYPE = "agent.order.payType";

    /**
     * 商品配送方式编码映射（兼容 legacy 整型编码）。
     */
    public static final String AGENT_PRODUCT_DELIVERY_TYPE = "agent.product.deliveryType";

    /**
     * 商品状态映射。
     */
    public static final String AGENT_PRODUCT_STATUS = "agent.product.status";

    /**
     * 用户性别映射。
     */
    public static final String AGENT_USER_GENDER = "agent.user.gender";

    /**
     * 用户状态映射。
     */
    public static final String AGENT_USER_STATUS = "agent.user.status";

    /**
     * 用户钱包状态映射。
     */
    public static final String AGENT_USER_WALLET_STATUS = "agent.user.wallet.status";

    /**
     * 钱包流水变动类型映射。
     */
    public static final String AGENT_USER_WALLET_CHANGE_TYPE = "agent.user.wallet.changeType";

    /**
     * 售后类型映射。
     */
    public static final String AGENT_AFTER_SALE_TYPE = "agent.afterSale.type";

    /**
     * 售后状态映射。
     */
    public static final String AGENT_AFTER_SALE_STATUS = "agent.afterSale.status";

    /**
     * 售后申请原因映射。
     */
    public static final String AGENT_AFTER_SALE_REASON = "agent.afterSale.reason";

    /**
     * 收货状态映射。
     */
    public static final String AGENT_AFTER_SALE_RECEIVE_STATUS = "agent.afterSale.receiveStatus";

    /**
     * 订单事件类型映射。
     */
    public static final String AGENT_ORDER_EVENT_TYPE = "agent.order.eventType";

    /**
     * 操作方类型映射。
     */
    public static final String AGENT_OPERATOR_TYPE = "agent.operator.type";

    /**
     * 物流状态映射。
     */
    public static final String AGENT_SHIPPING_STATUS = "agent.shipping.status";

    private static final Map<String, Map<String, String>> DICT = buildDict();

    private AgentCodeLabelRegistry() {
    }

    /**
     * 根据字典 key 与编码查询中文标签。
     *
     * @param dictKey 字典 key
     * @param code    编码值
     * @return 中文标签，未命中返回 null
     */
    public static String getLabel(String dictKey, String code) {
        if (dictKey == null || dictKey.isBlank() || code == null) {
            return null;
        }
        Map<String, String> mapping = DICT.get(dictKey);
        if (mapping == null) {
            return null;
        }
        return mapping.get(code);
    }

    private static Map<String, Map<String, String>> buildDict() {
        Map<String, Map<String, String>> dict = new LinkedHashMap<>();
        dict.put(AGENT_ORDER_STATUS, buildOrderStatusDict());
        dict.put(AGENT_ORDER_PAY_TYPE, buildPayTypeDict());
        dict.put(AGENT_PRODUCT_DELIVERY_TYPE, buildLegacyDeliveryTypeDict());
        dict.put(AGENT_PRODUCT_STATUS, buildProductStatusDict());
        dict.put(AGENT_USER_GENDER, buildUserGenderDict());
        dict.put(AGENT_USER_STATUS, buildUserStatusDict());
        dict.put(AGENT_USER_WALLET_STATUS, buildWalletStatusDict());
        dict.put(AGENT_USER_WALLET_CHANGE_TYPE, buildWalletChangeTypeDict());
        dict.put(AGENT_AFTER_SALE_TYPE, buildAfterSaleTypeDict());
        dict.put(AGENT_AFTER_SALE_STATUS, buildAfterSaleStatusDict());
        dict.put(AGENT_AFTER_SALE_REASON, buildAfterSaleReasonDict());
        dict.put(AGENT_AFTER_SALE_RECEIVE_STATUS, buildReceiveStatusDict());
        dict.put(AGENT_ORDER_EVENT_TYPE, buildOrderEventTypeDict());
        dict.put(AGENT_OPERATOR_TYPE, buildOperatorTypeDict());
        dict.put(AGENT_SHIPPING_STATUS, buildShippingStatusDict());
        return Map.copyOf(dict);
    }

    /**
     * Builds an order status dictionary by converting all {@link OrderStatusEnum} values
     * into key-value pairs, where the key is the status type and the value is the
     * corresponding display name.
     *
     * @return an unmodifiable map of order status codes to their display names
     */
    private static Map<String, String> buildOrderStatusDict() {
        Map<String, String> mapping = new LinkedHashMap<>();
        for (OrderStatusEnum statusEnum : OrderStatusEnum.values()) {
            mapping.put(statusEnum.getType(), statusEnum.getName());
        }
        return Map.copyOf(mapping);
    }

    /**
     * 构建支付方式字典，将 {@link PayTypeEnum} 中定义的支付方式编码映射为对应的描述信息。
     *
     * @return 以支付方式编码为键、支付方式描述为值的不可变映射
     */
    private static Map<String, String> buildPayTypeDict() {
        Map<String, String> mapping = new LinkedHashMap<>();
        for (PayTypeEnum payTypeEnum : PayTypeEnum.values()) {
            mapping.put(payTypeEnum.getType(), payTypeEnum.getDescription());
        }
        return Map.copyOf(mapping);
    }

    /**
     * 构建旧版配送方式字典。
     * 使用 {@link DeliveryTypeEnum} 的枚举声明顺序作为旧版编码，将枚举 ordinal 转换为字符串作为键，
     * 对应的配送方式名称作为值，以兼容历史整型编码到展示名称的映射查询。
     *
     * @return 不可修改的配送方式字典，键为旧版配送方式编码字符串，值为配送方式中文名称
     */
    private static Map<String, String> buildLegacyDeliveryTypeDict() {
        Map<String, String> mapping = new LinkedHashMap<>();
        for (DeliveryTypeEnum deliveryTypeEnum : DeliveryTypeEnum.values()) {
            mapping.put(String.valueOf(deliveryTypeEnum.ordinal()), deliveryTypeEnum.getName());
        }
        return Map.copyOf(mapping);
    }

    /**
     * 构建商品状态字典，定义商品状态编码与中文标签的对应关系。
     *
     * @return 商品状态映射，包含编码 {@code "1"} 对应 {@code "上架"}、编码 {@code "0"} 对应 {@code "下架"}，并以不可变 Map 形式返回
     */
    private static Map<String, String> buildProductStatusDict() {
        Map<String, String> mapping = new LinkedHashMap<>();
        mapping.put("1", "上架");
        mapping.put("0", "下架");
        return Map.copyOf(mapping);
    }

    /**
     * 构建用户性别字典，将用户性别编码映射为对应的中文标签。
     *
     * @return 不可变的用户性别映射，包含编码 {@code "0"} 对应 {@code "未知"}、编码 {@code "1"} 对应 {@code "男"}、编码 {@code "2"} 对应 {@code "女"}
     */
    private static Map<String, String> buildUserGenderDict() {
        Map<String, String> mapping = new LinkedHashMap<>();
        mapping.put("0", "未知");
        mapping.put("1", "男");
        mapping.put("2", "女");
        return Map.copyOf(mapping);
    }

    /**
     * 构建用户状态字典，将用户状态编码映射为对应的中文标签。
     *
     * @return 不可变的用户状态映射，包含编码 {@code "0"} 对应 {@code "正常"}、编码 {@code "1"} 对应 {@code "禁用"}
     */
    private static Map<String, String> buildUserStatusDict() {
        Map<String, String> mapping = new LinkedHashMap<>();
        mapping.put("0", "正常");
        mapping.put("1", "禁用");
        return Map.copyOf(mapping);
    }

    /**
     * 构建钱包状态字典，用于将钱包状态编码映射为对应的中文标签。
     * 包含的状态有：0 表示正常，1 表示冻结。
     *
     * @return 钱包状态编码与中文标签的不可变映射
     */
    private static Map<String, String> buildWalletStatusDict() {
        Map<String, String> mapping = new LinkedHashMap<>();
        mapping.put("0", "正常");
        mapping.put("1", "冻结");
        return Map.copyOf(mapping);
    }

    /**
     * 构建钱包变动类型字典，将 {@link WalletChangeTypeEnum} 中定义的变动类型编码与对应名称进行映射。
     *
     * @return 以钱包变动类型编码字符串为键、变动类型名称为值的不可变映射
     */
    private static Map<String, String> buildWalletChangeTypeDict() {
        Map<String, String> mapping = new LinkedHashMap<>();
        for (WalletChangeTypeEnum changeTypeEnum : WalletChangeTypeEnum.values()) {
            mapping.put(String.valueOf(changeTypeEnum.getCode()), changeTypeEnum.getName());
        }
        return Map.copyOf(mapping);
    }

    /**
     * 构建售后类型字典。
     * <p>
     * 将 {@code AfterSaleTypeEnum} 中定义的售后类型编码与对应名称收集为映射关系，
     * 键为售后类型的 {@code type}，值为售后类型的中文名称，并以不可变映射返回。
     *
     * @return 售后类型编码到售后类型名称的不可变映射
     */
    private static Map<String, String> buildAfterSaleTypeDict() {
        Map<String, String> mapping = new LinkedHashMap<>();
        for (AfterSaleTypeEnum afterSaleTypeEnum : AfterSaleTypeEnum.values()) {
            mapping.put(afterSaleTypeEnum.getType(), afterSaleTypeEnum.getName());
        }
        return Map.copyOf(mapping);
    }

    /**
     * 构建售后状态字典，将 {@link AfterSaleStatusEnum} 中定义的状态编码映射为对应的中文名称。
     *
     * @return 以售后状态编码为键、状态中文名称为值的不可变映射
     */
    private static Map<String, String> buildAfterSaleStatusDict() {
        Map<String, String> mapping = new LinkedHashMap<>();
        for (AfterSaleStatusEnum afterSaleStatusEnum : AfterSaleStatusEnum.values()) {
            mapping.put(afterSaleStatusEnum.getStatus(), afterSaleStatusEnum.getName());
        }
        return Map.copyOf(mapping);
    }

    /**
     * 构建售后原因字典，将售后原因枚举中的原因编码映射为对应的中文名称。
     *
     * @return 以售后原因编码为 key、售后原因名称为 value 的不可变字典
     */
    private static Map<String, String> buildAfterSaleReasonDict() {
        Map<String, String> mapping = new LinkedHashMap<>();
        for (AfterSaleReasonEnum afterSaleReasonEnum : AfterSaleReasonEnum.values()) {
            mapping.put(afterSaleReasonEnum.getReason(), afterSaleReasonEnum.getName());
        }
        return Map.copyOf(mapping);
    }

    /**
     * 构建收货状态字典，将 {@link ReceiveStatusEnum} 中定义的状态编码映射为对应中文名称。
     *
     * @return 以收货状态编码为键、收货状态名称为值的不可变映射
     */
    private static Map<String, String> buildReceiveStatusDict() {
        Map<String, String> mapping = new LinkedHashMap<>();
        for (ReceiveStatusEnum receiveStatusEnum : ReceiveStatusEnum.values()) {
            mapping.put(receiveStatusEnum.getStatus(), receiveStatusEnum.getName());
        }
        return Map.copyOf(mapping);
    }

    /**
     * Builds a dictionary of order event types by mapping each {@link OrderEventTypeEnum} type value
     * to its corresponding display name.
     *
     * @return an unmodifiable map whose keys are order event type codes and whose values are the
     * corresponding order event type names
     */
    private static Map<String, String> buildOrderEventTypeDict() {
        Map<String, String> mapping = new LinkedHashMap<>();
        for (OrderEventTypeEnum orderEventTypeEnum : OrderEventTypeEnum.values()) {
            mapping.put(orderEventTypeEnum.getType(), orderEventTypeEnum.getName());
        }
        return Map.copyOf(mapping);
    }

    /**
     * 构建操作方类型字典，将 {@code OperatorTypeEnum} 中定义的类型编码与中文名称进行映射。
     *
     * @return 以操作方类型编码为键、操作方名称为值的只读映射
     */
    private static Map<String, String> buildOperatorTypeDict() {
        Map<String, String> mapping = new LinkedHashMap<>();
        for (OperatorTypeEnum operatorTypeEnum : OperatorTypeEnum.values()) {
            mapping.put(operatorTypeEnum.getType(), operatorTypeEnum.getName());
        }
        return Map.copyOf(mapping);
    }

    /**
     * 构建物流状态字典，将 {@link ShippingStatusEnum} 中定义的状态类型映射为对应的中文名称。
     *
     * @return 以物流状态 type 为键、状态名称为值的不可变映射
     */
    private static Map<String, String> buildShippingStatusDict() {
        Map<String, String> mapping = new LinkedHashMap<>();
        for (ShippingStatusEnum shippingStatusEnum : ShippingStatusEnum.values()) {
            mapping.put(shippingStatusEnum.getType(), shippingStatusEnum.getName());
        }
        return Map.copyOf(mapping);
    }
}
