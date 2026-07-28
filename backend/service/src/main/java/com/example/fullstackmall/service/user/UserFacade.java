package com.example.fullstackmall.service.user;

import com.example.fullstackmall.contract.common.PageResponse;
import com.example.fullstackmall.contract.user.IUserFacade;
import com.example.fullstackmall.contract.user.UserQueryRequest;
import com.example.fullstackmall.contract.user.UserSummaryResponse;
import com.example.fullstackmall.service.user.entity.UserEntity;
import com.example.fullstackmall.service.user.service.UserDbService;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import jakarta.annotation.Resource;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户 Facade 的 service 模块实现。
 */
@Service
public class UserFacade implements IUserFacade {

    @Resource
    private UserDbService userDbService;

    @Override
    public PageResponse<UserSummaryResponse> queryUsers(UserQueryRequest request) {
        Page<UserEntity> entityPage = userDbService.queryPage(request);
        List<UserSummaryResponse> records = entityPage.getRecords()
                .stream()
                .map(this::toResponse)
                .toList();

        return new PageResponse<>(
                entityPage.getCurrent(),
                entityPage.getSize(),
                entityPage.getTotal(),
                entityPage.getPages(),
                records
        );
    }

    private UserSummaryResponse toResponse(UserEntity entity) {
        return new UserSummaryResponse(
                entity.getId(),
                entity.getUsername(),
                entity.getNickname(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }
}
