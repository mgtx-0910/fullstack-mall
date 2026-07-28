package com.example.fullstackmall.service.cart;

import com.example.fullstackmall.service.FullstackMallApplication;
import com.example.fullstackmall.service.cart.entity.CartItemEntity;
import com.example.fullstackmall.service.cart.mapper.CartItemMapper;
import com.example.fullstackmall.service.cart.service.CartItemDbService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = FullstackMallApplication.class)
@Transactional
class CartItemMapperTest {

    @Resource
    private CartItemMapper cartItemMapper;

    @Resource
    private CartItemDbService cartItemDbService;

    @Test
    void shouldMapCartItemAndIsolateRowsByUserId() {
        CartItemEntity first = cartItemMapper.selectById(1L);
        List<CartItemEntity> userOneItems = cartItemDbService.listByUserId(1L);
        List<CartItemEntity> userTwoItems = cartItemDbService.listByUserId(2L);

        assertThat(first.getUserId()).isEqualTo(1L);
        assertThat(first.getSkuId()).isEqualTo(1L);
        assertThat(first.getQuantity()).isEqualTo(2);
        assertThat(first.getChecked()).isEqualTo(1);
        assertThat(userOneItems).extracting(CartItemEntity::getUserId).containsOnly(1L);
        assertThat(userTwoItems).extracting(CartItemEntity::getUserId).containsOnly(2L);
    }

    @Test
    void shouldRejectDuplicateUserAndSkuPair() {
        LocalDateTime now = LocalDateTime.now();
        CartItemEntity duplicate = new CartItemEntity();
        duplicate.setUserId(1L);
        duplicate.setSkuId(1L);
        duplicate.setQuantity(1);
        duplicate.setChecked(1);
        duplicate.setCreatedAt(now);
        duplicate.setUpdatedAt(now);

        assertThatThrownBy(() -> cartItemMapper.insert(duplicate))
                .isInstanceOf(DuplicateKeyException.class);
    }
}
