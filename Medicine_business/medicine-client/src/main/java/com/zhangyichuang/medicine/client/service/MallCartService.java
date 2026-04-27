package com.zhangyichuang.medicine.client.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhangyichuang.medicine.client.model.request.UpdateCartQuantityRequest;
import com.zhangyichuang.medicine.client.model.vo.CartItemVo;
import com.zhangyichuang.medicine.model.entity.MallCart;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * @author Chuang
 */
@Validated
public interface MallCartService extends IService<MallCart> {

    /**
     * 添加商品到购物车
     *
     * @param productId 商品ID
     * @param quantity  添加数量，默认为1
     * @return 是否添加成功
     */
    boolean addProduct(Long productId, Integer quantity);

    /**
     * 添加商品到购物车（默认数量为1）
     *
     * @param productId 商品ID
     * @return 是否添加成功
     */
    default boolean addProduct(Long productId) {
        return addProduct(productId, 1);
    }

    /**
     * 获取用户购物车列表
     *
     * @return 购物车商品列表
     */
    List<CartItemVo> getCartList();

    /**
     * 删除购物车商品
     *
     * @param cartIds 购物车ID列表
     * @return 是否删除成功
     */
    boolean removeCartItems(List<Long> cartIds);

    /**
     * 更新购物车商品数量
     *
     * @param request 更新购物车商品数量请求
     * @return 是否更新成功
     */
    boolean updateCartQuantity(UpdateCartQuantityRequest request);

    /**
     * 获取购物车商品数量
     *
     * @return 购物车商品数量
     */
    Long getCartProductCount();

}
