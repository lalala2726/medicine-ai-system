package com.zhangyichuang.medicine.admin.service.impl;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.zhangyichuang.medicine.admin.mapper.MallOrderMapper;
import com.zhangyichuang.medicine.admin.mapper.UserMapper;
import com.zhangyichuang.medicine.admin.service.*;
import com.zhangyichuang.medicine.model.dto.OrderDetailDto;
import com.zhangyichuang.medicine.model.entity.MallOrder;
import com.zhangyichuang.medicine.model.entity.MallOrderItem;
import com.zhangyichuang.medicine.model.entity.User;
import com.zhangyichuang.medicine.model.enums.DeliveryTypeEnum;
import com.zhangyichuang.medicine.model.enums.PayTypeEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MallOrderServiceImplGetOrderDetailByIdsTests {

    @Mock
    private MallOrderMapper mallOrderMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private MallOrderItemService mallOrderItemService;

    @Mock
    private MallProductImageService mallProductImageService;

    @Mock
    private MallOrderTimelineService mallOrderTimelineService;

    @Mock
    private UserWalletService userWalletService;

    @Mock
    private MallOrderShippingService mallOrderShippingService;

    @Mock
    private MallInventoryService mallInventoryService;

    @Spy
    @InjectMocks
    private MallOrderServiceImpl mallOrderService;

    @Test
    void getOrderDetailByIds_WhenInputIsNull_ShouldReturnEmptyList() {
        List<OrderDetailDto> result = mallOrderService.getOrderDetailByIds(null);

        assertTrue(result.isEmpty());
    }

    @Test
    void getOrderDetailByIds_WhenInputIsEmpty_ShouldReturnEmptyList() {
        List<OrderDetailDto> result = mallOrderService.getOrderDetailByIds(Collections.emptyList());

        assertTrue(result.isEmpty());
    }

    @Test
    void getOrderDetailByIds_WhenOrdersNotFound_ShouldReturnEmptyList() {
        doReturn(Collections.emptyList()).when(mallOrderService).listByIds(any());

        List<OrderDetailDto> result = mallOrderService.getOrderDetailByIds(List.of(1L, 2L));

        assertTrue(result.isEmpty());
    }

    @Test
    void getOrderDetailByIds_WhenSingleOrder_ShouldReturnOrderDetail() {
        // 准备订单数据
        MallOrder order = createMockOrder(1L, 100L, "ORDER001");
        doReturn(List.of(order)).when(mallOrderService).listByIds(anyList());

        // 准备用户数据
        User user = createMockUser(100L, "testuser", "13800138000");
        when(userMapper.selectByIds(anyList())).thenReturn(List.of(user));

        // 准备订单商品数据
        MallOrderItem orderItem = createMockOrderItem(1L, 1L, "商品1");
        mockOrderItemService(List.of(orderItem));

        List<OrderDetailDto> result = mallOrderService.getOrderDetailByIds(List.of(1L));

        assertEquals(1, result.size());
        OrderDetailDto detail = result.getFirst();

        // 验证订单信息
        assertNotNull(detail.getOrderInfo());
        assertEquals("ORDER001", detail.getOrderInfo().getOrderNo());
        assertEquals(new BigDecimal("100.00"), detail.getOrderInfo().getTotalAmount());

        // 验证用户信息
        assertNotNull(detail.getUserInfo());
        assertEquals("100", detail.getUserInfo().getUserId());
        assertEquals("testuser", detail.getUserInfo().getNickname());

        // 验证配送信息
        assertNotNull(detail.getDeliveryInfo());
        assertEquals("张三", detail.getDeliveryInfo().getReceiverName());

        // 验证商品信息
        assertNotNull(detail.getProductInfo());
        assertEquals(1, detail.getProductInfo().size());
        assertEquals("商品1", detail.getProductInfo().getFirst().getProductName());
    }

    @Test
    void getOrderDetailByIds_WhenMultipleOrders_ShouldReturnAllOrderDetails() {
        // 准备多个订单
        MallOrder order1 = createMockOrder(1L, 100L, "ORDER001");
        MallOrder order2 = createMockOrder(2L, 101L, "ORDER002");
        doReturn(List.of(order1, order2)).when(mallOrderService).listByIds(anyList());

        // 准备用户数据
        User user1 = createMockUser(100L, "user1", "13800138001");
        User user2 = createMockUser(101L, "user2", "13800138002");
        when(userMapper.selectByIds(anyList())).thenReturn(List.of(user1, user2));

        // 准备订单商品数据
        MallOrderItem item1 = createMockOrderItem(1L, 1L, "商品1");
        MallOrderItem item2 = createMockOrderItem(2L, 2L, "商品2");
        mockOrderItemService(List.of(item1, item2));

        List<OrderDetailDto> result = mallOrderService.getOrderDetailByIds(List.of(1L, 2L));

        assertEquals(2, result.size());
    }

    @Test
    void getOrderDetailByIds_WhenUserNotFound_ShouldStillReturnOrderWithNullUserInfo() {
        MallOrder order = createMockOrder(1L, 100L, "ORDER001");
        doReturn(List.of(order)).when(mallOrderService).listByIds(anyList());

        // 用户不存在
        when(userMapper.selectByIds(anyList())).thenReturn(Collections.emptyList());

        MallOrderItem orderItem = createMockOrderItem(1L, 1L, "商品1");
        mockOrderItemService(List.of(orderItem));

        List<OrderDetailDto> result = mallOrderService.getOrderDetailByIds(List.of(1L));

        assertEquals(1, result.size());
        assertNull(result.getFirst().getUserInfo());
        // 订单信息和配送信息应该仍然存在
        assertNotNull(result.getFirst().getOrderInfo());
        assertNotNull(result.getFirst().getDeliveryInfo());
    }

    @Test
    void getOrderDetailByIds_WhenNoOrderItems_ShouldReturnOrderWithEmptyProductInfo() {
        MallOrder order = createMockOrder(1L, 100L, "ORDER001");
        doReturn(List.of(order)).when(mallOrderService).listByIds(anyList());

        User user = createMockUser(100L, "testuser", "13800138000");
        when(userMapper.selectByIds(anyList())).thenReturn(List.of(user));

        // 没有订单商品
        mockOrderItemService(Collections.emptyList());

        List<OrderDetailDto> result = mallOrderService.getOrderDetailByIds(List.of(1L));

        assertEquals(1, result.size());
        assertNotNull(result.getFirst().getProductInfo());
        assertTrue(result.getFirst().getProductInfo().isEmpty());
    }

    // ==================== Helper Methods ====================

    private MallOrder createMockOrder(Long orderId, Long userId, String orderNo) {
        MallOrder order = new MallOrder();
        order.setId(orderId);
        order.setUserId(userId);
        order.setOrderNo(orderNo);
        order.setOrderStatus("PENDING_PAYMENT");
        order.setPayType(PayTypeEnum.WALLET.getType());
        order.setDeliveryType(DeliveryTypeEnum.EXPRESS.getType());
        order.setTotalAmount(new BigDecimal("100.00"));
        order.setPayAmount(new BigDecimal("100.00"));
        order.setFreightAmount(new BigDecimal("10.00"));
        order.setReceiverName("张三");
        order.setReceiverPhone("13800138000");
        order.setReceiverDetail("北京市朝阳区");
        return order;
    }

    private User createMockUser(Long userId, String nickname, String phone) {
        User user = new User();
        user.setId(userId);
        user.setNickname(nickname);
        user.setPhoneNumber(phone);
        return user;
    }

    private MallOrderItem createMockOrderItem(Long orderId, Long productId, String productName) {
        MallOrderItem item = new MallOrderItem();
        item.setOrderId(orderId);
        item.setProductId(productId);
        item.setProductName(productName);
        item.setImageUrl("http://example.com/image.jpg");
        item.setPrice(new BigDecimal("50.00"));
        item.setQuantity(2);
        item.setTotalPrice(new BigDecimal("100.00"));
        return item;
    }

    @SuppressWarnings("unchecked")
    private void mockOrderItemService(List<MallOrderItem> items) {
        LambdaQueryChainWrapper<MallOrderItem> mockWrapper = mock(LambdaQueryChainWrapper.class);
        when(mallOrderItemService.lambdaQuery()).thenReturn(mockWrapper);
        when(mockWrapper.in(any(), anyList())).thenReturn(mockWrapper);
        when(mockWrapper.list()).thenReturn(items);
    }
}
