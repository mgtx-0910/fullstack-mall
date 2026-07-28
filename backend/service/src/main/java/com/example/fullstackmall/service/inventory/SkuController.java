package com.example.fullstackmall.service.inventory;

import com.example.fullstackmall.contract.common.ApiResponse;
import com.example.fullstackmall.contract.inventory.ISkuFacade;
import com.example.fullstackmall.contract.inventory.SkuResponse;
import com.example.fullstackmall.service.common.trace.TraceIdContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 匿名用户查询已上架商品 SKU 的 HTTP 入口。
 */
@RestController
@RequestMapping("/api/products")
@Tag(name = "公开 SKU")
public class SkuController {

    @Resource
    private ISkuFacade skuFacade;

    @GetMapping("/{productId}/skus")
    @Operation(summary = "查询已上架商品 SKU")
    public ApiResponse<List<SkuResponse>> queryPublishedSkus(
            @PathVariable Long productId,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(
                skuFacade.queryPublishedSkus(productId),
                TraceIdContext.get(servletRequest)
        );
    }
}
