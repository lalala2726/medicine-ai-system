package com.zhangyichuang.medicine.client.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhangyichuang.medicine.client.mapper.MallAfterSaleMapper;
import com.zhangyichuang.medicine.client.mapper.MallOrderMapper;
import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.common.security.utils.SecurityUtils;
import com.zhangyichuang.medicine.model.entity.MallAfterSale;
import com.zhangyichuang.medicine.model.entity.MallOrder;
import org.springframework.stereotype.Component;

/**
 * 客户端商城分布式锁业务键解析器。
 * <p>
 * 统一负责把控制层/服务层请求参数中的 ID 转换为真正用于加锁的业务主键，
 * 保证客户端与管理端命中的锁 key 完全一致。
 * </p>
 *
 * @author Chuang
 */
@Component("mallDistributedLockKeyResolver")
public class MallDistributedLockKeyResolver {

    /**
     * 订单 Mapper。
     */
    private final MallOrderMapper mallOrderMapper;

    /**
     * 售后 Mapper。
     */
    private final MallAfterSaleMapper mallAfterSaleMapper;

    /**
     * 构造客户端商城分布式锁业务键解析器。
     *
     * @param mallOrderMapper     订单 Mapper
     * @param mallAfterSaleMapper 售后 Mapper
     */
    public MallDistributedLockKeyResolver(MallOrderMapper mallOrderMapper,
                                          MallAfterSaleMapper mallAfterSaleMapper) {
        this.mallOrderMapper = mallOrderMapper;
        this.mallAfterSaleMapper = mallAfterSaleMapper;
    }

    /**
     * 解析当前登录用户 ID。
     *
     * @return 当前登录用户 ID 字符串
     */
    public String currentUserId() {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new ServiceException(ResponseCode.OPERATION_ERROR, "当前用户未登录");
        }
        return String.valueOf(userId);
    }

    /**
     * 根据订单 ID 解析订单号。
     *
     * @param orderId 订单 ID
     * @return 订单号
     */
    public String orderKeyById(Long orderId) {
        if (orderId == null) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "订单ID不能为空");
        }
        MallOrder order = mallOrderMapper.selectOne(new LambdaQueryWrapper<MallOrder>()
                .eq(MallOrder::getId, orderId)
                .select(MallOrder::getOrderNo));
        if (order == null || order.getOrderNo() == null || order.getOrderNo().isBlank()) {
            throw new ServiceException(ResponseCode.RESULT_IS_NULL, "订单不存在");
        }
        return order.getOrderNo();
    }

    /**
     * 根据售后申请 ID 解析售后单号。
     *
     * @param afterSaleId 售后申请 ID
     * @return 售后单号
     */
    public String afterSaleKeyById(Long afterSaleId) {
        if (afterSaleId == null) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "售后申请ID不能为空");
        }
        MallAfterSale afterSale = mallAfterSaleMapper.selectOne(new LambdaQueryWrapper<MallAfterSale>()
                .eq(MallAfterSale::getId, afterSaleId)
                .select(MallAfterSale::getAfterSaleNo));
        if (afterSale == null || afterSale.getAfterSaleNo() == null || afterSale.getAfterSaleNo().isBlank()) {
            throw new ServiceException(ResponseCode.RESULT_IS_NULL, "售后申请不存在");
        }
        return afterSale.getAfterSaleNo();
    }
}
