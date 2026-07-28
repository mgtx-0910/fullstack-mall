package com.example.fullstackmall.service.product;

import com.example.fullstackmall.service.FullstackMallApplication;
import com.example.fullstackmall.service.product.entity.ProductEntity;
import com.example.fullstackmall.service.product.mapper.ProductMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = FullstackMallApplication.class)
class ProductMapperTest {

    @Resource
    private ProductMapper productMapper;

    @Test
    void shouldUseBaseMapperToReadProductTable() {
        ProductEntity product = productMapper.selectById(1L);
        List<ProductEntity> products = productMapper.selectList(null);

        assertThat(product.getTitle()).isEqualTo("九成新 iPhone 15");
        assertThat(product.getStatusCode()).isEqualTo("ON_SALE");
        assertThat(product.getCreatedBy()).isEqualTo(5L);
        assertThat(products).hasSize(4);
    }
}
