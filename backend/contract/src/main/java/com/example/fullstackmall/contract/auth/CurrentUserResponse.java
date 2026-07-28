package com.example.fullstackmall.contract.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 可以安全返回给前端的当前用户信息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrentUserResponse {

    /** 用户主键，由数据库生成，也是后端判断数据归属时使用的可信身份标识。 */
    private Long id;
    /** 登录用户名，来源于数据库，不是前端本次请求临时传入的展示值。 */
    private String username;
    /** 用户展示昵称，可以直接用于页面展示。 */
    private String nickname;
    /** 当前用户角色，由服务端根据数据库角色码转换，前端不能通过请求自行提升权限。 */
    private UserRole role;
}
