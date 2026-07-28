package com.example.fullstackmall.service.product;

import com.example.fullstackmall.contract.common.ApiResponse;
import com.example.fullstackmall.contract.common.PageResponse;
import com.example.fullstackmall.contract.product.IProductFacade;
import com.example.fullstackmall.contract.product.ProductDetailResponse;
import com.example.fullstackmall.contract.product.ProductQueryRequest;
import com.example.fullstackmall.contract.product.ProductSummaryResponse;
import com.example.fullstackmall.service.common.trace.TraceIdContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 匿名用户可访问的已上架商品 HTTP 入口。
 */
@RestController
@RequestMapping("/api/products")
@Tag(name = "公开商品")
public class ProductController {

    @Resource
    private IProductFacade productFacade;

    @GetMapping
    @Operation(summary = "分页查询已上架商品")
    public ApiResponse<PageResponse<ProductSummaryResponse>> queryProducts(
            @Valid @ParameterObject ProductQueryRequest request,
            HttpServletRequest servletRequest
    ) {
        PageResponse<ProductSummaryResponse> response = productFacade.queryPublishedProducts(request);
        return ApiResponse.success(response, TraceIdContext.get(servletRequest));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询已上架商品详情")
    public ApiResponse<ProductDetailResponse> detail(
            @PathVariable Long id,
            HttpServletRequest servletRequest
    ) {
        ProductDetailResponse response = productFacade.getPublishedProduct(id);
        return ApiResponse.success(response, TraceIdContext.get(servletRequest));
    }
}
