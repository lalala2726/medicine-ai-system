package com.zhangyichuang.medicine.agent.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.agent.model.vo.admin.*;
import com.zhangyichuang.medicine.agent.service.UserService;
import com.zhangyichuang.medicine.common.core.base.AjaxResult;
import com.zhangyichuang.medicine.common.core.base.PageRequest;
import com.zhangyichuang.medicine.common.core.base.TableDataResult;
import com.zhangyichuang.medicine.common.security.base.BaseController;
import com.zhangyichuang.medicine.model.dto.UserConsumeInfoDto;
import com.zhangyichuang.medicine.model.dto.UserContextDto;
import com.zhangyichuang.medicine.model.dto.UserListDto;
import com.zhangyichuang.medicine.model.dto.UserWalletFlowDto;
import com.zhangyichuang.medicine.model.request.UserListQueryRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 管理端智能体用户工具控制器。
 * <p>
 * 提供给管理端智能体使用的用户查询工具接口，
 * 需要具备用户查询权限或超级管理员角色才能访问。
 *
 * @author Chuang
 */
@RestController
@RequestMapping("/agent/admin/user")
@Tag(name = "管理端智能体用户工具", description = "用于管理端智能体用户查询接口")
@RequiredArgsConstructor
public class AgentUserController extends BaseController {


    private final UserService userService;

    /**
     * 分页查询用户列表。
     *
     * @param request 用户查询参数
     * @return 用户分页列表
     */
    @GetMapping("/list")
    @Operation(summary = "用户列表", description = "分页查询用户列表")
    @PreAuthorize("hasAuthority('system:user:list') or hasRole('super_admin')")
    public AjaxResult<TableDataResult> listUsers(UserListQueryRequest request) {
        Page<UserListDto> userPage = userService.listUsers(request);
        List<AgentUserListVo> rows = copyListProperties(userPage, AgentUserListVo.class);
        return getTableData(userPage, rows);
    }

    /**
     * 批量查询用户聚合上下文。
     * <p>
     * 用于智能体优先判断用户账号、钱包和风险状态，
     * 避免默认连续调用用户详情和钱包细粒度接口。
     *
     * @param userIds 用户 ID 列表
     * @return 按用户 ID 分组的用户聚合上下文
     */
    @GetMapping("/context/{userIds}")
    @Operation(summary = "用户聚合上下文", description = "根据用户ID批量获取智能体用户聚合上下文")
    @PreAuthorize("hasAuthority('system:user:query') or hasRole('super_admin')")
    public AjaxResult<Map<Long, UserContextDto>> getUserContext(@PathVariable List<Long> userIds) {
        return success(userService.getUserContextsByIds(userIds));
    }

    /**
     * 批量查询用户详情。
     *
     * @param userIds 用户 ID 列表
     * @return 用户详情列表
     */
    @GetMapping("/{userIds}/detail")
    @Operation(summary = "用户详情", description = "根据用户ID批量获取用户详情")
    @PreAuthorize("hasAuthority('system:user:query') or hasRole('super_admin')")
    public AjaxResult<List<UserDetailVo>> getUserDetail(@PathVariable List<Long> userIds) {
        List<UserDetailVo> userDetailVos = copyListProperties(userService.getUserDetailsByIds(userIds), UserDetailVo.class);
        return success(userDetailVos);
    }

    /**
     * 批量查询用户钱包信息。
     *
     * @param userIds 用户 ID 列表
     * @return 用户钱包信息列表
     */
    @GetMapping("/{userIds}/wallet")
    @Operation(summary = "用户钱包", description = "根据用户ID批量获取钱包余额与状态")
    @PreAuthorize("hasAuthority('system:user:query') or hasRole('super_admin')")
    public AjaxResult<List<UserWalletVo>> getUserWallet(@PathVariable List<Long> userIds) {
        List<UserWalletVo> userWalletVos = copyListProperties(userService.getUserWalletsByUserIds(userIds), UserWalletVo.class);
        return success(userWalletVos);
    }

    /**
     * 分页查询用户钱包流水。
     *
     * @param userId  用户ID
     * @param request 分页参数
     * @return 钱包流水分页列表
     */
    @GetMapping("/{userId:\\d+}/wallet_flow")
    @Operation(summary = "用户钱包流水", description = "分页查询用户钱包流水")
    @PreAuthorize("hasAuthority('system:user:query') or hasRole('super_admin')")
    public AjaxResult<TableDataResult> getUserWalletFlow(@PathVariable Long userId, PageRequest request) {
        Page<UserWalletFlowDto> walletFlowDtoPage = userService.getUserWalletFlow(userId, request);
        List<UserWalletFlowVo> rows = copyListProperties(walletFlowDtoPage, UserWalletFlowVo.class);
        return getTableData(walletFlowDtoPage, rows);
    }

    /**
     * 分页查询用户消费信息。
     *
     * @param userId  用户ID
     * @param request 分页参数
     * @return 用户消费分页列表
     */
    @GetMapping("/{userId:\\d+}/consume_info")
    @Operation(summary = "用户消费信息", description = "分页查询用户消费信息")
    @PreAuthorize("hasAuthority('system:user:query') or hasRole('super_admin')")
    public AjaxResult<TableDataResult> getConsumeInfo(@PathVariable Long userId, PageRequest request) {
        Page<UserConsumeInfoDto> consumeInfoDtoPage = userService.getConsumeInfo(userId, request);
        List<UserConsumeInfoVo> rows = copyListProperties(consumeInfoDtoPage.getRecords(), UserConsumeInfoVo.class);
        return getTableData(consumeInfoDtoPage, rows);
    }
}
