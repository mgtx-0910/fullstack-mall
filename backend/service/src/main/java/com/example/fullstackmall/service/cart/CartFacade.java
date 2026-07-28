package com.example.fullstackmall.service.cart;

import com.example.fullstackmall.contract.cart.CartAddRequest;
import com.example.fullstackmall.contract.cart.CartItemResponse;
import com.example.fullstackmall.contract.cart.CartResponse;
import com.example.fullstackmall.contract.cart.ICartFacade;
import com.example.fullstackmall.contract.common.ApiCode;
import com.example.fullstackmall.contract.product.ProductStatus;
import com.example.fullstackmall.service.cart.entity.CartItemEntity;
import com.example.fullstackmall.service.cart.service.CartItemDbService;
import com.example.fullstackmall.service.common.exception.BusinessException;
import com.example.fullstackmall.service.inventory.entity.SkuEntity;
import com.example.fullstackmall.service.inventory.service.SkuDbService;
import com.example.fullstackmall.service.product.entity.ProductEntity;
import com.example.fullstackmall.service.product.service.ProductDbService;
import com.example.fullstackmall.service.security.CurrentUserService;
import jakarta.annotation.Resource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 购物车用例实现：负责可信用户身份、可售校验、跨模块批量查询和响应组装。
 */
@Service
public class CartFacade implements ICartFacade {

    private static final int CHECKED = 1;
    private static final int MONEY_SCALE = 2;

    @Resource
    private CurrentUserService currentUserService;

    @Resource
    private CartItemDbService cartItemDbService;

    @Resource
    private SkuDbService skuDbService;

    @Resource
    private ProductDbService productDbService;

    @Override
    public CartItemResponse addItem(CartAddRequest request) {
        // userId 来自登录态而不是请求体，防止用户把商品加入他人的购物车。
        Long userId = currentUserService.requireCurrentUserId();
        SkuEntity sku = skuDbService.requireById(request.getSkuId());
        ProductEntity product = productDbService.requireById(sku.getProductId());

        requireSaleableProductAndSku(product, sku);
        if (request.getQuantity() > sku.getAvailableStock()) {
            throw conflict(ApiCode.CART_QUANTITY_EXCEEDS_STOCK);
        }
        if (cartItemDbService.existsByUserIdAndSkuId(userId, sku.getId())) {
            throw conflict(ApiCode.CART_ITEM_ALREADY_EXISTS);
        }

        LocalDateTime now = LocalDateTime.now();
        CartItemEntity entity = new CartItemEntity();
        entity.setUserId(userId);
        entity.setSkuId(sku.getId());
        entity.setQuantity(request.getQuantity());
        entity.setChecked(CHECKED);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        try {
            cartItemDbService.save(entity);
        } catch (DataIntegrityViolationException exception) {
            // user_id + sku_id 唯一索引处理并发重复加入，查询判断只用于提前给出友好提示。
            throw conflict(ApiCode.CART_ITEM_ALREADY_EXISTS);
        }
        return assembleItem(entity, sku, product);
    }

    @Override
    public CartResponse queryMyCart() {
        Long userId = currentUserService.requireCurrentUserId();
        List<CartItemEntity> cartItems = cartItemDbService.listByUserId(userId);
        if (cartItems.isEmpty()) {
            return CartResponse.builder()
                    .items(Collections.emptyList())
                    .totalQuantity(0)
                    .estimatedTotal(zeroMoney())
                    .build();
        }

        // 先收集全部 ID，再批量查询 SKU 和商品，避免循环中反复访问数据库的 N+1 问题。
        Set<Long> skuIds = cartItems.stream()
                .map(CartItemEntity::getSkuId)
                .collect(Collectors.toSet());
        Map<Long, SkuEntity> skuMap = skuDbService.listExistingByIds(skuIds).stream()
                .collect(Collectors.toMap(SkuEntity::getId, Function.identity()));

        Set<Long> productIds = skuMap.values().stream()
                .map(SkuEntity::getProductId)
                .collect(Collectors.toSet());
        Map<Long, ProductEntity> productMap = productDbService.listExistingByIds(productIds).stream()
                .collect(Collectors.toMap(ProductEntity::getId, Function.identity()));

        List<CartItemResponse> responses = cartItems.stream()
                .map(item -> {
                    SkuEntity sku = skuMap.get(item.getSkuId());
                    ProductEntity product = sku == null ? null : productMap.get(sku.getProductId());
                    return assembleItem(item, sku, product);
                })
                .toList();

        int totalQuantity = responses.stream()
                .mapToInt(CartItemResponse::getQuantity)
                .sum();
        // 购物车金额只是按“当前价格”估算；真正下单时还会重新校验并生成订单快照。
        BigDecimal estimatedTotal = responses.stream()
                .filter(item -> Boolean.TRUE.equals(item.getChecked()))
                .filter(item -> Boolean.TRUE.equals(item.getSaleable()))
                .map(CartItemResponse::getLineAmount)
                .reduce(zeroMoney(), BigDecimal::add);

        return CartResponse.builder()
                .items(responses)
                .totalQuantity(totalQuantity)
                .estimatedTotal(estimatedTotal)
                .build();
    }

    private void requireSaleableProductAndSku(ProductEntity product, SkuEntity sku) {
        boolean productOnSale = ProductStatus.ON_SALE.name().equals(product.getStatusCode());
        boolean positivePrice = sku.getSalePrice() != null && sku.getSalePrice().compareTo(BigDecimal.ZERO) > 0;
        boolean hasStock = sku.getAvailableStock() != null && sku.getAvailableStock() > 0;
        if (!productOnSale || !positivePrice || !hasStock) {
            throw conflict(ApiCode.CART_SKU_NOT_SALEABLE);
        }
    }

    private CartItemResponse assembleItem(
            CartItemEntity item,
            SkuEntity sku,
            ProductEntity product
    ) {
        BigDecimal unitPrice = sku == null ? null : sku.getSalePrice();
        BigDecimal lineAmount = unitPrice == null
                ? null
                : unitPrice.multiply(BigDecimal.valueOf(item.getQuantity())).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        boolean saleable = sku != null
                && product != null
                && ProductStatus.ON_SALE.name().equals(product.getStatusCode())
                && unitPrice != null
                && unitPrice.compareTo(BigDecimal.ZERO) > 0
                && sku.getAvailableStock() != null
                && sku.getAvailableStock() >= item.getQuantity();

        return CartItemResponse.builder()
                .id(item.getId())
                .skuId(item.getSkuId())
                .productId(sku == null ? null : sku.getProductId())
                .productTitle(product == null ? null : product.getTitle())
                .skuCode(sku == null ? null : sku.getSkuCode())
                .specText(sku == null ? null : sku.getSpecText())
                .currentUnitPrice(unitPrice)
                .quantity(item.getQuantity())
                .availableStock(sku == null ? null : sku.getAvailableStock())
                .checked(Integer.valueOf(CHECKED).equals(item.getChecked()))
                .saleable(saleable)
                .lineAmount(lineAmount)
                .updatedAt(item.getUpdatedAt())
                .build();
    }

    private BigDecimal zeroMoney() {
        return BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BusinessException conflict(ApiCode apiCode) {
        return new BusinessException(apiCode, apiCode.defaultMessage(), HttpStatus.CONFLICT);
    }
}
