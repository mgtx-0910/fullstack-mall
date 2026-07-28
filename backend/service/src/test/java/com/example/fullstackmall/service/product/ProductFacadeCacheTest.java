package com.example.fullstackmall.service.product;

import com.example.fullstackmall.contract.common.ApiCode;
import com.example.fullstackmall.contract.product.ProductCreateRequest;
import com.example.fullstackmall.contract.product.ProductDetailResponse;
import com.example.fullstackmall.contract.product.ProductStatus;
import com.example.fullstackmall.contract.product.ProductStatusChangeRequest;
import com.example.fullstackmall.service.category.entity.CategoryEntity;
import com.example.fullstackmall.service.category.service.CategoryDbService;
import com.example.fullstackmall.service.common.exception.BusinessException;
import com.example.fullstackmall.service.inventory.service.SkuDbService;
import com.example.fullstackmall.service.product.cache.ProductDetailCacheLookup;
import com.example.fullstackmall.service.product.cache.ProductDetailCacheService;
import com.example.fullstackmall.service.product.entity.ProductEntity;
import com.example.fullstackmall.service.product.service.ProductDbService;
import com.example.fullstackmall.service.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * ProductFacade 缓存编排测试：验证 HIT、MISS、空值和写后失效，不重复测试 Redis 命令细节。
 */
class ProductFacadeCacheTest {

    private ProductDbService productDbService;
    private CategoryDbService categoryDbService;
    private CurrentUserService currentUserService;
    private SkuDbService skuDbService;
    private ProductDetailCacheService cacheService;
    private ProductFacade productFacade;

    @BeforeEach
    void setUp() {
        productDbService = mock(ProductDbService.class);
        categoryDbService = mock(CategoryDbService.class);
        currentUserService = mock(CurrentUserService.class);
        skuDbService = mock(SkuDbService.class);
        cacheService = mock(ProductDetailCacheService.class);

        productFacade = new ProductFacade();
        ReflectionTestUtils.setField(productFacade, "productDbService", productDbService);
        ReflectionTestUtils.setField(productFacade, "categoryDbService", categoryDbService);
        ReflectionTestUtils.setField(productFacade, "currentUserService", currentUserService);
        ReflectionTestUtils.setField(productFacade, "skuDbService", skuDbService);
        ReflectionTestUtils.setField(productFacade, "productDetailCacheService", cacheService);
    }

    @Test
    void shouldReturnCacheHitWithoutQueryingDatabase() {
        ProductDetailResponse cached = sampleResponse();
        when(cacheService.lookup(1L)).thenReturn(ProductDetailCacheLookup.hit(cached));

        ProductDetailResponse response = productFacade.getPublishedProduct(1L);

        assertThat(response).isSameAs(cached);
        verifyNoInteractions(productDbService, categoryDbService);
    }

    @Test
    void shouldRestoreNotFoundFromNullCacheWithoutQueryingDatabase() {
        when(cacheService.lookup(9L))
                .thenReturn(ProductDetailCacheLookup.nullHit(ApiCode.PRODUCT_NOT_FOUND));

        assertThatThrownBy(() -> productFacade.getPublishedProduct(9L))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> {
                    BusinessException businessException = (BusinessException) exception;
                    assertThat(businessException.getCode()).isEqualTo(ApiCode.PRODUCT_NOT_FOUND);
                    assertThat(businessException.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });
        verifyNoInteractions(productDbService, categoryDbService);
    }

    @Test
    void shouldQueryDatabaseAndFillCacheOnMiss() {
        ProductEntity product = onSaleProduct();
        CategoryEntity category = enabledCategory();
        when(cacheService.lookup(1L)).thenReturn(ProductDetailCacheLookup.miss());
        when(productDbService.requirePublishedById(1L)).thenReturn(product);
        when(categoryDbService.requireEnabledCategory(1L)).thenReturn(category);

        ProductDetailResponse response = productFacade.getPublishedProduct(1L);

        assertThat(response.getTitle()).isEqualTo("二手手机");
        assertThat(response.getCategoryName()).isEqualTo("手机数码");
        verify(cacheService).put(1L, response);
    }

    @Test
    void shouldFillShortNullCacheWhenDatabaseReturnsStable404() {
        BusinessException notFound = new BusinessException(
                ApiCode.PRODUCT_NOT_FOUND,
                ApiCode.PRODUCT_NOT_FOUND.defaultMessage(),
                HttpStatus.NOT_FOUND
        );
        when(cacheService.lookup(9L)).thenReturn(ProductDetailCacheLookup.miss());
        when(productDbService.requirePublishedById(9L)).thenThrow(notFound);

        assertThatThrownBy(() -> productFacade.getPublishedProduct(9L)).isSameAs(notFound);
        verify(cacheService).putNull(9L, ApiCode.PRODUCT_NOT_FOUND);
        verify(categoryDbService, never()).requireEnabledCategory(any());
    }

    @Test
    void shouldContinueQueryingDatabaseWhenRedisIsDegraded() {
        ProductEntity product = onSaleProduct();
        when(cacheService.lookup(1L)).thenReturn(ProductDetailCacheLookup.degraded());
        when(productDbService.requirePublishedById(1L)).thenReturn(product);
        when(categoryDbService.requireEnabledCategory(1L)).thenReturn(enabledCategory());

        ProductDetailResponse response = productFacade.getPublishedProduct(1L);

        assertThat(response.getId()).isEqualTo(1L);
        verify(cacheService).put(1L, response);
    }

    @Test
    void shouldEvictGuessedNullCacheAfterCreatingProduct() {
        ProductCreateRequest request = new ProductCreateRequest();
        request.setCategoryId(1L);
        request.setTitle(" 新商品 ");
        request.setDescription(" 描述 ");
        when(categoryDbService.requireEnabledCategory(1L)).thenReturn(enabledCategory());
        when(currentUserService.requireCurrentUserId()).thenReturn(5L);
        when(productDbService.save(any(ProductEntity.class))).thenAnswer(invocation -> {
            ProductEntity product = invocation.getArgument(0);
            product.setId(20L);
            return true;
        });

        ProductDetailResponse response = productFacade.createProduct(request);

        assertThat(response.getId()).isEqualTo(20L);
        InOrder inOrder = inOrder(productDbService, cacheService);
        inOrder.verify(productDbService).save(any(ProductEntity.class));
        inOrder.verify(cacheService).evict(20L);
    }

    @Test
    void shouldEvictDetailOnlyAfterStatusUpdateSucceeds() {
        ProductEntity product = onSaleProduct();
        product.setStatusCode(ProductStatus.DRAFT.name());
        ProductStatusChangeRequest request = new ProductStatusChangeRequest();
        request.setTargetStatus(ProductStatus.ON_SALE);
        when(productDbService.requireById(1L)).thenReturn(product);
        when(categoryDbService.requireEnabledCategory(1L)).thenReturn(enabledCategory());
        when(productDbService.updateById(product)).thenReturn(true);

        productFacade.changeStatus(1L, request);

        InOrder inOrder = inOrder(productDbService, cacheService);
        inOrder.verify(productDbService).updateById(product);
        inOrder.verify(cacheService).evict(1L);
    }

    private ProductEntity onSaleProduct() {
        LocalDateTime time = LocalDateTime.of(2026, 7, 12, 12, 0);
        ProductEntity product = new ProductEntity();
        product.setId(1L);
        product.setCategoryId(1L);
        product.setTitle("二手手机");
        product.setDescription("九成新");
        product.setStatusCode(ProductStatus.ON_SALE.name());
        product.setCreatedBy(5L);
        product.setCreatedAt(time);
        product.setUpdatedAt(time);
        return product;
    }

    private CategoryEntity enabledCategory() {
        CategoryEntity category = new CategoryEntity();
        category.setId(1L);
        category.setName("手机数码");
        category.setStatus(1);
        return category;
    }

    private ProductDetailResponse sampleResponse() {
        ProductEntity product = onSaleProduct();
        return new ProductDetailResponse(
                product.getId(),
                product.getCategoryId(),
                "手机数码",
                product.getTitle(),
                product.getSubtitle(),
                product.getDescription(),
                ProductStatus.ON_SALE,
                product.getCreatedBy(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
