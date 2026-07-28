package com.example.fullstackmall.contract.product;

import com.example.fullstackmall.contract.common.PageResponse;

/**
 * 商品模块对外提供的用例契约。
 */
public interface IProductFacade {
    /**
     * 由管理员创建商品草稿；状态和创建人由服务端生成。
     *
     * @param request 商品分类、标题和描述
     * @return 商品详情
     */
    ProductDetailResponse createProduct(ProductCreateRequest request);
    /**
     * 按后端状态机变更商品状态；上架前还会校验分类和可售 SKU。
     *
     * @param productId 商品主键
     * @param request 目标状态
     * @return 变更后的商品详情
     */
    ProductDetailResponse changeStatus(Long productId, ProductStatusChangeRequest request);
    /**
     * 按管理端条件分页查询商品。
     *
     * @param request 分页和筛选条件
     * @return 商品摘要分页结果
     */
    PageResponse<ProductSummaryResponse> queryAdminProducts(ProductQueryRequest request);

    /**
     * 查询管理员商品详情；允许查看草稿、上架和下架商品。
     *
     * @param productId 商品主键
     * @return 商品详情
     */
    ProductDetailResponse getAdminProduct(Long productId);

    /**
     * 管理员编辑商品基础信息；修改成功后需要让公开商品详情缓存失效。
     *
     * @param productId 商品主键
     * @param request 新分类、标题和描述
     * @return 修改后的商品详情
     */
    ProductDetailResponse updateProduct(Long productId, ProductUpdateRequest request);
    /**
     * 分页查询公开商品；服务端强制只返回 ON_SALE。
     *
     * @param request 分页、关键字和分类条件
     * @return 公开商品分页结果
     */
    PageResponse<ProductSummaryResponse> queryPublishedProducts(ProductQueryRequest request);
    /**
     * 查询公开商品详情；商品未上架或分类停用时按不存在处理。
     *
     * @param productId 商品主键
     * @return 公开商品详情
     */
    ProductDetailResponse getPublishedProduct(Long productId);

    /**
     * 管理员修改商品副标题；修改成功后需要让公开商品详情缓存失效。
     *
     * @param productId 商品主键
     * @param request 新副标题
     * @return 修改后的商品详情
     */
    ProductDetailResponse updateSubtitle(Long productId, ProductSubtitleUpdateRequest request);
}
