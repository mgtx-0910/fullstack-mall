package com.example.fullstackmall.service.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.fullstackmall.contract.user.UserQueryRequest;
import com.example.fullstackmall.service.user.entity.UserEntity;
import com.example.fullstackmall.service.user.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * 用户数据库访问 Service。
 *
 * <p>这种继承 ServiceImpl 的写法与企业项目中常见的 dbbase Service 思路一致。</p>
 */
@Service
public class UserDbService extends ServiceImpl<UserMapper, UserEntity> {


    public Optional<UserEntity> findByUsername(String username) {
        return Optional.ofNullable(getOne(
                Wrappers.lambdaQuery(UserEntity.class)
                        .eq(UserEntity::getUsername, username),
                false
        ));
    }

    public Optional<UserEntity> findUserById(Long userId) {
        return Optional.ofNullable(getById(userId));
    }

    public boolean usernameExists(String username) {
        return count(Wrappers.lambdaQuery(UserEntity.class)
                .eq(UserEntity::getUsername, username)) > 0;
    }

    /**
     * 使用 MyBatis-Plus 分页和 Lambda Wrapper 查询用户。
     *
     * @param request 查询条件
     * @return Entity 分页结果，只在 service 模块内部流转
     */
    public Page<UserEntity> queryPage(UserQueryRequest request) {
        LambdaQueryWrapper<UserEntity> wrapper = Wrappers.lambdaQuery(UserEntity.class);

        String keyword = normalizeKeyword(request.getKeyword());
        if (StringUtils.hasText(keyword)) {
            wrapper.and(condition -> condition
                    .like(UserEntity::getUsername, keyword)
                    .or()
                    .like(UserEntity::getNickname, keyword));
        }

        if (request.getStatus() != null) {
            wrapper.eq(UserEntity::getStatus, request.getStatus());
        }

        wrapper.orderByDesc(UserEntity::getCreatedAt)
                .orderByDesc(UserEntity::getId);

        Page<UserEntity> page = new Page<>(request.getPageNum(), request.getPageSize());
        return page(page, wrapper);
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null ? null : keyword.trim();
    }
}
