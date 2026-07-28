package com.example.fullstackmall.service.inventory;

import com.example.fullstackmall.contract.common.ApiResponse;
import com.example.fullstackmall.contract.inventory.ISkuFacade;
import com.example.fullstackmall.contract.inventory.SkuCreateRequest;
import com.example.fullstackmall.contract.inventory.SkuPriceUpdateRequest;
import com.example.fullstackmall.contract.inventory.SkuResponse;
import com.example.fullstackmall.contract.inventory.SkuStockAdjustRequest;
import com.example.fullstackmall.service.common.trace.TraceIdContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理员 SKU、售价和库存 HTTP 入口。
 */
@RestController
@RequestMapping("/api/admin")
@Tag(name = "管理员 SKU 管理")
public class SkuAdminController {

    @Resource
    private ISkuFacade skuFacade;

    @PostMapping("/products/{productId}/skus")
    @Operation(summary = "为商品创建 SKU")
    public ResponseEntity<ApiResponse<SkuResponse>> createSku(
            @PathVariable Long productId,
            @Valid @RequestBody SkuCreateRequest request,
            HttpServletRequest servletRequest
    ) {
        SkuResponse response = skuFacade.createSku(productId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, TraceIdContext.get(servletRequest)));
    }

    @GetMapping("/products/{productId}/skus")
    @Operation(summary = "查询商品全部 SKU")
    public ApiResponse<List<SkuResponse>> querySkus(
            @PathVariable Long productId,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(
                skuFacade.queryAdminSkus(productId),
                TraceIdContext.get(servletRequest)
        );
    }

    @PatchMapping("/skus/{skuId}/price")
    @Operation(summary = "按版本修改 SKU 售价")
    public ApiResponse<SkuResponse> updatePrice(
            @PathVariable Long skuId,
            @Valid @RequestBody SkuPriceUpdateRequest request,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(
                skuFacade.updatePrice(skuId, request),
                TraceIdContext.get(servletRequest)
        );
    }

    @PatchMapping("/skus/{skuId}/stock")
    @Operation(summary = "按版本调整 SKU 可售库存")
    public ApiResponse<SkuResponse> adjustStock(
            @PathVariable Long skuId,
            @Valid @RequestBody SkuStockAdjustRequest request,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(
                skuFacade.adjustStock(skuId, request),
                TraceIdContext.get(servletRequest)
        );
    }
}
