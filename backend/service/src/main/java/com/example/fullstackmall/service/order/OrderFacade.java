package com.example.fullstackmall.service.order;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.fullstackmall.contract.common.ApiCode;
import com.example.fullstackmall.contract.common.PageResponse;
import com.example.fullstackmall.contract.order.IOrderFacade;
import com.example.fullstackmall.contract.order.OrderItemResponse;
import com.example.fullstackmall.contract.order.OrderQueryRequest;
import com.example.fullstackmall.contract.order.OrderResponse;
import com.example.fullstackmall.contract.order.OrderStatus;
import com.example.fullstackmall.contract.order.OrderSummaryResponse;
import com.example.fullstackmall.service.common.exception.BusinessException;
import com.example.fullstackmall.service.order.entity.OrderEntity;
import com.example.fullstackmall.service.order.entity.OrderItemEntity;
import com.example.fullstackmall.service.order.service.OrderDbService;
import com.example.fullstackmall.service.order.service.OrderItemDbService;
import com.example.fullstackmall.service.security.CurrentUserService;
import jakarta.annotation.Resource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 订单用例实现。非事务入口负责身份、幂等重试和响应组装。
 */
@Service
public class OrderFacade implements IOrderFacade {

    private static final Pattern IDEMPOTENCY_KEY_PATTERN =
            Pattern.compile("^[A-Za-z0-9._:-]{8,64}$");

    @Resource
    private CurrentUserService currentUserService;

    @Resource
    private OrderDbService orderDbService;

    @Resource
    private OrderItemDbService orderItemDbService;

    @Resource
    private OrderCreateTransactionService orderCreateTransactionService;

    @Resource
    private OrderCancelTransactionService orderCancelTransactionService;

    @Override
    public OrderResponse createOrder(String idempotencyKey) {
        Long userId = currentUserService.requireCurrentUserId();
        requireValidIdempotencyKey(idempotencyKey);

        // 相同用户重复提交同一个 Idempotency-Key 时直接返回原订单，不重复扣减库存。
        OrderEntity existingOrder = orderDbService.getByUserIdAndIdempotencyKey(userId, idempotencyKey);
        if (existingOrder != null) {
            return assembleOrder(existingOrder);
        }

        try {
            Long orderId = orderCreateTransactionService.createOrder(userId, idempotencyKey);
            return assembleOrder(orderDbService.requireByIdAndUserId(orderId, userId));
        } catch (DataIntegrityViolationException exception) {
            // 并发请求可能同时通过首次查询，数据库唯一约束冲突后再读取胜出的订单。
            OrderEntity concurrentOrder = orderDbService.getByUserIdAndIdempotencyKey(userId, idempotencyKey);
            if (concurrentOrder != null) {
                return assembleOrder(concurrentOrder);
            }
            throw exception;
        }
    }

    @Override
    public OrderResponse queryMyOrder(Long orderId) {
        Long userId = currentUserService.requireCurrentUserId();
        return assembleOrder(orderDbService.requireByIdAndUserId(orderId, userId));
    }

    @Override
    public OrderResponse cancelMyOrder(Long orderId) {
        Long userId = currentUserService.requireCurrentUserId();
        orderCancelTransactionService.cancelOrder(orderId, userId);
        return assembleOrder(orderDbService.requireByIdAndUserId(orderId, userId));
    }

    @Override
    public PageResponse<OrderSummaryResponse> queryAdminOrders(OrderQueryRequest request) {
        Page<OrderEntity> entityPage = orderDbService.queryAdminPage(request);
        List<OrderSummaryResponse> records = entityPage.getRecords()
                .stream()
                .map(this::toSummaryResponse)
                .toList();
        return new PageResponse<>(
                entityPage.getCurrent(),
                entityPage.getSize(),
                entityPage.getTotal(),
                entityPage.getPages(),
                records
        );
    }

    @Override
    public OrderResponse queryAdminOrder(Long orderId) {
        return assembleOrder(orderDbService.requireById(orderId));
    }

    @Override
    public OrderResponse cancelAdminOrder(Long orderId) {
        orderCancelTransactionService.cancelOrderByAdmin(orderId);
        return assembleOrder(orderDbService.requireById(orderId));
    }

    private void requireValidIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || !IDEMPOTENCY_KEY_PATTERN.matcher(idempotencyKey).matches()) {
            throw new BusinessException(
                    ApiCode.ORDER_IDEMPOTENCY_KEY_INVALID,
                    ApiCode.ORDER_IDEMPOTENCY_KEY_INVALID.defaultMessage(),
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private OrderResponse assembleOrder(OrderEntity order) {
        // Response 在 Facade 统一组装，Entity 不直接暴露给 Controller 和前端。
        List<OrderItemResponse> items = orderItemDbService.listByOrderId(order.getId())
                .stream()
                .map(this::toItemResponse)
                .toList();
        return OrderResponse.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .status(OrderStatus.valueOf(order.getStatusCode()))
                .totalAmount(order.getTotalAmount())
                .items(items)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .expiresAt(order.getExpiresAt())
                .paidAt(order.getPaidAt())
                .cancelledAt(order.getCancelledAt())
                .closedAt(order.getClosedAt())
                .build();
    }

    private OrderSummaryResponse toSummaryResponse(OrderEntity order) {
        return new OrderSummaryResponse(
                order.getId(),
                order.getOrderNo(),
                order.getUserId(),
                OrderStatus.valueOf(order.getStatusCode()),
                order.getTotalAmount(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getExpiresAt(),
                order.getPaidAt(),
                order.getCancelledAt(),
                order.getClosedAt()
        );
    }

    private OrderItemResponse toItemResponse(OrderItemEntity item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .skuId(item.getSkuId())
                .productId(item.getProductId())
                .productTitle(item.getProductTitle())
                .skuCode(item.getSkuCode())
                .specText(item.getSpecText())
                .unitPrice(item.getUnitPrice())
                .quantity(item.getQuantity())
                .lineAmount(item.getLineAmount())
                .build();
    }
}
