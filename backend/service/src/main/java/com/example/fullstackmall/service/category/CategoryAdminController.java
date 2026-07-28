package com.example.fullstackmall.service.category;

import com.example.fullstackmall.contract.category.CategorySummaryResponse;
import com.example.fullstackmall.contract.category.ICategoryFacade;
import com.example.fullstackmall.contract.common.ApiResponse;
import com.example.fullstackmall.service.common.trace.TraceIdContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理员分类管理 HTTP 入口。
 */
@RestController
@RequestMapping("/api/admin/categories")
@Tag(name = "管理员分类")
public class CategoryAdminController {

    @Resource
    private ICategoryFacade categoryFacade;

    @GetMapping
    @Operation(summary = "管理员查询分类列表")
    public ApiResponse<List<CategorySummaryResponse>> listCategories(HttpServletRequest servletRequest) {
        return ApiResponse.success(
                categoryFacade.listAdminCategories(),
                TraceIdContext.get(servletRequest)
        );
    }
}
