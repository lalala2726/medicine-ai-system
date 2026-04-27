package com.zhangyichuang.medicine.client.service.impl;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhangyichuang.medicine.client.mapper.UserMapper;
import com.zhangyichuang.medicine.client.model.dto.UserProfileDto;
import com.zhangyichuang.medicine.client.model.vo.UserBriefVo;
import com.zhangyichuang.medicine.client.service.MallOrderService;
import com.zhangyichuang.medicine.client.service.UserCouponService;
import com.zhangyichuang.medicine.client.service.UserService;
import com.zhangyichuang.medicine.client.service.UserWalletService;
import com.zhangyichuang.medicine.common.core.utils.Assert;
import com.zhangyichuang.medicine.common.core.utils.BeanCotyUtils;
import com.zhangyichuang.medicine.common.security.base.BaseService;
import com.zhangyichuang.medicine.model.entity.MallOrder;
import com.zhangyichuang.medicine.model.entity.User;
import com.zhangyichuang.medicine.model.enums.OrderStatusEnum;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Chuang
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService, BaseService {

    private final UserWalletService userWalletService;
    private final MallOrderService mallOrderService;
    private final UserCouponService userCouponService;

    public UserServiceImpl(UserWalletService userWalletService,
                           MallOrderService mallOrderService,
                           UserCouponService userCouponService) {
        this.userWalletService = userWalletService;
        this.mallOrderService = mallOrderService;
        this.userCouponService = userCouponService;
    }

    /**
     * 根据用户ID查询用户信息
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    @Override
    public User getUserById(Long userId) {
        return getById(userId);
    }

    /**
     * 根据用户名查询用户信息
     *
     * @param username 用户名
     * @return 用户信息
     */
    @Override
    public User getUserByUsername(String username) {
        LambdaQueryChainWrapper<User> eq = lambdaQuery().eq(User::getUsername, username);
        return eq.one();
    }

    /**
     * 根据用户ID查询用户角色集合
     *
     * @param userId 用户ID
     * @return 用户角色集合
     */
    @Override
    public Set<String> getUserRolesByUserId(Long userId) {
        List<String> roleCodes = baseMapper.listRoleCodesByUserId(userId);
        if (roleCodes == null || roleCodes.isEmpty()) {
            return Set.of();
        }
        return roleCodes.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(code -> !code.isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * 根据用户名查询用户角色集合
     *
     * @param username 用户名
     * @return 用户角色集合
     */
    @Override
    public Set<String> getUserRolesByUserName(String username) {
        User user = getUserByUsername(username);
        if (user == null) {
            return Set.of();
        }
        return getUserRolesByUserId(user.getId());
    }

    @Override
    public void updateLoginInfo(Long userId, String ip) {
        Assert.notNull(userId, "用户ID不能为空");
        Assert.notEmpty(ip, "用户IP不能为空");

        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setLastLoginTime(new Date());
        updateUser.setLastLoginIp(ip);
        updateById(updateUser);
    }

    @Override
    public UserBriefVo getUserBriefInfo() {
        Long userId = getUserId();

        // 获取当前用户信息
        User user = getUserById(userId);

        // 获取用户钱包余额
        BigDecimal userWalletBalance = userWalletService.getUserWalletBalance();

        // 获取当前用户订单的信息
        List<MallOrder> mallOrders = mallOrderService.lambdaQuery()
                .eq(MallOrder::getUserId, userId)
                .list();

        // 统计各种状态的订单数量
        Map<String, Long> orderCountMap = mallOrders.stream()
                .collect(Collectors.groupingBy(MallOrder::getOrderStatus, Collectors.counting()));

        // 待支付订单数量
        Integer payOrderCount = orderCountMap.getOrDefault(OrderStatusEnum.PENDING_PAYMENT.getType(), 0L).intValue();

        // 待发货订单数量
        Integer deliverOrderCount = orderCountMap.getOrDefault(OrderStatusEnum.PENDING_SHIPMENT.getType(), 0L).intValue();

        // 待收货订单数量
        Integer receiveOrderCount = orderCountMap.getOrDefault(OrderStatusEnum.PENDING_RECEIPT.getType(), 0L).intValue();

        // 已完成订单数量
        Integer completeOrderCount = orderCountMap.getOrDefault(OrderStatusEnum.COMPLETED.getType(), 0L).intValue();

        // 退货/售后订单数量(售后中)
        Integer afterSaleOrderCount = orderCountMap.getOrDefault(OrderStatusEnum.AFTER_SALE.getType(), 0L).intValue();

        // 统计客户端应展示的优惠券数量。
        Integer couponCount = Long.valueOf(userCouponService.countDisplayableCoupons(userId)).intValue();

        return UserBriefVo.builder()
                .avatarUrl(user.getAvatar())
                .nickName(user.getNickname())
                .phoneNumber(user.getPhoneNumber())
                .balance(userWalletBalance)
                .couponCount(couponCount)
                .payOrderCount(payOrderCount)
                .deliverOrderCount(deliverOrderCount)
                .receiveOrderCount(receiveOrderCount)
                .completeOrderCount(completeOrderCount)
                .afterSaleOrderCount(afterSaleOrderCount)
                .build();
    }

    /**
     * 获取当前登录用户的完整个人资料。
     *
     * @return 当前登录用户的完整个人资料
     */
    @Override
    public UserProfileDto getUserProfile() {
        Long userId = getUserId();
        User user = getUserById(userId);
        UserProfileDto userProfileDto = BeanCotyUtils.copyProperties(user, UserProfileDto.class);
        if (userProfileDto == null) {
            return null;
        }
        userProfileDto.setBirthday(convertDateToLocalDate(user.getBirthday()));
        return userProfileDto;
    }

    /**
     * 更新当前登录用户的个人资料。
     *
     * @param userProfileDto 用户个人资料参数
     * @return 是否更新成功
     */
    @Override
    public boolean updateUserProfile(UserProfileDto userProfileDto) {
        Long userId = getUserId();
        User user = BeanCotyUtils.copyProperties(userProfileDto, User.class);
        user.setId(userId);
        user.setBirthday(convertLocalDateToDate(userProfileDto.getBirthday()));
        return updateById(user);
    }

    /**
     * 将生日日期从 {@link Date} 转换为 {@link LocalDate}。
     *
     * @param birthday 生日日期
     * @return LocalDate 格式的生日日期
     */
    private LocalDate convertDateToLocalDate(Date birthday) {
        if (birthday == null) {
            return null;
        }
        return Instant.ofEpochMilli(birthday.getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    /**
     * 将生日日期从 {@link LocalDate} 转换为 {@link Date}。
     *
     * @param birthday 生日日期
     * @return Date 格式的生日日期
     */
    private Date convertLocalDateToDate(LocalDate birthday) {
        if (birthday == null) {
            return null;
        }
        return java.sql.Date.valueOf(birthday);
    }

}
