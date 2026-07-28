package com.example.fullstackmall.contract.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户列表向前端公开的摘要数据。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryResponse {

    /** 用户主键，由数据库生成。 */
    private Long id;
    /** 登录用户名，可供管理员识别账户。 */
    private String username;
    /** 用户展示昵称。 */
    private String nickname;
    /** 账户状态：1 表示启用，0 表示停用。 */
    private Integer status;
    /** 用户注册时间。 */
    private LocalDateTime createdAt;
}
