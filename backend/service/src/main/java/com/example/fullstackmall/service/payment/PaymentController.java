package com.example.fullstackmall.service.payment;

import com.example.fullstackmall.contract.common.ApiResponse;
import com.example.fullstackmall.contract.payment.IPaymentFacade;
import com.example.fullstackmall.contract.payment.PaymentResponse;
import com.example.fullstackmall.service.common.trace.TraceIdContext;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 当前登录用户的支付 HTTP 入口。
 */
@RestController
@RequestMapping("/api/orders/{orderId}")
public class PaymentController {

    @Resource
    private IPaymentFacade paymentFacade;

    @PostMapping("/payments")
    @Operation(summary = "为我的待支付订单创建支付流水")
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
            @PathVariable Long orderId,
            HttpServletRequest request
    ) {
        PaymentResponse response = paymentFacade.createMyPayment(orderId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, TraceIdContext.get(request)));
    }

    @GetMapping("/payment")
    @Operation(summary = "查询我的订单支付流水")
    public ApiResponse<PaymentResponse> queryPayment(
            @PathVariable Long orderId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(paymentFacade.queryMyPayment(orderId), TraceIdContext.get(request));
    }
}
