package com.example.fullstackmall.contract.user;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import lombok.Data;

/**
 * 用户分页查询条件。
 *
 * <p>GET 请求的查询参数会由 Spring MVC 绑定到这个对象。</p>
 */
@Data
public class UserQueryRequest {

    /** 请求页码，默认第 1 页。 */
    @Min(value = 1, message = "页码不能小于 1")
    private Integer pageNum = 1;

    /** 每页数量，默认 10，最多 100。 */
    @Min(value = 1, message = "每页数量不能小于 1")
    @Max(value = 100, message = "每页数量不能大于 100")
    private Integer pageSize = 10;

    /** 可选关键字，用于匹配用户名或昵称。 */
    @Size(max = 50, message = "查询关键字不能超过 50 个字符")
    private String keyword;

    /** 可选账户状态筛选：1 表示启用，0 表示停用。 */
    @Min(value = 0, message = "用户状态只能是 0 或 1")
    @Max(value = 1, message = "用户状态只能是 0 或 1")
    private Integer status;
}
