package com.example.fullstackmall.service.user;

import com.example.fullstackmall.service.FullstackMallApplication;
import com.example.fullstackmall.service.user.entity.UserEntity;
import com.example.fullstackmall.service.user.mapper.UserMapper;

import jakarta.annotation.Resource;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest(classes = FullstackMallApplication.class)
class UserMapperTest {

    @Resource
    private UserMapper userMapper;

    @Test
    void shouldUseBaseMapperToReadUserTable() {
        UserEntity user = userMapper.selectById(1L);
        List<UserEntity> users = userMapper.selectList(null);

        assertThat(user.getUsername()).isEqualTo("xiaoming");
        assertThat(user.getNickname()).isEqualTo("小明");
        assertThat(user.getPasswordHash()).startsWith("$2y$");
        assertThat(user.getRoleCode()).isEqualTo("USER");
        assertThat(users).hasSize(5);
    }
}
