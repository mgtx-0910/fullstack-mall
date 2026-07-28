package com.example.fullstackmall.service.order;

import com.example.fullstackmall.contract.common.ApiResponse;
import com.example.fullstackmall.contract.order.IOrderFacade;
import com.example.fullstackmall.contract.order.OrderResponse;
import com.example.fullstackmall.service.common.trace.TraceIdContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 当前登录用户的订单 HTTP 入口。
 */
@RestController
@RequestMapping("/api/orders")
@Tag(name = "订单")
public class OrderController {

    @Resource
    private IOrderFacade orderFacade;

    @PostMapping
    @Operation(summary = "使用已勾选购物车创建订单")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            HttpServletRequest servletRequest
    ) {
        OrderResponse response = orderFacade.createOrder(idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, TraceIdContext.get(servletRequest)));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "查询我的订单详情")
    public ApiResponse<OrderResponse> queryMyOrder(
            @PathVariable Long orderId,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(
                orderFacade.queryMyOrder(orderId),
                TraceIdContext.get(servletRequest)
        );
    }

    @PatchMapping("/{orderId}/cancel")
    @Operation(summary = "取消我的待支付订单")
    public ApiResponse<OrderResponse> cancelMyOrder(
            @PathVariable Long orderId,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(
                orderFacade.cancelMyOrder(orderId),
                TraceIdContext.get(servletRequest)
        );
    }
}
