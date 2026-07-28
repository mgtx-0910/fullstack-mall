package com.example.fullstackmall.service.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * mall_user 表对应的数据库实体。
 *
 * <p>passwordHash 可以在 service 内参与密码校验，但绝不能进入 API Response。</p>
 */
@TableName("mall_user")
@Data
public class UserEntity {

    /** 用户主键，由 MySQL 自增生成。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 登录用户名，数据库中要求唯一，用于定位用户账号。 */
    @TableField("username")
    private String username;

    /** 用户展示昵称，不参与身份认证。 */
    @TableField("nickname")
    private String nickname;

    /** BCrypt 处理后的密码哈希，绝不能保存或返回明文密码。 */
    @TableField("password_hash")
    private String passwordHash;

    /** 用户角色数据库编码，登录后转换为 UserRole 并用于接口授权。 */
    @TableField("role_code")
    private String roleCode;

    /** 账号状态：1 表示可用，0 表示禁用；禁用账号不能登录。 */
    @TableField("status")
    private Integer status;

    /** 用户创建时间，由服务端写入。 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 用户资料或状态最后更新时间。 */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
