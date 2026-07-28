package com.example.fullstackmall.contract.auth;

/**
 * 当前 MVP 支持的用户角色。
 */
public enum UserRole {
    /** 普通用户，可以访问自己的购物车、订单和支付信息。 */
    USER,
    /** 管理员，可以访问用户管理和商品后台接口。 */
    ADMIN;

    /** 将数据库中的角色码转换为 contract 枚举；未知值立即失败，避免静默授予错误权限。 */
    public static UserRole fromCode(String code) {
        for (UserRole role : values()) {
            if (role.name().equalsIgnoreCase(code)) {
                return role;
            }
        }
        throw new IllegalArgumentException("未知用户角色: " + code);
    }
}
