package com.example.fullstackmall.service.inventory.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.fullstackmall.contract.common.ApiCode;
import com.example.fullstackmall.service.common.exception.BusinessException;
import com.example.fullstackmall.service.inventory.entity.SkuEntity;
import com.example.fullstackmall.service.inventory.mapper.SkuMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * SKU 数据库访问 Service。
 */
@Service
public class SkuDbService extends ServiceImpl<SkuMapper, SkuEntity> {

    public SkuEntity requireById(Long skuId) {
        SkuEntity sku = getById(skuId);
        if (sku == null) {
            throw new BusinessException(
                    ApiCode.SKU_NOT_FOUND,
                    ApiCode.SKU_NOT_FOUND.defaultMessage(),
                    HttpStatus.NOT_FOUND
            );
        }
        return sku;
    }

    public List<SkuEntity> listByProductId(Long productId) {
        return lambdaQuery()
                .eq(SkuEntity::getProductId, productId)
                .orderByAsc(SkuEntity::getId)
                .list();
    }

    public List<SkuEntity> listExistingByIds(Collection<Long> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            return Collections.emptyList();
        }
        return lambdaQuery()
                .in(SkuEntity::getId, skuIds)
                .list();
    }

    public boolean existsBySkuCode(String skuCode) {
        return lambdaQuery()
                .eq(SkuEntity::getSkuCode, skuCode)
                .count() > 0;
    }

    /**
     * 使用前端上次读取到的 version 修改价格。
     * 返回 1 表示更新成功，返回 0 表示 SKU 不存在或 version 已被其他请求修改。
     */
    public int updatePriceByVersion(Long skuId, BigDecimal salePrice, Integer expectedVersion) {
        return baseMapper.updatePriceByVersion(skuId, salePrice, expectedVersion);
    }

    /**
     * 通过“版本一致且调整后库存不能小于 0”的单条 SQL 修改可售库存。
     * 条件判断和写入在数据库中原子完成，调用方必须检查影响行数是否为 1。
     */
    public int adjustAvailableStockByVersion(Long skuId, Integer delta, Integer expectedVersion) {
        return baseMapper.adjustAvailableStockByVersion(skuId, delta, expectedVersion);
    }

    public void requireSaleableSku(Long productId) {
        boolean exists = lambdaQuery()
                .eq(SkuEntity::getProductId, productId)
                .gt(SkuEntity::getSalePrice, BigDecimal.ZERO)
                .gt(SkuEntity::getAvailableStock, 0)
                .count() > 0;
        if (!exists) {
            throw new BusinessException(
                    ApiCode.PRODUCT_NOT_READY_FOR_SALE,
                    ApiCode.PRODUCT_NOT_READY_FOR_SALE.defaultMessage()
            );
        }
    }

    /**
     * 创建订单时把可售库存原子转为锁定库存；库存不足时 SQL 不更新并返回 0。
     */
    public int lockStock(Long skuId, Integer quantity) {
        return baseMapper.lockStock(skuId, quantity);
    }

    /**
     * 取消或关闭订单时把锁定库存退回可售库存；返回 0 通常表示锁定库存已不满足释放条件。
     */
    public int releaseLockedStock(Long skuId, Integer quantity) {
        return baseMapper.releaseLockedStock(skuId, quantity);
    }

    /**
     * 支付成功后只减少锁定库存，不再增加可售库存，表示这部分商品已经真正售出。
     */
    public int confirmLockedStock(Long skuId, Integer quantity) {
        return baseMapper.confirmLockedStock(skuId, quantity);
    }
}
