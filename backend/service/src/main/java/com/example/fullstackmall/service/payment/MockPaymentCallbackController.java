package com.example.fullstackmall.service.payment;

import com.example.fullstackmall.contract.common.ApiResponse;
import com.example.fullstackmall.contract.payment.IPaymentFacade;
import com.example.fullstackmall.contract.payment.MockPaymentCallbackRequest;
import com.example.fullstackmall.contract.payment.PaymentResponse;
import com.example.fullstackmall.service.common.trace.TraceIdContext;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 模拟外部支付平台回调入口，不使用用户 JWT。
 */
@RestController
@RequestMapping("/api/mock-payments")
public class MockPaymentCallbackController {

    @Resource
    private IPaymentFacade paymentFacade;

    @PostMapping("/callback")
    @Operation(summary = "接收本地模拟支付成功回调")
    public ApiResponse<PaymentResponse> callback(
            @RequestHeader(value = "X-Mock-Payment-Secret", required = false) String callbackSecret,
            @Valid @RequestBody MockPaymentCallbackRequest callbackRequest,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                paymentFacade.handleMockCallback(callbackSecret, callbackRequest),
                TraceIdContext.get(request)
        );
    }
}
