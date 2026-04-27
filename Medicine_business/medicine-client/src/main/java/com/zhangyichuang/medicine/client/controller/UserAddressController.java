package com.zhangyichuang.medicine.client.controller;

import com.zhangyichuang.medicine.client.model.request.UserAddressRequest;
import com.zhangyichuang.medicine.client.model.vo.UserAddressVo;
import com.zhangyichuang.medicine.client.service.UserAddressService;
import com.zhangyichuang.medicine.common.core.base.AjaxResult;
import com.zhangyichuang.medicine.common.core.utils.BeanCotyUtils;
import com.zhangyichuang.medicine.common.redis.annotation.PreventDuplicateSubmit;
import com.zhangyichuang.medicine.common.security.base.BaseController;
import com.zhangyichuang.medicine.model.entity.UserAddress;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户收货地址管理
 *
 * @author Chuang
 * created on 2025/11/13
 */
@Slf4j
@RestController
@RequestMapping("/user/address")
@Tag(name = "用户收货地址管理", description = "用户收货地址管理接口")
@RequiredArgsConstructor
@PreventDuplicateSubmit
public class UserAddressController extends BaseController {

    private final UserAddressService userAddressService;

    /**
     * 获取地址列表
     *
     * @return 地址列表
     */
    @GetMapping("/list")
    @Operation(summary = "获取地址列表", description = "获取当前用户的所有收货地址，按默认地址和更新时间排序")
    public AjaxResult<List<UserAddressVo>> getAddressList() {
        List<UserAddress> addressList = userAddressService.getUserAddressList();
        List<UserAddressVo> voList = addressList.stream()
                .map(address -> BeanCotyUtils.copyProperties(address, UserAddressVo.class))
                .collect(Collectors.toList());
        return success(voList);
    }

    /**
     * 获取地址详情
     *
     * @param id 地址ID
     * @return 地址详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取地址详情", description = "根据地址ID获取详细信息")
    public AjaxResult<UserAddressVo> getAddressById(
            @Parameter(description = "地址ID", required = true)
            @PathVariable("id") Long id) {
        UserAddress address = userAddressService.getAddressById(id);
        UserAddressVo vo = BeanCotyUtils.copyProperties(address, UserAddressVo.class);
        return success(vo);
    }

    /**
     * 新增地址
     *
     * @param request 地址信息
     * @return 操作结果
     */
    @PostMapping
    @Operation(summary = "新增地址", description = "添加新的收货地址，最多支持20个地址")
    public AjaxResult<Void> addAddress(@Validated @RequestBody UserAddressRequest request) {
        UserAddress address = BeanCotyUtils.copyProperties(request, UserAddress.class);
        boolean result = userAddressService.addAddress(address);
        return toAjax(result);
    }

    /**
     * 更新地址
     *
     * @param request 地址信息
     * @return 操作结果
     */
    @PutMapping
    @Operation(summary = "更新地址", description = "修改已有的收货地址信息")
    public AjaxResult<Void> updateAddress(@Validated @RequestBody UserAddressRequest request) {
        UserAddress address = BeanCotyUtils.copyProperties(request, UserAddress.class);
        boolean result = userAddressService.updateAddress(address);
        return toAjax(result);
    }

    /**
     * 删除地址
     *
     * @param id 地址ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除地址", description = "删除指定的收货地址")
    public AjaxResult<Void> deleteAddress(
            @Parameter(description = "地址ID", required = true)
            @PathVariable("id") Long id) {
        boolean result = userAddressService.deleteAddress(id);
        return toAjax(result);
    }

    /**
     * 设置默认地址
     *
     * @param id 地址ID
     * @return 操作结果
     */
    @PutMapping("/default/{id}")
    @Operation(summary = "设置默认地址", description = "将指定地址设为默认收货地址，自动取消其他默认地址")
    public AjaxResult<Void> setDefaultAddress(
            @Parameter(description = "地址ID", required = true)
            @PathVariable("id") Long id) {
        boolean result = userAddressService.setDefaultAddress(id);
        return toAjax(result);
    }

    /**
     * 获取默认地址
     *
     * @return 默认地址
     */
    @GetMapping("/default")
    @Operation(summary = "获取默认地址", description = "获取当前用户的默认收货地址")
    public AjaxResult<UserAddressVo> getDefaultAddress() {
        UserAddress address = userAddressService.getDefaultAddress();
        if (address == null) {
            return success(null);
        }
        UserAddressVo vo = BeanCotyUtils.copyProperties(address, UserAddressVo.class);
        return success(vo);
    }
}

