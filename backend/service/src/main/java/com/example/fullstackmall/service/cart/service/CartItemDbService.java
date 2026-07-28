package com.example.fullstackmall.service.cart.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.fullstackmall.service.cart.entity.CartItemEntity;
import com.example.fullstackmall.service.cart.mapper.CartItemMapper;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 购物车数据库访问 Service。所有用户资源查询都必须带 userId 条件。
 */
@Service
public class CartItemDbService extends ServiceImpl<CartItemMapper, CartItemEntity> {

    public List<CartItemEntity> listByUserId(Long userId) {
        return lambdaQuery()
                .eq(CartItemEntity::getUserId, userId)
                .orderByDesc(CartItemEntity::getUpdatedAt)
                .orderByDesc(CartItemEntity::getId)
                .list();
    }

    public CartItemEntity getByUserIdAndSkuId(Long userId, Long skuId) {
        return lambdaQuery()
                .eq(CartItemEntity::getUserId, userId)
                .eq(CartItemEntity::getSkuId, skuId)
                .one();
    }

    public boolean existsByUserIdAndSkuId(Long userId, Long skuId) {
        return lambdaQuery()
                .eq(CartItemEntity::getUserId, userId)
                .eq(CartItemEntity::getSkuId, skuId)
                .count() > 0;
    }

    public List<CartItemEntity> listCheckedByUserId(Long userId) {
        return lambdaQuery()
                .eq(CartItemEntity::getUserId, userId)
                .eq(CartItemEntity::getChecked, 1)
                .orderByAsc(CartItemEntity::getSkuId)
                .list();
    }

    /**
     * 删除本次下单使用的已勾选购物车项。
     *
     * <p>SQL 同时限制 userId、checked 状态和 id 集合：userId 防止删除其他用户的数据，
     * checked 防止误删未参与下单的商品；返回值是实际删除行数，事务层会用它校验数据是否被并发修改。</p>
     */
    public int removeCheckedByUserIdAndIds(Long userId, Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        return baseMapper.removeCheckedByUserIdAndIds(userId, ids);
    }

}
