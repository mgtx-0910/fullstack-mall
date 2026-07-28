package com.example.fullstackmall.service.inventory;

import com.example.fullstackmall.service.FullstackMallApplication;
import com.example.fullstackmall.service.inventory.entity.SkuEntity;
import com.example.fullstackmall.service.inventory.service.SkuDbService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = FullstackMallApplication.class)
class SkuConcurrencyTest {

    @Resource
    private SkuDbService skuDbService;

    @Test
    void shouldAllowOnlyOneUpdateForTheSameExpectedVersion() throws Exception {
        SkuEntity sku = createCommittedSku();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<Integer> first = executor.submit(() -> adjustAfterSignal(sku.getId(), ready, start));
            Future<Integer> second = executor.submit(() -> adjustAfterSignal(sku.getId(), ready, start));
            ready.await();
            start.countDown();

            int updatedRows = first.get() + second.get();
            SkuEntity current = skuDbService.getById(sku.getId());

            assertThat(updatedRows).isEqualTo(1);
            assertThat(current.getAvailableStock()).isZero();
            assertThat(current.getVersion()).isEqualTo(1);
            assertThat(current.getAvailableStock()).isGreaterThanOrEqualTo(0);
        } finally {
            executor.shutdownNow();
            skuDbService.removeById(sku.getId());
        }
    }

    private int adjustAfterSignal(Long skuId, CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        start.await();
        return skuDbService.adjustAvailableStockByVersion(skuId, -1, 0);
    }

    private SkuEntity createCommittedSku() {
        LocalDateTime now = LocalDateTime.now();
        SkuEntity sku = new SkuEntity();
        sku.setProductId(1L);
        sku.setSkuCode("CONCURRENT-" + UUID.randomUUID());
        sku.setSpecText("并发测试规格");
        sku.setSalePrice(new BigDecimal("1.00"));
        sku.setAvailableStock(1);
        sku.setLockedStock(0);
        sku.setVersion(0);
        sku.setCreatedAt(now);
        sku.setUpdatedAt(now);
        skuDbService.save(sku);
        return sku;
    }
}
