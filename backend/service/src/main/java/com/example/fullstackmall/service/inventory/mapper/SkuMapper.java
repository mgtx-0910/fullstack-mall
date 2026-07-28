package com.example.fullstackmall.service.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.fullstackmall.service.inventory.entity.SkuEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

/**
 * SKU MyBatis-Plus Mapper。并发敏感更新使用单条原子 SQL。
 */
public interface SkuMapper extends BaseMapper<SkuEntity> {

    /**
     * 乐观锁更新价格：WHERE 中的 version 不匹配时影响行数为 0。
     */
    @Update("""
            UPDATE mall_product_sku
            SET sale_price = #{salePrice},
                version = version + 1,
                updated_at = CURRENT_TIMESTAMP(3)
            WHERE id = #{skuId}
              AND version = #{expectedVersion}
            """)
    int updatePriceByVersion(
            @Param("skuId") Long skuId,
            @Param("salePrice") BigDecimal salePrice,
            @Param("expectedVersion") Integer expectedVersion
    );

    /**
     * 乐观锁调整库存，同时用 SQL 条件保证调整后的可售库存不小于 0。
     */
    @Update("""
            UPDATE mall_product_sku
            SET available_stock = available_stock + #{delta},
                version = version + 1,
                updated_at = CURRENT_TIMESTAMP(3)
            WHERE id = #{skuId}
              AND version = #{expectedVersion}
              AND available_stock + #{delta} >= 0
            """)
    int adjustAvailableStockByVersion(
            @Param("skuId") Long skuId,
            @Param("delta") Integer delta,
            @Param("expectedVersion") Integer expectedVersion
    );


    /**
     * 原子锁定库存：只有 available_stock 足够时才会更新成功。
     */
    @Update("""
            UPDATE mall_product_sku
            SET available_stock = available_stock - #{quantity},
                locked_stock = locked_stock + #{quantity},
                version = version + 1,
                updated_at = CURRENT_TIMESTAMP(3)
            WHERE id = #{skuId}
              AND available_stock >= #{quantity}
            """)
    int lockStock(
            @Param("skuId") Long skuId,
            @Param("quantity") Integer quantity
    );

    /**
     * 原子释放锁定库存：取消或关单时恢复可售数量。
     */
    @Update("""
            UPDATE mall_product_sku
            SET available_stock = available_stock + #{quantity},
                locked_stock = locked_stock - #{quantity},
                version = version + 1,
                updated_at = CURRENT_TIMESTAMP(3)
            WHERE id = #{skuId}
              AND locked_stock >= #{quantity}
            """)
    int releaseLockedStock(
            @Param("skuId") Long skuId,
            @Param("quantity") Integer quantity
    );

    /**
     * 原子确认售出：支付成功后仅扣减锁定库存。
     */
    @Update("""
            UPDATE mall_product_sku
            SET locked_stock = locked_stock - #{quantity},
                version = version + 1,
                updated_at = CURRENT_TIMESTAMP(3)
            WHERE id = #{skuId}
              AND locked_stock >= #{quantity}
            """)
    int confirmLockedStock(
            @Param("skuId") Long skuId,
            @Param("quantity") Integer quantity
    );
}
