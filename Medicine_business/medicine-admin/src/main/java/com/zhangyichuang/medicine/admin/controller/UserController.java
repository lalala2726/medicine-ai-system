package com.zhangyichuang.medicine.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.admin.model.request.*;
import com.zhangyichuang.medicine.admin.model.vo.*;
import com.zhangyichuang.medicine.admin.service.CouponActivationAdminService;
import com.zhangyichuang.medicine.admin.service.CouponAdminService;
import com.zhangyichuang.medicine.admin.service.UserService;
import com.zhangyichuang.medicine.common.core.base.AjaxResult;
import com.zhangyichuang.medicine.common.core.base.Option;
import com.zhangyichuang.medicine.common.core.base.PageRequest;
import com.zhangyichuang.medicine.common.core.base.TableDataResult;
import com.zhangyichuang.medicine.common.log.annotation.OperationLog;
import com.zhangyichuang.medicine.common.log.enums.OperationType;
import com.zhangyichuang.medicine.common.redis.annotation.PreventDuplicateSubmit;
import com.zhangyichuang.medicine.common.security.base.BaseController;
import com.zhangyichuang.medicine.common.security.utils.SecurityUtils;
import com.zhangyichuang.medicine.model.dto.*;
import com.zhangyichuang.medicine.model.request.UserAddRequest;
import com.zhangyichuang.medicine.model.request.UserListQueryRequest;
import com.zhangyichuang.medicine.model.request.UserUpdateRequest;
import com.zhangyichuang.medicine.model.vo.UserListVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端用户控制器。
 * <p>
 * 提供后台用户、钱包、消费统计等管理能力。
 * </p>
 */
@RestController
@RequestMapping("/system/user")
@RequiredArgsConstructor
@Tag(name = "用户接口", description = "提供用户的增删改查")
public class UserController extends BaseController {

    private final UserService userService;
    private final CouponAdminService couponAdminService;
    private final CouponActivationAdminService couponActivationAdminService;

    /**
     * 获取用户列表
     *
     * @param request 用户列表查询参数
     * @return 用户列表视图对象集合
     */
    @GetMapping("/list")
    @Operation(summary = "用户列表")
    @PreAuthorize("hasAuthority('system:user:list') or hasRole('super_admin')")
    public AjaxResult<TableDataResult> listUser(UserListQueryRequest request) {
        Page<UserListDto> userPage = userService.listUser(request);
        List<UserListVo> rows = copyListProperties(userPage.getRecords(), UserListVo.class);
        return getTableData(userPage, rows);
    }


    /**
     * 获取用户详情
     *
     * @param id 用户ID
     * @return 用户详情
     */
    @GetMapping("/{id:\\d+}/detail")
    @Operation(summary = "用户详情")
    @PreAuthorize("hasAuthority('system:user:query') or hasRole('super_admin')")
    public AjaxResult<UserDetailVo> getUserById(@PathVariable Long id) {
        UserDetailDto userDetailDto = userService.getUserDetailById(id);
        return success(toUserDetailVo(userDetailDto));
    }

    /**
     * 获取用户钱包流水
     */
    @GetMapping("/{userId:\\d+}/wallet-flow")
    @Operation(summary = "获取用户钱包流水")
    @PreAuthorize("hasAuthority('system:user:query') or hasRole('super_admin')")
    public AjaxResult<TableDataResult> getUserWalletFlow(@PathVariable Long userId, PageRequest request) {
        Page<UserWalletFlowDto> walletFlowDtoPage = userService.getUserWalletFlow(userId, request);
        List<UserWalletFlowInfoVo> rows = copyListProperties(walletFlowDtoPage.getRecords(), UserWalletFlowInfoVo.class);
        return getTableData(walletFlowDtoPage, rows);
    }

    /**
     * 获取消费信息
     */
    @GetMapping("/{userId:\\d+}/consume-info")
    @Operation(summary = "获取消费信息")
    @PreAuthorize("hasAuthority('system:user:query') or hasRole('super_admin')")
    public AjaxResult<TableDataResult> getConsumeInfo(@PathVariable Long userId, PageRequest request) {
        Page<UserConsumeInfoDto> consumeInfoDtoPage = userService.getConsumeInfo(userId, request);
        List<UserConsumeInfo> rows = copyListProperties(consumeInfoDtoPage.getRecords(), UserConsumeInfo.class);
        return getTableData(consumeInfoDtoPage, rows);
    }

    /**
     * 获取指定用户优惠券列表。
     *
     * @param userId  用户ID
     * @param request 查询参数
     * @return 当前用户优惠券列表
     */
    @GetMapping("/{userId:\\d+}/coupons")
    @Operation(summary = "获取指定用户优惠券列表")
    @PreAuthorize("hasAuthority('system:user:query') or hasRole('super_admin')")
    public AjaxResult<TableDataResult> getUserCoupons(@PathVariable Long userId, AdminUserCouponListRequest request) {
        request.setUserId(userId);
        Page<AdminUserCouponVo> couponPage = couponAdminService.listUserCoupons(request);
        return getTableData(couponPage, couponPage.getRecords());
    }

    /**
     * 获取指定用户优惠券日志列表。
     *
     * @param userId  用户ID
     * @param request 查询参数
     * @return 当前用户优惠券日志列表
     */
    @GetMapping("/{userId:\\d+}/coupon-logs")
    @Operation(summary = "获取指定用户优惠券日志列表")
    @PreAuthorize("hasAuthority('system:user:query') or hasRole('super_admin')")
    public AjaxResult<TableDataResult> getUserCouponLogs(@PathVariable Long userId, CouponLogListRequest request) {
        Page<CouponLogVo> logPage = couponAdminService.listUserCouponLogs(userId, request);
        return getTableData(logPage, logPage.getRecords());
    }

    /**
     * 获取指定用户激活码日志列表。
     *
     * @param userId  用户ID
     * @param request 查询参数
     * @return 当前用户激活码日志列表
     */
    @GetMapping("/{userId:\\d+}/activation-logs")
    @Operation(summary = "获取指定用户激活码日志列表")
    @PreAuthorize("hasAuthority('system:user:query') or hasRole('super_admin')")
    public AjaxResult<TableDataResult> getUserActivationLogs(@PathVariable Long userId, ActivationLogListRequest request) {
        Page<ActivationLogVo> logPage = couponActivationAdminService.listUserActivationLogs(userId, request);
        return getTableData(logPage, logPage.getRecords());
    }

    /**
     * 删除指定用户优惠券。
     *
     * @param userId   用户ID
     * @param couponId 用户优惠券ID
     * @return 删除结果
     */
    @DeleteMapping("/{userId:\\d+}/coupons/{couponId:\\d+}")
    @Operation(summary = "删除指定用户优惠券")
    @PreAuthorize("hasAuthority('system:user:update') or hasRole('super_admin')")
    @PreventDuplicateSubmit
    @OperationLog(module = "用户管理", action = "删除用户优惠券", type = OperationType.DELETE)
    public AjaxResult<Void> deleteUserCoupon(@PathVariable Long userId, @PathVariable Long couponId) {
        boolean result = couponAdminService.deleteUserCoupon(userId, couponId, SecurityUtils.getUsername());
        return toAjax(result);
    }

    /**
     * 添加用户
     *
     * @param request 添加用户参数
     * @return 添加结果
     */
    @PostMapping
    @Operation(summary = "添加用户")
    @PreAuthorize("hasAuthority('system:user:add') or hasRole('super_admin')")
    @PreventDuplicateSubmit
    @OperationLog(module = "用户管理", action = "新增用户", type = OperationType.ADD)
    public AjaxResult<Void> addUser(@RequestBody UserAddRequest request) {
        boolean result = userService.addUser(request);
        return toAjax(result);
    }

    /**
     * 修改用户
     *
     * @param request 修改用户参数
     * @return 修改结果
     */
    @PutMapping
    @Operation(summary = "修改用户")
    @PreAuthorize("hasAuthority('system:user:update') or hasRole('super_admin')")
    @PreventDuplicateSubmit
    @OperationLog(module = "用户管理", action = "修改用户", type = OperationType.UPDATE)
    public AjaxResult<Void> updateUser(@RequestBody UserUpdateRequest request) {
        boolean result = userService.updateUser(request);
        return toAjax(result);
    }

    /**
     * 删除用户
     *
     * @param ids 用户ID
     * @return 删除结果
     */
    @DeleteMapping("/{ids}")
    @Operation(summary = "删除用户")
    @PreAuthorize("hasAuthority('system:user:delete') or hasRole('super_admin')")
    @PreventDuplicateSubmit
    @OperationLog(module = "用户管理", action = "删除用户", type = OperationType.DELETE)
    public AjaxResult<Void> deleteUser(@PathVariable List<Long> ids) {
        boolean result = userService.deleteUser(ids);
        return toAjax(result);
    }

    /**
     * 获取用户钱包金额
     *
     * @param userId 用户ID
     * @return 钱包金额
     */
    @GetMapping("/{userId:\\d+}/wallet")
    @Operation(summary = "获取用户钱包金额")
    @PreAuthorize("hasAuthority('system:user:query') or hasRole('super_admin')")
    public AjaxResult<UserWalletVo> getUserWalletBalance(@PathVariable Long userId) {
        UserWalletDto userWalletDto = userService.getUserWallet(userId);
        return success(toUserWalletVo(userWalletDto));
    }


    /**
     * 开通用户钱包
     *
     * @param request 开通用户钱包参数
     * @return 开通结果
     */
    @PostMapping("/wallet/open")
    @Operation(summary = "开通用户钱包")
    @PreAuthorize("hasAuthority('system:user:update') or hasRole('super_admin')")
    @PreventDuplicateSubmit
    @OperationLog(module = "用户管理", action = "开通用户钱包", type = OperationType.UPDATE)
    public AjaxResult<Void> openUserWallet(@Validated @RequestBody OpenUserWalletRequest request) {
        boolean result = userService.openUserWallet(request);
        return toAjax(result);
    }

    /**
     * 冻结用户钱包
     *
     * @param request 冻结用户钱包参数
     * @return 关闭结果
     */
    @PostMapping("/wallet/freeze")
    @Operation(summary = "冻结用户钱包")
    @PreAuthorize("hasAuthority('system:user:update') or hasRole('super_admin')")
    @PreventDuplicateSubmit
    @OperationLog(module = "用户管理", action = "冻结用户钱包", type = OperationType.UPDATE)
    public AjaxResult<Void> freezeUserWallet(@Validated @RequestBody FreezeOrUnUserWalletRequest request) {
        boolean result = userService.freezeUserWallet(request);
        return toAjax(result);
    }

    /**
     * 解冻用户钱包
     *
     * @param request 解冻用户钱包参数
     * @return 解冻结果
     */
    @PostMapping("/wallet/unfreeze")
    @Operation(summary = "解冻用户钱包")
    @PreAuthorize("hasAuthority('system:user:update') or hasRole('super_admin')")
    @PreventDuplicateSubmit
    @OperationLog(module = "用户管理", action = "解冻用户钱包", type = OperationType.UPDATE)
    public AjaxResult<Void> unfreezeUserWallet(@Validated @RequestBody FreezeOrUnUserWalletRequest request) {
        boolean result = userService.unfreezeUserWallet(request);
        return toAjax(result);
    }


    /**
     * 钱包充值/扣款
     *
     * @param request 钱包充值/扣款
     * @return 钱包充值/扣款
     */
    @PostMapping("/wallet/change")
    @Operation(summary = "钱包充值/扣款")
    @PreAuthorize("hasAuthority('system:user:update') or hasRole('super_admin')")
    @PreventDuplicateSubmit
    @OperationLog(module = "用户管理", action = "钱包余额变更", type = OperationType.UPDATE)
    public AjaxResult<Void> rechargeUserWallet(@Validated @RequestBody WalletChangeRequest request) {
        boolean result = userService.walletAmountChange(request);
        return toAjax(result);
    }

    /**
     * 批量获取用户ID与用户名映射
     *
     * @param userIds 用户ID列表
     * @return 用户选项列表
     */
    @PostMapping("/options")
    @Operation(summary = "批量获取用户选项")
    @PreAuthorize("hasAuthority('system:user:query') or hasRole('super_admin')")
    public AjaxResult<List<Option<Long>>> listUserOptions(@RequestBody List<Long> userIds) {
        List<Option<Long>> options = userService.listUserOptionsByIds(userIds);
        return success(options);
    }

    private UserDetailVo toUserDetailVo(UserDetailDto source) {
        if (source == null) {
            return null;
        }
        UserDetailVo target = copyProperties(source, UserDetailVo.class);
        target.setBasicInfo(copyProperties(source.getBasicInfo(), UserDetailVo.BasicInfo.class));
        target.setSecurityInfo(copyProperties(source.getSecurityInfo(), UserDetailVo.SecurityInfo.class));
        return target;
    }

    private UserWalletVo toUserWalletVo(UserWalletDto source) {
        return copyProperties(source, UserWalletVo.class);
    }

}
