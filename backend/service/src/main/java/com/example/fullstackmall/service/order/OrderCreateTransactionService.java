package com.example.fullstackmall.service.order;

import com.example.fullstackmall.contract.common.ApiCode;
import com.example.fullstackmall.contract.order.OrderStatus;
import com.example.fullstackmall.contract.product.ProductStatus;
import com.example.fullstackmall.service.cart.entity.CartItemEntity;
import com.example.fullstackmall.service.cart.service.CartItemDbService;
import com.example.fullstackmall.service.common.exception.BusinessException;
import com.example.fullstackmall.service.inventory.entity.SkuEntity;
import com.example.fullstackmall.service.inventory.service.SkuDbService;
import com.example.fullstackmall.service.order.entity.OrderEntity;
import com.example.fullstackmall.service.order.entity.OrderItemEntity;
import com.example.fullstackmall.service.order.service.OrderDbService;
import com.example.fullstackmall.service.order.service.OrderItemDbService;
import com.example.fullstackmall.service.product.entity.ProductEntity;
import com.example.fullstackmall.service.product.service.ProductDbService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 创建订单的事务边界。方法内任一步抛异常，订单、库存、订单项和购物车修改都会回滚。
 */
@Service
public class OrderCreateTransactionService {

    private static final int MONEY_SCALE = 2;

    @Resource
    private CartItemDbService cartItemDbService;

    @Resource
    private SkuDbService skuDbService;

    @Resource
    private ProductDbService productDbService;

    @Resource
    private OrderDbService orderDbService;

    @Resource
    private OrderItemDbService orderItemDbService;

    @Value("${mall.order.payment-timeout-minutes:15}")
    private long paymentTimeoutMinutes;

    /**
     * 在一个数据库事务中校验购物车、锁定库存、保存订单与明细并删除已结算购物车项。
     * 任一步抛异常都会整体回滚，避免只生成订单却没有锁定库存。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(Long userId, String idempotencyKey) {
        // 按 skuId 固定库存更新顺序，多个并发订单尽量以相同顺序加锁，降低死锁概率。
        List<CartItemEntity> cartItems = cartItemDbService.listCheckedByUserId(userId)
                .stream()
                .sorted(Comparator.comparing(CartItemEntity::getSkuId))
                .toList();
        if (cartItems.isEmpty()) {
            throw conflict(ApiCode.ORDER_CART_EMPTY);
        }

        Set<Long> skuIds = cartItems.stream()
                .map(CartItemEntity::getSkuId)
                .collect(Collectors.toSet());
        Map<Long, SkuEntity> skuMap = skuDbService.listExistingByIds(skuIds)
                .stream()
                .collect(Collectors.toMap(SkuEntity::getId, Function.identity()));

        Set<Long> productIds = skuMap.values().stream()
                .map(SkuEntity::getProductId)
                .collect(Collectors.toSet());
        Map<Long, ProductEntity> productMap = productDbService.listExistingByIds(productIds)
                .stream()
                .collect(Collectors.toMap(ProductEntity::getId, Function.identity()));

        LocalDateTime now = LocalDateTime.now();
        // 下单时复制商品名、SKU 规格和价格，形成不会随商品资料变化的订单快照。
        List<OrderItemEntity> orderItems = cartItems.stream()
                .map(cartItem -> buildOrderItem(cartItem, skuMap, productMap, now))
                .toList();
        BigDecimal totalAmount = orderItems.stream()
                .map(OrderItemEntity::getLineAmount)
                .reduce(zeroMoney(), BigDecimal::add)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        OrderEntity order = new OrderEntity();
        order.setOrderNo("O" + UUID.randomUUID().toString().replace("-", "").toUpperCase());
        order.setUserId(userId);
        order.setStatusCode(OrderStatus.PENDING_PAYMENT.name());
        order.setTotalAmount(totalAmount);
        order.setIdempotencyKey(idempotencyKey);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        order.setExpiresAt(now.plusMinutes(paymentTimeoutMinutes));
        if (!orderDbService.save(order)) {
            throw new IllegalStateException("保存订单主表失败");
        }

        for (OrderItemEntity item : orderItems) {
            // 单条条件 UPDATE 同时检查可售库存并转入锁定库存，受影响 1 行才算成功。
            int affectedRows = skuDbService.lockStock(item.getSkuId(), item.getQuantity());
            if (affectedRows != 1) {
                throw conflict(ApiCode.ORDER_INSUFFICIENT_STOCK);
            }
            item.setOrderId(order.getId());
        }

        if (!orderItemDbService.saveBatch(orderItems)) {
            throw new IllegalStateException("保存订单项失败");
        }

        List<Long> cartItemIds = cartItems.stream()
                .map(CartItemEntity::getId)
                .toList();
        int removedRows = cartItemDbService.removeCheckedByUserIdAndIds(userId, cartItemIds);
        // 删除数量不一致说明购物车并发变化，抛异常让整个事务连同库存锁定一起回滚。
        if (removedRows != cartItems.size()) {
            throw conflict(ApiCode.ORDER_CART_CHANGED);
        }
        return order.getId();
    }

    private OrderItemEntity buildOrderItem(
            CartItemEntity cartItem,
            Map<Long, SkuEntity> skuMap,
            Map<Long, ProductEntity> productMap,
            LocalDateTime now
    ) {
        SkuEntity sku = skuMap.get(cartItem.getSkuId());
        ProductEntity product = sku == null ? null : productMap.get(sku.getProductId());
        if (sku == null
                || product == null
                || !ProductStatus.ON_SALE.name().equals(product.getStatusCode())
                || sku.getSalePrice() == null
                || sku.getSalePrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw conflict(ApiCode.ORDER_ITEM_NOT_SALEABLE);
        }
        if (sku.getAvailableStock() == null || sku.getAvailableStock() < cartItem.getQuantity()) {
            throw conflict(ApiCode.ORDER_INSUFFICIENT_STOCK);
        }

        BigDecimal unitPrice = sku.getSalePrice().setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal lineAmount = unitPrice
                .multiply(BigDecimal.valueOf(cartItem.getQuantity()))
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        OrderItemEntity item = new OrderItemEntity();
        item.setSkuId(sku.getId());
        item.setProductId(product.getId());
        item.setProductTitle(product.getTitle());
        item.setSkuCode(sku.getSkuCode());
        item.setSpecText(sku.getSpecText());
        item.setUnitPrice(unitPrice);
        item.setQuantity(cartItem.getQuantity());
        item.setLineAmount(lineAmount);
        item.setCreatedAt(now);
        return item;
    }

    private BigDecimal zeroMoney() {
        return BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BusinessException conflict(ApiCode apiCode) {
        return new BusinessException(apiCode, apiCode.defaultMessage(), HttpStatus.CONFLICT);
    }
}
