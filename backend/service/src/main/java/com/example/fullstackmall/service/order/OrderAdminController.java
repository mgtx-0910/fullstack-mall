package com.example.fullstackmall.service.order;

import com.example.fullstackmall.contract.common.ApiResponse;
import com.example.fullstackmall.contract.common.PageResponse;
import com.example.fullstackmall.contract.order.IOrderFacade;
import com.example.fullstackmall.contract.order.OrderQueryRequest;
import com.example.fullstackmall.contract.order.OrderResponse;
import com.example.fullstackmall.contract.order.OrderSummaryResponse;
import com.example.fullstackmall.service.common.trace.TraceIdContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员订单管理 HTTP 入口。
 */
@RestController
@RequestMapping("/api/admin/orders")
@Tag(name = "管理员订单")
public class OrderAdminController {

    @Resource
    private IOrderFacade orderFacade;

    @GetMapping
    @Operation(summary = "管理员分页查询订单")
    public ApiResponse<PageResponse<OrderSummaryResponse>> queryOrders(
            @Valid @ParameterObject OrderQueryRequest request,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(
                orderFacade.queryAdminOrders(request),
                TraceIdContext.get(servletRequest)
        );
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "管理员查询订单详情")
    public ApiResponse<OrderResponse> queryOrder(
            @PathVariable Long orderId,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(
                orderFacade.queryAdminOrder(orderId),
                TraceIdContext.get(servletRequest)
        );
    }

    @PatchMapping("/{orderId}/cancel")
    @Operation(summary = "管理员取消待支付订单")
    public ApiResponse<OrderResponse> cancelOrder(
            @PathVariable Long orderId,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(
                orderFacade.cancelAdminOrder(orderId),
                TraceIdContext.get(servletRequest)
        );
    }
}
