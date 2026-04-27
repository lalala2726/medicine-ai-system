package com.zhangyichuang.medicine.agent.controller;

import com.zhangyichuang.medicine.common.core.base.AjaxResult;
import com.zhangyichuang.medicine.common.security.base.BaseController;
import com.zhangyichuang.medicine.common.security.entity.AuthUser;
import com.zhangyichuang.medicine.common.security.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;

/**
 * 智能体认证授权控制器。
 * <p>
 * 提供智能体获取当前登录用户信息的接口，用于智能体调用时确认用户身份。
 *
 * @author Chuang
 * @since 2026/2/17
 */
@RestController
@RequestMapping("/agent/authorization")
@Tag(name = "认证授权接口", description = "认证授权接口")
public class AgentAuthorizationController extends BaseController {

    /**
     * 获取当前登录用户的认证信息。
     * <p>
     * 返回用户的认证相关信息，包括用户ID、用户名、角色、权限等，
     * 供智能体确认调用者身份使用。
     *
     * @return 用户认证信息
     */
    @GetMapping
    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的详细信息")
    public AjaxResult<HashMap<String, Object>> getCurrentUser() {
        AuthUser user = getLoginUser().getUser();
        HashMap<String, Object> userInfo = new HashMap<>();

        userInfo.put("user", user);
        userInfo.put("roles", SecurityUtils.getRoles());
        userInfo.put("permissions", SecurityUtils.getPermissions());
        return success(userInfo);
    }
}
