package com.example.fullstackmall.service.inventory;

import com.example.fullstackmall.contract.common.ApiCode;
import com.example.fullstackmall.contract.inventory.ISkuFacade;
import com.example.fullstackmall.contract.inventory.SkuCreateRequest;
import com.example.fullstackmall.contract.inventory.SkuPriceUpdateRequest;
import com.example.fullstackmall.contract.inventory.SkuResponse;
import com.example.fullstackmall.contract.inventory.SkuStockAdjustRequest;
import com.example.fullstackmall.contract.product.ProductStatus;
import com.example.fullstackmall.service.common.exception.BusinessException;
import com.example.fullstackmall.service.inventory.entity.SkuEntity;
import com.example.fullstackmall.service.inventory.service.SkuDbService;
import com.example.fullstackmall.service.product.entity.ProductEntity;
import com.example.fullstackmall.service.product.service.ProductDbService;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * SKU 用例实现：编排商品状态、金额规范化和库存并发规则。
 */
@Service
public class SkuFacade implements ISkuFacade {

    @Resource
    private ProductDbService productDbService;

    @Resource
    private SkuDbService skuDbService;

    @Override
    public SkuResponse createSku(Long productId, SkuCreateRequest request) {
        ProductEntity product = productDbService.requireById(productId);
        ProductStatus status = ProductStatus.valueOf(product.getStatusCode());
        if (status == ProductStatus.ON_SALE) {
            throw new BusinessException(
                    ApiCode.PRODUCT_SKU_NOT_EDITABLE,
                    ApiCode.PRODUCT_SKU_NOT_EDITABLE.defaultMessage()
            );
        }

        String skuCode = request.getSkuCode().trim();
        if (skuDbService.existsBySkuCode(skuCode)) {
            throw skuCodeAlreadyExists();
        }

        LocalDateTime now = LocalDateTime.now();
        SkuEntity sku = new SkuEntity();
        sku.setProductId(productId);
        sku.setSkuCode(skuCode);
        sku.setSpecText(request.getSpecText().trim());
        sku.setSalePrice(normalizePrice(request.getSalePrice()));
        sku.setAvailableStock(request.getInitialStock());
        sku.setLockedStock(0);
        sku.setVersion(0);
        sku.setCreatedAt(now);
        sku.setUpdatedAt(now);

        try {
            skuDbService.save(sku);
        } catch (DuplicateKeyException exception) {
            // sku_code 的数据库唯一索引负责兜住并发创建，不能只依赖写入前查询。
            throw skuCodeAlreadyExists();
        }
        return toResponse(sku);
    }

    @Override
    public List<SkuResponse> queryAdminSkus(Long productId) {
        productDbService.requireById(productId);
        return toResponses(skuDbService.listByProductId(productId));
    }

    @Override
    public List<SkuResponse> queryPublishedSkus(Long productId) {
        productDbService.requirePublishedById(productId);
        return toResponses(skuDbService.listByProductId(productId));
    }

    @Override
    public SkuResponse updatePrice(Long skuId, SkuPriceUpdateRequest request) {
        BigDecimal price = normalizePrice(request.getSalePrice());
        // UPDATE 条件同时包含 version；受影响行数为 0 表示数据已被其他请求修改。
        int updated = skuDbService.updatePriceByVersion(skuId, price, request.getExpectedVersion());
        if (updated == 0) {
            explainVersionUpdateFailure(skuId, request.getExpectedVersion());
        }
        return toResponse(skuDbService.requireById(skuId));
    }

    @Override
    public SkuResponse adjustStock(Long skuId, SkuStockAdjustRequest request) {
        // 库存增减在 SQL 中原子完成，并限制结果不能小于 0，避免“先查再改”的并发超卖。
        int updated = skuDbService.adjustAvailableStockByVersion(
                skuId,
                request.getDelta(),
                request.getExpectedVersion()
        );
        if (updated == 0) {
            explainStockUpdateFailure(skuId, request.getDelta(), request.getExpectedVersion());
        }
        return toResponse(skuDbService.requireById(skuId));
    }

    private BigDecimal normalizePrice(BigDecimal price) {
        try {
            BigDecimal normalized = price.setScale(2, RoundingMode.UNNECESSARY);
            if (normalized.compareTo(BigDecimal.ZERO) <= 0) {
                throw invalidPrice();
            }
            return normalized;
        } catch (ArithmeticException exception) {
            throw invalidPrice();
        }
    }

    private void explainVersionUpdateFailure(Long skuId, Integer expectedVersion) {
        SkuEntity current = skuDbService.requireById(skuId);
        if (!current.getVersion().equals(expectedVersion)) {
            throw versionConflict();
        }
        throw versionConflict();
    }

    private void explainStockUpdateFailure(Long skuId, Integer delta, Integer expectedVersion) {
        SkuEntity current = skuDbService.requireById(skuId);
        if (!current.getVersion().equals(expectedVersion)) {
            throw versionConflict();
        }
        long remaining = (long) current.getAvailableStock() + delta;
        if (remaining < 0) {
            throw new BusinessException(
                    ApiCode.INSUFFICIENT_STOCK,
                    ApiCode.INSUFFICIENT_STOCK.defaultMessage()
            );
        }
        throw versionConflict();
    }

    private BusinessException invalidPrice() {
        return new BusinessException(
                ApiCode.VALIDATION_ERROR,
                "销售价必须大于 0 且最多保留 2 位小数",
                HttpStatus.BAD_REQUEST
        );
    }

    private BusinessException skuCodeAlreadyExists() {
        return new BusinessException(
                ApiCode.SKU_CODE_ALREADY_EXISTS,
                ApiCode.SKU_CODE_ALREADY_EXISTS.defaultMessage()
        );
    }

    private BusinessException versionConflict() {
        return new BusinessException(
                ApiCode.INVENTORY_VERSION_CONFLICT,
                ApiCode.INVENTORY_VERSION_CONFLICT.defaultMessage()
        );
    }

    private List<SkuResponse> toResponses(List<SkuEntity> skus) {
        return skus.stream().map(this::toResponse).toList();
    }

    private SkuResponse toResponse(SkuEntity sku) {
        return new SkuResponse(
                sku.getId(),
                sku.getProductId(),
                sku.getSkuCode(),
                sku.getSpecText(),
                sku.getSalePrice(),
                sku.getAvailableStock(),
                sku.getLockedStock(),
                sku.getVersion(),
                sku.getCreatedAt(),
                sku.getUpdatedAt()
        );
    }
}
