package com.example.fullstackmall.service.cart;

import com.example.fullstackmall.contract.cart.CartAddRequest;
import com.example.fullstackmall.contract.cart.CartItemResponse;
import com.example.fullstackmall.contract.cart.CartResponse;
import com.example.fullstackmall.contract.cart.ICartFacade;
import com.example.fullstackmall.contract.common.ApiResponse;
import com.example.fullstackmall.service.common.trace.TraceIdContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 当前登录用户购物车 HTTP 入口。
 */
@RestController
@RequestMapping("/api/cart/items")
@Tag(name = "购物车")
public class CartController {

    @Resource
    private ICartFacade cartFacade;

    @PostMapping
    @Operation(summary = "加入购物车")
    public ResponseEntity<ApiResponse<CartItemResponse>> addItem(
            @Valid @RequestBody CartAddRequest request,
            HttpServletRequest servletRequest
    ) {
        CartItemResponse response = cartFacade.addItem(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, TraceIdContext.get(servletRequest)));
    }

    @GetMapping
    @Operation(summary = "查询我的购物车")
    public ApiResponse<CartResponse> queryMyCart(HttpServletRequest servletRequest) {
        return ApiResponse.success(
                cartFacade.queryMyCart(),
                TraceIdContext.get(servletRequest)
        );
    }
}
