package com.example.fullstackmall.contract.category;

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
}
