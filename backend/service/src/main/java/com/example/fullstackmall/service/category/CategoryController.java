package com.example.fullstackmall.service.category;

import com.example.fullstackmall.contract.category.CategoryDraftRequest;
import com.example.fullstackmall.contract.category.CategoryDraftResponse;
import com.example.fullstackmall.contract.category.ICategoryFacade;
import com.example.fullstackmall.contract.common.ApiResponse;
import com.example.fullstackmall.service.common.trace.TraceIdContext;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 分类相关 HTTP 接口。
 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    @Resource
    private ICategoryFacade categoryFacade;

    @Operation(
            summary = "校验分类草稿",
            description = "校验并规范化分类草稿，本阶段不保存数据库"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "校验成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数错误"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "违反业务规则")
    })
    @PostMapping("/validate")
    public ApiResponse<CategoryDraftResponse> validateDraft(
            @Valid @RequestBody CategoryDraftRequest request,
            HttpServletRequest httpRequest
    ) {
        CategoryDraftResponse result = categoryFacade.validateDraft(request);
        return ApiResponse.success(result, TraceIdContext.get(httpRequest));
    }
}
