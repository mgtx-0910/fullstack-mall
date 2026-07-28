package com.example.fullstackmall.service.inventory;

import com.example.fullstackmall.service.FullstackMallApplication;
import com.example.fullstackmall.service.inventory.entity.SkuEntity;
import com.example.fullstackmall.service.inventory.mapper.SkuMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = FullstackMallApplication.class)
@Transactional
class SkuMapperTest {

    @Resource
    private SkuMapper skuMapper;

    @Test
    void shouldUseBaseMapperToReadSkuAndBigDecimal() {
        SkuEntity sku = skuMapper.selectById(1L);
        List<SkuEntity> skus = skuMapper.selectList(null);

        assertThat(sku.getSkuCode()).isEqualTo("IPHONE15-BLACK-128G");
        assertThat(sku.getSalePrice()).isEqualByComparingTo(new BigDecimal("4599.00"));
        assertThat(sku.getAvailableStock()).isEqualTo(10);
        assertThat(sku.getLockedStock()).isZero();
        assertThat(sku.getVersion()).isZero();
        assertThat(skus).hasSize(4);
    }

    @Test
    void shouldApplyOptimisticLockerWhenUsingUpdateById() {
        SkuEntity first = skuMapper.selectById(1L);
        SkuEntity stale = new SkuEntity();
        stale.setId(first.getId());
        stale.setVersion(first.getVersion());
        stale.setSalePrice(new BigDecimal("4399.00"));

        first.setSalePrice(new BigDecimal("4499.00"));
        int firstUpdated = skuMapper.updateById(first);

        int staleUpdated = skuMapper.updateById(stale);

        SkuEntity current = skuMapper.selectById(1L);
        assertThat(firstUpdated).isEqualTo(1);
        assertThat(staleUpdated).isZero();
        assertThat(first.getVersion()).isEqualTo(1);
        assertThat(current.getSalePrice()).isEqualByComparingTo("4499.00");
        assertThat(current.getVersion()).isEqualTo(1);
    }
}
