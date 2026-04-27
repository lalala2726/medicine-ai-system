package com.zhangyichuang.medicine.client.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangyichuang.medicine.client.model.request.OrderListRequest;
import com.zhangyichuang.medicine.client.model.vo.AssistantOrderListVo;
import com.zhangyichuang.medicine.client.model.vo.OrderListVo;
import com.zhangyichuang.medicine.client.service.MallOrderService;
import com.zhangyichuang.medicine.common.core.base.AjaxResult;
import com.zhangyichuang.medicine.common.core.base.TableDataResult;
import com.zhangyichuang.medicine.common.security.base.BaseController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author Chuang
 * <p>
 * created on 2025/11/22
 */
@Slf4j
@RestController
@RequestMapping("/assistant")
@RequiredArgsConstructor
@Tag(name = "咨询管理", description = "咨询管理接口")
public class AssistantController extends BaseController {

    private final MallOrderService mallOrderService;


    /**
     * 获取订单列表
     *
     * @param request 订单列表请求参数
     * @return 订单列表
     */
    @GetMapping("/order/list")
    @Operation(summary = "获取订单列表", description = "获取订单列表")
    public AjaxResult<TableDataResult> getOrderList(OrderListRequest request) {
        Page<OrderListVo> orderList = mallOrderService.getOrderList(request);
        List<AssistantOrderListVo> assistantOrderListVos = copyListProperties(orderList, AssistantOrderListVo.class);
        return getTableData(orderList, assistantOrderListVos);
    }
}
