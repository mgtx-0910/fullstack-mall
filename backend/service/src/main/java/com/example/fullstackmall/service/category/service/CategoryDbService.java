package com.example.fullstackmall.service.category.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.fullstackmall.contract.common.ApiCode;
import com.example.fullstackmall.service.category.entity.CategoryEntity;
import com.example.fullstackmall.service.category.mapper.CategoryMapper;
import com.example.fullstackmall.service.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 商品分类数据库访问 Service。
 */
@Service
public class CategoryDbService extends ServiceImpl<CategoryMapper, CategoryEntity> {

    private static final int ENABLED = 1;

    public CategoryEntity requireCategory(Long categoryId) {
        CategoryEntity category = getById(categoryId);
        if (category == null) {
            throw categoryNotFound();
        }
        return category;
    }

    public CategoryEntity requireEnabledCategory(Long categoryId) {
        CategoryEntity category = requireCategory(categoryId);
        if (category.getStatus() != ENABLED) {
            throw categoryNotFound();
        }
        return category;
    }

    private BusinessException categoryNotFound() {
        return new BusinessException(
                ApiCode.CATEGORY_NOT_FOUND,
                ApiCode.CATEGORY_NOT_FOUND.defaultMessage(),
                HttpStatus.NOT_FOUND
        );
    }

    /**
     * 一次批量查询后按分类 ID 建立 Map，供商品列表组装分类信息。
     * 这样不会在遍历每个商品时各查一次数据库，可避免典型的 N+1 查询问题。
     */
    public Map<Long, CategoryEntity> findCategoryMap(Collection<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return listByIds(categoryIds).stream()
                .collect(Collectors.toMap(CategoryEntity::getId, Function.identity()));
    }
}
