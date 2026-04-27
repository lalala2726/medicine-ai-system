package com.zhangyichuang.medicine.client.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhangyichuang.medicine.client.mapper.MallProductViewHistoryMapper;
import com.zhangyichuang.medicine.client.model.request.ViewHistoryRequest;
import com.zhangyichuang.medicine.client.model.vo.ViewHistoryVo;
import com.zhangyichuang.medicine.client.service.MallOrderItemService;
import com.zhangyichuang.medicine.client.service.MallProductViewHistoryService;
import com.zhangyichuang.medicine.common.core.utils.Assert;
import com.zhangyichuang.medicine.model.entity.MallProductViewHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Chuang
 */
@Service
@RequiredArgsConstructor
public class MallProductViewHistoryServiceImpl extends ServiceImpl<MallProductViewHistoryMapper, MallProductViewHistory>
        implements MallProductViewHistoryService {

    private final MallOrderItemService mallOrderItemService;

    @Override
    public Page<ViewHistoryVo> listViewHistory(Long userId, ViewHistoryRequest request) {
        Assert.isPositive(userId, "用户ID不能为空");
        ViewHistoryRequest pageRequest = request == null ? new ViewHistoryRequest() : request;
        Page<ViewHistoryVo> page = pageRequest.toPage();
        Page<ViewHistoryVo> resultPage = baseMapper.listViewHistory(page, userId);

        if (resultPage.getRecords().isEmpty()) {
            return resultPage;
        }

        Map<Long, Integer> salesMap = mallOrderItemService.getCompletedSalesByProductIds(
                resultPage.getRecords().stream()
                        .map(ViewHistoryVo::getProductId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .collect(Collectors.toList()));

        resultPage.getRecords().forEach(item -> item.setSales(salesMap.getOrDefault(item.getProductId(), 0)));
        return resultPage;
    }

    @Override
    public void recordViewHistory(Long userId, Long productId) {
        Assert.isPositive(userId, "用户ID不能为空");
        Assert.isPositive(productId, "商品ID不能为空");

        MallProductViewHistory history = getViewHistory(userId, productId);
        Date now = new Date();

        if (history == null) {
            MallProductViewHistory newHistory = new MallProductViewHistory();
            newHistory.setUserId(userId);
            newHistory.setProductId(productId);
            newHistory.setViewCount(1);
            newHistory.setFirstViewTime(now);
            newHistory.setLastViewTime(now);
            save(newHistory);
            return;
        }

        int nextCount = history.getViewCount() == null ? 1 : history.getViewCount() + 1;
        lambdaUpdate()
                .eq(MallProductViewHistory::getUserId, userId)
                .eq(MallProductViewHistory::getProductId, productId)
                .set(MallProductViewHistory::getViewCount, nextCount)
                .set(MallProductViewHistory::getLastViewTime, now)
                .update();
    }

    @Override
    public void deleteViewHistory(Long userId, Long productId) {
        Assert.isPositive(userId, "用户ID不能为空");
        Assert.isPositive(productId, "商品ID不能为空");
        lambdaUpdate()
                .eq(MallProductViewHistory::getUserId, userId)
                .eq(MallProductViewHistory::getProductId, productId)
                .remove();
    }

    @Override
    public void deleteAllViewHistory(Long userId) {
        Assert.isPositive(userId, "用户ID不能为空");
        lambdaUpdate()
                .eq(MallProductViewHistory::getUserId, userId)
                .remove();
    }

    @Override
    public MallProductViewHistory getViewHistory(Long userId, Long productId) {
        Assert.isPositive(userId, "用户ID不能为空");
        Assert.isPositive(productId, "商品ID不能为空");
        return lambdaQuery()
                .eq(MallProductViewHistory::getUserId, userId)
                .eq(MallProductViewHistory::getProductId, productId)
                .one();
    }
}
