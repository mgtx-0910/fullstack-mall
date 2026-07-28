package com.example.fullstackmall.service.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.fullstackmall.contract.common.ApiCode;
import com.example.fullstackmall.contract.product.ProductQueryRequest;
import com.example.fullstackmall.contract.product.ProductStatus;
import com.example.fullstackmall.service.common.exception.BusinessException;
import com.example.fullstackmall.service.product.entity.ProductEntity;
import com.example.fullstackmall.service.product.mapper.ProductMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 商品数据库访问 Service。
 */
@Service
public class ProductDbService extends ServiceImpl<ProductMapper, ProductEntity> {

    public ProductEntity requireById(Long productId) {
        ProductEntity product = getById(productId);
        if (product == null) {
            throw productNotFound();
        }
        return product;
    }

    public ProductEntity requirePublishedById(Long productId) {
        ProductEntity product = lambdaQuery()
                .eq(ProductEntity::getId, productId)
                .eq(ProductEntity::getStatusCode, ProductStatus.ON_SALE.name())
                .one();
        if (product == null) {
            throw productNotFound();
        }
        return product;
    }

    public List<ProductEntity> listExistingByIds(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyList();
        }
        return lambdaQuery()
                .in(ProductEntity::getId, productIds)
                .list();
    }

    /**
     * 后台查询允许管理员使用请求中的状态条件查看草稿、上架和下架商品。
     */
    public Page<ProductEntity> queryAdminPage(ProductQueryRequest request) {
        return queryPage(request, request.getStatus());
    }

    /**
     * 公开查询强制限定为 ON_SALE，不能让客户端通过请求参数绕过商品可见性规则。
     */
    public Page<ProductEntity> queryPublishedPage(ProductQueryRequest request) {
        return queryPage(request, ProductStatus.ON_SALE);
    }

    /**
     * 复用后台与公开列表的分页条件组装；forcedStatus 由调用入口决定可信的状态范围。
     */
    private Page<ProductEntity> queryPage(ProductQueryRequest request, ProductStatus forcedStatus) {
        LambdaQueryWrapper<ProductEntity> wrapper = Wrappers.lambdaQuery(ProductEntity.class);
        String keyword = normalizeKeyword(request.getKeyword());
        if (StringUtils.hasText(keyword)) {
            wrapper.and(condition -> condition
                    .like(ProductEntity::getTitle, keyword)
                    .or()
                    .like(ProductEntity::getDescription, keyword));
        }
        if (request.getCategoryId() != null) {
            wrapper.eq(ProductEntity::getCategoryId, request.getCategoryId());
        }
        if (forcedStatus != null) {
            wrapper.eq(ProductEntity::getStatusCode, forcedStatus.name());
        }
        wrapper.orderByDesc(ProductEntity::getCreatedAt)
                .orderByDesc(ProductEntity::getId);
        return page(new Page<>(request.getPageNum(), request.getPageSize()), wrapper);
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null ? null : keyword.trim();
    }

    private BusinessException productNotFound() {
        return new BusinessException(
                ApiCode.PRODUCT_NOT_FOUND,
                ApiCode.PRODUCT_NOT_FOUND.defaultMessage(),
                HttpStatus.NOT_FOUND
        );
    }
    public ProductEntity updateSubtitle(Long productId,String subtitle){
        LocalDateTime now=LocalDateTime.now();

        LambdaUpdateWrapper<ProductEntity> wrapper=Wrappers.lambdaUpdate(ProductEntity.class)
                .eq(ProductEntity::getId,productId)
                .set(ProductEntity::getSubtitle,subtitle)
                .set(ProductEntity::getUpdatedAt,now);

        int affectedRows = baseMapper.update(null, wrapper);
        if (affectedRows == 0) {
            throw productNotFound();
        }
        return requireById(productId);
    }
}
