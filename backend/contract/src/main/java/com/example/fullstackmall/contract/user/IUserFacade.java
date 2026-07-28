package com.example.fullstackmall.contract.user;

import com.example.fullstackmall.contract.common.PageResponse;

/**
 * 用户模块对外提供的能力契约。
 */
public interface IUserFacade {

    /**
     * 按条件分页查询用户。
     *
     * @param request 查询条件
     * @return 用户摘要分页结果
     */
    PageResponse<UserSummaryResponse> queryUsers(UserQueryRequest request);
}
