package com.zhangyichuang.medicine.admin.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhangyichuang.medicine.admin.mapper.MallAfterSaleMapper;
import com.zhangyichuang.medicine.admin.mapper.MallOrderMapper;
import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.model.entity.MallAfterSale;
import com.zhangyichuang.medicine.model.entity.MallOrder;
import org.springframework.stereotype.Component;

/**
 * 管理端商城分布式锁业务键解析器。
 * <p>
 * 统一把后台接口中的主键 ID 转换成订单号、售后单号等业务唯一键，
 * 保证后台与客户端获取的是同一把分布式锁。
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
     * 构造管理端商城分布式锁业务键解析器。
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
