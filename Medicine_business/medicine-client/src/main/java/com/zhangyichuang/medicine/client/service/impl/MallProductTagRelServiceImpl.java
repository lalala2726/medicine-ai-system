package com.zhangyichuang.medicine.client.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhangyichuang.medicine.client.mapper.MallProductTagRelMapper;
import com.zhangyichuang.medicine.client.service.MallProductTagRelService;
import com.zhangyichuang.medicine.model.entity.MallProductTagRel;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 商品标签关联服务实现（客户端）。
 *
 * @author Chuang
 */
@Service
public class MallProductTagRelServiceImpl extends ServiceImpl<MallProductTagRelMapper, MallProductTagRel>
        implements MallProductTagRelService {

    /**
     * 按商品ID列表查询标签关联。
     *
     * @param productIds 商品ID列表
     * @return 标签关联列表
     */
    @Override
    public List<MallProductTagRel> listByProductIds(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        return lambdaQuery()
                .in(MallProductTagRel::getProductId, productIds)
                .list();
    }
}
