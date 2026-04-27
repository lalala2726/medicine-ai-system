package com.zhangyichuang.medicine.client.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhangyichuang.medicine.client.enums.ProductViewPeriod;
import com.zhangyichuang.medicine.client.model.dto.MallProductSearchResultDto;
import com.zhangyichuang.medicine.client.model.request.MallProductSearchRequest;
import com.zhangyichuang.medicine.client.model.vo.AssistantProductPurchaseCardsVo;
import com.zhangyichuang.medicine.client.model.vo.MallProductSearchTagFilterVo;
import com.zhangyichuang.medicine.client.model.vo.MallProductSearchVo;
import com.zhangyichuang.medicine.client.model.vo.MallProductVo;
import com.zhangyichuang.medicine.common.core.base.PageResult;
import com.zhangyichuang.medicine.model.dto.MallProductDetailDto;
import com.zhangyichuang.medicine.model.dto.MallProductWithImageDto;
import com.zhangyichuang.medicine.model.entity.MallProduct;
import com.zhangyichuang.medicine.model.vo.RecommendListVo;

import java.util.List;

/**
 * 商品服务接口（客户端）
 *
 * @author Chuang
 */
public interface MallProductService extends IService<MallProduct> {


    /**
     * 推荐商品
     *
     * @return 推荐结果
     */
    List<RecommendListVo> recommend();

    /**
     * 获取商品信息
     *
     * @param id 商品ID
     * @return 商品信息
     */
    MallProduct getMallProductById(Long id);

    /**
     * 获取商品详情（包含图片和药品详情）
     *
     * @param id 商品ID
     * @return 商品详情VO
     */
    MallProductVo getMallProductDetail(Long id);

    /**
     * 获取商品信息（包含图片）
     *
     * @param id 商品ID
     * @return 商品信息
     */
    MallProductWithImageDto getProductWithImagesById(Long id);

    /**
     * 获取聊天商品购买卡片商品信息。
     *
     * @param productIds 商品ID列表
     * @return 商品购买卡片数据
     */
    AssistantProductPurchaseCardsVo getAssistantProductPurchaseCards(List<Long> productIds);


    /**
     * 记录商品浏览：累计浏览次数并刷新热度排行榜
     *
     * @param productId 商品ID
     */
    void recordView(Long productId);

    /**
     * 查询商品浏览量
     *
     * @param productId 商品ID
     * @param period    统计周期（为空时按总量返回）
     * @return 浏览次数
     */
    long getViewCount(Long productId, ProductViewPeriod period);


    /**
     * 扣减库存
     *
     * @param productId 商品ID
     * @param quantity  数量
     */
    void deductStock(Long productId, Integer quantity);

    /**
     * 恢复库存
     *
     * @param productId 商品ID
     * @param quantity  数量
     */
    void restoreStock(Long productId, Integer quantity);

    /**
     * 搜索商品（名称/品牌/功效等）。
     *
     * @return 搜索结果
     */
    PageResult<MallProductSearchVo> search(MallProductSearchRequest request);

    /**
     * 搜索商品并返回动态筛选结果。
     *
     * @param request 搜索请求
     * @return 商品分页结果与动态筛选数据
     */
    MallProductSearchResultDto searchWithTagFilters(MallProductSearchRequest request);

    /**
     * 查询搜索筛选弹窗使用的全量标签分组。
     *
     * @return 按标签类型分组后的筛选标签列表
     */
    List<MallProductSearchTagFilterVo> listAllEnabledSearchTagFilters();

    /**
     * 搜索建议（商品名/分类名/通用名）。
     *
     * @param keyword 关键字
     * @return 建议列表
     */
    List<String> suggest(String keyword);

    /**
     * 获取商品详情（包含药品信息）
     *
     * @param id 商品ID
     * @return 商品详情
     */
    MallProductDetailDto getProductAndDrugInfoById(Long id);
}
