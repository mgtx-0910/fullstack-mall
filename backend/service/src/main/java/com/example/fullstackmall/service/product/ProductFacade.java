package com.example.fullstackmall.service.product;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.fullstackmall.contract.common.ApiCode;
import com.example.fullstackmall.contract.common.PageResponse;
import com.example.fullstackmall.contract.product.*;
import com.example.fullstackmall.service.category.entity.CategoryEntity;
import com.example.fullstackmall.service.category.service.CategoryDbService;
import com.example.fullstackmall.service.common.exception.BusinessException;
import com.example.fullstackmall.service.inventory.service.SkuDbService;
import com.example.fullstackmall.service.product.cache.ProductDetailCacheLookup;
import com.example.fullstackmall.service.product.cache.ProductDetailCacheService;
import com.example.fullstackmall.service.product.cache.ProductDetailCacheStatus;
import com.example.fullstackmall.service.product.entity.ProductEntity;
import com.example.fullstackmall.service.product.service.ProductDbService;
import com.example.fullstackmall.service.security.CurrentUserService;
import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 商品用例实现：编排分类校验、当前用户、状态机和持久化。
 */
@Service
public class ProductFacade implements IProductFacade {

    @Resource
    private ProductDbService productDbService;

    @Resource
    private CategoryDbService categoryDbService;

    @Resource
    private CurrentUserService currentUserService;

    @Resource
    private SkuDbService skuDbService;

    @Resource
    private ProductDetailCacheService productDetailCacheService;

    @Override
    public ProductDetailResponse createProduct(ProductCreateRequest request) {
        CategoryEntity category = categoryDbService.requireEnabledCategory(request.getCategoryId());
        LocalDateTime now = LocalDateTime.now();

        ProductEntity product = new ProductEntity();
        product.setCategoryId(category.getId());
        product.setTitle(request.getTitle().trim());
        product.setDescription(normalizeOptionalText(request.getDescription()));
        // 后台新建商品只能从草稿开始，不能由前端直接伪造为已上架。
        product.setStatusCode(ProductStatus.DRAFT.name());
        product.setCreatedBy(currentUserService.requireCurrentUserId());
        product.setCreatedAt(now);
        product.setUpdatedAt(now);
        productDbService.save(product);
        // 先写 MySQL，再删除可能由“提前猜 ID”产生的空值缓存。
        productDetailCacheService.evict(product.getId());

        return toDetailResponse(product, category);
    }

    @Override
    public ProductDetailResponse changeStatus(Long productId, ProductStatusChangeRequest request) {
        ProductEntity product = productDbService.requireById(productId);
        ProductStatus currentStatus = ProductStatus.valueOf(product.getStatusCode());
        ProductStatus targetStatus = request.getTargetStatus();
        // 状态机由后端控制，不能只相信前端传入的目标状态。
        validateTransition(currentStatus, targetStatus);
        CategoryEntity category = targetStatus == ProductStatus.ON_SALE
                ? categoryDbService.requireEnabledCategory(product.getCategoryId())
                : categoryDbService.requireCategory(product.getCategoryId());
        if (targetStatus == ProductStatus.ON_SALE) {
            // 上架前必须至少存在一个价格有效且有可售库存的 SKU。
            skuDbService.requireSaleableSku(productId);
        }

        product.setStatusCode(targetStatus.name());
        product.setUpdatedAt(LocalDateTime.now());
        productDbService.updateById(product);
        // 状态变化会影响公开可见性，数据库成功后必须让旧详情缓存失效。
        productDetailCacheService.evict(productId);

        return toDetailResponse(product, category);
    }

    @Override
    public PageResponse<ProductSummaryResponse> queryAdminProducts(ProductQueryRequest request) {
        return toPageResponse(productDbService.queryAdminPage(request));
    }

    @Override
    public PageResponse<ProductSummaryResponse> queryPublishedProducts(ProductQueryRequest request) {
        return toPageResponse(productDbService.queryPublishedPage(request));
    }

    @Override
    public ProductDetailResponse getAdminProduct(Long productId) {
        ProductEntity product = productDbService.requireById(productId);
        CategoryEntity category = categoryDbService.requireCategory(product.getCategoryId());
        return toDetailResponse(product, category);
    }

    @Override
    public ProductDetailResponse updateProduct(Long productId, ProductUpdateRequest request) {
        CategoryEntity category = categoryDbService.requireEnabledCategory(request.getCategoryId());
        ProductEntity product = productDbService.updateBasicInfo(
                productId,
                category.getId(),
                request.getTitle().trim(),
                normalizeOptionalText(request.getDescription())
        );
        // 基础信息会影响公开详情展示，数据库成功后必须让旧详情缓存失效。
        productDetailCacheService.evict(productId);
        return toDetailResponse(product, category);
    }

    @Override
    public ProductDetailResponse getPublishedProduct(Long productId) {
        ProductDetailCacheLookup cacheLookup = productDetailCacheService.lookup(productId);
        if (cacheLookup.getStatus() == ProductDetailCacheStatus.HIT) {
            return cacheLookup.getResponse();
        }
        if (cacheLookup.getStatus() == ProductDetailCacheStatus.NULL_HIT) {
            ApiCode apiCode = cacheLookup.getNullApiCode();
            throw new BusinessException(apiCode, apiCode.defaultMessage(), HttpStatus.NOT_FOUND);
        }

        // MISS、DEGRADED 和 BYPASS 都回源 MySQL；Redis 不是商品的真实数据来源。
        try {
            ProductEntity product = productDbService.requirePublishedById(productId);
            CategoryEntity category = categoryDbService.requireEnabledCategory(product.getCategoryId());
            ProductDetailResponse response = toDetailResponse(product, category);
            productDetailCacheService.put(productId, response);
            return response;
        } catch (BusinessException exception) {
            // 只缓存稳定的 404，其他业务冲突或系统异常不能被伪装成“不存在”。
            if (exception.getCode() == ApiCode.PRODUCT_NOT_FOUND
                    || exception.getCode() == ApiCode.CATEGORY_NOT_FOUND) {
                productDetailCacheService.putNull(productId, exception.getCode());
            }
            throw exception;
        }
    }

    private void validateTransition(ProductStatus currentStatus, ProductStatus targetStatus) {
        boolean allowed = (currentStatus == ProductStatus.DRAFT && targetStatus == ProductStatus.ON_SALE)
                || (currentStatus == ProductStatus.ON_SALE && targetStatus == ProductStatus.OFF_SHELF)
                || (currentStatus == ProductStatus.OFF_SHELF && targetStatus == ProductStatus.ON_SALE);
        if (!allowed) {
            throw new BusinessException(
                    ApiCode.INVALID_PRODUCT_STATUS_TRANSITION,
                    "商品状态不能从 " + currentStatus + " 变更为 " + targetStatus
            );
        }
    }

    private PageResponse<ProductSummaryResponse> toPageResponse(Page<ProductEntity> entityPage) {
        // 一次性批量查询分类，再用 Map 组装，避免每个商品单独查询一次形成 N+1。
        Set<Long> categoryIds = entityPage.getRecords().stream()
                .map(ProductEntity::getCategoryId)
                .collect(Collectors.toSet());
        Map<Long, CategoryEntity> categoryMap = categoryDbService.findCategoryMap(categoryIds);
        List<ProductSummaryResponse> records = entityPage.getRecords().stream()
                .map(product -> toSummaryResponse(product, categoryMap.get(product.getCategoryId())))
                .toList();
        return new PageResponse<>(
                entityPage.getCurrent(),
                entityPage.getSize(),
                entityPage.getTotal(),
                entityPage.getPages(),
                records
        );
    }

    private ProductSummaryResponse toSummaryResponse(ProductEntity product, CategoryEntity category) {
        return new ProductSummaryResponse(
                product.getId(),
                product.getCategoryId(),
                category == null ? null : category.getName(),
                product.getTitle(),
                ProductStatus.valueOf(product.getStatusCode()),
                product.getCreatedAt()
        );
    }

    private ProductDetailResponse toDetailResponse(ProductEntity product, CategoryEntity category) {
        return new ProductDetailResponse(
                product.getId(),
                product.getCategoryId(),
                category.getName(),
                product.getTitle(),
                product.getSubtitle(),
                product.getDescription(),
                ProductStatus.valueOf(product.getStatusCode()),
                product.getCreatedBy(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    private String normalizeOptionalText(String text) {
        if (text == null) {
            return null;
        }
        String normalized = text.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    @Override
    public ProductDetailResponse updateSubtitle(Long productId, ProductSubtitleUpdateRequest request) {
        String subtitle = request.getSubtitle().trim();

        ProductEntity product = productDbService.updateSubtitle(productId, subtitle);

        // MySQL 修改成功后再删除缓存；Redis 删除失败时 ProductDetailCacheService 内部会吞掉异常并记录日志。
        productDetailCacheService.evict(productId);

        CategoryEntity category = categoryDbService.requireCategory(product.getCategoryId());
        return toDetailResponse(product, category);
    }
}
