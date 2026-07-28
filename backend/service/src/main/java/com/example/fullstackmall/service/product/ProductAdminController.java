package com.example.fullstackmall.service.product;

import com.example.fullstackmall.contract.common.ApiResponse;
import com.example.fullstackmall.contract.common.PageResponse;
import com.example.fullstackmall.contract.product.*;
import com.example.fullstackmall.service.common.trace.TraceIdContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员商品管理 HTTP 入口。
 */
@RestController
@RequestMapping("/api/admin/products")
@Tag(name = "管理员商品管理")
public class ProductAdminController {

    @Resource
    private IProductFacade productFacade;

    @PostMapping
    @Operation(summary = "创建商品草稿")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> createProduct(
            @Valid @RequestBody ProductCreateRequest request,
            HttpServletRequest servletRequest
    ) {
        ProductDetailResponse response = productFacade.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, TraceIdContext.get(servletRequest)));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "变更商品状态")
    public ApiResponse<ProductDetailResponse> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody ProductStatusChangeRequest request,
            HttpServletRequest servletRequest
    ) {
        ProductDetailResponse response = productFacade.changeStatus(id, request);
        return ApiResponse.success(response, TraceIdContext.get(servletRequest));
    }
    @PatchMapping("/{id}/subtitle")
    @Operation(summary = "修改商品副标题")
    public ApiResponse<ProductDetailResponse> updateSubtitle(
            @PathVariable Long id,
            @Valid @RequestBody ProductSubtitleUpdateRequest request,
            HttpServletRequest servletRequest
    ) {
        ProductDetailResponse response = productFacade.updateSubtitle(id, request);
        return ApiResponse.success(response, TraceIdContext.get(servletRequest));
    }

    @GetMapping
    @Operation(summary = "管理员分页查询商品")
    public ApiResponse<PageResponse<ProductSummaryResponse>> queryProducts(
            @Valid @ParameterObject ProductQueryRequest request,
            HttpServletRequest servletRequest
    ) {
        PageResponse<ProductSummaryResponse> response = productFacade.queryAdminProducts(request);
        return ApiResponse.success(response, TraceIdContext.get(servletRequest));
    }
}
