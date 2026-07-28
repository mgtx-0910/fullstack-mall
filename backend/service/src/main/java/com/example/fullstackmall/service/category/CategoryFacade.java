package com.example.fullstackmall.service.category;

import com.example.fullstackmall.contract.category.CategoryDraftRequest;
import com.example.fullstackmall.contract.category.CategoryDraftResponse;
import com.example.fullstackmall.contract.category.CategorySummaryResponse;
import com.example.fullstackmall.contract.category.ICategoryFacade;
import com.example.fullstackmall.contract.common.ApiCode;
import com.example.fullstackmall.service.category.entity.CategoryEntity;
import com.example.fullstackmall.service.category.service.CategoryDbService;
import com.example.fullstackmall.service.common.exception.BusinessException;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 分类契约的业务实现。
 */
@Service
public class CategoryFacade implements ICategoryFacade {

    private static final String RESERVED_CATEGORY_NAME = "全部";

    @Resource
    private CategoryDbService categoryDbService;

    @Override
    public CategoryDraftResponse validateDraft(CategoryDraftRequest request) {
        String normalizedName = request.getName().strip();
        String normalizedDescription = normalizeOptionalText(request.getDescription());

        if (RESERVED_CATEGORY_NAME.equals(normalizedName)) {
            throw new BusinessException(
                    ApiCode.CATEGORY_NAME_RESERVED,
                    "分类名称“全部”是系统保留名称"
            );
        }

        return new CategoryDraftResponse(
                normalizedName,
                normalizedDescription,
                request.getSortOrder()
        );
    }

    @Override
    public List<CategorySummaryResponse> listAdminCategories() {
        return categoryDbService.listAdminCategories().stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    private CategorySummaryResponse toSummaryResponse(CategoryEntity category) {
        return new CategorySummaryResponse(
                category.getId(),
                category.getName(),
                category.getStatus(),
                category.getSortOrder(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }

    private String normalizeOptionalText(String text) {
        if (text == null) {
            return null;
        }
        String normalized = text.strip();
        return normalized.isEmpty() ? null : normalized;
    }
}
