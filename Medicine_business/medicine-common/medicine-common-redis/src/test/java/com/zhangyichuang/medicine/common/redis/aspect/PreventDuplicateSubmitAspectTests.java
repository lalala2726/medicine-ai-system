package com.zhangyichuang.medicine.common.redis.aspect;

import com.zhangyichuang.medicine.common.core.constants.RedisConstants;
import com.zhangyichuang.medicine.common.core.exception.DuplicateSubmitException;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.common.redis.annotation.PreventDuplicateSubmit;
import com.zhangyichuang.medicine.common.redis.support.PreventDuplicateSubmitFingerprintBuilder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 防重复提交切面测试。
 */
@ExtendWith(MockitoExtension.class)
class PreventDuplicateSubmitAspectTests {

    /**
     * Redis 模板。
     */
    @Mock
    private RedisTemplate<Object, Object> redisTemplate;

    /**
     * Redis ValueOperations。
     */
    @Mock
    private ValueOperations<Object, Object> valueOperations;

    /**
     * 每个用例结束后清理请求上下文与安全上下文。
     */
    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    /**
     * 验证相同用户对相同接口和相同参数重复提交时，
     * 第二次会被识别为重复提交并抛出业务异常。
     */
    @Test
    void around_WhenSameUserSubmitsSamePayloadTwice_ShouldThrowDuplicateSubmitException() throws Throwable {
        PreventDuplicateSubmitAspect aspect = buildAspect();
        Method method = ClassLevelDummyController.class.getMethod("create", Map.class);
        ProceedingJoinPoint joinPoint = buildJoinPoint(
                new ClassLevelDummyController(),
                method,
                new String[]{"request"},
                new Object[]{Map.of("name", "coupon")}
        );

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mall/coupon/template");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new UserPrincipal(1001L), null, List.of())
        );

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), any(), anyLong(), eq(TimeUnit.MILLISECONDS)))
                .thenReturn(true)
                .thenReturn(false);
        when(joinPoint.proceed()).thenReturn("ok");

        Object firstResult = aspect.around(joinPoint);
        DuplicateSubmitException exception = assertThrows(DuplicateSubmitException.class, () -> aspect.around(joinPoint));

        assertEquals("ok", firstResult);
        assertEquals("请勿重复提交", exception.getMessage());
        verify(joinPoint, times(1)).proceed();
    }

    /**
     * 验证类上存在注解时，GET 请求不会进入 Redis 防抖判定逻辑。
     */
    @Test
    void around_WhenRequestMethodIsGet_ShouldBypassDuplicateCheck() throws Throwable {
        PreventDuplicateSubmitAspect aspect = buildAspect();
        Method method = ClassLevelDummyController.class.getMethod("query", Long.class);
        ProceedingJoinPoint joinPoint = buildJoinPoint(
                new ClassLevelDummyController(),
                method,
                new String[]{"id"},
                new Object[]{1L}
        );

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/mall/coupon/template/1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new UserPrincipal(1002L), null, List.of())
        );
        when(joinPoint.proceed()).thenReturn("query-ok");

        Object result = aspect.around(joinPoint);

        assertEquals("query-ok", result);
        verify(redisTemplate, never()).opsForValue();
    }

    /**
     * 验证方法级注解可以使用自定义提示语，
     * 并且重复提交时会把该提示语返回给调用方。
     */
    @Test
    void around_WhenMethodAnnotationOverridesClassConfig_ShouldUseCustomFailMessage() throws Throwable {
        PreventDuplicateSubmitAspect aspect = buildAspect();
        Method method = MethodLevelDummyController.class.getMethod("customSubmit", Map.class);
        ProceedingJoinPoint joinPoint = buildJoinPoint(
                new MethodLevelDummyController(),
                method,
                new String[]{"request"},
                new Object[]{Map.of("id", 10L)}
        );

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mall/custom/submit");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new UserPrincipal(1003L), null, List.of())
        );

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), any(), anyLong(), eq(TimeUnit.MILLISECONDS)))
                .thenReturn(false);

        DuplicateSubmitException exception = assertThrows(DuplicateSubmitException.class, () -> aspect.around(joinPoint));

        assertEquals("请稍后再试", exception.getMessage());
    }

    /**
     * 验证当前请求没有可用 userId 时会直接失败，
     * 且不会回退到 IP 或其他兜底主体。
     */
    @Test
    void around_WhenAuthenticationHasNoUserId_ShouldThrowServiceException() throws Throwable {
        PreventDuplicateSubmitAspect aspect = buildAspect();
        Method method = ClassLevelDummyController.class.getMethod("create", Map.class);
        ProceedingJoinPoint joinPoint = buildJoinPoint(
                new ClassLevelDummyController(),
                method,
                new String[]{"request"},
                new Object[]{Map.of("name", "coupon")}
        );

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mall/coupon/template");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new Object(), null, List.of())
        );

        ServiceException exception = assertThrows(ServiceException.class, () -> aspect.around(joinPoint));

        assertEquals("防重复提交未获取到用户ID", exception.getMessage());
        verify(redisTemplate, never()).opsForValue();
    }

    /**
     * 验证防抖 key 中会包含 userId、HTTP 方法和请求路径，
     * 便于将判定粒度锁定到单用户单接口单参数。
     */
    @Test
    void around_WhenSubmittingWriteRequest_ShouldBuildExpectedRedisKeyShape() throws Throwable {
        PreventDuplicateSubmitAspect aspect = buildAspect();
        Method method = ClassLevelDummyController.class.getMethod("create", Map.class);
        ProceedingJoinPoint joinPoint = buildJoinPoint(
                new ClassLevelDummyController(),
                method,
                new String[]{"request"},
                new Object[]{Map.of("name", "coupon", "amount", 100)}
        );

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mall/coupon/template");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new UserPrincipal(888L), null, List.of())
        );

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), any(), anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(true);
        when(joinPoint.proceed()).thenReturn(Boolean.TRUE);

        aspect.around(joinPoint);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).setIfAbsent(keyCaptor.capture(),
                any(),
                eq(RedisConstants.DuplicateSubmit.DEFAULT_INTERVAL_MILLIS),
                eq(TimeUnit.MILLISECONDS));
        String redisKey = keyCaptor.getValue();
        assertTrue(redisKey.startsWith("repeat_submit:888:POST:/mall/coupon/template:"));
    }

    /**
     * 构造待测试的切面对象。
     *
     * @return 防重复提交切面
     */
    private PreventDuplicateSubmitAspect buildAspect() {
        return new PreventDuplicateSubmitAspect(redisTemplate, new PreventDuplicateSubmitFingerprintBuilder());
    }

    /**
     * 构造切点对象。
     *
     * @param target         目标对象
     * @param method         目标方法
     * @param parameterNames 参数名数组
     * @param args           参数值数组
     * @return 切点对象
     */
    private ProceedingJoinPoint buildJoinPoint(Object target,
                                               Method method,
                                               String[] parameterNames,
                                               Object[] args) {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        lenient().when(signature.getParameterNames()).thenReturn(parameterNames);
        lenient().when(joinPoint.getArgs()).thenReturn(args);
        lenient().when(joinPoint.getTarget()).thenReturn(target);
        return joinPoint;
    }

    /**
     * 类级别防重复提交示例控制器。
     */
    @PreventDuplicateSubmit
    private static class ClassLevelDummyController {

        /**
         * 模拟新增动作。
         *
         * @param request 请求参数
         * @return 执行结果
         */
        public String create(Map<String, Object> request) {
            return String.valueOf(request);
        }

        /**
         * 模拟查询动作。
         *
         * @param id 业务ID
         * @return 执行结果
         */
        public String query(Long id) {
            return String.valueOf(id);
        }
    }

    /**
     * 方法级别防重复提交示例控制器。
     */
    private static class MethodLevelDummyController {

        /**
         * 模拟自定义重复提交提示动作。
         *
         * @param request 请求参数
         * @return 执行结果
         */
        @PreventDuplicateSubmit(intervalMillis = 1500L, failMessage = "请稍后再试")
        public String customSubmit(Map<String, Object> request) {
            return String.valueOf(request);
        }
    }

    /**
     * 测试用用户主体。
     */
    private static class UserPrincipal {

        /**
         * 用户ID。
         */
        private final Long userId;

        /**
         * 构造测试用用户主体。
         *
         * @param userId 用户ID
         */
        private UserPrincipal(Long userId) {
            this.userId = userId;
        }

        /**
         * 获取用户ID。
         *
         * @return 用户ID
         */
        public Long getUserId() {
            return userId;
        }
    }
}
