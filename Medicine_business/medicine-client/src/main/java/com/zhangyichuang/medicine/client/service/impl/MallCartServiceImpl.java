package com.zhangyichuang.medicine.client.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhangyichuang.medicine.client.mapper.MallCartMapper;
import com.zhangyichuang.medicine.client.model.request.UpdateCartQuantityRequest;
import com.zhangyichuang.medicine.client.model.vo.CartItemVo;
import com.zhangyichuang.medicine.client.service.MallCartService;
import com.zhangyichuang.medicine.client.service.MallProductImageService;
import com.zhangyichuang.medicine.client.service.MallProductService;
import com.zhangyichuang.medicine.common.core.enums.ResponseCode;
import com.zhangyichuang.medicine.common.core.exception.ServiceException;
import com.zhangyichuang.medicine.common.security.base.BaseService;
import com.zhangyichuang.medicine.model.entity.MallCart;
import com.zhangyichuang.medicine.model.entity.MallProduct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @author Chuang
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MallCartServiceImpl extends ServiceImpl<MallCartMapper, MallCart>
        implements MallCartService, BaseService {

    private final MallProductService mallProductService;
    private final MallProductImageService mallProductImageService;

    /**
     * 添加商品到购物车（带数量参数）
     *
     * @param productId 商品ID
     * @param quantity  添加数量
     * @return 添加结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addProduct(Long productId, Integer quantity) {
        // 参数验证
        if (productId == null || productId <= 0) {
            throw new ServiceException(ResponseCode.PARAM_ERROR, "商品ID无效");
        }
        if (quantity == null || quantity <= 0) {
            log.warn("用户{}尝试添加无效数量{}的商品{}", getUserId(), quantity, productId);
            throw new ServiceException(ResponseCode.PARAM_ERROR, "添加数量必须大于0");
        }

        Long currentUserId = getUserId();
        log.info("用户{}开始添加商品{}到购物车，数量：{}", currentUserId, productId, quantity);

        try {
            // 验证商品信息（只验证商品存在和状态，不扣减库存）
            MallProduct mallProduct = validateProduct(productId);

            // 执行购物车操作
            boolean result = addToCartOperation(currentUserId, productId, quantity, mallProduct);

            if (result) {
                log.info("用户{}成功添加商品{}到购物车，数量：{}", currentUserId, productId, quantity);
            } else {
                log.error("用户{}添加商品{}到购物车失败", currentUserId, productId);
            }

            return result;
        } catch (Exception e) {
            log.error("用户{}添加商品{}到购物车时发生异常：{}", currentUserId, productId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 验证商品信息
     */
    private MallProduct validateProduct(Long productId) {
        MallProduct mallProduct = mallProductService.getMallProductById(productId);
        if (mallProduct == null) {
            log.warn("商品{}不存在", productId);
            throw new ServiceException(ResponseCode.RESULT_IS_NULL, "商品不存在");
        }

        // 1代表在售
        if (mallProduct.getStatus() != 1) {
            log.warn("商品{}已下架", productId);
            throw new ServiceException(ResponseCode.RESULT_IS_NULL, "商品已下架");
        }

        return mallProduct;
    }

    /**
     * 添加购物车操作
     *
     * @param userId      用户ID
     * @param productId   商品ID
     * @param quantity    数量
     * @param mallProduct 商品信息
     * @return 添加结果
     */
    private boolean addToCartOperation(Long userId, Long productId, Integer quantity, MallProduct mallProduct) {
        // 获取商品封面图片
        String productCoverImage = getProductCoverImage(productId);

        // 查询现有购物车项
        MallCart existingCart = lambdaQuery()
                .eq(MallCart::getUserId, userId)
                .eq(MallCart::getProductId, productId)
                .one();

        if (existingCart == null) {
            // 创建新的购物车项
            return createNewCartItem(userId, productId, quantity, mallProduct, productCoverImage);
        } else {
            // 更新现有购物车项数量
            return updateExistingCartItem(existingCart, quantity, mallProduct);
        }
    }

    /**
     * 创建新的购物车项
     */
    private boolean createNewCartItem(Long userId, Long productId, Integer quantity,
                                      MallProduct mallProduct, String productCoverImage) {
        MallCart newCart = MallCart.builder()
                .userId(userId)
                .productId(productId)
                .productName(mallProduct.getName())
                .productImage(productCoverImage)
                .cartNum(quantity)
                .createTime(new Date())
                .updateTime(new Date())
                .build();

        boolean result = save(newCart);
        if (result) {
            log.info("为用户{}创建新的购物车项，商品：{}，数量：{}", userId, mallProduct.getName(), quantity);
        }
        return result;
    }

    /**
     * 更新现有购物车项数量
     */
    private boolean updateExistingCartItem(MallCart existingCart, Integer additionalQuantity,
                                           MallProduct mallProduct) {
        boolean updateResult = lambdaUpdate()
                .eq(MallCart::getId, existingCart.getId())
                .set(MallCart::getUpdateTime, new Date())
                .set(MallCart::getCartNum, existingCart.getCartNum() + additionalQuantity)
                .update();

        if (updateResult) {
            log.info("更新用户{}的购物车项，商品：{}，增加数量：{}",
                    existingCart.getUserId(), mallProduct.getName(), additionalQuantity);
        }
        return updateResult;
    }

    /**
     * 获取商品封面图片
     */
    private String getProductCoverImage(Long productId) {
        try {
            String coverImage = mallProductImageService.getProductCoverImage(productId);
            if (coverImage == null || coverImage.isEmpty()) {
                log.warn("商品{}没有封面图片，使用默认图片", productId);
                return "/images/default-product.jpg";
            }
            return coverImage;
        } catch (Exception e) {
            log.warn("获取商品{}封面图片失败：{}，使用默认图片", productId, e.getMessage());
            return "/images/default-product.jpg";
        }
    }

    @Override
    public List<CartItemVo> getCartList() {
        Long userId = getUserId();

        // 查询用户购物车
        List<MallCart> cartList = lambdaQuery()
                .eq(MallCart::getUserId, userId)
                .orderByDesc(MallCart::getCreateTime)
                .list();

        if (cartList.isEmpty()) {
            return Collections.emptyList();
        }

        // 转换为VO并补充商品信息
        List<CartItemVo> cartItemVos = new ArrayList<>();
        for (MallCart cart : cartList) {
            MallProduct product = mallProductService.getById(cart.getProductId());
            if (product == null || product.getStatus() != 1) {
                // 商品不存在或已下架，跳过
                log.warn("购物车商品{}不存在或已下架，跳过", cart.getProductId());
                continue;
            }

            BigDecimal subtotal = product.getPrice()
                    .multiply(new BigDecimal(cart.getCartNum()))
                    .setScale(2, RoundingMode.HALF_UP);

            CartItemVo vo = CartItemVo.builder()
                    .id(cart.getId())
                    .productId(cart.getProductId())
                    .productName(cart.getProductName())
                    .productImage(cart.getProductImage())
                    .price(product.getPrice())
                    .cartNum(cart.getCartNum())
                    .subtotal(subtotal)
                    .stock(product.getStock())
                    .build();

            cartItemVos.add(vo);
        }

        log.info("用户{}查询购物车，共{}件商品", userId, cartItemVos.size());
        return cartItemVos;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeCartItems(List<Long> cartIds) {
        if (cartIds == null || cartIds.isEmpty()) {
            return true;
        }

        Long userId = getUserId();

        // 只能删除自己的购物车商品
        boolean result = lambdaUpdate()
                .eq(MallCart::getUserId, userId)
                .in(MallCart::getId, cartIds)
                .remove();

        if (result) {
            log.info("用户{}删除购物车商品，ID列表：{}", userId, cartIds);
        }

        return result;
    }

    /**
     * 更新购物车商品数量
     *
     * @param request 更新购物车商品数量请求
     * @return 是否更新成功
     */
    @Override
    public boolean updateCartQuantity(UpdateCartQuantityRequest request) {
        MallCart mallCart = getById(request.getCartId());
        if (mallCart == null) {
            throw new ServiceException(ResponseCode.NOT_FOUND, "购物车商品不存在");
        }
        if (!mallCart.getUserId().equals(getUserId())) {
            throw new ServiceException(ResponseCode.FORBIDDEN, "系统出错啦!尝试重新登录一下吧~~");
        }
        return lambdaUpdate()
                .eq(MallCart::getId, request.getCartId())
                .set(MallCart::getCartNum, request.getQuantity())
                .update();
    }

    @Override
    public Long getCartProductCount() {
        Long userId = getUserId();
        List<MallCart> mallCarts = lambdaQuery().eq(MallCart::getUserId, userId).list();
        return mallCarts.stream()
                .mapToLong(MallCart::getCartNum)
                .sum();
    }
}




