package com.zhangyichuang.medicine.common.redis.aspect;

import com.zhangyichuang.medicine.common.core.exception.DistributedLockException;
import com.zhangyichuang.medicine.common.redis.annotation.DistributedLock;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁切面。
 * <p>
 * 该切面固定早于事务切面执行，确保先获取分布式锁，再进入数据库事务。
 * </p>
 *
 * @author Chuang
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class DistributedLockAspect {

    /**
     * 日志记录器。
     */
    private static final Logger log = LoggerFactory.getLogger(DistributedLockAspect.class);

    /**
     * 参数名发现器。
     */
    private static final DefaultParameterNameDiscoverer PARAMETER_NAME_DISCOVERER =
            new DefaultParameterNameDiscoverer();

    /**
     * SpEL 解析器。
     */
    private static final ExpressionParser EXPRESSION_PARSER = new SpelExpressionParser();

    /**
     * Redisson 客户端。
     */
    private final RedissonClient redissonClient;

    /**
     * BeanFactory 解析器。
     */
    private final BeanFactoryResolver beanFactoryResolver;

    /**
     * 构造分布式锁切面。
     *
     * @param redissonClient Redisson 客户端
     * @param beanFactory    BeanFactory
     */
    public DistributedLockAspect(RedissonClient redissonClient,
                                 org.springframework.beans.factory.BeanFactory beanFactory) {
        this.redissonClient = redissonClient;
        this.beanFactoryResolver = new BeanFactoryResolver(beanFactory);
    }

    /**
     * 拦截标注了分布式锁注解的方法。
     *
     * @param joinPoint 切点信息
     * @return 目标方法执行结果
     * @throws Throwable 目标方法执行异常
     */
    @Around("@annotation(com.zhangyichuang.medicine.common.redis.annotation.DistributedLock)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        DistributedLock distributedLock = resolveDistributedLock(joinPoint, method);
        String lockName = buildLockName(joinPoint, method, distributedLock);
        long startTime = System.currentTimeMillis();
        RLock lock = redissonClient.getLock(lockName);
        boolean locked = false;
        try {
            locked = tryAcquireLock(lock, distributedLock);
            if (!locked) {
                log.warn("获取分布式锁失败，lockName={}，waitTimeMillis={}，leaseTimeMillis={}",
                        lockName,
                        distributedLock.waitTimeMillis(),
                        distributedLock.leaseTimeMillis());
                throw new DistributedLockException(distributedLock.failMessage());
            }

            log.info("获取分布式锁成功，lockName={}，costTimeMillis={}",
                    lockName,
                    System.currentTimeMillis() - startTime);
            return joinPoint.proceed();
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
     * 解析分布式锁注解配置。
     *
     * @param joinPoint 切点信息
     * @param method    当前方法
     * @return 分布式锁注解配置
     */
    private DistributedLock resolveDistributedLock(ProceedingJoinPoint joinPoint, Method method) {
        DistributedLock distributedLock = method.getAnnotation(DistributedLock.class);
        if (distributedLock != null) {
            return distributedLock;
        }
        Method targetMethod = resolveTargetMethod(joinPoint, method);
        distributedLock = targetMethod.getAnnotation(DistributedLock.class);
        if (distributedLock == null) {
            throw new DistributedLockException("未找到分布式锁注解配置");
        }
        return distributedLock;
    }

    /**
     * 获取目标类的具体方法定义。
     *
     * @param joinPoint 切点信息
     * @param method    方法签名中的方法
     * @return 目标类方法
     */
    private Method resolveTargetMethod(ProceedingJoinPoint joinPoint, Method method) {
        Object target = joinPoint.getTarget();
        if (target == null) {
            return method;
        }
        try {
            return target.getClass().getMethod(method.getName(), method.getParameterTypes());
        } catch (NoSuchMethodException ex) {
            log.debug("未获取到目标类方法定义，使用签名方法继续处理，method={}", method.getName(), ex);
            return method;
        }
    }

    /**
     * 尝试获取分布式锁。
     *
     * @param lock            Redisson 锁对象
     * @param distributedLock 分布式锁注解
     * @return true 表示获取成功
     */
    private boolean tryAcquireLock(RLock lock, DistributedLock distributedLock) {
        validateLockTiming(distributedLock.waitTimeMillis(), distributedLock.leaseTimeMillis());
        try {
            if (distributedLock.leaseTimeMillis() > 0) {
                return lock.tryLock(distributedLock.waitTimeMillis(),
                        distributedLock.leaseTimeMillis(),
                        TimeUnit.MILLISECONDS);
            }
            return lock.tryLock(distributedLock.waitTimeMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new DistributedLockException("获取分布式锁时线程被中断");
        }
    }

    /**
     * 组装最终锁名。
     *
     * @param joinPoint       切点信息
     * @param method          当前方法
     * @param distributedLock 分布式锁注解
     * @return 最终锁名
     */
    private String buildLockName(ProceedingJoinPoint joinPoint, Method method, DistributedLock distributedLock) {
        String businessKey = evaluateBusinessKey(joinPoint, method, distributedLock.key());
        if (!StringUtils.hasText(businessKey)) {
            throw new DistributedLockException("分布式锁业务键不能为空");
        }
        String normalizedBusinessKey = businessKey.trim();
        String prefix = distributedLock.prefix();
        if (!StringUtils.hasText(prefix)) {
            return normalizedBusinessKey;
        }
        if (prefix.contains("%s")) {
            return String.format(prefix, normalizedBusinessKey);
        }
        return prefix + normalizedBusinessKey;
    }

    /**
     * 解析 SpEL 业务键。
     *
     * @param joinPoint     切点信息
     * @param method        当前方法
     * @param keyExpression 业务键表达式
     * @return 解析后的业务键字符串
     */
    private String evaluateBusinessKey(ProceedingJoinPoint joinPoint, Method method, String keyExpression) {
        if (!StringUtils.hasText(keyExpression)) {
            throw new DistributedLockException("分布式锁 key 表达式不能为空");
        }
        MethodBasedEvaluationContext evaluationContext = new MethodBasedEvaluationContext(
                joinPoint.getTarget(),
                method,
                joinPoint.getArgs(),
                PARAMETER_NAME_DISCOVERER
        );
        evaluationContext.setBeanResolver(beanFactoryResolver);
        Expression expression = EXPRESSION_PARSER.parseExpression(keyExpression);
        Object value = expression.getValue(evaluationContext);
        return value == null ? null : value.toString();
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
}
