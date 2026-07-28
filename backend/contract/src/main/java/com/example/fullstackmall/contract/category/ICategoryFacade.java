package com.example.fullstackmall.contract.category;

import java.util.List;

/**
 * 分类相关的对外能力契约。
 */
public interface ICategoryFacade {

    /**
     * 校验并规范化分类草稿，但不保存到数据库。
     *
     * @param request 分类草稿
     * @return 规范化结果
     */
    CategoryDraftResponse validateDraft(CategoryDraftRequest request);

    /**
     * 按后台展示顺序列出全部分类，包括停用分类。
     *
     * @return 分类摘要列表
     */
    List<CategorySummaryResponse> listAdminCategories();
}
