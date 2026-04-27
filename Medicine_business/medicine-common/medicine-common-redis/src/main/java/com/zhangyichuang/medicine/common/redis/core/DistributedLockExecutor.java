package com.zhangyichuang.medicine.common.redis.core;

import com.zhangyichuang.medicine.common.core.exception.DistributedLockException;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 分布式锁执行器。
 * <p>
 * 适用于返回值敏感、需要在抢锁失败时返回特殊协议值或执行自定义兜底逻辑的场景，
 * 例如支付回调、MQ 消费与定时补偿任务。
 * </p>
 *
 * @author Chuang
 */
@Component
public class DistributedLockExecutor {

    /**
     * 默认的锁冲突提示语。
     */
    private static final String DEFAULT_FAIL_MESSAGE = "操作处理中，请勿重复提交";

    /**
     * 日志记录器。
     */
    private static final Logger log = LoggerFactory.getLogger(DistributedLockExecutor.class);

    /**
     * Redisson 客户端。
     */
    private final RedissonClient redissonClient;

    /**
     * 构造分布式锁执行器。
     *
     * @param redissonClient Redisson 客户端
     */
    public DistributedLockExecutor(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 获取锁并执行带返回值的业务逻辑。
     *
     * @param lockName        锁名称
     * @param waitTimeMillis  等待时间（毫秒）
     * @param leaseTimeMillis 锁租约时间（毫秒）
     * @param failMessage     获取锁失败提示语
     * @param action          业务逻辑
     * @param <T>             返回值类型
     * @return 业务执行结果
     */
    public <T> T execute(String lockName,
                         long waitTimeMillis,
                         long leaseTimeMillis,
                         String failMessage,
                         Supplier<T> action) {
        return tryExecuteOrElse(lockName,
                waitTimeMillis,
                leaseTimeMillis,
                action,
                () -> {
                    throw new DistributedLockException(resolveFailMessage(failMessage));
                });
    }

    /**
     * 获取锁并执行无返回值的业务逻辑。
     *
     * @param lockName        锁名称
     * @param waitTimeMillis  等待时间（毫秒）
     * @param leaseTimeMillis 锁租约时间（毫秒）
     * @param failMessage     获取锁失败提示语
     * @param action          业务逻辑
     */
    public void executeVoid(String lockName,
                            long waitTimeMillis,
                            long leaseTimeMillis,
                            String failMessage,
                            Runnable action) {
        execute(lockName, waitTimeMillis, leaseTimeMillis, failMessage, () -> {
            action.run();
            return null;
        });
    }

    /**
     * 尝试获取锁并执行带有失败分支的业务逻辑。
     *
     * @param lockName         锁名称
     * @param waitTimeMillis   等待时间（毫秒）
     * @param leaseTimeMillis  锁租约时间（毫秒）
     * @param successAction    获取锁成功后的业务逻辑
     * @param lockFailedAction 获取锁失败后的逻辑
     * @param <T>              返回值类型
     * @return 业务执行结果
     */
    public <T> T tryExecuteOrElse(String lockName,
                                  long waitTimeMillis,
                                  long leaseTimeMillis,
                                  Supplier<T> successAction,
                                  Supplier<T> lockFailedAction) {
        validateLockName(lockName);
        validateLockTiming(waitTimeMillis, leaseTimeMillis);
        RLock lock = redissonClient.getLock(lockName);
        boolean locked = false;
        long startTime = System.currentTimeMillis();
        try {
            locked = tryAcquireLock(lock, waitTimeMillis, leaseTimeMillis);
            if (!locked) {
                log.warn("获取分布式锁失败，lockName={}，waitTimeMillis={}，leaseTimeMillis={}",
                        lockName,
                        waitTimeMillis,
                        leaseTimeMillis);
                return lockFailedAction.get();
            }

            log.info("获取分布式锁成功，lockName={}，costTimeMillis={}",
                    lockName,
                    System.currentTimeMillis() - startTime);
            return successAction.get();
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("释放分布式锁完成，lockName={}，holdTimeMillis={}",
                        lockName,
                        System.currentTimeMillis() - startTime);
            }
        }
    }

    /**
     * 尝试获取分布式锁。
     *
     * @param lock            Redisson 锁对象
     * @param waitTimeMillis  等待时间（毫秒）
     * @param leaseTimeMillis 锁租约时间（毫秒）
     * @return true 表示获取成功
     */
    private boolean tryAcquireLock(RLock lock, long waitTimeMillis, long leaseTimeMillis) {
        try {
            if (leaseTimeMillis > 0) {
                return lock.tryLock(waitTimeMillis, leaseTimeMillis, TimeUnit.MILLISECONDS);
            }
            return lock.tryLock(waitTimeMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new DistributedLockException("获取分布式锁时线程被中断");
        }
    }

    /**
     * 校验锁名称是否有效。
     *
     * @param lockName 锁名称
     */
    private void validateLockName(String lockName) {
        if (!StringUtils.hasText(lockName)) {
            throw new DistributedLockException("分布式锁名称不能为空");
        }
    }

    /**
     * 校验锁等待时间和租约时间是否合法。
     *
     * @param waitTimeMillis  等待时间（毫秒）
     * @param leaseTimeMillis 租约时间（毫秒）
     */
    private void validateLockTiming(long waitTimeMillis, long leaseTimeMillis) {
        if (waitTimeMillis < 0) {
            throw new DistributedLockException("分布式锁等待时间不能小于0");
        }
        if (leaseTimeMillis == 0) {
            throw new DistributedLockException("分布式锁租约时间不能为0，请使用正数或 -1");
        }
    }

    /**
     * 解析锁获取失败提示语。
     *
     * @param failMessage 原始失败提示语
     * @return 归一化后的失败提示语
     */
    private String resolveFailMessage(String failMessage) {
        return StringUtils.hasText(failMessage) ? failMessage : DEFAULT_FAIL_MESSAGE;
    }
}
