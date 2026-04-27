package com.zhangyichuang.medicine.client.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.client.model.dto.UserProfileDto;
import com.zhangyichuang.medicine.client.model.request.UserWalletBillRequest;
import com.zhangyichuang.medicine.client.model.vo.UserBriefVo;
import com.zhangyichuang.medicine.client.model.vo.UserWalletBillDetailVo;
import com.zhangyichuang.medicine.client.model.vo.UserWalletBillVo;
import com.zhangyichuang.medicine.client.service.UserService;
import com.zhangyichuang.medicine.client.service.UserWalletService;
import com.zhangyichuang.medicine.common.core.base.AjaxResult;
import com.zhangyichuang.medicine.common.core.base.TableDataResult;
import com.zhangyichuang.medicine.common.core.utils.BeanCotyUtils;
import com.zhangyichuang.medicine.common.redis.annotation.PreventDuplicateSubmit;
import com.zhangyichuang.medicine.common.security.base.BaseController;
import com.zhangyichuang.medicine.common.security.utils.SecurityUtils;
import com.zhangyichuang.medicine.model.entity.User;
import com.zhangyichuang.medicine.model.entity.UserWalletLog;
import com.zhangyichuang.medicine.model.vo.CurrentUserInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 客户端用户控制器。
 * <p>
 * 提供个人资料、当前用户信息、钱包与订单统计相关接口。
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/user")
@Tag(name = "用户管理", description = "用户信息管理")
@RequiredArgsConstructor
@PreventDuplicateSubmit
public class UserController extends BaseController {

    private final UserService userService;
    private final UserWalletService userWalletService;


    /**
     * 获取用户信息
     *
     * @return 用户信息
     */
    @GetMapping("/profile")
    @Operation(summary = "获取用户信息")
    public AjaxResult<UserProfileDto> getUserProfile() {
        UserProfileDto userProfileDto = userService.getUserProfile();
        return success(userProfileDto);
    }

    /**
     * 更新用户信息
     *
     * @param userProfileDto 用户信息
     * @return 更新结果
     */
    @PutMapping("/profile")
    @Operation(summary = "更新用户信息")
    public AjaxResult<Void> updateUserProfile(@RequestBody UserProfileDto userProfileDto) {
        boolean result = userService.updateUserProfile(userProfileDto);
        return toAjax(result);
    }

    /**
     * 获取当前登录用户信息（包含角色集合）。
     *
     * @return 当前用户信息
     */
    @Operation(summary = "获取当前用户信息", description = "根据认证上下文返回当前登录用户信息")
    @GetMapping("/current")
    public AjaxResult<CurrentUserInfoVo> currentUser() {
        Long userId = SecurityUtils.getUserId();
        User user = userService.getUserById(userId);
        CurrentUserInfoVo vo = BeanCotyUtils.copyProperties(user, CurrentUserInfoVo.class);
        vo.setRoles(userService.getUserRolesByUserId(userId));
        return success(vo);
    }

    /**
     * 获取用户简略信息
     * <p>
     * 用于个人中心页面展示，包括用户基本信息、钱包余额、订单统计等
     * </p>
     *
     * @return 用户简略信息
     */
    @GetMapping("/brief")
    @Operation(summary = "获取用户简略信息")
    public AjaxResult<UserBriefVo> getUserBriefInfo() {
        UserBriefVo userBriefVo = userService.getUserBriefInfo();
        return success(userBriefVo);
    }

    /**
     * 获取用户钱包余额
     *
     * @return 钱包余额
     */
    @GetMapping("/wallet/balance")
    @Operation(summary = "获取用户钱包余额")
    public AjaxResult<BigDecimal> getUserWalletBalance() {
        BigDecimal balance = userWalletService.getUserWalletBalance();
        return success(balance);
    }


    /**
     * 获取用户钱包流水
     *
     * @param request 查询参数
     * @return 流水列表
     */
    @GetMapping("/wallet/bill")
    @Operation(summary = "获取用户钱包流水")
    public AjaxResult<TableDataResult> getBillList(UserWalletBillRequest request) {
        Page<UserWalletLog> walletLogPage = userWalletService.getBillList(request);
        AtomicLong counter = new AtomicLong((walletLogPage.getCurrent() - 1) * walletLogPage.getSize() + 1);
        ArrayList<UserWalletBillVo> userWalletBillVos = new ArrayList<>();
        walletLogPage.getRecords().forEach(walletLog -> {
            userWalletBillVos.add(buildUserWalletBillVo(walletLog, counter.getAndIncrement()));
        });
        return getTableData(walletLogPage, userWalletBillVos);
    }

    /**
     * 获取当前用户钱包流水详情。
     *
     * @param billId 流水ID
     * @return 流水详情
     */
    @GetMapping("/wallet/bill/{billId}")
    @Operation(summary = "获取用户钱包流水详情")
    public AjaxResult<UserWalletBillDetailVo> getBillDetail(@PathVariable Long billId) {
        UserWalletLog walletLog = userWalletService.getBillDetail(billId);
        return success(buildUserWalletBillDetailVo(walletLog));
    }

    /**
     * 构建钱包流水列表视图对象。
     *
     * @param walletLog 钱包流水实体
     * @param index 流水索引
     * @return 钱包流水列表视图对象
     */
    private UserWalletBillVo buildUserWalletBillVo(UserWalletLog walletLog, Long index) {
        return UserWalletBillVo.builder()
                .id(walletLog.getId())
                .index(index)
                .isRecharge(walletLog.getChangeType() == 1)
                .title(walletLog.getReason())
                .amount(walletLog.getAmount())
                .time(walletLog.getCreatedAt())
                .build();
    }

    /**
     * 构建钱包流水详情视图对象。
     *
     * @param walletLog 钱包流水实体
     * @return 钱包流水详情视图对象
     */
    private UserWalletBillDetailVo buildUserWalletBillDetailVo(UserWalletLog walletLog) {
        return UserWalletBillDetailVo.builder()
                .id(walletLog.getId())
                .flowNo(walletLog.getFlowNo())
                .title(walletLog.getReason())
                .bizId(walletLog.getBizId())
                .changeType(walletLog.getChangeType())
                .amount(walletLog.getAmount())
                .beforeBalance(walletLog.getBeforeBalance())
                .afterBalance(walletLog.getAfterBalance())
                .remark(walletLog.getRemark())
                .time(walletLog.getCreatedAt())
                .build();
    }
}
