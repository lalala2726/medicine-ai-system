package com.zhangyichuang.medicine.common.core.constants;

/**
 * Redis 常量
 *
 * @author Chuang
 */
public interface RedisConstants {
    /**
     * 验证码前缀
     */
    String CAPTCHA_CODE = "captcha:code:";

    /**
     * 接口访问限流前缀
     */
    String ACCESS_LIMIT_PREFIX = "access_limit:";

    /**
     * IP限流前缀
     */
    String ACCESS_LIMIT_IP = ACCESS_LIMIT_PREFIX + "ip:";

    /**
     * 用户ID限流前缀
     */
    String ACCESS_LIMIT_USER = ACCESS_LIMIT_PREFIX + "user:";

    /**
     * 自定义限流前缀
     */
    String ACCESS_LIMIT_CUSTOM = ACCESS_LIMIT_PREFIX + "custom:";

    /**
     * 字典模块缓存接口
     */
    interface Dict {
        /**
         * 字典缓存前缀
         */
        String DICT_CACHE_PREFIX = "system:dict:data:";

        /**
         * 字典数据缓存Key格式: system:dict:data:{dictType}
         */
        String DICT_DATA_KEY = DICT_CACHE_PREFIX + "%s";

        /**
         * 字典缓存过期时间（秒）- 24小时
         */
        int DICT_CACHE_EXPIRE_TIME = 24 * 60 * 60;
    }


    interface StorageConfig {
        String ACTIVE_TYPE = "storage:active_type";
        String CURRENT_STORAGE_CONFIG = "storage:current_storage_config";
        String CONFIGURATION_FILE_TYPE = "storage:configuration_file_type";
        String CONFIG_TYPE_DATABASE = "database";
    }

    /**
     * 系统配置模块。
     */
    interface SystemConfig {

        /**
         * 软件协议配置缓存 Key。
         */
        String AGREEMENT_CONFIG_KEY = "system:config:agreement";
    }


    /**
     * 认证模块
     */
    interface Auth {

        /**
         * 登录安全配置缓存Key。
         */
        String LOGIN_SECURITY_CONFIG_KEY = "auth:login:security:config";

        /**
         * 用户访问令牌 Key 前缀，后接访问令牌会话ID。
         */
        String USER_ACCESS_TOKEN = "auth:token:access:";

        /**
         * 用户刷新令牌 Key 前缀，后接刷新令牌会话ID。
         */
        String USER_REFRESH_TOKEN = "auth:token:refresh:";

        /**
         * 用户访问令牌索引 Key 模板，参数为用户ID，用于按用户清理访问令牌。
         */
        String USER_ACCESS_TOKEN_INDEX = "auth:token:user:access:%s";

        /**
         * 用户刷新令牌索引 Key 模板，参数为用户ID，用于按用户清理刷新令牌。
         */
        String USER_REFRESH_TOKEN_INDEX = "auth:token:user:refresh:%s";

        String ROLE_KEY = "auth:role:";

        String SESSIONS_INDEX_KEY = "auth:session:index:";

        String SESSIONS_DEVICE_KEY = "auth:session:device:";

        /**
         * 密码重试限制前缀
         */
        String PASSWORD_RETRY_PREFIX = "auth:password:retry:";

        /**
         * 密码重试次数缓存Key格式: auth:password:retry:{source}:{username}
         */
        String PASSWORD_RETRY_COUNT_KEY = PASSWORD_RETRY_PREFIX + "%s:%s";

        /**
         * 密码锁定状态缓存Key格式: auth:password:lock:{source}:{username}
         */
        String PASSWORD_LOCK_KEY = "auth:password:lock:%s:%s";

        /**
         * 登录频率限制前缀
         */
        String LOGIN_FREQUENCY_PREFIX = "auth:login:frequency:";

        /**
         * 每日登录失败次数Key格式: auth:login:frequency:fail:day:{username}
         */
        String LOGIN_FAIL_DAY_KEY = LOGIN_FREQUENCY_PREFIX + "fail:day:%s";

        /**
         * 每小时登录失败次数Key格式: auth:login:frequency:fail:hour:{username}
         */
        String LOGIN_FAIL_HOUR_KEY = LOGIN_FREQUENCY_PREFIX + "fail:hour:%s";

        /**
         * 每日登录成功次数Key格式: auth:login:frequency:success:day:{username}
         */
        String LOGIN_SUCCESS_DAY_KEY = LOGIN_FREQUENCY_PREFIX + "success:day:%s";

        /**
         * 每小时登录成功次数Key格式: auth:login:frequency:success:hour:{username}
         */
        String LOGIN_SUCCESS_HOUR_KEY = LOGIN_FREQUENCY_PREFIX + "success:hour:%s";

    }


    /**
     * 商城商品缓存相关常量
     */
    interface MallProduct {
        /**
         * 缓存名称
         */
        String CACHE_NAME = "mall:product:detail";

        /**
         * 缓存 Key 前缀
         */
        String KEY_PREFIX = "mall:product:detail:";

        /**
         * Key 模板 mall:product:detail:{productId}
         */
        String DETAIL_KEY = KEY_PREFIX + "%s";

        /**
         * 缓存有效期（秒）- 30分钟
         */
        long CACHE_TTL_SECONDS = 30 * 60;
    }

    /**
     * 商品索引增量同步相关缓存
     */
    interface MallProductIndex {
        /**
         * 商品销量增量计数 key（每满固定阈值触发 ES 刷新）
         */
        String SALES_SYNC_COUNTER_KEY = "mall:product:index:sales:delta:%s";
    }

    /**
     * Agent 配置缓存相关常量
     */
    interface AgentConfig {
        /**
         * Agent 全量配置缓存 key
         */
        String ALL_CONFIG_KEY = "agent:config:all";

        /**
         * Agent 单条提示词运行时缓存 key 模板。
         */
        String PROMPT_CONFIG_KEY_TEMPLATE = "agent:prompt:%s";

        /**
         * 历史聚合版 Agent 提示词运行时缓存 key。
         * <p>
         * 该 key 仅用于重构阶段的启动清理，不再作为运行时写入目标。
         */
        String LEGACY_PROMPT_ALL_CONFIG_KEY = "agent:prompt:all";
    }

    /**
     * 商城分布式锁相关缓存 key。
     */
    interface Lock {

        /**
         * 订单级分布式锁 key 模板：mall:lock:order:{orderNo}
         */
        String ORDER_KEY = "mall:lock:order:%s";

        /**
         * 用户提单防重锁 key 模板：mall:lock:order:submit:user:{userId}
         */
        String ORDER_SUBMIT_USER_KEY = "mall:lock:order:submit:user:%s";

        /**
         * 售后单级分布式锁 key 模板：mall:lock:after-sale:{afterSaleNo}
         */
        String AFTER_SALE_KEY = "mall:lock:after-sale:%s";

        /**
         * 发券批次消息分布式锁 key 模板：mall:lock:coupon:batch-issue:{sourceBizNo}
         */
        String COUPON_BATCH_ISSUE_KEY = "mall:lock:coupon:batch-issue:%s";

        /**
         * 单用户发券消息分布式锁 key 模板：mall:lock:coupon:issue:{sourceBizNo:userId}
         */
        String COUPON_ISSUE_KEY = "mall:lock:coupon:issue:%s";

        /**
         * 优惠券过期任务分布式锁 key：mall:lock:coupon:expire:task
         */
        String COUPON_EXPIRE_TASK_KEY = "mall:lock:coupon:expire:task";

        /**
         * 激活码兑换分布式锁 key 模板：mall:lock:coupon:activation:{normalizedCode}
         */
        String COUPON_ACTIVATION_KEY = "mall:lock:coupon:activation:%s";
    }

    /**
     * 防重复提交相关缓存 key。
     */
    interface DuplicateSubmit {

        /**
         * 防重复提交 key 模板：repeat_submit:{userId}:{httpMethod}:{requestUri}:{paramHash}
         */
        String KEY_TEMPLATE = "repeat_submit:%s:%s:%s:%s";

        /**
         * 默认防重复提交时间窗口（毫秒）。
         */
        long DEFAULT_INTERVAL_MILLIS = 300L;
    }

    /**
     * 滑动窗口限流相关缓存 key。
     */
    interface AccessLimit {

        /**
         * 限流 key 模板：access_limit:{identityType}:{identityValue}:{resourceKey}
         */
        String KEY_TEMPLATE = ACCESS_LIMIT_PREFIX + "%s:%s:%s";

        /**
         * 用户维度标识。
         */
        String IDENTITY_USER = "user";

        /**
         * IP 维度标识。
         */
        String IDENTITY_IP = "ip";

        /**
         * 默认限流提示语。
         */
        String DEFAULT_FAIL_MESSAGE = "请求过于频繁，请稍后再试";
    }

}
